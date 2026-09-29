package com.somepro.domain.crossmatch;

import com.somepro.common.exception.BizException;
import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.crossmatch.model.CrossmatchResult;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配血申请聚合单测：申请即 PENDING、判读只判一次、单号格式/一次性、必填校验。
 */
class CrossmatchTest {

    private static Crossmatch newApplication() {
        return Crossmatch.apply(1001L, "血液科", "P0001", "张三",
                BloodGroup.A, RhFactor.POSITIVE,
                ComponentType.RBC, CrossmatchMethod.MAJOR,
                "手术备血", LocalDateTime.now());
    }

    @Test
    void apply_startsPending_andRecordsPatientAsFilled() {
        Crossmatch cm = newApplication();
        assertEquals(CrossmatchResult.PENDING, cm.getResult());
        assertTrue(cm.isPending());
        assertEquals("P0001", cm.getPatientNo());
        assertEquals(BloodGroup.A, cm.getPatientGroup());
        assertEquals(CrossmatchMethod.MAJOR, cm.getMethod());
    }

    @Test
    void assignRequestNo_acceptsFormat_once() {
        Crossmatch cm = newApplication();
        cm.assignRequestNo("CM-2026-0001");
        assertEquals("CM-2026-0001", cm.getRequestNo());
        assertThrows(BizException.class, () -> cm.assignRequestNo("CM-2026-0002"));
        assertThrows(BizException.class, () -> newApplication().assignRequestNo("XX-2026-0001"));
    }

    @Test
    void judge_compatibleThenLocks() {
        Crossmatch cm = newApplication();
        // 患者 A 阳性，给 O 阳性（万能供者）-> 相合
        cm.judge(BloodGroup.O, RhFactor.POSITIVE, LocalDateTime.now(), "admin");
        assertEquals(CrossmatchResult.COMPATIBLE, cm.getResult());
        assertFalse(cm.isPending());
        assertEquals("admin", cm.getMatchedBy());
        // 只能判一次
        assertThrows(BizException.class,
                () -> cm.judge(BloodGroup.A, RhFactor.POSITIVE, LocalDateTime.now(), "admin"));
    }

    @Test
    void judge_incompatibleWhenRhPositiveToNegativePatient() {
        Crossmatch cm = newApplication();
        // 申请单改成 A 阴性患者，供者 A 阳性 -> Rh 不合
        Crossmatch negPatient = Crossmatch.apply(1001L, "血液科", "P0002", "李四",
                BloodGroup.A, RhFactor.NEGATIVE,
                ComponentType.RBC, CrossmatchMethod.BOTH, "急救", LocalDateTime.now());
        negPatient.judge(BloodGroup.A, RhFactor.POSITIVE, LocalDateTime.now(), "admin");
        assertEquals(CrossmatchResult.INCOMPATIBLE, negPatient.getResult());
    }

    @Test
    void apply_rejectsBlankRequiredFields() {
        assertThrows(BizException.class, () -> Crossmatch.apply(null, "血液科", "P", "张三",
                BloodGroup.A, RhFactor.POSITIVE, ComponentType.RBC, CrossmatchMethod.MAJOR, "原因", LocalDateTime.now()));
        assertThrows(BizException.class, () -> Crossmatch.apply(1L, "  ", "P", "张三",
                BloodGroup.A, RhFactor.POSITIVE, ComponentType.RBC, CrossmatchMethod.MAJOR, "原因", LocalDateTime.now()));
        assertThrows(BizException.class, () -> Crossmatch.apply(1L, "血液科", "P", "张三",
                null, RhFactor.POSITIVE, ComponentType.RBC, CrossmatchMethod.MAJOR, "原因", LocalDateTime.now()));
        assertThrows(BizException.class, () -> Crossmatch.apply(1L, "血液科", "P", "张三",
                BloodGroup.A, null, ComponentType.RBC, CrossmatchMethod.MAJOR, "原因", LocalDateTime.now()));
        assertThrows(BizException.class, () -> Crossmatch.apply(1L, "血液科", "P", "张三",
                BloodGroup.A, RhFactor.POSITIVE, null, CrossmatchMethod.MAJOR, "原因", LocalDateTime.now()));
        assertThrows(BizException.class, () -> Crossmatch.apply(1L, "血液科", "P", "张三",
                BloodGroup.A, RhFactor.POSITIVE, ComponentType.RBC, null, "原因", LocalDateTime.now()));
        assertThrows(BizException.class, () -> Crossmatch.apply(1L, "血液科", "P", "张三",
                BloodGroup.A, RhFactor.POSITIVE, ComponentType.RBC, CrossmatchMethod.MAJOR, "", LocalDateTime.now()));
    }
}
