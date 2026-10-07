package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.flow.FlowEngine;
import com.xinao.mes.produce.domain.model.MtPick;
import com.xinao.mes.produce.dto.PickQueryDTO;
import com.xinao.mes.produce.infrastructure.mapper.MtPickMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 领退料确认应用服务（车间对K3/仓库推送单据的确认）
 *
 * <p>单号走编码引擎：领料 JDSC1L... / 退料 JDSC1T...；
 * 确认走 flow（mt_pick 待确认→已确认），确认人/时间回填。
 */
@Service
@RequiredArgsConstructor
public class PickAppService {

    private final MtPickMapper pickMapper;
    private final CodeEngine codeEngine;
    private final FlowEngine flowEngine;

    public PageResult<MtPick> page(PickQueryDTO query) {
        Page<MtPick> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<MtPick> result = pickMapper.selectPage(p, Wrappers.<MtPick>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getOrderNo()), MtPick::getOrderNo, query.getOrderNo())
                .like(StringUtils.isNotBlank(query.getBatchNo()), MtPick::getBatchNo, query.getBatchNo())
                .eq(query.getWorkshopId() != null, MtPick::getWorkshopId, query.getWorkshopId())
                .eq(query.getPickType() != null, MtPick::getPickType, query.getPickType())
                .eq(StringUtils.isNotBlank(query.getDocStatus()), MtPick::getDocStatus, query.getDocStatus())
                .orderByDesc(MtPick::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 保存（M5 K3 插件接入前支持手工补录）：按类型取号，初始待确认 */
    @Transactional
    public Long save(MtPick pick) {
        if (pick == null || pick.getPickType() == null) {
            throw new BizException("请选择单据类型（领料单/退料单）");
        }
        if (pick.getId() == null) {
            String rule = pick.getPickType() == MtPick.TYPE_PICK ? "领料单" : "退料单";
            pick.setPickNo(codeEngine.next(rule, pick.getOrderNo()));
            pick.setDocStatus(MtPick.STATUS_WAIT);
        } else {
            require(pick.getId());
        }
        if (pick.getId() == null) {
            pickMapper.insert(pick);
        } else {
            pickMapper.updateById(pick);
        }
        return pick.getId();
    }

    /** 确认：flow 迁移 + CAS + 确认人/时间回填 */
    @Transactional
    public void confirm(java.util.List<Long> ids) {
        for (Long id : ids) {
            MtPick pick = require(id);
            flowEngine.fire("mt_pick", pick.getDocStatus(), "confirm", roles());
            if (pickMapper.update(null, Wrappers.<MtPick>lambdaUpdate()
                    .eq(MtPick::getId, id).eq(MtPick::getDocStatus, MtPick.STATUS_WAIT)
                    .set(MtPick::getDocStatus, MtPick.STATUS_CONFIRMED)
                    .set(MtPick::getPicker, currentUser())
                    .set(MtPick::getPickTime, LocalDateTime.now())) == 0) {
                throw new BizException("单据[" + pick.getPickNo() + "]状态已变更，请刷新后重试");
            }
        }
    }

    @Transactional
    public void delete(Long id) {
        MtPick pick = require(id);
        if (MtPick.STATUS_CONFIRMED.equals(pick.getDocStatus())) {
            throw new BizException("单据[" + pick.getPickNo() + "]已确认，不可删除");
        }
        pickMapper.deleteById(id);
    }

    private Set<String> roles() {
        var user = AuthContext.get();
        return user == null || user.getRoles() == null ? Set.of() : user.getRoles();
    }

    private String currentUser() {
        var user = AuthContext.get();
        return user == null ? "system" : user.getUserName();
    }

    private MtPick require(Long id) {
        MtPick pick = pickMapper.selectById(id);
        if (pick == null) {
            throw new BizException("领退料单据不存在: " + id);
        }
        return pick;
    }
}
