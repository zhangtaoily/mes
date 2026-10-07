package com.xinao.mes.qc.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.engine.flow.FlowEngine;
import com.xinao.mes.engine.meta.entity.BaseQcIndicator;
import com.xinao.mes.engine.meta.mapper.BaseQcIndicatorMapper;
import com.xinao.mes.engine.script.FormulaResult;
import com.xinao.mes.engine.script.FormulaService;
import com.xinao.mes.engine.script.entity.QcFormula;
import com.xinao.mes.engine.script.mapper.QcFormulaMapper;
import com.xinao.mes.qc.domain.model.BaseQcScheme;
import com.xinao.mes.qc.domain.model.BaseQcSchemeItem;
import com.xinao.mes.qc.domain.model.QcReport;
import com.xinao.mes.qc.domain.model.QcReportItem;
import com.xinao.mes.qc.dto.QcReportQueryDTO;
import com.xinao.mes.qc.dto.QcReportSaveDTO;
import com.xinao.mes.qc.infrastructure.mapper.BaseQcSchemeItemMapper;
import com.xinao.mes.qc.infrastructure.mapper.BaseQcSchemeMapper;
import com.xinao.mes.qc.infrastructure.mapper.QcReportItemMapper;
import com.xinao.mes.qc.infrastructure.mapper.QcReportMapper;
import com.xinao.mes.qc.vo.QcReportDetailVO;
import com.xinao.mes.qc.vo.QcStatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 检验报告应用服务
 *
 * <p>M4 核心链路：选方案生成指标矩阵 → 录入试样值 → 公式计算（script 引擎 $BQ 取试样值集合，
 * 衍生指标如不匀率）→ 越限判定（含衍生指标，超上限/低于下限置 is_over）→ 提交 → 公示。
 * 不变量：作废报告不计入统计且可恢复（doc_status=已作废 + 取消作废回已提交）。
 */
@Service
@RequiredArgsConstructor
public class QcReportAppService {

    private static final String BILL_TYPE = "qc_report";

    private final QcReportMapper reportMapper;
    private final QcReportItemMapper itemMapper;
    private final BaseQcSchemeMapper schemeMapper;
    private final BaseQcSchemeItemMapper schemeItemMapper;
    private final BaseQcIndicatorMapper indicatorMapper;
    private final QcFormulaMapper formulaMapper;
    private final FormulaService formulaService;
    private final CodeEngine codeEngine;
    private final FlowEngine flowEngine;

    public PageResult<QcReport> page(QcReportQueryDTO query) {
        Page<QcReport> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<QcReport> result = reportMapper.selectPage(p, Wrappers.<QcReport>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getQcNo()), QcReport::getQcNo, query.getQcNo())
                .like(StringUtils.isNotBlank(query.getBatchNo()), QcReport::getBatchNo, query.getBatchNo())
                .eq(query.getWorkshopId() != null, QcReport::getWorkshopId, query.getWorkshopId())
                .eq(StringUtils.isNotBlank(query.getQcType()), QcReport::getQcType, query.getQcType())
                .eq(StringUtils.isNotBlank(query.getDocStatus()), QcReport::getDocStatus, query.getDocStatus())
                .eq(Boolean.TRUE.equals(query.getPublicOnly()), QcReport::getIsPublic, 1)
                .orderByDesc(QcReport::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 详情：主表 + 指标矩阵（含指标名/单位/越限标志） */
    public QcReportDetailVO detail(Long id) {
        QcReport report = mustGet(id);
        QcReportDetailVO vo = new QcReportDetailVO();
        vo.setReport(report);
        List<QcReportItem> items = itemsOf(id);
        Map<Long, BaseQcIndicator> indicators = indicatorsOf(items);
        vo.setItems(items.stream().map(i -> {
            QcReportDetailVO.ItemVO item = new QcReportDetailVO.ItemVO();
            item.setIndicatorId(i.getIndicatorId());
            BaseQcIndicator ind = indicators.get(i.getIndicatorId());
            if (ind != null) {
                item.setIndicatorCode(ind.getIndicatorCode());
                item.setIndicatorName(ind.getIndicatorName());
                item.setUnit(ind.getUnit());
                item.setIndicatorType(ind.getIndicatorType());
            }
            item.setSampleNo(i.getSampleNo());
            item.setValueStr(i.getValueStr());
            item.setIsOver(i.getIsOver());
            item.setStandardValue(i.getStandardValue());
            item.setUpperLimit(i.getUpperLimit());
            item.setLowerLimit(i.getLowerLimit());
            return item;
        }).toList());
        return vo;
    }

    /** 保存：新建按方案生成指标矩阵；编辑仅编制中可改，试样值按 指标+试样号 定位更新 */
    @Transactional
    public Long save(QcReportSaveDTO dto) {
        QcReport report;
        if (dto.getId() == null) {
            report = new QcReport();
            copyMain(dto, report);
            if (report.getInspector() == null || report.getInspector().isBlank()) {
                throw new BizException("检验员不能为空");
            }
            if (report.getInspectDate() == null) {
                throw new BizException("检验日期不能为空");
            }
            report.setQcNo(codeEngine.next("检验报告", dto.getBatchNo()));
            report.setDocStatus(BillStatus.DRAFT);
            report.setIsPublic(0);
            if (report.getLabType() == null) {
                report.setLabType(0);
            }
            reportMapper.insert(report);
            if (report.getSchemeId() != null) {
                generateMatrix(report);
            }
        } else {
            report = mustGet(dto.getId());
            report.ensureEditable();
            copyMain(dto, report);
            reportMapper.updateById(report);
        }
        // 试样值更新（保留矩阵结构与方案上下限快照）
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            Map<String, QcReportItem> idx = itemsOf(report.getId()).stream()
                    .collect(Collectors.toMap(i -> i.getIndicatorId() + "-" + i.getSampleNo(), i -> i, (a, b) -> a));
            for (QcReportSaveDTO.ItemVal v : dto.getItems()) {
                QcReportItem item = idx.get(v.getIndicatorId() + "-" + v.getSampleNo());
                if (item != null) {
                    item.setValueStr(v.getValueStr());
                    item.setIsOver(judgeOver(item));
                    itemMapper.updateById(item);
                }
            }
        }
        return report.getId();
    }

    /**
     * 公式计算（script 引擎在此上线）：
     * $BQ(指标ID) 取该指标全部试样值集合 → 计算衍生指标（如不匀率=极差/均值）→ 写回矩阵；
     * 随后对全部指标值（含衍生）按方案上下限判定越限。仅执行目标指标存在于本报告矩阵的公式。
     */
    @Transactional
    public QcReportDetailVO calc(Long id) {
        QcReport report = mustGet(id);
        report.ensureEditable();
        List<QcReportItem> items = itemsOf(id);
        if (items.isEmpty()) {
            throw new BizException("指标矩阵为空，请先选择质检方案");
        }
        // 1. 公式计算：values = 指标ID → 试样值集合
        Map<Object, Object> values = new HashMap<>();
        items.stream()
                .collect(Collectors.groupingBy(QcReportItem::getIndicatorId))
                .forEach((indicatorId, list) -> values.put(indicatorId,
                        list.stream().map(i -> i.getValueStr() == null ? "" : i.getValueStr()).toList()));
        Set<Long> matrixIndicators = items.stream().map(QcReportItem::getIndicatorId).collect(Collectors.toSet());
        List<QcFormula> formulas = formulaMapper.selectList(Wrappers.<QcFormula>lambdaQuery()
                .orderByAsc(QcFormula::getSortNo));
        for (QcFormula formula : formulas) {
            if (!matrixIndicators.contains(formula.getTargetIndicatorId())) {
                continue;
            }
            FormulaResult r = formulaService.evalById(formula.getId(), values);
            if (!r.isSuccess()) {
                throw new BizException("公式[" + formula.getFormulaName() + "]执行失败: " + r.getError());
            }
            if (r.getValue() != null) {
                writeBackDerived(id, formula.getTargetIndicatorId(), String.valueOf(r.getValue()));
            }
        }
        // 2. 越限判定（全部指标值含衍生）
        for (QcReportItem item : itemsOf(id)) {
            item.setIsOver(judgeOver(item));
            itemMapper.updateById(item);
        }
        return detail(id);
    }

    /** 提交（flow：编制中→已提交） */
    @Transactional
    public void submit(Long id) {
        QcReport report = mustGet(id);
        report.ensureEditable();
        if (itemsOf(id).isEmpty()) {
            throw new BizException("指标矩阵为空，请先选择质检方案");
        }
        report.setDocStatus(flowEngine.fire(BILL_TYPE, report.getDocStatus(), "submit", roles()));
        reportMapper.updateById(report);
    }

    /** 作废（不计入统计；公示中的报告同时取消公示） */
    @Transactional
    public void voidReport(Long id) {
        QcReport report = mustGet(id);
        report.ensureVoidable(true);
        report.setDocStatus(flowEngine.fire(BILL_TYPE, report.getDocStatus(), "void", roles()));
        if (Integer.valueOf(1).equals(report.getIsPublic())) {
            report.setIsPublic(0);
        }
        reportMapper.updateById(report);
    }

    /** 取消作废（恢复为已提交，重新计入统计） */
    @Transactional
    public void unvoid(Long id) {
        QcReport report = mustGet(id);
        report.ensureVoidable(false);
        report.setDocStatus(flowEngine.fire(BILL_TYPE, report.getDocStatus(), "unvoid", roles()));
        reportMapper.updateById(report);
    }

    /** 公示（仅已提交可公示，对外公示的检验数据） */
    @Transactional
    public void publicize(Long id) {
        QcReport report = mustGet(id);
        if (!BillStatus.SUBMITTED.equals(report.getDocStatus())) {
            throw new BizException("检验报告[" + report.getQcNo() + "]仅[已提交]可公示，当前: " + report.getDocStatus());
        }
        report.setIsPublic(1);
        reportMapper.updateById(report);
    }

    /** 统计（不变量：作废报告不计入；不匀率均值为衍生指标参与统计的证明） */
    public QcStatisticsVO statistics(Long workshopId) {
        QcStatisticsVO vo = new QcStatisticsVO();
        vo.setTotal(reportMapper.selectCount(Wrappers.<QcReport>lambdaQuery()
                .eq(workshopId != null, QcReport::getWorkshopId, workshopId)
                .ne(QcReport::getDocStatus, BillStatus.VOIDED)));
        vo.setSubmitted(reportMapper.selectCount(Wrappers.<QcReport>lambdaQuery()
                .eq(workshopId != null, QcReport::getWorkshopId, workshopId)
                .eq(QcReport::getDocStatus, BillStatus.SUBMITTED)));
        vo.setVoided(reportMapper.selectCount(Wrappers.<QcReport>lambdaQuery()
                .eq(workshopId != null, QcReport::getWorkshopId, workshopId)
                .eq(QcReport::getDocStatus, BillStatus.VOIDED)));
        List<Long> validIds = reportMapper.selectList(Wrappers.<QcReport>lambdaQuery()
                        .eq(workshopId != null, QcReport::getWorkshopId, workshopId)
                        .ne(QcReport::getDocStatus, BillStatus.VOIDED))
                .stream().map(QcReport::getId).toList();
        if (validIds.isEmpty()) {
            vo.setOverLimitCount(0L);
            vo.setAvgByl(BigDecimal.ZERO);
            return vo;
        }
        List<QcReportItem> items = itemMapper.selectList(Wrappers.<QcReportItem>lambdaQuery()
                .in(QcReportItem::getQcReportId, validIds));
        vo.setOverLimitCount(items.stream().filter(i -> Integer.valueOf(1).equals(i.getIsOver())).count());
        List<BigDecimal> byls = items.stream()
                .filter(i -> Long.valueOf(6L).equals(i.getIndicatorId()))
                .map(i -> toDecimal(i.getValueStr())).filter(v -> v != null).toList();
        vo.setAvgByl(byls.isEmpty() ? BigDecimal.ZERO
                : byls.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(byls.size()), 2, RoundingMode.HALF_UP));
        return vo;
    }

    // ==================== 私有方法 ====================

    private void generateMatrix(QcReport report) {
        BaseQcScheme scheme = schemeMapper.selectById(report.getSchemeId());
        if (scheme == null) {
            throw new BizException("质检方案不存在: " + report.getSchemeId());
        }
        List<BaseQcSchemeItem> schemeItems = schemeItemMapper.selectList(Wrappers.<BaseQcSchemeItem>lambdaQuery()
                .eq(BaseQcSchemeItem::getSchemeId, scheme.getId())
                .orderByAsc(BaseQcSchemeItem::getSortNo));
        if (schemeItems.isEmpty()) {
            throw new BizException("质检方案[" + scheme.getSchemeName() + "]未配置指标项");
        }
        int sampleCount = scheme.getSampleCount() == null || scheme.getSampleCount() < 1 ? 1 : scheme.getSampleCount();
        for (BaseQcSchemeItem si : schemeItems) {
            for (int n = 1; n <= sampleCount; n++) {
                QcReportItem item = new QcReportItem();
                item.setQcReportId(report.getId());
                item.setIndicatorId(si.getIndicatorId());
                item.setSampleNo(n);
                item.setIsOver(0);
                item.setStandardValue(si.getStandardValue());
                item.setUpperLimit(si.getUpperLimit());
                item.setLowerLimit(si.getLowerLimit());
                itemMapper.insert(item);
            }
        }
    }

    /** 衍生指标写回：矩阵已有该指标行则更新首个试样行，否则插入（sampleNo=1） */
    private void writeBackDerived(Long reportId, Long targetIndicatorId, String value) {
        QcReportItem first = itemMapper.selectList(Wrappers.<QcReportItem>lambdaQuery()
                        .eq(QcReportItem::getQcReportId, reportId)
                        .eq(QcReportItem::getIndicatorId, targetIndicatorId)
                        .orderByAsc(QcReportItem::getSampleNo))
                .stream().findFirst().orElse(null);
        if (first == null) {
            first = new QcReportItem();
            first.setQcReportId(reportId);
            first.setIndicatorId(targetIndicatorId);
            first.setSampleNo(1);
            first.setIsOver(0);
            first.setValueStr(value);
            itemMapper.insert(first);
        } else {
            first.setValueStr(value);
            itemMapper.updateById(first);
        }
    }

    /** 越限判定：超上限或低于下限（无法数值化的值不判越限） */
    private Integer judgeOver(QcReportItem item) {
        BigDecimal val = toDecimal(item.getValueStr());
        if (val == null) {
            return 0;
        }
        BigDecimal upper = toDecimal(item.getUpperLimit());
        if (upper != null && val.compareTo(upper) > 0) {
            return 1;
        }
        BigDecimal lower = toDecimal(item.getLowerLimit());
        if (lower != null && val.compareTo(lower) < 0) {
            return 1;
        }
        return 0;
    }

    private BigDecimal toDecimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<QcReportItem> itemsOf(Long reportId) {
        return itemMapper.selectList(Wrappers.<QcReportItem>lambdaQuery()
                .eq(QcReportItem::getQcReportId, reportId)
                .orderByAsc(QcReportItem::getIndicatorId).orderByAsc(QcReportItem::getSampleNo));
    }

    private Map<Long, BaseQcIndicator> indicatorsOf(List<QcReportItem> items) {
        List<Long> ids = items.stream().map(QcReportItem::getIndicatorId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return indicatorMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(BaseQcIndicator::getId, i -> i));
    }

    private void copyMain(QcReportSaveDTO dto, QcReport report) {
        report.setWorkshopId(dto.getWorkshopId());
        report.setProdLineId(dto.getProdLineId());
        report.setQcType(dto.getQcType());
        report.setLabType(dto.getLabType());
        report.setEquipmentId(dto.getEquipmentId());
        report.setTeamId(dto.getTeamId());
        report.setBatchNo(dto.getBatchNo());
        report.setDyeVatNo(dto.getDyeVatNo());
        report.setInspector(dto.getInspector());
        report.setInspectDate(dto.getInspectDate());
        report.setSampleQty(dto.getSampleQty());
        report.setRetainQty(dto.getRetainQty());
        report.setSchemeId(dto.getSchemeId());
        report.setRawMaterial(dto.getRawMaterial());
        report.setSpinCountDrop(dto.getSpinCountDrop());
        report.setSpinCount(dto.getSpinCount());
        report.setRawBatchNo(dto.getRawBatchNo());
        report.setMergeFeedQty(dto.getMergeFeedQty());
        report.setRemark(dto.getRemark());
    }

    private QcReport mustGet(Long id) {
        QcReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException("检验报告不存在: " + id);
        }
        return report;
    }

    private Set<String> roles() {
        var user = AuthContext.get();
        return user == null || user.getRoles() == null ? Set.of() : user.getRoles();
    }
}
