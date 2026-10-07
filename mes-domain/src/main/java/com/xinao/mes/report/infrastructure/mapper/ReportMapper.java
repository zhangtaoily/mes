package com.xinao.mes.report.infrastructure.mapper;

import com.xinao.mes.report.vo.DefectStatsVO;
import com.xinao.mes.report.vo.IndicatorStatsVO;
import com.xinao.mes.report.vo.OutputDailyVO;
import com.xinao.mes.report.vo.PackInboundVO;
import com.xinao.mes.report.vo.ProgressTrackingVO;
import com.xinao.mes.report.vo.QcDailyVO;
import com.xinao.mes.report.vo.RawQcSummaryVO;
import com.xinao.mes.report.vo.TubeWeightOutputVO;
import com.xinao.mes.report.vo.WoolRateVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * M6 报表专用只读 Mapper（P0 12 张，全部跨域聚合查询，不写库）。
 * 口径约定：作废检验报告不计入统计（doc_status != '已作废'）；
 * 产量按工序组过滤——细毛线 1-5（混条/针梳）/ 前纺 1-6（含粗纱）/ 后纺 7-10（细纱~摇绞）。
 */
@Mapper
public interface ReportMapper {

    // ==================== 1. 车间产品质量检验日报表（主口径：按日+车间+批次） ====================

    /** 日报表主行：报告数/检验项数/越限项数/取样数 */
    @Select("""
            <script>
            SELECT r.inspect_date, r.workshop_id, w.workshop_name, r.batch_no,
                   COUNT(DISTINCT r.id) report_count, COUNT(i.id) item_count,
                   COALESCE(SUM(i.is_over), 0) over_count, COALESCE(SUM(r.sample_qty), 0) sample_qty
            FROM qc_report r
            JOIN qc_report_item i ON i.qc_report_id = r.id
            LEFT JOIN base_workshop w ON w.id = r.workshop_id
            WHERE r.deleted = 0 AND r.doc_status != '已作废'
              AND r.inspect_date BETWEEN #{dateBegin} AND #{dateEnd}
              <if test="workshopId != null">AND r.workshop_id = #{workshopId}</if>
              <if test="batchNo != null and batchNo != ''">AND r.batch_no LIKE CONCAT('%', #{batchNo}, '%')</if>
            GROUP BY r.inspect_date, r.workshop_id, w.workshop_name, r.batch_no
            ORDER BY r.inspect_date DESC, r.batch_no
            </script>
            """)
    List<QcDailyVO> qcDaily(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                            @Param("workshopId") Long workshopId, @Param("batchNo") String batchNo);

    /** 日报表指标矩阵：按指标聚合均值/极值/越限数（CAST 兼容 value_str 纵表） */
    @Select("""
            <script>
            SELECT i.indicator_id, ind.indicator_name, ind.unit,
                   COUNT(i.id) value_count, SUM(i.is_over) over_count,
                   AVG(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) avg_value,
                   MIN(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) min_value,
                   MAX(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) max_value
            FROM qc_report_item i
            JOIN qc_report r ON r.id = i.qc_report_id
            JOIN base_qc_indicator ind ON ind.id = i.indicator_id
            WHERE r.deleted = 0 AND r.doc_status != '已作废'
              AND r.inspect_date BETWEEN #{dateBegin} AND #{dateEnd}
              <if test="workshopId != null">AND r.workshop_id = #{workshopId}</if>
            GROUP BY i.indicator_id, ind.indicator_name, ind.unit
            ORDER BY i.indicator_id
            </script>
            """)
    List<IndicatorStatsVO> qcDailyIndicators(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                             @Param("workshopId") Long workshopId);

    // ==================== 2. 疵纱统计表（包装质检 15 类疵纱） ====================

    /** 按入库批次汇总疵纱（含一等品数/合格率，defect_type 明细由 Java 组装为矩阵） */
    @Select("""
            <script>
            SELECT pr.id, pr.warehouse_batch_no, pr.batch_no, w.workshop_name AS workshop_name,
                   qw.workshop_name AS qc_workshop_name, pr.doc_date, pr.first_grade_qty, pr.pass_rate,
                   d.defect_type, COALESCE(SUM(d.value), 0) defect_value
            FROM qc_package_report pr
            LEFT JOIN qc_package_defect d ON d.package_qc_id = pr.id
            LEFT JOIN base_workshop w ON w.id = pr.workshop_id
            LEFT JOIN base_workshop qw ON qw.id = pr.qc_workshop_id
            WHERE pr.deleted = 0
              <if test="dateBegin != null and dateBegin != ''">AND pr.doc_date &gt;= #{dateBegin}</if>
              <if test="dateEnd != null and dateEnd != ''">AND pr.doc_date &lt;= #{dateEnd}</if>
              <if test="workshopId != null">AND pr.workshop_id = #{workshopId}</if>
            GROUP BY pr.id, pr.warehouse_batch_no, pr.batch_no, w.workshop_name, qw.workshop_name,
                     pr.doc_date, pr.first_grade_qty, pr.pass_rate, d.defect_type
            ORDER BY pr.doc_date DESC, pr.warehouse_batch_no
            </script>
            """)
    List<DefectStatsVO.DefectRow> defectRows(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                             @Param("workshopId") Long workshopId);

    /** 疵纱类型合计（全局口径：按类型汇总数量） */
    @Select("""
            <script>
            SELECT d.defect_type, COALESCE(SUM(d.value), 0) defect_value, COUNT(d.id) record_count
            FROM qc_package_defect d
            JOIN qc_package_report pr ON pr.id = d.package_qc_id
            WHERE pr.deleted = 0
              <if test="dateBegin != null and dateBegin != ''">AND pr.doc_date &gt;= #{dateBegin}</if>
              <if test="dateEnd != null and dateEnd != ''">AND pr.doc_date &lt;= #{dateEnd}</if>
              <if test="workshopId != null">AND pr.workshop_id = #{workshopId}</if>
            GROUP BY d.defect_type
            ORDER BY defect_value DESC
            </script>
            """)
    List<DefectStatsVO.DefectRow> defectTypeSummary(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                                    @Param("workshopId") Long workshopId);

    // ==================== 3. 筒子称重产量表（按日+入库批次：箱数/筒数/五列重量合计） ====================

    @Select("""
            <script>
            SELECT DATE(b.box_time) AS box_date, e.warehouse_batch_no, e.pack_batch_no,
                   t.product_spec, COUNT(*) box_count, COALESCE(SUM(b.actual_tube), 0) tube_count,
                   COALESCE(SUM(b.gross_weight), 0) gross_weight, COALESCE(SUM(b.tare_weight), 0) tare_weight,
                   COALESCE(SUM(b.net_weight), 0) net_weight, COALESCE(SUM(b.public_weight), 0) public_weight
            FROM pk_box b
            JOIN pk_execute e ON e.id = b.pk_execute_id
            JOIN pk_task t ON t.id = e.pk_task_id
            WHERE b.deleted = 0
              <if test="dateBegin != null and dateBegin != ''">AND b.box_time &gt;= #{dateBegin}</if>
              <if test="dateEnd != null and dateEnd != ''">AND b.box_time &lt; DATE_ADD(#{dateEnd}, INTERVAL 1 DAY)</if>
              <if test="warehouseBatchNo != null and warehouseBatchNo != ''">AND e.warehouse_batch_no LIKE CONCAT('%', #{warehouseBatchNo}, '%')</if>
            GROUP BY DATE(b.box_time), e.warehouse_batch_no, e.pack_batch_no, t.product_spec
            ORDER BY box_date DESC, e.warehouse_batch_no
            </script>
            """)
    List<TubeWeightOutputVO> tubeWeightOutput(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                              @Param("warehouseBatchNo") String warehouseBatchNo);

    // ==================== 4. 包装批次入库汇总（托数/箱数/公重/入库状态） ====================

    @Select("""
            <script>
            SELECT w.warehouse_batch_no, w.batch_no, w.dye_vat_no,
                   COUNT(DISTINCT w.pallet_code) pallet_count, COUNT(*) box_count,
                   COALESCE(SUM(w.public_weight), 0) public_weight, COALESCE(SUM(w.net_weight), 0) net_weight,
                   MIN(w.in_time) first_in_time, MAX(w.in_time) last_in_time, MAX(w.wms_status) wms_status
            FROM pk_wms_in w
            WHERE 1 = 1
              <if test="warehouseBatchNo != null and warehouseBatchNo != ''">AND w.warehouse_batch_no LIKE CONCAT('%', #{warehouseBatchNo}, '%')</if>
              <if test="dateBegin != null and dateBegin != ''">AND w.in_time &gt;= #{dateBegin}</if>
              <if test="dateEnd != null and dateEnd != ''">AND w.in_time &lt; DATE_ADD(#{dateEnd}, INTERVAL 1 DAY)</if>
            GROUP BY w.warehouse_batch_no, w.batch_no, w.dye_vat_no
            ORDER BY first_in_time DESC
            </script>
            """)
    List<PackInboundVO> packInbound(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                    @Param("warehouseBatchNo") String warehouseBatchNo);

    // ==================== 5. 纺纱车间生产进度跟踪（计划/累计产出/完成率） ====================

    /** 累计产出 = 任务末道报产工序（seq 最大且 is_report=1）的 output_qty，避免多工序相加重复计数 */
    @Select("""
            <script>
            SELECT t.id, t.task_no, t.batch_no, t.product_spec, t.actual_count, t.plan_qty,
                   t.delivery_date, t.is_finished, t.exec_status, w.workshop_name,
                   (SELECT r.output_qty FROM prod_task_route r
                     WHERE r.task_id = t.id AND r.is_report = 1
                     ORDER BY r.seq DESC LIMIT 1) AS output_qty,
                   (SELECT COUNT(*) FROM prod_schedule s
                     WHERE s.task_id = t.id AND s.deleted = 0) AS schedule_count
            FROM prod_task t
            LEFT JOIN base_workshop w ON w.id = t.workshop_id
            WHERE t.deleted = 0
              <if test="workshopId != null">AND t.workshop_id = #{workshopId}</if>
              <if test="batchNo != null and batchNo != ''">AND t.batch_no LIKE CONCAT('%', #{batchNo}, '%')</if>
            ORDER BY t.create_time DESC
            </script>
            """)
    List<ProgressTrackingVO> progressTracking(@Param("workshopId") Long workshopId, @Param("batchNo") String batchNo);

    // ==================== 6/7/8. 产量日报（细毛 1-5 / 前纺 1-6 / 后纺 7-10，同一 SQL 按工序组过滤） ====================

    @Select("""
            <script>
            SELECT o.report_date, t.workshop_id, w.workshop_name, p.process_name,
                   COUNT(*) report_count, COALESCE(SUM(o.output_qty), 0) output_qty,
                   COALESCE(SUM(o.rework_qty), 0) rework_qty, COALESCE(SUM(o.return_wool_qty), 0) return_wool_qty,
                   COALESCE(SUM(o.wool_qty), 0) wool_qty
            FROM rep_output o
            LEFT JOIN prod_task t ON t.id = o.task_id
            LEFT JOIN base_workshop w ON w.id = t.workshop_id
            JOIN base_process p ON p.id = o.process_id
            WHERE o.deleted = 0
              AND o.report_date BETWEEN #{dateBegin} AND #{dateEnd}
              <if test="workshopId != null">AND t.workshop_id = #{workshopId}</if>
              AND o.process_id IN
              <foreach item="pid" collection="processIds" open="(" separator="," close=")">#{pid}</foreach>
            GROUP BY o.report_date, t.workshop_id, w.workshop_name, p.process_name
            ORDER BY o.report_date DESC, p.process_name
            </script>
            """)
    List<OutputDailyVO> outputDaily(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                    @Param("workshopId") Long workshopId, @Param("processIds") List<Long> processIds);

    // ==================== 9. 车间用毛率统计（投料/产出 vs 标准值） ====================

    /** 月度投料量（仅投料完毕封账的单据计入） */
    @Select("""
            SELECT DATE_FORMAT(f.feed_time, '%Y-%m') AS period_month, f.workshop_id,
                   COALESCE(SUM(f.feed_qty), 0) AS feed_qty
            FROM mt_feed f
            WHERE f.is_finish = 1
            GROUP BY DATE_FORMAT(f.feed_time, '%Y-%m'), f.workshop_id
            """)
    List<WoolRateVO.WoolRateRow> feedQtyByMonth();

    /** 月度产出量（后纺工序合计，车间取任务归属） */
    @Select("""
            SELECT DATE_FORMAT(o.report_date, '%Y-%m') AS period_month, t.workshop_id,
                   COALESCE(SUM(o.output_qty), 0) AS output_qty
            FROM rep_output o
            JOIN prod_task t ON t.id = o.task_id
            WHERE o.deleted = 0 AND o.process_id IN (7, 8, 9, 10)
            GROUP BY DATE_FORMAT(o.report_date, '%Y-%m'), t.workshop_id
            """)
    List<WoolRateVO.WoolRateRow> outputQtyByMonth();

    /** 月度回毛量（退回毛料，扣减分子口径的可选项，单列展示） */
    @Select("""
            SELECT DATE_FORMAT(rw.register_date, '%Y-%m') AS period_month, rw.workshop_id,
                   COALESCE(SUM(rw.weight), 0) AS return_wool_qty
            FROM mt_return_wool rw
            GROUP BY DATE_FORMAT(rw.register_date, '%Y-%m'), rw.workshop_id
            """)
    List<WoolRateVO.WoolRateRow> returnWoolByMonth();

    /** 标准用毛率（qc_wool_rate，M6 前无域实现，报表直接读取） */
    @Select("""
            SELECT period_month, workshop_id, standard_rate
            FROM qc_wool_rate
            ORDER BY period_month DESC, workshop_id
            """)
    List<WoolRateVO.StandardRateRow> standardWoolRate();

    // ==================== 10/11. 纱线物理指标统计 / 条干CV统计 ====================

    /** 按指标聚合（全指标矩阵，条件同日报表明细） */
    @Select("""
            <script>
            SELECT i.indicator_id, ind.indicator_name, ind.unit,
                   COUNT(DISTINCT r.id) report_count, COUNT(i.id) value_count, SUM(i.is_over) over_count,
                   AVG(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) avg_value,
                   MIN(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) min_value,
                   MAX(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) max_value
            FROM qc_report_item i
            JOIN qc_report r ON r.id = i.qc_report_id
            JOIN base_qc_indicator ind ON ind.id = i.indicator_id
            WHERE r.deleted = 0 AND r.doc_status != '已作废'
              AND r.inspect_date BETWEEN #{dateBegin} AND #{dateEnd}
              <if test="workshopId != null">AND r.workshop_id = #{workshopId}</if>
              <if test="indicatorId != null">AND i.indicator_id = #{indicatorId}</if>
            GROUP BY i.indicator_id, ind.indicator_name, ind.unit
            ORDER BY i.indicator_id
            </script>
            """)
    List<IndicatorStatsVO> indicatorStats(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                          @Param("workshopId") Long workshopId, @Param("indicatorId") Long indicatorId);

    /** 条干CV专项：按批次+日期明细（指标固定为"条干CV值"） */
    @Select("""
            <script>
            SELECT r.inspect_date, r.batch_no, COUNT(i.id) value_count, SUM(i.is_over) over_count,
                   AVG(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) avg_value,
                   MIN(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) min_value,
                   MAX(CAST(NULLIF(i.value_str, '') AS DECIMAL(12, 4))) max_value
            FROM qc_report_item i
            JOIN qc_report r ON r.id = i.qc_report_id
            WHERE r.deleted = 0 AND r.doc_status != '已作废'
              AND i.indicator_id = #{indicatorId}
              AND r.inspect_date BETWEEN #{dateBegin} AND #{dateEnd}
              <if test="workshopId != null">AND r.workshop_id = #{workshopId}</if>
            GROUP BY r.inspect_date, r.batch_no
            ORDER BY r.inspect_date DESC, r.batch_no
            </script>
            """)
    List<IndicatorStatsVO> cvStatsByBatch(@Param("dateBegin") String dateBegin, @Param("dateEnd") String dateEnd,
                                          @Param("workshopId") Long workshopId, @Param("indicatorId") Long indicatorId);

    /** 按指标编码查指标 id（条干CV值等专项报表用） */
    @Select("SELECT id FROM base_qc_indicator WHERE indicator_name = #{name} AND deleted = 0 LIMIT 1")
    Long findIndicatorIdByName(@Param("name") String name);

    /** 车间名映射（用毛率等报表补齐车间名） */
    @Select("SELECT id AS workshop_id, workshop_name FROM base_workshop")
    List<WoolRateVO.StandardRateRow> workshopNames();

    // ==================== 12. 原料检验汇总（中心实验室，17 项原料指标台账） ====================

    @Select("""
            <script>
            SELECT q.id, c.craft_no, c.batch_no, q.sample_batch, q.sample_product,
                   q.weight_per_meter, q.uneven_rate, q.neps_50g, q.impurity, q.moisture,
                   q.wool_piece, q.alien_color, q.alien_fiber,
                   q.neps_large, q.neps_mid, q.neps_small, q.oil, q.grass_50g,
                   q.length, q.dispersion
            FROM plan_craft_raw_qc q
            JOIN plan_craft c ON c.id = q.plan_craft_id
            WHERE c.deleted = 0
              <if test="batchNo != null and batchNo != ''">AND c.batch_no LIKE CONCAT('%', #{batchNo}, '%')</if>
            ORDER BY q.id DESC
            </script>
            """)
    List<RawQcSummaryVO> rawQcSummary(@Param("batchNo") String batchNo);
}
