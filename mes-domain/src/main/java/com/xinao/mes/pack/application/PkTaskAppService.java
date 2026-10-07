package com.xinao.mes.pack.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.pack.domain.model.PkExecute;
import com.xinao.mes.pack.domain.model.PkLabelReprint;
import com.xinao.mes.pack.domain.model.PkTask;
import com.xinao.mes.pack.domain.model.PkTubePrint;
import com.xinao.mes.pack.dto.PkTaskQueryDTO;
import com.xinao.mes.pack.infrastructure.mapper.PkExecuteMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkLabelReprintMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkTaskMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkTubePrintMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 包装任务应用服务（含倒毛包装任务；执行生成包装执行单，打印留痕，了批/缸号了批批量操作） */
@Service
@RequiredArgsConstructor
public class PkTaskAppService {

    private final PkTaskMapper taskMapper;
    private final PkExecuteMapper executeMapper;
    private final PkTubePrintMapper tubePrintMapper;
    private final PkLabelReprintMapper labelReprintMapper;
    private final CodeEngine codeEngine;

    public PageResult<PkTask> page(PkTaskQueryDTO query) {
        Page<PkTask> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<PkTask> result = taskMapper.selectPage(p, Wrappers.<PkTask>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getTaskNo()), PkTask::getTaskNo, query.getTaskNo())
                .like(StringUtils.isNotBlank(query.getBatchNo()), PkTask::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getWarehouseBatchNo()), PkTask::getWarehouseBatchNo, query.getWarehouseBatchNo())
                .eq(StringUtils.isNotBlank(query.getTaskVariant()), PkTask::getTaskVariant, query.getTaskVariant())
                .eq(query.getIsFinished() != null, PkTask::getIsFinished, query.getIsFinished())
                .orderByDesc(PkTask::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public PkTask detail(Long id) {
        PkTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("包装任务不存在: " + id);
        }
        return task;
    }

    /** 保存（新建生成任务编号 PS...；了批任务不可改） */
    @Transactional
    public Long save(PkTask task) {
        if (StringUtils.isBlank(task.getWarehouseBatchNo())) {
            throw new BizException("入库批号不能为空");
        }
        if (StringUtils.isBlank(task.getWeightMode())) {
            throw new BizException("计重方式不能为空（标重交货/实磅交货）");
        }
        if (task.getId() == null) {
            if (task.getTaskVariant() == null) {
                task.setTaskVariant("普通");
            }
            task.setTaskNo(codeEngine.next("包装任务", task.getWarehouseBatchNo()));
            task.setIsFinished(0);
            task.setVatFinished(0);
            taskMapper.insert(task);
        } else {
            PkTask old = detail(task.getId());
            if (Integer.valueOf(1).equals(old.getIsFinished())) {
                throw new BizException("包装任务[" + old.getTaskNo() + "]已了批，不可修改");
            }
            task.setTaskNo(old.getTaskNo());
            task.setIsFinished(old.getIsFinished());
            task.setVatFinished(old.getVatFinished());
            taskMapper.updateById(task);
        }
        return task.getId();
    }

    /** 执行：生成包装执行单（产品信息/回潮/筒子数从任务带出） */
    @Transactional
    public Long execute(Long taskId) {
        PkTask task = detail(taskId);
        if (Integer.valueOf(1).equals(task.getIsFinished())) {
            throw new BizException("包装任务[" + task.getTaskNo() + "]已了批，不可执行");
        }
        PkExecute execute = new PkExecute();
        execute.setExecuteNo(codeEngine.next("包装执行", task.getWarehouseBatchNo()));
        execute.setPkTaskId(task.getId());
        execute.setWarehouseBatchNo(task.getWarehouseBatchNo());
        execute.setDyeVatNo(task.getDyeVatNo());
        execute.setProductName(task.getProductName());
        execute.setNameEn(task.getNameEn());
        execute.setRawMaterial(task.getRawMaterial());
        execute.setColorNo(task.getColorNo());
        execute.setProductSpec(task.getProductSpec());
        execute.setPackRequire(task.getPackRequire());
        execute.setTubeCount(task.getTubeCount());
        execute.setStandardWeight(task.getGrossWeight());
        execute.setMoistureStandard(task.getMoistureStandard());
        execute.setMoistureActual(task.getMoistureActual());
        execute.setIsPacked(0);
        execute.setSumPublicWeight(BigDecimal.ZERO);
        execute.setSumGrossWeight(BigDecimal.ZERO);
        execute.setSumNetWeight(BigDecimal.ZERO);
        execute.setSumTube(0);
        execute.setBoxCount(0);
        executeMapper.insert(execute);
        return execute.getId();
    }

    /** 任务了批（批量） */
    @Transactional
    public void finish(List<Long> ids) {
        finishFlag(ids, true, false);
    }

    /** 取消任务了批（批量） */
    @Transactional
    public void cancelFinish(List<Long> ids) {
        finishFlag(ids, false, false);
    }

    /** 缸号了批（批量） */
    @Transactional
    public void finishVat(List<Long> ids) {
        finishFlag(ids, true, true);
    }

    /** 取消缸号了批（批量） */
    @Transactional
    public void cancelFinishVat(List<Long> ids) {
        finishFlag(ids, false, true);
    }

    /** 筒贴打印（留痕 pk_tube_print：包装人员/数量可追溯） */
    @Transactional
    public void printTube(Long taskId, Integer qty) {
        PkTask task = detail(taskId);
        PkTubePrint print = new PkTubePrint();
        print.setPackWorkshopId(task.getPackWorkshopId());
        print.setBatchNo(task.getBatchNo());
        print.setWarehouseBatchNo(task.getWarehouseBatchNo());
        print.setDyeVatNo(task.getDyeVatNo());
        print.setProductSpec(task.getProductSpec());
        print.setQty(qty == null || qty < 1 ? 1 : qty);
        print.setPrintDate(LocalDate.now());
        tubePrintMapper.insert(print);
    }

    /** 箱单/箱贴打印留痕（pk_label_reprint，doc_no=FG...） */
    @Transactional
    public void printBox(Long taskId, String bizType) {
        PkTask task = detail(taskId);
        PkLabelReprint reprint = new PkLabelReprint();
        reprint.setDocNo(codeEngine.next("箱贴补打", task.getWarehouseBatchNo()));
        reprint.setPkTaskId(task.getId());
        reprint.setBatchNo(task.getBatchNo());
        reprint.setK3OrderNo(task.getK3OrderNo());
        reprint.setDyeVatNo(task.getDyeVatNo());
        reprint.setWarehouseBatchNo(task.getWarehouseBatchNo());
        reprint.setProductName(task.getProductName());
        reprint.setNameEn(task.getNameEn());
        reprint.setProductSpec(task.getProductSpec());
        reprint.setCustomerName(task.getCustomerName());
        reprint.setDocDate(LocalDate.now());
        reprint.setBizType(StringUtils.isBlank(bizType) ? "箱单打印" : bizType);
        reprint.setRawDesc(task.getRawMaterial());
        reprint.setCustomerBatchNo(task.getCustomerBatchNo());
        reprint.setPrintTime(LocalDateTime.now());
        labelReprintMapper.insert(reprint);
    }

    private void finishFlag(List<Long> ids, boolean finish, boolean vat) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择包装任务");
        }
        for (Long id : ids) {
            PkTask task = detail(id);
            if (vat) {
                task.setVatFinished(finish ? 1 : 0);
                task.setVatFinishTime(finish ? LocalDateTime.now() : null);
            } else {
                task.setIsFinished(finish ? 1 : 0);
                task.setFinishTime(finish ? LocalDateTime.now() : null);
            }
            taskMapper.updateById(task);
        }
    }
}
