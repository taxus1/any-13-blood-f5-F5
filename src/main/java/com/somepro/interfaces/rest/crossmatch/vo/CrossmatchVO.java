package com.somepro.interfaces.rest.crossmatch.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 配血记录对外 VO（不可变 record）。
 *
 * 行里带申请单号 requestNo，方便和医院那边的纸质单对上；
 * 同时给出患者血型（照申请单）与血袋血型 unitGroup/unitRh（照血袋档案）以及判读结果。
 * 不含 delFlag 及审计字段。
 */
public record CrossmatchVO(Long id,
                           String requestNo,
                           Long unitId,
                           String unitNo,
                           String unitGroup,
                           String unitRh,
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
