package com.somepro.domain.crossmatch.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;

/**
 * 配血相合判读规则（领域服务，纯函数，无状态）。
 *
 * 按需求给定的「红细胞成分看主侧」规则，判读供者血袋红细胞与患者是否相合：
 *
 * ABO：
 *  - 同型肯定相合；
 *  - O 型红细胞是万能供者，给 A / B / AB 患者都能配；
 *  - AB 型患者是万能受者，A / B / O 型红细胞都能接。
 * 合并即：供者为 O、或患者为 AB、或同型 → ABO 相合。
 *
 * Rh：
 *  - 阴性患者只接阴性的血；
 *  - 阳性患者阴性、阳性都能接。
 * 即唯一禁忌：患者 NEGATIVE 而血袋 POSITIVE。
 *
 * 两条同时满足判 COMPATIBLE，否则 INCOMPATIBLE。判读只产出终态，不产出 PENDING（PENDING 是尚未判读）。
 * 患者血型取自申请单，血袋血型取自所选血袋档案，二者各自照实传入，规则里不做任何改写。
 */
public final class BloodCompatibility {

    private BloodCompatibility() {
    }

    /**
     * 判读一袋血与一位患者是否相合。
     *
     * @param donorGroup   血袋 ABO 血型（照血袋档案）
     * @param donorRh      血袋 Rh 因子（照血袋档案）
     * @param patientGroup 患者 ABO 血型（照申请单）
     * @param patientRh    患者 Rh 因子（照申请单）
     * @return COMPATIBLE 相合 / INCOMPATIBLE 不合
     */
    public static CrossmatchResult match(BloodGroup donorGroup, RhFactor donorRh,
                                         BloodGroup patientGroup, RhFactor patientRh) {
        if (donorGroup == null || donorRh == null || patientGroup == null || patientRh == null) {
            throw new BizException("判读配血时，患者与血袋的 ABO 血型、Rh 因子都不能为空");
        }
        boolean compatible = aboCompatible(donorGroup, patientGroup) && rhCompatible(donorRh, patientRh);
        return compatible ? CrossmatchResult.COMPATIBLE : CrossmatchResult.INCOMPATIBLE;
    }

    /** ABO 主侧相合：供者 O（万能供者）/ 患者 AB（万能受者）/ 同型。 */
    static boolean aboCompatible(BloodGroup donor, BloodGroup patient) {
        return donor == BloodGroup.O
                || patient == BloodGroup.AB
                || donor == patient;
    }

    /** Rh 相合：阴性患者只接阴性；阳性患者阴阳皆可。 */
    static boolean rhCompatible(RhFactor donor, RhFactor patient) {
        return patient == RhFactor.POSITIVE || donor == RhFactor.NEGATIVE;
    }
}
