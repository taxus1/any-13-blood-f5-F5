package com.somepro.domain.crossmatch.repository;

import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchQuery;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 交叉配血申请仓储端口（领域层定义，基础设施层实现）。
 *
 * 分页返回 {@link CrossmatchListItem}：列表行需要血袋编号，由仓储 join 血袋表带出。
 */
public interface CrossmatchRepository {

    Mono<Crossmatch> save(Crossmatch crossmatch);

    Mono<Crossmatch> findById(Long id);

    Mono<PageResult<CrossmatchListItem>> page(CrossmatchQuery query);

    /**
     * 某袋血是否还挂着一条没走完（PENDING）的申请。
     * 同一袋血在一条申请出结果前不允许再提第二条，故含全部未删记录，只看 PENDING。
     */
    Mono<Boolean> existsPendingByUnit(Long unitId);

    /**
     * 按当前年份取下一个申请单号（CM-yyyy-序号）。
     * 含已逻辑删除记录，并发碰撞靠 uk_request_no 唯一索引兜底 + 重试。
     */
    Mono<String> nextRequestNo();
}
