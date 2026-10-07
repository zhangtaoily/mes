package com.xinao.mes.plan.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.plan.domain.model.BaseCraft;
import com.xinao.mes.plan.domain.model.BaseCraftParam;
import com.xinao.mes.plan.domain.model.BaseCraftWorkshop;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.infrastructure.mapper.BaseCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseCraftParamMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseCraftWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.vo.CraftSchemeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 工艺库引用服务：技术工艺库(base_craft) / 车间工艺库(base_craft_workshop)
 *
 * <p>两库共用 base_craft_param（craft_id 分别指向两张主表，车间库机型列非空、技术库为 NULL）。
 * 引用即快照：带出工艺项写入单据子表，后续库调整不影响已引用单据。
 */
@Service
@RequiredArgsConstructor
public class CraftLibraryService {

    private final BaseCraftMapper craftMapper;
    private final BaseCraftWorkshopMapper workshopCraftMapper;
    private final BaseCraftParamMapper paramMapper;
    private final BaseProcessMapper processMapper;

    /** 按来源+方案+工序(+机型)取工艺项（技术库机型列为NULL；车间库按 工序+机型 匹配） */
    public List<BaseCraftParam> params(int refSource, Long schemeId, Collection<Long> processIds, String machineModel) {
        if (processIds == null || processIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<BaseCraftParam> w = Wrappers.<BaseCraftParam>lambdaQuery()
                .eq(BaseCraftParam::getCraftId, schemeId)
                .in(BaseCraftParam::getProcessId, processIds)
                .orderByAsc(BaseCraftParam::getSortNo);
        if (refSource == PlanCraftCraft.REF_WORKSHOP) {
            if (machineModel == null || machineModel.isBlank()) {
                throw new BizException("车间工艺库需选择机型");
            }
            w.eq(BaseCraftParam::getMachineModel, machineModel);
        } else {
            w.isNull(BaseCraftParam::getMachineModel);
        }
        return paramMapper.selectList(w);
    }

    /** 方案编号：技术库用工艺编号(CT...)，车间库用 CJ+id */
    public String schemeNo(int refSource, Long schemeId) {
        if (refSource == PlanCraftCraft.REF_WORKSHOP) {
            return "CJ" + schemeId;
        }
        BaseCraft c = craftMapper.selectById(schemeId);
        if (c == null) {
            throw new BizException("技术工艺方案不存在: " + schemeId);
        }
        return c.getCraftCode();
    }

    /** 工艺配置选择面板：按工序(+机型)列出可用方案及工艺项数 */
    public List<CraftSchemeVO> schemes(int refSource, Long processId, String machineModel) {
        if (processId == null) {
            throw new BizException("请选择工序");
        }
        BaseProcess process = processMapper.selectById(processId);
        String processName = process == null ? null : process.getProcessName();
        List<CraftSchemeVO> result = new ArrayList<>();
        if (refSource == PlanCraftCraft.REF_TECH) {
            for (BaseCraft c : craftMapper.selectList(Wrappers.<BaseCraft>lambdaQuery().eq(BaseCraft::getEnabled, 1))) {
                Long cnt = paramMapper.selectCount(Wrappers.<BaseCraftParam>lambdaQuery()
                        .eq(BaseCraftParam::getCraftId, c.getId())
                        .eq(BaseCraftParam::getProcessId, processId)
                        .isNull(BaseCraftParam::getMachineModel));
                if (cnt != null && cnt > 0) {
                    result.add(vo(c.getId(), c.getCraftCode(), c.getCraftName(), refSource, processId, processName, null, cnt.intValue()));
                }
            }
        } else {
            for (BaseCraftWorkshop wc : workshopCraftMapper.selectList(
                    Wrappers.<BaseCraftWorkshop>lambdaQuery().eq(BaseCraftWorkshop::getEnabled, 1))) {
                LambdaQueryWrapper<BaseCraftParam> w = Wrappers.<BaseCraftParam>lambdaQuery()
                        .eq(BaseCraftParam::getCraftId, wc.getId())
                        .eq(BaseCraftParam::getProcessId, processId)
                        .isNotNull(BaseCraftParam::getMachineModel);
                if (machineModel != null && !machineModel.isBlank()) {
                    w.eq(BaseCraftParam::getMachineModel, machineModel);
                }
                List<BaseCraftParam> params = paramMapper.selectList(w);
                // 按机型展开为可选方案行（三级：工序>机型>工艺项）
                params.stream().map(BaseCraftParam::getMachineModel).distinct().forEach(mm ->
                        result.add(vo(wc.getId(), "CJ" + wc.getId(), wc.getCraftName(), refSource, processId, processName, mm,
                                (int) params.stream().filter(p -> mm.equals(p.getMachineModel())).count())));
            }
        }
        return result;
    }

    private CraftSchemeVO vo(Long schemeId, String schemeNo, String schemeName, int refSource,
                             Long processId, String processName, String machineModel, int paramCount) {
        CraftSchemeVO v = new CraftSchemeVO();
        v.setSchemeId(schemeId);
        v.setSchemeNo(schemeNo);
        v.setSchemeName(schemeName);
        v.setRefSource(refSource);
        v.setProcessId(processId);
        v.setProcessName(processName);
        v.setMachineModel(machineModel);
        v.setParamCount(paramCount);
        return v;
    }
}
