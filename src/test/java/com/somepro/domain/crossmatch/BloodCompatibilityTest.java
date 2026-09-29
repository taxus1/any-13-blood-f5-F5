package com.somepro.domain.crossmatch;

import com.somepro.domain.crossmatch.model.CrossmatchResult;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.somepro.domain.crossmatch.model.BloodCompatibility.match;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 配血相合判读规则单测：覆盖需求给定的「红细胞看主侧」ABO/Rh 全组合。
 */
class BloodCompatibilityTest {

    static Stream<Arguments> aboRhMatrix() {
        // 供者ABO, 供者Rh, 患者ABO, 患者Rh, 期望
        return Stream.of(
                // —— 同型肯定相合（Rh 也同） ——
                Arguments.of(BloodGroup.A, RhFactor.POSITIVE, BloodGroup.A, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                Arguments.of(BloodGroup.B, RhFactor.POSITIVE, BloodGroup.B, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                Arguments.of(BloodGroup.AB, RhFactor.POSITIVE, BloodGroup.AB, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                Arguments.of(BloodGroup.O, RhFactor.POSITIVE, BloodGroup.O, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),

                // —— O 型万能供者：给 A / B / AB 患者都能配 ——
                Arguments.of(BloodGroup.O, RhFactor.POSITIVE, BloodGroup.A, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                Arguments.of(BloodGroup.O, RhFactor.POSITIVE, BloodGroup.B, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                Arguments.of(BloodGroup.O, RhFactor.POSITIVE, BloodGroup.AB, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),

                // —— AB 型万能受者：A / B / O 红细胞都能接 ——
                Arguments.of(BloodGroup.A, RhFactor.POSITIVE, BloodGroup.AB, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                Arguments.of(BloodGroup.B, RhFactor.POSITIVE, BloodGroup.AB, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),

                // —— A 供 B、B 供 A 均不合 ——
                Arguments.of(BloodGroup.A, RhFactor.POSITIVE, BloodGroup.B, RhFactor.POSITIVE, CrossmatchResult.INCOMPATIBLE),
                Arguments.of(BloodGroup.B, RhFactor.POSITIVE, BloodGroup.A, RhFactor.POSITIVE, CrossmatchResult.INCOMPATIBLE),
                // A / B 供 O 患者不合（O 不是万能受者）
                Arguments.of(BloodGroup.A, RhFactor.POSITIVE, BloodGroup.O, RhFactor.POSITIVE, CrossmatchResult.INCOMPATIBLE),
                Arguments.of(BloodGroup.B, RhFactor.POSITIVE, BloodGroup.O, RhFactor.POSITIVE, CrossmatchResult.INCOMPATIBLE),
                Arguments.of(BloodGroup.AB, RhFactor.POSITIVE, BloodGroup.O, RhFactor.POSITIVE, CrossmatchResult.INCOMPATIBLE),

                // —— Rh：阴性患者只接阴性；阳性患者阴阳皆可 ——
                // 同 ABO，患者阴、供者阳 -> 不合
                Arguments.of(BloodGroup.O, RhFactor.POSITIVE, BloodGroup.O, RhFactor.NEGATIVE, CrossmatchResult.INCOMPATIBLE),
                Arguments.of(BloodGroup.A, RhFactor.POSITIVE, BloodGroup.A, RhFactor.NEGATIVE, CrossmatchResult.INCOMPATIBLE),
                // 患者阴、供者阴 -> 合
                Arguments.of(BloodGroup.O, RhFactor.NEGATIVE, BloodGroup.O, RhFactor.NEGATIVE, CrossmatchResult.COMPATIBLE),
                // 患者阳、供者阴 -> 合
                Arguments.of(BloodGroup.O, RhFactor.NEGATIVE, BloodGroup.A, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                // 万能供者 O 阴给 AB 阳患者 -> 合
                Arguments.of(BloodGroup.O, RhFactor.NEGATIVE, BloodGroup.AB, RhFactor.POSITIVE, CrossmatchResult.COMPATIBLE),
                // ABO 合但 Rh 不合 -> 整体不合
                Arguments.of(BloodGroup.O, RhFactor.POSITIVE, BloodGroup.AB, RhFactor.NEGATIVE, CrossmatchResult.INCOMPATIBLE)
        );
    }

    @DisplayName("ABO/Rh 主侧相合矩阵")
    @ParameterizedTest(name = "{0}{1} -> 患者{2}{3} 期望 {4}")
    @MethodSource("aboRhMatrix")
    void matrix(BloodGroup donorGroup, RhFactor donorRh,
                BloodGroup patientGroup, RhFactor patientRh,
                CrossmatchResult expected) {
        assertEquals(expected, match(donorGroup, donorRh, patientGroup, patientRh));
    }
}
