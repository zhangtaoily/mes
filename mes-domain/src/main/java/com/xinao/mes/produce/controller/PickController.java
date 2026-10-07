package com.xinao.mes.produce.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.produce.application.PickAppService;
import com.xinao.mes.produce.domain.model.MtPick;
import com.xinao.mes.produce.dto.PickQueryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 领退料确认（生产管理；单号 JDSC1L/T 走编码引擎） */
@RestController
@RequestMapping("/api/prod/pick")
@RequiredArgsConstructor
public class PickController {

    private final PickAppService pickAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("prod:feed:list")
    public R<PageResult<MtPick>> page(PickQueryDTO query) {
        return R.ok(pickAppService.page(query));
    }

    /** 保存（M5 插件接入前手工补录） */
    @PostMapping
    @RequiresPerm("prod:feed:list")
    @OperLog(title = "保存领退料单")
    public R<Long> save(@Valid @RequestBody MtPick pick) {
        return R.ok(pickAppService.save(pick));
    }

    /** 批量确认（flow：待确认→已确认，确认人/时间回填） */
    @PostMapping("/confirm")
    @RequiresPerm("prod:feed:list")
    @OperLog(title = "领退料确认")
    public R<Void> confirm(@RequestBody List<Long> ids) {
        pickAppService.confirm(ids);
        return R.ok();
    }

    /** 删除（仅待确认） */
    @DeleteMapping("/{id}")
    @RequiresPerm("prod:feed:list")
    @OperLog(title = "删除领退料单")
    public R<Void> delete(@PathVariable Long id) {
        pickAppService.delete(id);
        return R.ok();
    }
}
