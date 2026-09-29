package com.somepro.domain.crossmatch.model;

import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.unit.model.ComponentType;

/**
 * 配血记录分页查询条件（领域值对象）。
 *
 * 全部条件可空、任意组合：一个都不填就是整份记录分页。
 * - applyDept：申请科室模糊匹配
 * - patientNo：患者住院号精确对号
 * - result：配血结果精确（PENDING / COMPATIBLE / INCOMPATIBLE）
 * 另支持 requestNo 精确、血袋编号 unitNo 精确，方便和医院、血袋两边的单子对上。
 */
public record CrossmatchQuery(int pageNum,
                              int pageSize,
                              String requestNo,
                              String unitNo,
                              String applyDept,
                              String patientNo,
                              CrossmatchResult result,
                              BloodGroup patientGroup,
                              ComponentType component,
                              CrossmatchMethod method) {
}
