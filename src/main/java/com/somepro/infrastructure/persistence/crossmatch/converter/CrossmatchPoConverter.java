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
 * 枚举列容错解析（存量数据可能为人工录入）。
 */
public final class CrossmatchPoConverter {

    private CrossmatchPoConverter() {
    }

    public static CrossmatchPO toPo(Crossmatch c) {
        CrossmatchPO po = new CrossmatchPO();
        po.setId(c.getId());
        po.setRequestNo(c.getRequestNo());
        po.setUnitId(c.getUnitId());
        po.setApplyDept(c.getApplyDept());
        po.setPatientNo(c.getPatientNo());
        po.setPatientName(c.getPatientName());
        po.setPatientGroup(c.getPatientGroup() == null ? null : c.getPatientGroup().name());
        po.setPatientRh(c.getPatientRh() == null ? null : c.getPatientRh().name());
        po.setComponent(c.getComponent() == null ? null : c.getComponent().name());
        po.setMethod(c.getMethod() == null ? null : c.getMethod().name());
        po.setResult(c.getResult() == null ? null : c.getResult().name());
        po.setApplyReason(c.getApplyReason());
        po.setRequestedAt(c.getRequestedAt());
        po.setMatchedAt(c.getMatchedAt());
        po.setMatchedBy(c.getMatchedBy());
        po.setDelFlag(c.getDelFlag());
        po.setCreateBy(c.getCreateBy());
        po.setCreateTime(c.getCreateTime());
        po.setUpdateBy(c.getUpdateBy());
        po.setUpdateTime(c.getUpdateTime());
        return po;
    }

    public static Crossmatch toDomain(CrossmatchPO po) {
        Crossmatch c = new Crossmatch();
        c.setId(po.getId());
        c.setRequestNo(po.getRequestNo());
        c.setUnitId(po.getUnitId());
        c.setApplyDept(po.getApplyDept());
        c.setPatientNo(po.getPatientNo());
        c.setPatientName(po.getPatientName());
        c.setPatientGroup(valueOf(BloodGroup.class, po.getPatientGroup()));
        c.setPatientRh(valueOf(RhFactor.class, po.getPatientRh()));
        c.setComponent(valueOf(ComponentType.class, po.getComponent()));
        c.setMethod(valueOf(CrossmatchMethod.class, po.getMethod()));
        c.setResult(po.getResult() == null ? CrossmatchResult.PENDING : valueOf(CrossmatchResult.class, po.getResult()));
        c.setApplyReason(po.getApplyReason());
        c.setRequestedAt(po.getRequestedAt());
        c.setMatchedAt(po.getMatchedAt());
        c.setMatchedBy(po.getMatchedBy());
        c.setDelFlag(po.getDelFlag());
        c.setCreateBy(po.getCreateBy());
        c.setCreateTime(po.getCreateTime());
        c.setUpdateBy(po.getUpdateBy());
        c.setUpdateTime(po.getUpdateTime());
        return c;
    }

    /** join 列表行 → 领域列表值对象。血袋被软删时 unitNo 为 null，配血记录仍照常返回。 */
    public static CrossmatchListItem toListItem(CrossmatchListRowPO row) {
        return new CrossmatchListItem(
                row.getId(),
                row.getRequestNo(),
                row.getUnitId(),
                row.getUnitNo(),
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
