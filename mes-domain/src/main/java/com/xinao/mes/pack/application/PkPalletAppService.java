package com.xinao.mes.pack.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkExecute;
import com.xinao.mes.pack.domain.model.PkPallet;
import com.xinao.mes.pack.domain.model.PkTask;
import com.xinao.mes.pack.dto.PalletizeDTO;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkExecuteMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkPalletMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 组托应用服务（托号 TP...；一批箱绑定到新托号，已组托的箱子拒绝重复组托） */
@Service
@RequiredArgsConstructor
public class PkPalletAppService {

    private final PkPalletMapper palletMapper;
    private final PkBoxMapper boxMapper;
    private final PkExecuteMapper executeMapper;
    private final PkTaskMapper taskMapper;
    private final CodeEngine codeEngine;

    public PageResult<PkPallet> page(String palletCode, String batchNo, int pageNum, int pageSize) {
        Page<PkPallet> p = Page.of(pageNum, pageSize);
        Page<PkPallet> result = palletMapper.selectPage(p, Wrappers.<PkPallet>lambdaQuery()
                .like(StringUtils.isNotBlank(palletCode), PkPallet::getPalletCode, palletCode)
                .like(StringUtils.isNotBlank(batchNo), PkPallet::getBatchNo, batchNo)
                .orderByDesc(PkPallet::getPalletTime));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 组托：箱子绑定托号 + 组托记录留痕（重量从箱子带出，车间/批号从任务带出） */
    @Transactional
    public String palletize(PalletizeDTO dto) {
        if (dto.getBoxCodes() == null || dto.getBoxCodes().isEmpty()) {
            throw new BizException("请选择要组托的箱子");
        }
        PkExecute execute = executeMapper.selectById(dto.getExecuteId());
        if (execute == null) {
            throw new BizException("包装执行不存在: " + dto.getExecuteId());
        }
        PkTask task = taskMapper.selectById(execute.getPkTaskId());
        List<PkBox> boxes = boxMapper.selectList(Wrappers.<PkBox>lambdaQuery()
                .in(PkBox::getBoxCode, dto.getBoxCodes()));
        if (boxes.size() != new HashSet<>(dto.getBoxCodes()).size()) {
            throw new BizException("部分箱子不存在，请刷新后重试");
        }
        Set<String> requested = new HashSet<>(dto.getBoxCodes());
        for (PkBox box : boxes) {
            if (!box.getPkExecuteId().equals(execute.getId())) {
                throw new BizException("箱子[" + box.getBoxCode() + "]不属于包装执行[" + execute.getExecuteNo() + "]");
            }
            if (StringUtils.isNotBlank(box.getPalletCode())) {
                throw new BizException("箱子[" + box.getBoxCode() + "]已组托[" + box.getPalletCode() + "]，不可重复组托");
            }
        }
        String palletCode = codeEngine.next("托盘", execute.getWarehouseBatchNo());
        String user = currentUser();
        LocalDateTime now = LocalDateTime.now();
        for (PkBox box : boxes) {
            PkPallet pallet = new PkPallet();
            pallet.setPalletCode(palletCode);
            pallet.setBoxCode(box.getBoxCode());
            pallet.setPackWorkshopId(task == null ? null : task.getPackWorkshopId());
            pallet.setBatchNo(task == null ? null : task.getBatchNo());
            pallet.setDyeVatNo(execute.getDyeVatNo());
            pallet.setStandardWeight(execute.getStandardWeight());
            pallet.setGrossWeight(box.getGrossWeight());
            pallet.setNetWeight(box.getNetWeight());
            pallet.setStandardTube(dto.getStandardTube());
            pallet.setPalletUser(user);
            pallet.setPalletTime(now);
            palletMapper.insert(pallet);
            box.setPalletCode(palletCode);
            boxMapper.updateById(box);
        }
        return palletCode;
    }

    private String currentUser() {
        var user = AuthContext.get();
        return user == null ? "system" : user.getUserName();
    }
}
