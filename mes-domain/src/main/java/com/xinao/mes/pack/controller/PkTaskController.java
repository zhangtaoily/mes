package com.xinao.mes.pack.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.pack.application.PkTaskAppService;
import com.xinao.mes.pack.domain.model.PkTask;
import com.xinao.mes.pack.dto.PkTaskQueryDTO;
import com.xinao.mes.plan.dto.BatchIdsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 包装任务（含倒毛包装任务；执行生成包装执行单，了批/缸号了批批量操作） */
@RestController
@RequestMapping("/api/pack/tasks")
@RequiredArgsConstructor
public class PkTaskController {

    private final PkTaskAppService taskAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("pack:task:list")
    public R<PageResult<PkTask>> page(PkTaskQueryDTO query) {
        return R.ok(taskAppService.page(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    @RequiresPerm("pack:task:list")
    public R<PkTask> detail(@PathVariable Long id) {
        return R.ok(taskAppService.detail(id));
    }

    /** 保存（新建生成任务编号 PS...） */
    @PostMapping
    @RequiresPerm("pack:task:list")
    @OperLog(title = "保存包装任务")
    public R<Long> save(@RequestBody PkTask task) {
        return R.ok(taskAppService.save(task));
    }

    /** 执行（生成包装执行单 PC...） */
    @PostMapping("/{id}/execute")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "执行包装任务")
    public R<Long> execute(@PathVariable Long id) {
        return R.ok(taskAppService.execute(id));
    }

    /** 任务了批（批量） */
    @PostMapping("/finish")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "包装任务了批")
    public R<Void> finish(@RequestBody BatchIdsDTO dto) {
        taskAppService.finish(dto.getIds());
        return R.ok();
    }

    /** 取消任务了批（批量） */
    @PostMapping("/cancel-finish")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "取消包装任务了批")
    public R<Void> cancelFinish(@RequestBody BatchIdsDTO dto) {
        taskAppService.cancelFinish(dto.getIds());
        return R.ok();
    }

    /** 缸号了批（批量） */
    @PostMapping("/finish-vat")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "缸号了批")
    public R<Void> finishVat(@RequestBody BatchIdsDTO dto) {
        taskAppService.finishVat(dto.getIds());
        return R.ok();
    }

    /** 取消缸号了批（批量） */
    @PostMapping("/cancel-finish-vat")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "取消缸号了批")
    public R<Void> cancelFinishVat(@RequestBody BatchIdsDTO dto) {
        taskAppService.cancelFinishVat(dto.getIds());
        return R.ok();
    }

    /** 筒贴打印（留痕） */
    @PostMapping("/{id}/print-tube")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "筒贴打印")
    public R<Void> printTube(@PathVariable Long id, @RequestParam(required = false) Integer qty) {
        taskAppService.printTube(id, qty);
        return R.ok();
    }

    /** 箱单打印（留痕） */
    @PostMapping("/{id}/print-box")
    @RequiresPerm("pack:task:list")
    @OperLog(title = "箱单打印")
    public R<Void> printBox(@PathVariable Long id,
                            @RequestParam(required = false, defaultValue = "箱单打印") String bizType) {
        taskAppService.printBox(id, bizType);
        return R.ok();
    }
}
