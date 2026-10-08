package com.xinao.mes.plan.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskChange;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskChangeMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 任务变更（生产管理菜单；无审批流：编制中→已提交，提交时新值回写原任务） */
@RestController
@RequestMapping("/api/prod/task-changes")
@RequiredArgsConstructor
public class TaskChangeController {

    private final ProdTaskChangeMapper changeMapper;
    private final ProdTaskMapper taskMapper;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("prod:taskChange:list")
    public R<PageResult<ProdTaskChange>> page(@RequestParam(required = false) String changeNo,
                                              @RequestParam(required = false) Long taskId,
                                              @RequestParam(required = false) String docStatus,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<ProdTaskChange> p = changeMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<ProdTaskChange>lambdaQuery()
                        .like(StringUtils.isNotBlank(changeNo), ProdTaskChange::getChangeNo, changeNo)
                        .eq(taskId != null, ProdTaskChange::getTaskId, taskId)
                        .eq(StringUtils.isNotBlank(docStatus), ProdTaskChange::getDocStatus, docStatus)
                        .orderByDesc(ProdTaskChange::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存（id 空=新建，changeNo 查重；有 id=更新） */
    @PostMapping
    @RequiresPerm("prod:taskChange:list")
    @OperLog(title = "保存任务变更单")
    public R<Long> save(@RequestBody ProdTaskChange change) {
        if (change.getTaskId() == null) {
            throw new BizException("请选择原任务");
        }
        if (StringUtils.isBlank(change.getChangeNo())) {
            throw new BizException("变更单号不能为空");
        }
        Long count = changeMapper.selectCount(Wrappers.<ProdTaskChange>lambdaQuery()
                .eq(ProdTaskChange::getChangeNo, change.getChangeNo())
                .ne(change.getId() != null, ProdTaskChange::getId, change.getId()));
        if (count != null && count > 0) {
            throw new BizException("变更单号[" + change.getChangeNo() + "]已存在");
        }
        if (change.getId() == null) {
            change.setDocStatus("编制中");
            changeMapper.insert(change);
        } else {
            ProdTaskChange old = changeMapper.selectById(change.getId());
            if (old == null) {
                throw new BizException("变更单不存在");
            }
            if (!"编制中".equals(old.getDocStatus())) {
                throw new BizException("仅[编制中]可编辑");
            }
            change.setDocStatus(old.getDocStatus());
            changeMapper.updateById(change);
        }
        return R.ok(change.getId());
    }

    /** 提交：编制中→已提交，并把非空新值回写原任务 */
    @PostMapping("/{id}/submit")
    @RequiresPerm("prod:taskChange:list")
    @OperLog(title = "提交任务变更单")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> submit(@PathVariable Long id) {
        ProdTaskChange change = changeMapper.selectById(id);
        if (change == null) {
            throw new BizException("变更单不存在");
        }
        if (!"编制中".equals(change.getDocStatus())) {
            throw new BizException("仅[编制中]可提交，当前: " + change.getDocStatus());
        }
        ProdTask task = taskMapper.selectById(change.getTaskId());
        if (task == null) {
            throw new BizException("原任务不存在");
        }
        if (change.getNewPlanFeedQty() != null) {
            task.setPlanFeedQty(change.getNewPlanFeedQty());
        }
        if (change.getNewPlanQty() != null) {
            task.setPlanQty(change.getNewPlanQty());
        }
        if (change.getNewDeliveryDate() != null) {
            task.setDeliveryDate(change.getNewDeliveryDate());
        }
        taskMapper.updateById(task);
        change.setDocStatus("已提交");
        changeMapper.updateById(change);
        return R.ok();
    }

    /** 删除（仅编制中，物理删） */
    @DeleteMapping("/{id}")
    @RequiresPerm("prod:taskChange:list")
    @OperLog(title = "删除任务变更单")
    public R<Void> delete(@PathVariable Long id) {
        ProdTaskChange change = changeMapper.selectById(id);
        if (change != null && !"编制中".equals(change.getDocStatus())) {
            throw new BizException("仅[编制中]可删除");
        }
        changeMapper.deleteById(id);
        return R.ok();
    }

    /** 变更基准参考：原任务当前值（前端新建时带出） */
    @GetMapping("/task/{taskId}/baseline")
    @RequiresPerm("prod:taskChange:list")
    public R<ProdTask> baseline(@PathVariable Long taskId) {
        return R.ok(taskMapper.selectById(taskId));
    }
}
