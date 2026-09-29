package com.somepro.domain.crossmatch.repository;

import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 配血申请仓储端口（领域层定义，基础设施层实现）。
 *
 * 详情/分页统一返回 {@link CrossmatchListItem}：记录要带血袋编号与血袋血型，由仓储 join 血袋表带出。
 */
public interface CrossmatchRepository {

    Mono<Crossmatch> save(Crossmatch crossmatch);

    /** 读聚合（判读用：要在领域对象上改结果）。 */
    Mono<Crossmatch> findById(Long id);

    /** 读列表行（详情展示用：带血袋编号与血袋血型）。 */
    Mono<CrossmatchListItem> findListItemById(Long id);

    Mono<PageResult<CrossmatchListItem>> page(CrossmatchQuery query);

    /**
     * 某袋血是否已挂着一条还没走完（PENDING）的申请。
     * 同一袋血在结果落定前不允许再递新申请。软删记录不算。
     */
    Mono<Boolean> existsPendingByUnit(Long unitId);

    /**
     * 按当前年份取下一个申请单号（CM-yyyy-序号）。
     * 含已逻辑删除记录，并发碰撞靠 uk_request_no 唯一索引兜底 + 重试。
     */
    Mono<String> nextRequestNo();
}
