package com.xinao.mes.produce.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.produce.application.OutputAppService;
import com.xinao.mes.produce.domain.model.RepOutput;
import com.xinao.mes.produce.dto.OutputQueryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 产量汇报（现场管理；排程报工回溯数据源） */
@RestController
@RequestMapping("/api/site/output")
@RequiredArgsConstructor
public class OutputController {

    private final OutputAppService outputAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("site:outputReport:list")
    public R<PageResult<RepOutput>> page(OutputQueryDTO query) {
        return R.ok(outputAppService.page(query));
    }

    /** 排程关联汇报明细（排程列表"报工回溯"） */
    @GetMapping("/of-schedule/{scheduleId}")
    @RequiresPerm({ "site:outputReport:list", "prod:schedule:list" })
    public R<List<RepOutput>> listOfSchedule(@PathVariable Long scheduleId) {
        return R.ok(outputAppService.listOfSchedule(scheduleId));
    }

    /** 保存产量汇报 */
    @PostMapping
    @RequiresPerm("site:outputReport:list")
    @OperLog(title = "产量汇报")
    public R<Long> save(@Valid @RequestBody RepOutput output) {
        return R.ok(outputAppService.save(output));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @RequiresPerm("site:outputReport:list")
    @OperLog(title = "删除产量汇报")
    public R<Void> delete(@PathVariable Long id) {
        outputAppService.delete(id);
        return R.ok();
    }
}
