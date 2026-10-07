package com.xinao.mes.plan.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.BaseBatchFlag;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.BaseProdLine;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.infrastructure.mapper.BaseBatchFlagMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProdLineMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** plan 域基础数据下拉（车间/产线/工序/标识牌/机台） */
@RestController
@RequestMapping("/api/plan/base")
@RequiredArgsConstructor
public class PlanBaseController {

    private final BaseWorkshopMapper workshopMapper;
    private final BaseProdLineMapper prodLineMapper;
    private final BaseProcessMapper processMapper;
    private final BaseBatchFlagMapper batchFlagMapper;
    private final BaseEquipmentMapper equipmentMapper;

    @GetMapping("/workshops")
    public R<List<BaseWorkshop>> workshops() {
        return R.ok(workshopMapper.selectList(Wrappers.<BaseWorkshop>lambdaQuery()
                .eq(BaseWorkshop::getStatus, 1).orderByAsc(BaseWorkshop::getId)));
    }

    @GetMapping("/prod-lines")
    public R<List<BaseProdLine>> prodLines(@RequestParam(required = false) Long workshopId) {
        return R.ok(prodLineMapper.selectList(Wrappers.<BaseProdLine>lambdaQuery()
                .eq(workshopId != null, BaseProdLine::getWorkshopId, workshopId)
                .orderByAsc(BaseProdLine::getId)));
    }

    @GetMapping("/processes")
    public R<List<BaseProcess>> processes() {
        return R.ok(processMapper.selectList(Wrappers.<BaseProcess>lambdaQuery()
                .eq(BaseProcess::getStatus, 1).orderByAsc(BaseProcess::getSortNo)));
    }

    @GetMapping("/batch-flags")
    public R<List<BaseBatchFlag>> batchFlags() {
        return R.ok(batchFlagMapper.selectList(Wrappers.<BaseBatchFlag>lambdaQuery()
                .eq(BaseBatchFlag::getStatus, 1).orderByAsc(BaseBatchFlag::getId)));
    }

    @GetMapping("/equipments")
    public R<List<BaseEquipment>> equipments(@RequestParam(required = false) Long workshopId) {
        return R.ok(equipmentMapper.selectList(Wrappers.<BaseEquipment>lambdaQuery()
                .eq(workshopId != null, BaseEquipment::getWorkshopId, workshopId)
                .orderByAsc(BaseEquipment::getId)));
    }
}
