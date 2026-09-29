package com.somepro.interfaces.rest.crossmatch.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配血申请/记录对外 VO（不可变 record）。
 *
 * 列表行额外带血袋编号 unitNo（详情里 unitNo 可能为 null，例如血袋已被删）。
 * 患者血型与结果都如实暴露，不含 delFlag 及审计字段。
 */
public record CrossmatchVO(Long id,
                           String requestNo,
                           Long unitId,
                           String unitNo,
                           String applyDept,
                           String patientNo,
                           String patientName,
                           String patientGroup,
                           String patientRh,
                           String component,
                           String method,
                           String result,
                           String applyReason,
                           LocalDateTime requestedAt,
                           LocalDateTime matchedAt,
                           String matchedBy,
                           LocalDateTime createTime)
        implements Serializable {
}
