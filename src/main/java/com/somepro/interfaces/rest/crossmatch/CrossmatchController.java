package com.somepro.interfaces.rest.crossmatch;

import com.somepro.application.crossmatch.CrossmatchAppService;
import com.somepro.common.Result;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchMethod;
import com.somepro.domain.crossmatch.model.CrossmatchQuery;
import com.somepro.domain.crossmatch.model.CrossmatchResult;
import com.somepro.domain.shared.model.BloodGroup;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.model.ComponentType;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.crossmatch.converter.CrossmatchVoConverter;
import com.somepro.interfaces.rest.crossmatch.vo.ApplyCrossmatchRequest;
import com.somepro.interfaces.rest.crossmatch.vo.CrossmatchVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 交叉配血接口（用户接口层）：递交申请、判配、查看、组合条件分页查询。
 *
 * 查询条件全部可空、可任意组合（申请单号、血袋编号、申请科室、患者住院号、结果、
 * 患者血型、成分、配血方法），都不填就是整份记录分页；每行带 requestNo，方便和医院的单子对号。
 * 枚举码 query 参数统一收 String，空串视为不填，非空交给领域枚举 parse 校验。
 */
@RestController
@RequestMapping("/api/crossmatches")
public class CrossmatchController {

    private final CrossmatchAppService crossmatchAppService;

    public CrossmatchController(CrossmatchAppService crossmatchAppService) {
        this.crossmatchAppService = crossmatchAppService;
    }

    /** 递交配血申请：单号系统取号（CM-yyyy-序号），结果固定 PENDING；血袋须在库未过期且无在途申请。 */
    @PostMapping
    public Mono<Result<CrossmatchVO>> apply(@Valid @RequestBody ApplyCrossmatchRequest request) {
        return crossmatchAppService.apply(CrossmatchVoConverter.toCmd(request))
                .map(CrossmatchVoConverter::toVo)
                .map(Result::ok);
    }

    /** 判配：按血袋当下真实血型与患者血型，依红细胞主侧规矩落 COMPATIBLE / INCOMPATIBLE。 */
    @PostMapping("/{id}/judge")
    public Mono<Result<CrossmatchVO>> judge(@PathVariable Long id) {
        return crossmatchAppService.judge(id)
                .map(CrossmatchVoConverter::toVo)
                .map(Result::ok);
    }

    /** 看单条。 */
    @GetMapping("/{id}")
    public Mono<Result<CrossmatchVO>> detail(@PathVariable Long id) {
        return crossmatchAppService.detail(id)
                .map(CrossmatchVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 分页记录：申请科室模糊、患者住院号精确、结果精确等随意组合；都不填翻整份记录。
     * 每行带 requestNo / unitNo，方便与医院及血袋两边单据对号。
     */
    @GetMapping
    public Mono<Result<PageVO<CrossmatchVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String requestNo,
            @RequestParam(required = false) String unitNo,
            @RequestParam(required = false) String applyDept,
            @RequestParam(required = false) String patientNo,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String patientGroup,
            @RequestParam(required = false) String component,
            @RequestParam(required = false) String method) {
        CrossmatchQuery query = new CrossmatchQuery(
                pageNum, pageSize,
                blankToNull(requestNo),
                blankToNull(unitNo),
                blankToNull(applyDept),
                blankToNull(patientNo),
                blankToNull(result) == null ? null : CrossmatchResult.parse(result),
                blankToNull(patientGroup) == null ? null : BloodGroup.parse(patientGroup),
                blankToNull(component) == null ? null : ComponentType.parse(component),
                blankToNull(method) == null ? null : CrossmatchMethod.parse(method));
        return crossmatchAppService.page(query)
                .map(CrossmatchController::toPageVo)
                .map(Result::ok);
    }

    private static PageVO<CrossmatchVO> toPageVo(PageResult<CrossmatchListItem> page) {
        List<CrossmatchVO> content = page.content().stream()
                .map(CrossmatchVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
