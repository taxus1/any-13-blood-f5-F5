package com.somepro.interfaces.rest.crossmatch.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 递交配血申请请求体。
 *
 * 患者 ABO/Rh 由申请单如实填写；所选血袋只传 unitId，血袋血型系统判配时读血袋当下记录。
 * 申请单号、结果不在请求里：单号系统取号（CM-yyyy-序号），新单固定 PENDING。
 */
public record ApplyCrossmatchRequest(
        @NotNull(message = "必须挑选一袋血（unitId 不能为空）")
        Long unitId,

        @NotBlank(message = "申请科室不能为空")
        @Size(max = 64, message = "申请科室长度不能超过 64")
        String applyDept,

        @NotBlank(message = "患者住院号不能为空")
        @Size(max = 32, message = "患者住院号长度不能超过 32")
        String patientNo,

        @NotBlank(message = "患者姓名不能为空")
        @Size(max = 64, message = "患者姓名长度不能超过 64")
        String patientName,

        @NotBlank(message = "患者 ABO 血型不能为空")
        @Pattern(regexp = "A|B|AB|O", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "患者 ABO 血型取值 A / B / AB / O")
        String patientGroup,

        @NotBlank(message = "患者 Rh 因子不能为空")
        @Pattern(regexp = "POSITIVE|NEGATIVE", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "患者 Rh 因子取值 POSITIVE 阳性 / NEGATIVE 阴性")
        String patientRh,

        @NotBlank(message = "申请成分不能为空")
        @Pattern(regexp = "WHOLE|RBC|PLASMA|PLATELET", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "申请成分取值 WHOLE 全血 / RBC 红细胞 / PLASMA 血浆 / PLATELET 血小板")
        String component,

        @NotBlank(message = "配血方法不能为空")
        @Pattern(regexp = "MAJOR|MINOR|BOTH", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "配血方法取值 MAJOR 主侧 / MINOR 次侧 / BOTH 双侧")
        String method,

        @NotBlank(message = "申请原因不能为空")
        @Size(max = 255, message = "申请原因长度不能超过 255")
        String applyReason)
        implements Serializable {
}
