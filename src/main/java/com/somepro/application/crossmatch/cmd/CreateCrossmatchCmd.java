package com.somepro.application.crossmatch.cmd;

import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;

/**
 * 提交配血申请命令（应用层入参）。
 *
 * 申请单号、结果、申请时刻都不在命令里：单号系统按年取号，结果从 PENDING 起，时刻取提交时刻。
 * 患者 ABO/Rh 由申请单照实填；血袋血型不在命令里，判读时读血袋档案照实带入。
 */
public record CreateCrossmatchCmd(Long unitId,
                                  String applyDept,
                                  String patientNo,
                                  String patientName,
                                  BloodGroup patientGroup,
                                  RhFactor patientRh,
                                  ComponentType component,
                                  CrossmatchMethod method,
                                  String applyReason) {
}
