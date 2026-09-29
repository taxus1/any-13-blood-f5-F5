package com.somepro.infrastructure.persistence.crossmatch;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchListRowPO;
import com.somepro.infrastructure.persistence.crossmatch.po.CrossmatchPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配血申请 Mapper（基础设施层）。阻塞 JDBC API，只能在 boundedElastic 线程上调用。
 *
 * 列表/详情是手写 SQL：配血记录要带血袋编号与血袋血型，用一条 LEFT JOIN 取，避免逐行回查血袋。
 * 软删条件两边都要带：@TableLogic 只对单表 selectList/selectById 生效，手写 SQL 不会被它改写。
 */
@Mapper
public interface CrossmatchMapper extends BaseMapper<CrossmatchPO> {

    /**
     * 配血记录分页列表（PageHelper 拦截它做 limit/count）。
     * 条件全部可空，空则不加；申请单号、科室、住院号、结果均精确匹配。
     */
    @Select("""
            <script>
            SELECT c.id, c.request_no, c.unit_id, u.unit_no,
                   u.blood_group AS unit_group, u.rh AS unit_rh,
                   c.apply_dept, c.patient_no, c.patient_name,
                   c.patient_group, c.patient_rh, c.component, c.method, c.result,
                   c.apply_reason, c.requested_at, c.matched_at, c.matched_by
            FROM t_crossmatch c
            LEFT JOIN t_blood_unit u ON u.id = c.unit_id AND u.del_flag = 0
            WHERE c.del_flag = 0
            <if test="requestNo != null and requestNo != ''">
                AND c.request_no = #{requestNo}
            </if>
            <if test="applyDept != null and applyDept != ''">
                AND c.apply_dept = #{applyDept}
            </if>
            <if test="patientNo != null and patientNo != ''">
                AND c.patient_no = #{patientNo}
            </if>
            <if test="result != null">AND c.result = #{result}</if>
            ORDER BY c.request_no ASC, c.id ASC
            </script>
            """)
    List<CrossmatchListRowPO> selectListPage(@Param("requestNo") String requestNo,
                                             @Param("applyDept") String applyDept,
                                             @Param("patientNo") String patientNo,
                                             @Param("result") String result);

    /** 配血记录详情行（同样 LEFT JOIN 血袋，取单条；不存在返回 null）。 */
    @Select("""
            SELECT c.id, c.request_no, c.unit_id, u.unit_no,
                   u.blood_group AS unit_group, u.rh AS unit_rh,
                   c.apply_dept, c.patient_no, c.patient_name,
                   c.patient_group, c.patient_rh, c.component, c.method, c.result,
                   c.apply_reason, c.requested_at, c.matched_at, c.matched_by
            FROM t_crossmatch c
            LEFT JOIN t_blood_unit u ON u.id = c.unit_id AND u.del_flag = 0
            WHERE c.del_flag = 0 AND c.id = #{id}
            """)
    CrossmatchListRowPO selectListRowById(@Param("id") Long id);

    /**
     * 取某年配血申请单号的最大数字序号（CM-yyyy-序号），含软删记录（uk_request_no 物理唯一，单号不可复用）。
     *
     * @return 最大序号；该年尚无记录返回 null
     */
    @Select("SELECT MAX(CAST(SUBSTRING_INDEX(request_no, '-', -1) AS UNSIGNED)) "
            + "FROM t_crossmatch WHERE request_no LIKE CONCAT('CM-', #{year}, '-%')")
    Integer maxSerialOfYear(@Param("year") int year);
}
