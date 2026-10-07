package com.xinao.mes.pack.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.script.FormulaResult;
import com.xinao.mes.engine.script.FormulaService;
import com.xinao.mes.engine.script.entity.QcFormula;
import com.xinao.mes.engine.script.mapper.QcFormulaMapper;
import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkBoxDelete;
import com.xinao.mes.pack.domain.model.PkExecute;
import com.xinao.mes.pack.dto.BoxSaveDTO;
import com.xinao.mes.pack.dto.PkExecuteQueryDTO;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxDeleteMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkExecuteMapper;
import com.xinao.mes.pack.vo.PkExecuteDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 包装执行应用服务（逐箱称重）
 *
 * <p>M4 五列重量恒等式（聚合校验）：
 * <ul>
 *   <li>毛重 = 去皮重量 + 净重（容差 0.01，不成立拒绝称重）</li>
 *   <li>净→公重按回潮公式：公重 = 净重×(1+公定回潮)/(1+实测回潮)，
 *       公式存 qc_formula（formula_type=包装称重）外置脚本，口径变化改配置不发版</li>
 * </ul>
 * 删箱审计留痕（pk_box_delete）+ 汇总量随箱子增删实时重算；了包后不可再称重/删箱。
 */
@Service
@RequiredArgsConstructor
public class PkExecuteAppService {

    /** 毛=皮+净 容差 */
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final PkExecuteMapper executeMapper;
    private final PkBoxMapper boxMapper;
    private final PkBoxDeleteMapper boxDeleteMapper;
    private final QcFormulaMapper formulaMapper;
    private final FormulaService formulaService;
    private final CodeEngine codeEngine;

    public PageResult<PkExecute> page(PkExecuteQueryDTO query) {
        Page<PkExecute> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<PkExecute> result = executeMapper.selectPage(p, Wrappers.<PkExecute>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getExecuteNo()), PkExecute::getExecuteNo, query.getExecuteNo())
                .like(StringUtils.isNotBlank(query.getPackBatchNo()), PkExecute::getPackBatchNo, query.getPackBatchNo())
                .like(StringUtils.isNotBlank(query.getWarehouseBatchNo()), PkExecute::getWarehouseBatchNo, query.getWarehouseBatchNo())
                .eq(query.getPkTaskId() != null, PkExecute::getPkTaskId, query.getPkTaskId())
                .orderByDesc(PkExecute::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 详情：主表 + 箱子明细（逐箱称重记录） */
    public PkExecuteDetailVO detail(Long id) {
        PkExecute execute = mustGet(id);
        PkExecuteDetailVO vo = new PkExecuteDetailVO();
        vo.setExecute(execute);
        vo.setBoxes(boxesOf(id));
        return vo;
    }

    /** 逐箱称重：恒等式校验 + 公重公式换算 + 汇总重算 */
    @Transactional
    public PkBox addBox(BoxSaveDTO dto) {
        PkExecute execute = mustGet(dto.getExecuteId());
        if (Integer.valueOf(1).equals(execute.getIsPacked())) {
            throw new BizException("包装执行[" + execute.getExecuteNo() + "]已了包，不可再称重");
        }
        if (dto.getNetWeight() == null || dto.getTareWeight() == null || dto.getGrossWeight() == null) {
            throw new BizException("净重/去皮重量/毛重必填");
        }
        // 不变量1：毛重 = 去皮 + 净重（容差 0.01）
        BigDecimal expect = dto.getTareWeight().add(dto.getNetWeight());
        if (dto.getGrossWeight().subtract(expect).abs().compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new BizException("五列重量恒等式不成立：毛重" + dto.getGrossWeight()
                    + " ≠ 去皮" + dto.getTareWeight() + " + 净重" + dto.getNetWeight() + "（容差0.01）");
        }
        // 不变量2：净→公重按回潮公式（script 外置，编辑即生效）
        BigDecimal publicWeight = calcPublicWeight(execute, dto.getNetWeight());

        PkBox box = new PkBox();
        box.setPkExecuteId(execute.getId());
        box.setBoxCode(codeEngine.next("包装箱", execute.getWarehouseBatchNo()));
        box.setBoxStatus("新建");
        box.setIsStockNotice(0);
        box.setSeqNo(boxesOf(execute.getId()).size() + 1);
        box.setGrainCount(dto.getGrainCount());
        box.setPublicWeight(publicWeight);
        box.setNetWeight(dto.getNetWeight());
        box.setGrossWeight(dto.getGrossWeight());
        box.setTareWeight(dto.getTareWeight());
        box.setPackSpec(dto.getPackSpec());
        box.setTieLineColor(dto.getTieLineColor());
        box.setLineType(dto.getLineType());
        box.setMachineCode(dto.getMachineCode());
        box.setPackUser(dto.getPackUser());
        box.setBoxTime(LocalDateTime.now());
        box.setActualTube(dto.getActualTube());
        boxMapper.insert(box);
        refreshSummary(execute.getId());
        return box;
    }

    /** 删箱（审计留痕 + 汇总回退；了包后不可删） */
    @Transactional
    public void deleteBox(Long boxId) {
        PkBox box = boxMapper.selectById(boxId);
        if (box == null) {
            throw new BizException("箱子不存在: " + boxId);
        }
        PkExecute execute = mustGet(box.getPkExecuteId());
        if (Integer.valueOf(1).equals(execute.getIsPacked())) {
            throw new BizException("包装执行[" + execute.getExecuteNo() + "]已了包，不可删箱");
        }
        PkBoxDelete audit = new PkBoxDelete();
        audit.setBoxCode(box.getBoxCode());
        audit.setPalletCode(box.getPalletCode());
        audit.setSeqNo(box.getSeqNo());
        audit.setTareWeight(box.getTareWeight());
        audit.setGrossWeight(box.getGrossWeight());
        audit.setTubeCount(box.getActualTube());
        audit.setDeleteUser(currentUser());
        audit.setDeleteTime(LocalDateTime.now());
        boxDeleteMapper.insert(audit);
        boxMapper.deleteById(boxId);
        refreshSummary(execute.getId());
    }

    /** 了包（至少一箱才可了包） */
    @Transactional
    public void finish(Long executeId) {
        PkExecute execute = mustGet(executeId);
        if (boxesOf(executeId).isEmpty()) {
            throw new BizException("包装执行[" + execute.getExecuteNo() + "]无箱子明细，不可了包");
        }
        execute.setIsPacked(1);
        executeMapper.updateById(execute);
    }

    // ==================== 私有方法 ====================

    /** 公重公式换算（qc_formula formula_type=包装称重 外置脚本） */
    private BigDecimal calcPublicWeight(PkExecute execute, BigDecimal netWeight) {
        QcFormula formula = formulaMapper.selectList(Wrappers.<QcFormula>lambdaQuery()
                        .eq(QcFormula::getFormulaType, "包装称重")
                        .orderByAsc(QcFormula::getSortNo))
                .stream().findFirst().orElse(null);
        if (formula == null) {
            throw new BizException("公重换算公式未配置（qc_formula formula_type=包装称重）");
        }
        Map<Object, Object> values = new HashMap<>();
        values.put("net", netWeight);
        values.put("ms", execute.getMoistureStandard());
        values.put("ma", execute.getMoistureActual());
        FormulaResult r = formulaService.evalById(formula.getId(), values);
        if (!r.isSuccess() || r.getValue() == null) {
            throw new BizException("公重公式[" + formula.getFormulaName() + "]执行失败: "
                    + (r.isSuccess() ? "无返回值" : r.getError()));
        }
        return new BigDecimal(String.valueOf(r.getValue()));
    }

    /** 汇总重算：公重/毛重/净重/筒子数总和 + 包装箱数 */
    private void refreshSummary(Long executeId) {
        List<PkBox> boxes = boxesOf(executeId);
        PkExecute execute = mustGet(executeId);
        execute.setSumPublicWeight(boxes.stream().map(PkBox::getPublicWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        execute.setSumGrossWeight(boxes.stream().map(PkBox::getGrossWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        execute.setSumNetWeight(boxes.stream().map(PkBox::getNetWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        execute.setSumTube(boxes.stream().map(b -> b.getActualTube() == null ? 0 : b.getActualTube())
                .reduce(0, Integer::sum));
        execute.setBoxCount(boxes.size());
        executeMapper.updateById(execute);
    }

    private List<PkBox> boxesOf(Long executeId) {
        return boxMapper.selectList(Wrappers.<PkBox>lambdaQuery()
                .eq(PkBox::getPkExecuteId, executeId)
                .orderByAsc(PkBox::getSeqNo));
    }

    private PkExecute mustGet(Long id) {
        PkExecute execute = executeMapper.selectById(id);
        if (execute == null) {
            throw new BizException("包装执行不存在: " + id);
        }
        return execute;
    }

    private String currentUser() {
        var user = AuthContext.get();
        return user == null ? "system" : user.getUserName();
    }
}
