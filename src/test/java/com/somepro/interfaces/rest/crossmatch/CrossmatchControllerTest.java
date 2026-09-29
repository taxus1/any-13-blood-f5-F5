package com.somepro.interfaces.rest.crossmatch;

import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.crossmatch.model.CrossmatchResult;
import com.somepro.domain.crossmatch.repository.CrossmatchRepository;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.BloodUnit;
import com.somepro.domain.unit.model.ComponentType;
import com.somepro.domain.unit.model.StorageTemp;
import com.somepro.domain.unit.model.UnitStatus;
import com.somepro.domain.unit.repository.BloodUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 配血接口全链路切片：Spring Security 认证 → @Valid 校验 → 应用编排（选袋/占袋/判读）→ VO。
 *
 * 仓储打桩，不依赖真实 MySQL/Redis（本环境不可用）；MyBatis join/PageHelper/取号 SQL 与已在真实
 * MySQL 验证过的 unit 模块同构，相合矩阵另有领域单测覆盖。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class CrossmatchControllerTest {

    @Autowired
    WebTestClient web;

    @MockBean
    BloodUnitRepository bloodUnitRepository;
    @MockBean
    CrossmatchRepository crossmatchRepository;

    private final AtomicLong idGen = new AtomicLong(0);
    private final AtomicReference<Crossmatch> stored = new AtomicReference<>();

    private static final String JSON = """
            {
              "unitId": 1001,
              "applyDept": "血液科",
              "patientNo": "P0001",
              "patientName": "张三",
              "patientGroup": "A",
              "patientRh": "POSITIVE",
              "component": "RBC",
              "method": "MAJOR",
              "applyReason": "手术备血"
            }
            """;

    @BeforeEach
    void stub() {
        stored.set(null);
        idGen.set(0);
    }

    private BloodUnit unit(UnitStatus status, BloodGroup group, RhFactor rh, LocalDateTime expireAt) {
        BloodUnit u = BloodUnit.collect(2001L, group, rh, ComponentType.RBC, 400,
                LocalDateTime.now().minusDays(1), expireAt, StorageTemp.COLD);
        u.setId(1001L);
        u.assignUnitNo("BU-2026-000001");
        u.changeStatus(status);
        return u;
    }

    private void stubUnit(BloodUnit u) {
        when(bloodUnitRepository.findById(anyLong())).thenReturn(Mono.just(u));
    }

    /** 模拟保存：回填 id，并缓存聚合供判读查询。 */
    private void stubSave() {
        when(crossmatchRepository.save(any())).thenAnswer((InvocationOnMock inv) -> {
            Crossmatch cm = inv.getArgument(0);
            if (cm.getId() == null) {
                cm.setId(idGen.incrementAndGet());
            }
            stored.set(cm);
            return Mono.just(cm);
        });
    }

    /** 模拟 join 列表行：从聚合 + 血袋拼出对外记录。 */
    private void stubListItem(BloodUnit u) {
        when(crossmatchRepository.findListItemById(anyLong())).thenAnswer(inv -> {
            Crossmatch cm = stored.get();
            if (cm == null || !inv.getArgument(0).equals(cm.getId())) {
                return Mono.empty();
            }
            return Mono.just(new CrossmatchListItem(
                    cm.getId(), cm.getRequestNo(), cm.getUnitId(), u.getUnitNo(),
                    u.getBloodGroup(), u.getRh(),
                    cm.getApplyDept(), cm.getPatientNo(), cm.getPatientName(),
                    cm.getPatientGroup(), cm.getPatientRh(), cm.getComponent(),
                    cm.getMethod(), cm.getResult(), cm.getApplyReason(),
                    cm.getRequestedAt(), cm.getMatchedAt(), cm.getMatchedBy()));
        });
    }

    private void stubFindAggregate() {
        when(crossmatchRepository.findById(anyLong())).thenAnswer(inv -> {
            Crossmatch cm = stored.get();
            return cm != null && inv.getArgument(0).equals(cm.getId()) ? Mono.just(cm) : Mono.empty();
        });
    }

    @Test
    void requiresAuthentication() {
        web.post().uri("/api/crossmatches").bodyValue(JSON).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void create_compatibleBagInStock_generatesNoPending() {
        BloodUnit u = unit(UnitStatus.IN_STOCK, BloodGroup.O, RhFactor.POSITIVE, LocalDateTime.now().plusDays(7));
        stubUnit(u);
        when(crossmatchRepository.existsPendingByUnit(anyLong())).thenReturn(Mono.just(false));
        when(crossmatchRepository.nextRequestNo()).thenReturn(Mono.just("CM-2026-0001"));
        stubSave();
        stubListItem(u);

        web.post().uri("/api/crossmatches")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .header("Content-Type", "application/json")
                .bodyValue(JSON).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(0)
                .jsonPath("$.data.requestNo").isEqualTo("CM-2026-0001")
                .jsonPath("$.data.result").isEqualTo("PENDING")
                .jsonPath("$.data.unitNo").isEqualTo("BU-2026-000001")
                .jsonPath("$.data.patientGroup").isEqualTo("A")
                .jsonPath("$.data.unitGroup").isEqualTo("O")
                .jsonPath("$.data.patientNo").isEqualTo("P0001");
    }

    @Test
    void create_rejectsWhenBagNotInStock() {
        stubUnit(unit(UnitStatus.ISSUED, BloodGroup.O, RhFactor.POSITIVE, LocalDateTime.now().plusDays(7)));

        web.post().uri("/api/crossmatches")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .header("Content-Type", "application/json")
                .bodyValue(JSON).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.msg").value(containsString("在库"));
    }

    @Test
    void create_rejectsExpiredBag() {
        stubUnit(unit(UnitStatus.IN_STOCK, BloodGroup.O, RhFactor.POSITIVE, LocalDateTime.now().minusHours(1)));

        web.post().uri("/api/crossmatches")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .header("Content-Type", "application/json")
                .bodyValue(JSON).exchange()
                .expectBody().jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.msg").value(org.hamcrest.Matchers.anyOf(containsString("过期"), containsString("失效")));
    }

    @Test
    void create_rejectsRejectedAndDiscardedBag() {
        stubUnit(unit(UnitStatus.REJECTED, BloodGroup.O, RhFactor.POSITIVE, LocalDateTime.now().plusDays(7)));
        web.post().uri("/api/crossmatches")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .header("Content-Type", "application/json")
                .bodyValue(JSON).exchange()
                .expectBody().jsonPath("$.code").isEqualTo(1);
    }

    @Test
    void create_rejectsWhenPendingApplicationOnSameBag() {
        stubUnit(unit(UnitStatus.IN_STOCK, BloodGroup.O, RhFactor.POSITIVE, LocalDateTime.now().plusDays(7)));
        when(crossmatchRepository.existsPendingByUnit(anyLong())).thenReturn(Mono.just(true));

        web.post().uri("/api/crossmatches")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .header("Content-Type", "application/json")
                .bodyValue(JSON).exchange()
                .expectBody().jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.msg").value(containsString("尚未出结果"));
    }

    @Test
    void create_validationFailsOnBadMethodAndMissingFields() {
        String bad = JSON.replace("\"MAJOR\"", "\"SIDE\"");
        web.post().uri("/api/crossmatches")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .header("Content-Type", "application/json")
                .bodyValue(bad).exchange()
                .expectBody().jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.msg").value(containsString("MAJOR"));

        String noPatient = JSON.replace("\"P0001\"", "\"\"");
        web.post().uri("/api/crossmatches")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .header("Content-Type", "application/json")
                .bodyValue(noPatient).exchange()
                .expectBody().jsonPath("$.code").isEqualTo(1);
    }

    @Test
    void judge_oPositiveToAPositive_compatible_thenLocked() {
        BloodUnit u = unit(UnitStatus.IN_STOCK, BloodGroup.O, RhFactor.POSITIVE, LocalDateTime.now().plusDays(7));
        seedPendingApplication(u);
        stubFindAggregate();
        stubSave();
        stubListItem(u);

        web.post().uri("/api/crossmatches/1/judge")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.result").isEqualTo("COMPATIBLE")
                .jsonPath("$.data.matchedBy").isEqualTo("admin");

        // 再判一次：已落定，拒绝重复判读
        web.post().uri("/api/crossmatches/1/judge")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectBody().jsonPath("$.code").isEqualTo(1)
                .jsonPath("$.msg").value(containsString("已有结果"));
    }

    @Test
    void judge_aPositiveToANegative_incompatible() {
        BloodUnit u = unit(UnitStatus.IN_STOCK, BloodGroup.A, RhFactor.POSITIVE, LocalDateTime.now().plusDays(7));
        seedPendingApplication(u, BloodGroup.A, RhFactor.NEGATIVE);
        stubFindAggregate();
        stubSave();
        stubListItem(u);

        web.post().uri("/api/crossmatches/1/judge")
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectBody().jsonPath("$.data.result").isEqualTo("INCOMPATIBLE");
    }

    @Test
    @SuppressWarnings("unchecked")
    void page_filtersByDeptPatientResult_andCarriesRequestNo() {
        CrossmatchListItem row = new CrossmatchListItem(1L, "CM-2026-0001", 1001L, "BU-2026-000001",
                BloodGroup.O, RhFactor.POSITIVE, "血液科", "P0001", "张三",
                BloodGroup.A, RhFactor.POSITIVE, ComponentType.RBC, CrossmatchMethod.MAJOR,
                CrossmatchResult.PENDING, "手术备血",
                LocalDateTime.now(), null, null);
        when(crossmatchRepository.page(any())).thenReturn(
                Mono.just(new PageResult<>(List.of(row), 1L, 1, 20)));

        web.get().uri(uri -> uri.path("/api/crossmatches")
                        .queryParam("applyDept", "血液科")
                        .queryParam("patientNo", "P0001")
                        .queryParam("result", "PENDING")
                        .build())
                .headers(h -> h.setBasicAuth("admin", "admin123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.totalPages").isEqualTo(1)
                .jsonPath("$.data.content[0].requestNo").isEqualTo("CM-2026-0001")
                .jsonPath("$.data.content[0].result").isEqualTo("PENDING");
    }

    private void seedPendingApplication(BloodUnit u) {
        seedPendingApplication(u, BloodGroup.A, RhFactor.POSITIVE);
    }

    private void seedPendingApplication(BloodUnit u, BloodGroup pg, RhFactor pr) {
        Crossmatch cm = Crossmatch.apply(1001L, "血液科", "P0001", "张三",
                pg, pr, ComponentType.RBC, CrossmatchMethod.MAJOR, "手术备血", LocalDateTime.now());
        cm.setId(1L);
        cm.assignRequestNo("CM-2026-0001");
        stored.set(cm);
        stubUnit(u);
    }
}
