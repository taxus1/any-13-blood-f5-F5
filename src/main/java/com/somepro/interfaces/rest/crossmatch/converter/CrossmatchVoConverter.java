package com.somepro.interfaces.rest.crossmatch.converter;

import com.somepro.application.crossmatch.cmd.ApplyCrossmatchCmd;
import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;
import com.somepro.interfaces.rest.crossmatch.vo.ApplyCrossmatchRequest;
import com.somepro.interfaces.rest.crossmatch.vo.CrossmatchVO;

/**
 * 配血接口层转换器：请求体 → 应用命令，配血聚合 / 列表行 → VO。
 * 枚举字符串在这里经领域 parse 做取值校验。
 */
public final class CrossmatchVoConverter {

    private CrossmatchVoConverter() {
    }

    public static ApplyCrossmatchCmd toCmd(ApplyCrossmatchRequest req) {
        return new ApplyCrossmatchCmd(
                req.unitId(),
                req.applyDept(),
                req.patientNo(),
                req.patientName(),
                BloodGroup.parse(req.patientGroup()),
                RhFactor.parse(req.patientRh()),
                ComponentType.parse(req.component()),
                CrossmatchMethod.parse(req.method()),
                req.applyReason());
    }

    /** 详情：只有申请单自身信息，血袋编号留空（需要可走列表/血袋接口）。 */
    public static CrossmatchVO toVo(Crossmatch c) {
        return new CrossmatchVO(
                c.getId(),
                c.getRequestNo(),
                c.getUnitId(),
                null,
                c.getApplyDept(),
                c.getPatientNo(),
                c.getPatientName(),
                c.getPatientGroup() == null ? null : c.getPatientGroup().name(),
                c.getPatientRh() == null ? null : c.getPatientRh().name(),
                c.getComponent() == null ? null : c.getComponent().name(),
                c.getMethod() == null ? null : c.getMethod().name(),
                c.getResult() == null ? null : c.getResult().name(),
                c.getApplyReason(),
                c.getRequestedAt(),
                c.getMatchedAt(),
                c.getMatchedBy(),
                c.getCreateTime());
    }

    /** 列表行：带血袋编号。 */
    public static CrossmatchVO toVo(CrossmatchListItem item) {
        return new CrossmatchVO(
                item.id(),
                item.requestNo(),
                item.unitId(),
                item.unitNo(),
                item.applyDept(),
                item.patientNo(),
                item.patientName(),
                item.patientGroup() == null ? null : item.patientGroup().name(),
                item.patientRh() == null ? null : item.patientRh().name(),
                item.component() == null ? null : item.component().name(),
                item.method() == null ? null : item.method().name(),
                item.result() == null ? null : item.result().name(),
                item.applyReason(),
                item.requestedAt(),
                item.matchedAt(),
                item.matchedBy(),
                null);
    }
}
