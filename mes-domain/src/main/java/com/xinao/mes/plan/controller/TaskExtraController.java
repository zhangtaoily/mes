package com.xinao.mes.plan.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.domain.model.ProdTaskExtra;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskExtraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 生产任务其它信息（环境与工艺开关，1:1 挂任务；生产管理菜单） */
@RestController
@RequestMapping("/api/prod/task-extras")
@RequiredArgsConstructor
public class TaskExtraController {

    private final ProdTaskExtraMapper extraMapper;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("prod:taskExtra:list")
    public R<PageResult<ProdTaskExtra>> page(@RequestParam(required = false) Long taskId,
                                             @RequestParam(required = false) String batchNo,
                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<ProdTaskExtra> p = extraMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<ProdTaskExtra>lambdaQuery()
                        .eq(taskId != null, ProdTaskExtra::getTaskId, taskId)
                        .like(StringUtils.isNotBlank(batchNo), ProdTaskExtra::getBatchNo, batchNo)
                        .orderByDesc(ProdTaskExtra::getId));
        return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
    }

    /** 保存（1:1：taskId 已存在则更新） */
    @PostMapping
    @RequiresPerm("prod:taskExtra:list")
    @OperLog(title = "保存任务其它信息")
    public R<Long> save(@RequestBody ProdTaskExtra extra) {
        if (extra.getTaskId() == null) {
            throw new BizException("请选择生产任务");
        }
        ProdTaskExtra old = extraMapper.selectOne(Wrappers.<ProdTaskExtra>lambdaQuery()
                .eq(ProdTaskExtra::getTaskId, extra.getTaskId()).last("LIMIT 1"));
        if (old != null) {
            extra.setId(old.getId());
            extraMapper.updateById(extra);
        } else {
            extraMapper.insert(extra);
        }
        return R.ok(extra.getId());
    }
}
