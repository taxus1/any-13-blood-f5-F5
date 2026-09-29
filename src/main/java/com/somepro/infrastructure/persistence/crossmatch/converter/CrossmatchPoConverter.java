package com.somepro.infrastructure.persistence.crossmatch.converter;

import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.crossmatch.model.CrossmatchResult;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchListRowPO;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchPO;

/**
 * CrossmatchPO / CrossmatchListRowPO（表）↔ 配血领域模型转换器（基础设施层）。
 *
 * 枚举列做容错解析（存量数据可能为人工录入）；result 缺省按 PENDING 对待。
 */
public final class CrossmatchPoConverter {

    private CrossmatchPoConverter() {
    }

    public static CrossmatchPO toPo(Crossmatch m) {
        CrossmatchPO po = new CrossmatchPO();
        po.setId(m.getId());
        po.setRequestNo(m.getRequestNo());
        po.setUnitId(m.getUnitId());
        po.setApplyDept(m.getApplyDept());
        po.setPatientNo(m.getPatientNo());
        po.setPatientName(m.getPatientName());
        po.setPatientGroup(m.getPatientGroup() == null ? null : m.getPatientGroup().name());
        po.setPatientRh(m.getPatientRh() == null ? null : m.getPatientRh().name());
        po.setComponent(m.getComponent() == null ? null : m.getComponent().name());
        po.setMethod(m.getMethod() == null ? null : m.getMethod().name());
        po.setResult(m.getResult() == null ? null : m.getResult().name());
        po.setApplyReason(m.getApplyReason());
        po.setRequestedAt(m.getRequestedAt());
        po.setMatchedAt(m.getMatchedAt());
        po.setMatchedBy(m.getMatchedBy());
        po.setDelFlag(m.getDelFlag());
        po.setCreateBy(m.getCreateBy());
        po.setCreateTime(m.getCreateTime());
        po.setUpdateBy(m.getUpdateBy());
        po.setUpdateTime(m.getUpdateTime());
        return po;
    }

    public static Crossmatch toDomain(CrossmatchPO po) {
        Crossmatch m = new Crossmatch();
        m.setId(po.getId());
        m.setRequestNo(po.getRequestNo());
        m.setUnitId(po.getUnitId());
        m.setApplyDept(po.getApplyDept());
        m.setPatientNo(po.getPatientNo());
        m.setPatientName(po.getPatientName());
        m.setPatientGroup(valueOf(BloodGroup.class, po.getPatientGroup()));
        m.setPatientRh(valueOf(RhFactor.class, po.getPatientRh()));
        m.setComponent(valueOf(ComponentType.class, po.getComponent()));
        m.setMethod(valueOf(CrossmatchMethod.class, po.getMethod()));
        m.setResult(po.getResult() == null ? CrossmatchResult.PENDING : valueOf(CrossmatchResult.class, po.getResult()));
        m.setApplyReason(po.getApplyReason());
        m.setRequestedAt(po.getRequestedAt());
        m.setMatchedAt(po.getMatchedAt());
        m.setMatchedBy(po.getMatchedBy());
        m.setDelFlag(po.getDelFlag());
        m.setCreateBy(po.getCreateBy());
        m.setCreateTime(po.getCreateTime());
        m.setUpdateBy(po.getUpdateBy());
        m.setUpdateTime(po.getUpdateTime());
        return m;
    }

    /** join 列表行 → 领域列表值对象。血袋被软删时编号/血袋血型为 null，配血行仍照常返回。 */
    public static CrossmatchListItem toListItem(CrossmatchListRowPO row) {
        return new CrossmatchListItem(
                row.getId(),
                row.getRequestNo(),
                row.getUnitId(),
                row.getUnitNo(),
                valueOf(BloodGroup.class, row.getUnitGroup()),
                valueOf(RhFactor.class, row.getUnitRh()),
                row.getApplyDept(),
                row.getPatientNo(),
                row.getPatientName(),
                valueOf(BloodGroup.class, row.getPatientGroup()),
                valueOf(RhFactor.class, row.getPatientRh()),
                valueOf(ComponentType.class, row.getComponent()),
                valueOf(CrossmatchMethod.class, row.getMethod()),
                valueOf(CrossmatchResult.class, row.getResult()),
                row.getApplyReason(),
                row.getRequestedAt(),
                row.getMatchedAt(),
                row.getMatchedBy());
    }

    private static <E extends Enum<E>> E valueOf(Class<E> enumType, String value) {
        return value == null ? null : Enum.valueOf(enumType, value.trim().toUpperCase());
    }
}
