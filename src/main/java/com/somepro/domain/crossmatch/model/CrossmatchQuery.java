package com.somepro.domain.crossmatch.model;

/**
 * 配血记录分页查询条件（领域值对象）。
 *
 * 翻查配血记录用的三个条件：申请科室、患者住院号、结果；外加申请单号便于和医院单据对号。
 * 所有条件均可为空，可任意组合，一个不填就是整份记录分页。
 * - applyDept：按申请科室精确匹配
 * - patientNo：按患者住院号精确匹配
 * - result：按结果精确匹配（PENDING / COMPATIBLE / INCOMPATIBLE）
 * - requestNo：按申请单号精确匹配
 */
public record CrossmatchQuery(int pageNum,
                              int pageSize,
                              String requestNo,
                              String applyDept,
                              String patientNo,
                              CrossmatchResult result) {
}
