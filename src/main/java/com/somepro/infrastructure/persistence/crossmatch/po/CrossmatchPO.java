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
 * method / result / component / patient_group / patient_rh 存枚举名称字符串。
 * matched_at / matched_by 判读后才有值，ALWAYS 保证可随更新写库（这里两列只增不改，配默认策略亦可）。
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

    @TableField("apply_reason")
    private String applyReason;

    @TableField("requested_at")
    private LocalDateTime requestedAt;

    @TableField(value = "matched_at", updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime matchedAt;

    @TableField(value = "matched_by", updateStrategy = FieldStrategy.ALWAYS)
    private String matchedBy;
}
