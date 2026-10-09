package com.xinao.mes.qc.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.engine.script.entity.QcFormula;
import com.xinao.mes.engine.script.mapper.QcFormulaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 质检公式（JS 脚本配置化，$BQ 取值计算衍生指标，热生效无需重启） */
@RestController
@RequestMapping("/api/qc/formulas")
@RequiredArgsConstructor
public class QcFormulaController {

    private final QcFormulaMapper formulaMapper;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("qc:formula:list")
    public R<PageResult<QcFormula>> page(@RequestParam(required = false) String formulaName,
                                         @RequestParam(required = false) String formulaType,
                                         @RequestParam(required = false) Long processId,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<QcFormula> p = formulaMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<QcFormula>lambdaQuery()
                        .like(StringUtils.isNotBlank(formulaName), QcFormula::getFormulaName, formulaName)
                        .eq(StringUtils.isNotBlank(formulaType), QcFormula::getFormulaType, formulaType)
                        .eq(processId != null, QcFormula::getProcessId, processId)
                        .orderByAsc(QcFormula::getSortNo).orderByAsc(QcFormula::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 全量下拉（指标配置用） */
    @GetMapping("/all")
    @RequiresPerm("qc:formula:list")
    public R<List<QcFormula>> all() {
        return R.ok(formulaMapper.selectList(Wrappers.<QcFormula>lambdaQuery()
                .orderByAsc(QcFormula::getSortNo).orderByAsc(QcFormula::getId)));
    }

    /** 新增 */
    @PostMapping
    @RequiresPerm("qc:formula:list")
    @OperLog(title = "新增质检公式")
    public R<Long> save(@RequestBody QcFormula formula) {
        checkScript(formula);
        if (StringUtils.isBlank(formula.getFormulaName())) {
            throw new BizException("公式名称不能为空");
        }
        formulaMapper.insert(formula);
        return R.ok(formula.getId());
    }

    /** 修改 */
    @PutMapping("/{id}")
    @RequiresPerm("qc:formula:list")
    @OperLog(title = "修改质检公式")
    public R<Void> update(@PathVariable Long id, @RequestBody QcFormula formula) {
        checkScript(formula);
        formula.setId(id);
        formulaMapper.updateById(formula);
        return R.ok();
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @RequiresPerm("qc:formula:list")
    @OperLog(title = "删除质检公式")
    public R<Void> delete(@PathVariable Long id) {
        formulaMapper.deleteById(id);
        return R.ok();
    }

    private void checkScript(QcFormula formula) {
        if (StringUtils.isBlank(formula.getScript())) {
            throw new BizException("公式脚本不能为空");
        }
        if (formula.getTargetIndicatorId() == null) {
            throw new BizException("请选择目标指标");
        }
    }
}
