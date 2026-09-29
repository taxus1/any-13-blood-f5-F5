package com.somepro.domain.crossmatch.model;

import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;

import java.time.LocalDateTime;

/**
 * 配血记录列表行（领域值对象）。
 *
 * 行里必须带申请单号，方便和医院那边的纸质单对上；同时要能一眼看到「患者怎么填、血袋什么样」
 * 以及判出来的结果，所以除申请单自身字段外，冗余带出所选血袋编号与其 ABO/Rh（仓储用一条
 * join 查出来，避免逐行回查血袋）。血袋被软删时 unitNo / unitGroup / unitRh 可能为 null，
 * 但配血底账不能从名单里消失，故用 LEFT JOIN。
 */
public record CrossmatchListItem(Long id,
                                 String requestNo,
                                 Long unitId,
                                 String unitNo,
                                 BloodGroup unitGroup,
                                 RhFactor unitRh,
                                 String applyDept,
                                 String patientNo,
                                 String patientName,
                                 BloodGroup patientGroup,
                                 RhFactor patientRh,
                                 ComponentType component,
                                 CrossmatchMethod method,
                                 CrossmatchResult result,
                                 String applyReason,
                                 LocalDateTime requestedAt,
                                 LocalDateTime matchedAt,
                                 String matchedBy) {
}
