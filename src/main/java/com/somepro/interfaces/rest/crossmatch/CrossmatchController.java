package com.somepro.interfaces.rest.crossmatch;

import com.somepro.application.crossmatch.CrossmatchAppService;
import com.somepro.common.Result;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchQuery;
import com.somepro.domain.crossmatch.model.CrossmatchResult;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.crossmatch.converter.CrossmatchVoConverter;
import com.somepro.interfaces.rest.crossmatch.vo.CreateCrossmatchRequest;
import com.somepro.interfaces.rest.crossmatch.vo.CrossmatchVO;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
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
 * 配血接口（用户接口层）：提交配血申请、判读相合、查看详情、按条件分页翻记录。
 *
 * 提交时由系统选袋把关（在库且未过期、同袋无未出结果的申请），单号系统取号（CM-yyyy-序号），
 * 结果从 PENDING 起；判读按血袋档案血型与申请单患者血型判 COMPATIBLE / INCOMPATIBLE，只能判一次。
 * 翻记录可按申请科室、患者住院号、结果组合过滤，分页返回，每行带 requestNo 便于和医院单据对号。
 */
@RestController
@RequestMapping("/api/crossmatches")
public class CrossmatchController {

    private final CrossmatchAppService crossmatchAppService;

    public CrossmatchController(CrossmatchAppService crossmatchAppService) {
        this.crossmatchAppService = crossmatchAppService;
    }

    /** 提交配血申请：单号系统取号，结果初始 PENDING。 */
    @PostMapping
    public Mono<Result<CrossmatchVO>> create(@Valid @RequestBody CreateCrossmatchRequest request) {
        return crossmatchAppService.create(CrossmatchVoConverter.toCmd(request))
                .map(CrossmatchVoConverter::toVo)
                .map(Result::ok);
    }

    /** 判读：对仍在 PENDING 的申请判相合/不合；判读人取当前登录账号。 */
    @PostMapping("/{id}/judge")
    public Mono<Result<CrossmatchVO>> judge(@PathVariable Long id) {
        return currentOperator()
                .flatMap(operator -> crossmatchAppService.judge(id, operator))
                .map(CrossmatchVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<CrossmatchVO>> detail(@PathVariable Long id) {
        return crossmatchAppService.detail(id)
                .map(CrossmatchVoConverter::toVo)
                .map(Result::ok);
    }

    /**
     * 分页翻配血记录：applyDept 申请科室 / patientNo 患者住院号 / result 结果，均可空、可组合，
     * 另支持 requestNo 精确对号。每行带 requestNo。
     */
    @GetMapping
    public Mono<Result<PageVO<CrossmatchVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String requestNo,
            @RequestParam(required = false) String applyDept,
            @RequestParam(required = false) String patientNo,
            @RequestParam(required = false) String result) {
        CrossmatchQuery query = new CrossmatchQuery(
                pageNum, pageSize,
                blankToNull(requestNo),
                blankToNull(applyDept),
                blankToNull(patientNo),
                blankToNull(result) == null ? null : CrossmatchResult.parse(result));
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

    /** 从登录态解析判读人；未认证/匿名兜底为 system（全路由需认证，正常不会走到匿名）。 */
    private static Mono<String> currentOperator() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> resolveOperator(auth))
                .defaultIfEmpty("system");
    }

    private static String resolveOperator(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return auth.getName();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
