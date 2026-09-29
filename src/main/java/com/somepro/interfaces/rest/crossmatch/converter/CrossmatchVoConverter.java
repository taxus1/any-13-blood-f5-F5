package com.somepro.interfaces.rest.crossmatch.converter;

import com.somepro.application.crossmatch.cmd.CreateCrossmatchCmd;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.RhFactor;
import com.somepro.domain.unit.model.ComponentType;
import com.somepro.interfaces.rest.crossmatch.vo.CreateCrossmatchRequest;
import com.somepro.interfaces.rest.crossmatch.vo.CrossmatchVO;

/**
 * 配血接口层转换器：请求体 → 应用命令，配血列表行 → VO。
 */
public final class CrossmatchVoConverter {

    private CrossmatchVoConverter() {
    }

    public static CreateCrossmatchCmd toCmd(CreateCrossmatchRequest req) {
        return new CreateCrossmatchCmd(
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

    public static CrossmatchVO toVo(CrossmatchListItem item) {
        return new CrossmatchVO(
                item.id(),
                item.requestNo(),
                item.unitId(),
                item.unitNo(),
                item.unitGroup() == null ? null : item.unitGroup().name(),
                item.unitRh() == null ? null : item.unitRh().name(),
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
