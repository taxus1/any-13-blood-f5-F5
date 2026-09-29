package com.somepro.infrastructure.persistence.crossmatch.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * t_crossmatch 表的持久化对象（PO，基础设施层）。
 *
 * 只描述「表长什么样」，不放业务规则（规则在领域对象 Crossmatch）。
 * patient_group / patient_rh / component / method / result 存枚举名称字符串。
 */
@Getter
@Setter
@TableName("t_crossmatch")
public class CrossmatchPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("request_no")
    private String requestNo;

    @TableField("unit_id")
    private Long unitId;

    @TableField("apply_dept")
    private String applyDept;

    @TableField("patient_no")
    private String patientNo;

    @TableField("patient_name")
    private String patientName;

    @TableField("patient_group")
    private String patientGroup;

    @TableField("patient_rh")
    private String patientRh;

    @TableField("component")
    private String component;

    @TableField("method")
    private String method;

    @TableField("result")
    private String result;

    /** 申请原因可空（表上允许）；ALWAYS 保证清空也能落库。 */
    @TableField(value = "apply_reason", updateStrategy = FieldStrategy.ALWAYS)
    private String applyReason;

    @TableField("requested_at")
    private LocalDateTime requestedAt;

    /** 判配前为空，字段可空；ALWAYS 不影响。 */
    @TableField(value = "matched_at", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime matchedAt;

    @TableField(value = "matched_by", updateStrategy = FieldStrategy.ALWAYS)
    private String matchedBy;
}
