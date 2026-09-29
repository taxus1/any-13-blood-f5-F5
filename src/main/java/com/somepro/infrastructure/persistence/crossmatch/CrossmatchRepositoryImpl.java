package com.somepro.infrastructure.persistence.crossmatch;

import cn.hutool.core.util.IdUtil;
import com.github.pagehelper.PageHelper;
import com.somepro.domain.crossmatch.model.Crossmatch;
import com.somepro.domain.crossmatch.model.CrossmatchListItem;
import com.somepro.domain.crossmatch.model.CrossmatchQuery;
import com.somepro.domain.crossmatch.repository.CrossmatchRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.persistence.base.BlockingJdbcSupport;
import com.somepro.infrastructure.persistence.crossmatch.converter.CrossmatchPoConverter;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchListRowPO;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 交叉配血仓储适配器（基础设施层）：MyBatis-Plus + 一条手写 join 列表 SQL 实现 {@link CrossmatchRepository}。
 *
 * 约束与血袋仓储一致：Mapper 只在 blocking 桥接里调用、@TableLogic 软删、PageHelper 分页、
 * 单号按年取号（含软删）+ uk_request_no 兜底，碰撞由应用层重试。
 */
@Repository
public class CrossmatchRepositoryImpl extends BlockingJdbcSupport implements CrossmatchRepository {

    /** CM-年-4 位序号，如 CM-2026-0001。 */
    private static final String NO_FORMAT = "CM-%04d-%04d";

    private final CrossmatchMapper crossmatchMapper;

    public CrossmatchRepositoryImpl(CrossmatchMapper crossmatchMapper) {
        this.crossmatchMapper = crossmatchMapper;
    }

    @Override
    public Mono<Crossmatch> save(Crossmatch crossmatch) {
        return blocking(() -> {
            CrossmatchPO po = CrossmatchPoConverter.toPo(crossmatch);
            if (po.getId() == null) {
                po.setId(IdUtil.getSnowflakeNextId());
                crossmatchMapper.insert(po);
            } else {
                crossmatchMapper.updateById(po);
            }
            return CrossmatchPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<Crossmatch> findById(Long id) {
        return blocking(() -> {
            CrossmatchPO po = crossmatchMapper.selectById(id);
            return po == null ? null : CrossmatchPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PageResult<CrossmatchListItem>> page(CrossmatchQuery q) {
        return this.<PageResult<CrossmatchListItem>>blocking(() -> {
            try {
                PageHelper.startPage(q.pageNum(), q.pageSize());
                List<CrossmatchListRowPO> rows = crossmatchMapper.selectListPage(
                        trimToNull(q.requestNo()),
                        trimToNull(q.unitNo()),
                        trimToNull(q.applyDept()),
                        trimToNull(q.patientNo()),
                        q.result() == null ? null : q.result().name(),
                        q.patientGroup() == null ? null : q.patientGroup().name(),
                        q.component() == null ? null : q.component().name(),
                        q.method() == null ? null : q.method().name());
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<CrossmatchListItem> content = rows.stream()
                        .map(CrossmatchPoConverter::toListItem)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, q.pageNum(), q.pageSize());
            } finally {
                PageHelper.clearPage();
            }
        });
    }

    @Override
    public Mono<Boolean> existsPendingByUnit(Long unitId) {
        return blocking(() -> crossmatchMapper.countPendingByUnit(unitId) > 0);
    }

    @Override
    public Mono<String> nextRequestNo() {
        return blocking(() -> {
            int year = LocalDate.now().getYear();
            Integer max = crossmatchMapper.maxSerialOfYear(year);
            return String.format(NO_FORMAT, year, (max == null ? 0 : max) + 1);
        });
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
