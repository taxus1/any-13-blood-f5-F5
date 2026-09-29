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
 * 医院先递一条配血申请，血站判读所选血袋与病房患者合不合，相合才谈发血。
 * 申请单号 {@code requestNo} 全局唯一（CM-yyyy-序号）。
 * 申请提交即 PENDING 待配；{@link #judge} 按 {@link BloodCompatibility} 判读后落定为
 * COMPATIBLE 相合 / INCOMPATIBLE 不合，且只能判一次。
 *
 * 患者血型（{@code patientGroup/patientRh}）来自申请单，血袋血型不在本聚合保存，
 * 判读时由应用层读所选血袋档案照实带入，保证「患者怎么填、血袋什么样」各自如实。
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

    /** 所选血袋 id（拿哪袋血跟病人配）。 */
    private Long unitId;

    private String applyDept;

    private String patientNo;

    private String patientName;

    private BloodGroup patientGroup;

    private RhFactor patientRh;

    private ComponentType component;

    private CrossmatchMethod method;

    private CrossmatchResult result;

    private String applyReason;

    private LocalDateTime requestedAt;

    private LocalDateTime matchedAt;

    private String matchedBy;

    /**
     * 工厂方法：提交一条配血申请。结果固定从 PENDING 待配开始，申请时刻取提交时刻。
     */
    public static Crossmatch apply(Long unitId, String applyDept, String patientNo, String patientName,
                                   BloodGroup patientGroup, RhFactor patientRh,
                                   ComponentType component, CrossmatchMethod method,
                                   String applyReason, LocalDateTime requestedAt) {
        if (unitId == null) {
            throw new BizException("配血申请必须选择血袋");
        }
        if (isBlank(applyDept)) {
            throw new BizException("申请科室不能为空");
        }
        if (isBlank(patientNo)) {
            throw new BizException("患者住院号不能为空");
        }
        if (isBlank(patientName)) {
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
        if (isBlank(applyReason)) {
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
        if (isBlank(requestNo)) {
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
     * 判读：用血袋档案上的血型与申请单上的患者血型，按 {@link BloodCompatibility} 落定结果。
     * 只能对仍在 PENDING 的申请判一次；已经判过的不允许再改结果。
     *
     * @param donorGroup 血袋 ABO 血型（照血袋档案，不照申请单）
     * @param donorRh    血袋 Rh 因子（照血袋档案，不照申请单）
     * @param matchedAt  判读时刻
     * @param matchedBy  判读人（登录账号）
     */
    public void judge(BloodGroup donorGroup, RhFactor donorRh,
                      LocalDateTime matchedAt, String matchedBy) {
        if (this.result != CrossmatchResult.PENDING) {
            throw new BizException("该配血申请已有结果（" + result + "），不能重复判读");
        }
        if (matchedAt == null) {
            throw new BizException("配血时刻不能为空");
        }
        this.result = BloodCompatibility.match(donorGroup, donorRh, this.patientGroup, this.patientRh);
        this.matchedAt = matchedAt;
        this.matchedBy = matchedBy;
    }

    /** 是否仍未走完（待配）。同一袋血已挂着 PENDING 申请时，不能再对它递新申请。 */
    public boolean isPending() {
        return result == CrossmatchResult.PENDING;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
