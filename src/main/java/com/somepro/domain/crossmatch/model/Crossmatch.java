package com.somepro.domain.crossmatch.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 交叉配血申请聚合根（领域层）。
 *
 * 医院要血先递申请：指定一袋血、写上患者信息（住院号/姓名/ABO/Rh）、申请成分、
 * 配血方法（MAJOR 主侧 / MINOR 次侧 / BOTH 双侧）和申请原因。新申请固定 PENDING 待配；
 * 判配后 {@link #judge} 按红细胞主侧规矩落到 COMPATIBLE / INCOMPATIBLE，相合才谈发血。
 *
 * 患者血型照申请单原样记（patientGroup/patientRh），血袋血型不落在本单上 ——
 * 判配时按 unitId 读血袋当下的真实血型，保证「血袋是什么样就拿什么样判」。
 *
 * 纯领域对象，不落框架注解：落库形状见 CrossmatchPO，对外形状见 CrossmatchVO。
 */
@Getter
@Setter
public class Crossmatch extends BaseEntity {

    /** 申请单号格式：CM-2026-0001（年份 + 4 位序号，序号溢出后自然加宽）。 */
    public static final Pattern NO_PATTERN = Pattern.compile("^CM-\\d{4}-\\d{4,}$");

    private Long id;

    private String requestNo;

    /** 挑选的血袋 id。 */
    private Long unitId;

    private String applyDept;

    /** 患者住院号。 */
    private String patientNo;

    private String patientName;

    /** 患者 ABO 血型：照申请单如实记录。 */
    private BloodGroup patientGroup;

    /** 患者 Rh 因子：照申请单如实记录。 */
    private RhFactor patientRh;

    /** 申请成分。 */
    private ComponentType component;

    private CrossmatchMethod method;

    private CrossmatchResult result;

    private String applyReason;

    private LocalDateTime requestedAt;

    private LocalDateTime matchedAt;

    /** 配血人（登录账号），判配时带入。 */
    private String matchedBy;

    /**
     * 工厂方法：递交一条新的配血申请，结果固定 PENDING。
     */
    public static Crossmatch apply(Long unitId, String applyDept, String patientNo, String patientName,
                                   BloodGroup patientGroup, RhFactor patientRh,
                                   ComponentType component, CrossmatchMethod method,
                                   String applyReason, LocalDateTime requestedAt) {
        if (unitId == null) {
            throw new BizException("必须挑选一袋血");
        }
        if (applyDept == null || applyDept.isBlank()) {
            throw new BizException("申请科室不能为空");
        }
        if (patientNo == null || patientNo.isBlank()) {
            throw new BizException("患者住院号不能为空");
        }
        if (patientName == null || patientName.isBlank()) {
            throw new BizException("患者姓名不能为空");
        }
        if (patientGroup == null) {
            throw new BizException("患者 ABO 血型不能为空");
        }
        if (patientRh == null) {
            throw new BizException("患者 Rh 因子不能为空");
        }
        if (component == null) {
            throw new BizException("申请成分不能为空");
        }
        if (method == null) {
            throw new BizException("配血方法不能为空");
        }
        if (applyReason == null || applyReason.isBlank()) {
            throw new BizException("申请原因不能为空");
        }
        if (requestedAt == null) {
            throw new BizException("申请时刻不能为空");
        }
        Crossmatch cm = new Crossmatch();
        cm.unitId = unitId;
        cm.applyDept = applyDept.trim();
        cm.patientNo = patientNo.trim();
        cm.patientName = patientName.trim();
        cm.patientGroup = patientGroup;
        cm.patientRh = patientRh;
        cm.component = component;
        cm.method = method;
        cm.applyReason = applyReason.trim();
        cm.requestedAt = requestedAt;
        cm.result = CrossmatchResult.PENDING;
        return cm;
    }

    /** 分配全局唯一申请单号（由仓储按年取号），只允许分配一次。 */
    public void assignRequestNo(String requestNo) {
        if (requestNo == null || requestNo.isBlank()) {
            throw new BizException("申请单号不能为空");
        }
        if (!NO_PATTERN.matcher(requestNo).matches()) {
            throw new BizException("申请单号格式非法：" + requestNo + "，应为 CM-年份-序号，如 CM-2026-0001");
        }
        if (this.requestNo != null) {
            throw new BizException("申请单号已分配，不允许更改");
        }
        this.requestNo = requestNo;
    }

    /**
     * 判配：只能对 PENDING 的申请判一次。按红细胞主侧规矩判相合与否，
     * 落入 COMPATIBLE / INCOMPATIBLE，并记下配血时刻与配血人。
     *
     * @param unitGroup 所选血袋当下的真实 ABO 血型
     * @param unitRh    所选血袋当下的真实 Rh 因子
     * @param matchedAt 判配时刻
     * @param matchedBy 配血人（登录账号）
     */
    public void judge(BloodGroup unitGroup, RhFactor unitRh,
                      LocalDateTime matchedAt, String matchedBy) {
        if (this.result != CrossmatchResult.PENDING) {
            throw new BizException("该申请已出配血结果（" + result + "），不能重复判配");
        }
        if (unitGroup == null) {
            throw new BizException("血袋 ABO 血型缺失，无法判配");
        }
        if (unitRh == null) {
            throw new BizException("血袋 Rh 因子缺失，无法判配");
        }
        if (matchedAt == null) {
            throw new BizException("配血时刻不能为空");
        }
        this.result = compatible(unitGroup, unitRh)
                ? CrossmatchResult.COMPATIBLE
                : CrossmatchResult.INCOMPATIBLE;
        this.matchedAt = matchedAt;
        this.matchedBy = matchedBy;
    }

    /**
     * 红细胞成分主侧相合规矩：
     * - ABO：同型肯定配上；O 型红细胞是万能供者，给 A/B/AB 都行；
     *   AB 型病人是万能受者，A/B/O 的红细胞都能接。
     * - Rh：阴性病人只接阴性血；阳性病人阴/阳都能接。
     * ABO 与 Rh 都相合才算相合。
     */
    private boolean compatible(BloodGroup unitGroup, RhFactor unitRh) {
        boolean abo;
        switch (patientGroup) {
            case A -> abo = unitGroup == BloodGroup.A || unitGroup == BloodGroup.O;
            case B -> abo = unitGroup == BloodGroup.B || unitGroup == BloodGroup.O;
            case AB -> abo = unitGroup == BloodGroup.A || unitGroup == BloodGroup.B
                    || unitGroup == BloodGroup.AB || unitGroup == BloodGroup.O;
            case O -> abo = unitGroup == BloodGroup.O;
            default -> abo = false;
        }
        boolean rh = patientRh == RhFactor.POSITIVE || unitRh == RhFactor.NEGATIVE;
        return abo && rh;
    }
}
