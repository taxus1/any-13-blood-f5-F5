package com.somepro.infrastructure.persistence.crossmatch.po;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 配血分页列表的查询行（基础设施层）：t_crossmatch LEFT JOIN t_blood_unit 的结果形状。
 *
 * 不是任何一张表的映射（没有 @TableName），只服务于列表/详情查询；列靠全局驼峰映射回填，
 * 血袋血型因两表列名同名，用 SQL 别名 unit_group / unit_rh 区分（驼峰映射回 unitGroup / unitRh）。
 * 用 LEFT JOIN：血袋即使被软删，配血底账也不能跟着消失，此时 unitNo / unitGroup / unitRh 为 null。
 */
@Getter
@Setter
public class CrossmatchListRowPO {

    private Long id;

    private String requestNo;

    private Long unitId;

    private String unitNo;

    private String unitGroup;

    private String unitRh;

    private String applyDept;

    private String patientNo;

    private String patientName;

    private String patientGroup;

    private String patientRh;

    private String component;

    private String method;

    private String result;

    private String applyReason;

    private LocalDateTime requestedAt;

    private LocalDateTime matchedAt;

    private String matchedBy;
}
