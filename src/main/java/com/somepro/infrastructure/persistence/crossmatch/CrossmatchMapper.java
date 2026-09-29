package com.somepro.infrastructure.persistence.crossmatch;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchListRowPO;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 交叉配血 Mapper（基础设施层）。阻塞 JDBC API，只能在 boundedElastic 线程上调用。
 *
 * 列表查询是手写 SQL：配血记录要带血袋编号，用一条 LEFT JOIN 取，避免逐行回查血袋（N+1）。
 * 软删条件两边都要带：@TableLogic 只对单表 selectList 生效，手写 SQL 不会被它改写。
 */
@Mapper
public interface CrossmatchMapper extends BaseMapper<CrossmatchPO> {

    /**
     * 配血记录分页列表（PageHelper 拦截它做 limit/count）。
     * 条件全部可空，空则不加该条件；申请单号/患者住院号/血袋编号精确，申请科室模糊，其余精确。
     */
    @Select("""
            <script>
            SELECT c.id, c.request_no, c.unit_id, u.unit_no, c.apply_dept,
                   c.patient_no, c.patient_name, c.patient_group, c.patient_rh,
                   c.component, c.method, c.result, c.apply_reason,
                   c.requested_at, c.matched_at, c.matched_by
            FROM t_crossmatch c
            LEFT JOIN t_blood_unit u ON u.id = c.unit_id AND u.del_flag = 0
            WHERE c.del_flag = 0
            <if test="requestNo != null and requestNo != ''">
                AND c.request_no = #{requestNo}
            </if>
            <if test="unitNo != null and unitNo != ''">
                AND u.unit_no = #{unitNo}
            </if>
            <if test="applyDept != null and applyDept != ''">
                AND c.apply_dept LIKE CONCAT('%', #{applyDept}, '%')
            </if>
            <if test="patientNo != null and patientNo != ''">
                AND c.patient_no = #{patientNo}
            </if>
            <if test="result != null">AND c.result = #{result}</if>
            <if test="patientGroup != null">AND c.patient_group = #{patientGroup}</if>
            <if test="component != null">AND c.component = #{component}</if>
            <if test="method != null">AND c.method = #{method}</if>
            ORDER BY c.request_no ASC, c.id ASC
            </script>
            """)
    List<CrossmatchListRowPO> selectListPage(@Param("requestNo") String requestNo,
                                             @Param("unitNo") String unitNo,
                                             @Param("applyDept") String applyDept,
                                             @Param("patientNo") String patientNo,
                                             @Param("result") String result,
                                             @Param("patientGroup") String patientGroup,
                                             @Param("component") String component,
                                             @Param("method") String method);

    /**
     * 是否存在指定血袋的在途（PENDING）申请。@TableLogic 自动带 c.del_flag = 0。
     */
    @Select("SELECT COUNT(1) FROM t_crossmatch WHERE unit_id = #{unitId} AND result = 'PENDING' AND del_flag = 0")
    long countPendingByUnit(@Param("unitId") Long unitId);

    /**
     * 取某年申请单号的最大数字序号（CM-yyyy-序号），含软删记录（uk_request_no 物理唯一，编号不可复用）。
     *
     * @return 最大序号；该年尚无记录返回 null
     */
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(request_no, '-', -1) AS UNSIGNED)) "
            + "FROM t_crossmatch WHERE request_no LIKE CONCAT('CM-', #{year}, '-%')")
    Integer maxSerialOfYear(@Param("year") int year);
}
