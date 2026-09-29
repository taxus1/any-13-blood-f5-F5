package com.somepro.domain.crossmatch.model;

import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;

import java.time.LocalDateTime;

/**
 * 配血记录列表行（领域值对象）。
 *
 * 翻记录每行必带申请单号 requestNo，方便和医院那边的纸质/电子单对号；
 * 同时冗余带出所选血袋编号 unitNo（仓储 join t_blood_unit 查出，避免逐行回查），
 * 患者血型与血袋血型各自照实列着，相合与否看 result。
 */
public record CrossmatchListItem(Long id,
                                 String requestNo,
                                 Long unitId,
                                 String unitNo,
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
