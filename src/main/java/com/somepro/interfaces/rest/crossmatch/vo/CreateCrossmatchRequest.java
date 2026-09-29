package com.somepro.interfaces.rest.crossmatch.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 提交配血申请请求体。
 *
 * 对应申请单上要写的内容：选哪袋血、申请科室、患者住院号/姓名、患者 ABO/Rh、申请成分、
 * 配血方法（MAJOR/MINOR/BOTH 三选一）、申请原因。
 * 不含申请单号、结果、申请时刻：单号系统取号，结果从 PENDING 起，时刻取提交时刻。
 */
public record CreateCrossmatchRequest(
        @NotNull(message = "所选血袋 id 不能为空")
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
