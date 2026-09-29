package com.somepro.application.crossmatch.cmd;

import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;

/**
 * 递交配血申请命令（应用层入参）。
 *
 * 患者 ABO/Rh 照申请单如实带入；所选血袋的血型不在这里传 —— 判配时按 unitId 读血袋当下真实血型。
 * 申请单号、结果、申请时刻也不在命令里：单号系统按年取号，新单固定 PENDING，时刻取当下。
 */
public record ApplyCrossmatchCmd(Long unitId,
                                 String applyDept,
                                 String patientNo,
                                 String patientName,
                                 BloodGroup patientGroup,
                                 RhFactor patientRh,
                                 ComponentType component,
                                 CrossmatchMethod method,
                                 String applyReason) {
}
