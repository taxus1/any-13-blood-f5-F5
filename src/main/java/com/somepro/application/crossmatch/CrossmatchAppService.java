package com.somepro.application.crossmatch;

import com.somepro.application.crossmatch.cmd.ApplyCrossmatchCmd;
import com.somepro.application.crossmatch.port.OperatorPort;
import com.somepro.common.exception.BizException;
import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchQuery;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
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
 * 交叉配血应用服务：编排递交申请、判配、查看、分页查询用例。
 *
 * 申请单号取号、撞号重试与血袋/献血者一致（uk_request_no 唯一索引兜底）。
 */
@Service
public class CrossmatchAppService {

    private static final int MAX_ATTEMPTS = 3;

    private final CrossmatchRepository crossmatchRepository;
    private final BloodUnitRepository bloodUnitRepository;
    private final OperatorPort operatorPort;

    public CrossmatchAppService(CrossmatchRepository crossmatchRepository,
                                BloodUnitRepository bloodUnitRepository,
                                OperatorPort operatorPort) {
        this.crossmatchRepository = crossmatchRepository;
        this.bloodUnitRepository = bloodUnitRepository;
        this.operatorPort = operatorPort;
    }

    /** 递交配血申请：校验血袋可用、无在途申请，取号落单，结果固定 PENDING。 */
    public Mono<Crossmatch> apply(ApplyCrossmatchCmd cmd) {
        return bloodUnitRepository.findById(cmd.unitId())
                .switchIfEmpty(Mono.error(new BizException("所选血袋不存在或已删除，无法配血：unitId=" + cmd.unitId())))
                .flatMap(unit -> {
                    assertUnitUsable(unit);
                    return crossmatchRepository.existsPendingByUnit(cmd.unitId());
                })
                .flatMap(pending -> {
                    if (Boolean.TRUE.equals(pending)) {
                        return Mono.<Crossmatch>error(new BizException(
                                "该血袋已有一条尚未出结果的配血申请，请等本条有结果后再提"));
                    }
                    return assignNoAndSave(cmd, 1);
                });
    }

    private Mono<Crossmatch> assignNoAndSave(ApplyCrossmatchCmd cmd, int attempt) {
        return crossmatchRepository.nextRequestNo()
                .flatMap(no -> {
                    // 每次尝试重建聚合：单号一经分配不可更改，撞号重试时旧对象不能复用
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

    /**
     * 判配：读血袋当下真实血型，按红细胞主侧规矩判相合与否，落 COMPATIBLE / INCOMPATIBLE。
     * 判配前再校一次血袋可用性，防止申请后血袋被发出/报废/检测判废。
     */
    public Mono<Crossmatch> judge(Long id) {
        return crossmatchRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("配血申请不存在或已删除：id=" + id)))
                .flatMap(cm -> bloodUnitRepository.findById(cm.getUnitId())
                        .switchIfEmpty(Mono.error(new BizException("所选血袋不存在或已删除，无法判配")))
                        .flatMap(unit -> {
                            assertUnitUsable(unit);
                            return operatorPort.withOperator(operator -> {
                                cm.judge(unit.getBloodGroup(), unit.getRh(), LocalDateTime.now(), operator);
                                return crossmatchRepository.save(cm);
                            });
                        }));
    }

    public Mono<Crossmatch> detail(Long id) {
        return crossmatchRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("配血申请不存在或已删除：id=" + id)));
    }

    public Mono<PageResult<CrossmatchListItem>> page(CrossmatchQuery query) {
        return crossmatchRepository.page(query);
    }

    /**
     * 配血用血袋资格：必须在库（IN_STOCK）且未过期。
     * 检测没过（REJECTED）、已发出（ISSUED）、已报废（DISCARDED）、待检（COLLECTED）、
     * 仅检测合格但未入在库（QUALIFIED）、或已删除（仓储查不到，提前拦）都不能拿来配。
     */
    private void assertUnitUsable(BloodUnit unit) {
        if (unit.getStatus() != UnitStatus.IN_STOCK) {
            throw new BizException("血袋当前状态为 " + unit.getStatus()
                    + "，只有在库（IN_STOCK）的血袋才能配血");
        }
        if (unit.getExpireAt() != null && !unit.getExpireAt().isAfter(LocalDateTime.now())) {
            throw new BizException("血袋已过期（失效时刻 " + unit.getExpireAt() + "），不能拿来配血");
        }
    }
}
