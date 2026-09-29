package com.somepro.application.crossmatch;

import com.somepro.application.crossmatch.cmd.CreateCrossmatchCmd;
import com.somepro.common.exception.BizException;
import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchQuery;
import com.somepro.domain.crossmatch.repository.CrossmatchRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.model.BloodUnit;
import com.somepro.domain.unit.model.UnitStatus;
import com.somepro.domain.unit.repository.BloodUnitRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 配血应用服务：编排「递申请 → 判读 → 翻记录」用例。
 *
 * 递申请时先把选袋关：
 * 1. 血袋必须存在（被软删视为不存在）；
 * 2. 血袋必须 IN_STOCK 在库且未过失效时刻 —— 检测没过（REJECTED/QUALIFIED 待入库等）、
 *    已发放 ISSUED、已报废 DISCARDED 都不能拿来配；
 * 3. 同一袋血已挂着一条 PENDING 未走完的申请时，先别再递，等这条有结果再说。
 *
 * 判读时读血袋档案上的 ABO/Rh（不照申请单），与申请单上的患者血型一起交领域规则判相合。
 * 申请单号按年取号、撞号重试，策略与血袋/献血者一致（uk_request_no 唯一索引兜底）。
 */
@Service
public class CrossmatchAppService {

    private static final int MAX_ATTEMPTS = 3;

    private final CrossmatchRepository crossmatchRepository;
    private final BloodUnitRepository bloodUnitRepository;

    public CrossmatchAppService(CrossmatchRepository crossmatchRepository,
                                BloodUnitRepository bloodUnitRepository) {
        this.crossmatchRepository = crossmatchRepository;
        this.bloodUnitRepository = bloodUnitRepository;
    }

    /** 提交一条配血申请：校验选袋、占袋、取号落库，结果从 PENDING 起。 */
    public Mono<CrossmatchListItem> create(CreateCrossmatchCmd cmd) {
        return bloodUnitRepository.findById(cmd.unitId())
                .switchIfEmpty(Mono.error(new BizException("所选血袋不存在或已删除，无法配血：unitId=" + cmd.unitId())))
                .flatMap(this::assertUsableForCrossmatch)
                // 同一袋血有未走完的申请时先拦住
                .flatMap(unit -> crossmatchRepository.existsPendingByUnit(unit.getId()))
                .flatMap(pending -> {
                    if (Boolean.TRUE.equals(pending)) {
                        return Mono.<Crossmatch>error(new BizException(
                                "该血袋已有一条尚未出结果的配血申请，请等该申请判读后再申请"));
                    }
                    return assignNoAndSave(cmd);
                })
                .flatMap(saved -> crossmatchRepository.findListItemById(saved.getId()));
    }

    /** 判读：只有 PENDING 申请可判，按血袋档案血型判相合/不合，只能判一次。 */
    public Mono<CrossmatchListItem> judge(Long id, String operator) {
        return crossmatchRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("配血申请不存在或已删除：id=" + id)))
                .flatMap(cm -> bloodUnitRepository.findById(cm.getUnitId())
                        // 判读只看血袋档案上的血型；血袋若被删，档案血型已随申请冗余无从回溯，需明确报错
                        .switchIfEmpty(Mono.error(new BizException("配血申请所选血袋不存在或已删除，无法判读：unitId=" + cm.getUnitId())))
                        .flatMap(unit -> {
                            cm.judge(unit.getBloodGroup(), unit.getRh(), LocalDateTime.now(), operator);
                            return crossmatchRepository.save(cm);
                        }))
                .flatMap(saved -> crossmatchRepository.findListItemById(saved.getId()));
    }

    public Mono<CrossmatchListItem> detail(Long id) {
        return crossmatchRepository.findListItemById(id)
                .switchIfEmpty(Mono.error(new BizException("配血申请不存在或已删除：id=" + id)));
    }

    public Mono<PageResult<CrossmatchListItem>> page(CrossmatchQuery query) {
        return crossmatchRepository.page(query);
    }

    /** 选袋关：必须在库且未过期。状态异常逐类给出原因，过期单列。 */
    private Mono<BloodUnit> assertUsableForCrossmatch(BloodUnit unit) {
        if (unit.getStatus() != UnitStatus.IN_STOCK) {
            return Mono.error(new BizException("血袋 " + unit.getUnitNo()
                    + " 当前状态为 " + unit.getStatus() + "，只有在库 IN_STOCK 且未过期的血袋才能配血"
                    + "（检测不合格、已发放、已报废均不可）"));
        }
        if (unit.getExpireAt() != null && !unit.getExpireAt().isAfter(LocalDateTime.now())) {
            return Mono.error(new BizException("血袋 " + unit.getUnitNo() + " 已过失效时刻（"
                    + unit.getExpireAt() + "），不能拿来配血"));
        }
        return Mono.just(unit);
    }

    private Mono<Crossmatch> assignNoAndSave(CreateCrossmatchCmd cmd) {
        return assignNoAndSave(cmd, 1);
    }

    private Mono<Crossmatch> assignNoAndSave(CreateCrossmatchCmd cmd, int attempt) {
        return crossmatchRepository.nextRequestNo()
                .flatMap(no -> {
                    // 每次尝试重建聚合，理由同血袋/献血者取号重试
                    Crossmatch cm = Crossmatch.apply(cmd.unitId(), cmd.applyDept(), cmd.patientNo(),
                            cmd.patientName(), cmd.patientGroup(), cmd.patientRh(),
                            cmd.component(), cmd.method(), cmd.applyReason(), LocalDateTime.now());
                    cm.assignRequestNo(no);
                    return crossmatchRepository.save(cm);
                })
                .onErrorResume(DuplicateKeyException.class, e -> {
                    if (attempt >= MAX_ATTEMPTS) {
                        return Mono.error(new BizException("申请单号冲突，多次取号仍失败，请重试"));
                    }
                    return assignNoAndSave(cmd, attempt + 1);
                });
    }
}
