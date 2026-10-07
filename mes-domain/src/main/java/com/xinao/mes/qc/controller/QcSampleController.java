package com.xinao.mes.qc.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plan.dto.BatchIdsDTO;
import com.xinao.mes.qc.application.QcSampleAppService;
import com.xinao.mes.qc.domain.model.QcSample;
import com.xinao.mes.qc.dto.QcSampleQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 抽样登记（质检管理：对下批批次登记抽样） */
@RestController
@RequestMapping("/api/qc/samples")
@RequiredArgsConstructor
public class QcSampleController {

    private final QcSampleAppService sampleAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("qc:sample:list")
    public R<PageResult<QcSample>> page(QcSampleQueryDTO query) {
        return R.ok(sampleAppService.page(query));
    }

    /** 登记（批量） */
    @PostMapping("/register")
    @RequiresPerm("qc:sample:list")
    @OperLog(title = "抽样登记")
    public R<Void> register(@RequestBody BatchIdsDTO dto) {
        sampleAppService.register(dto.getIds());
        return R.ok();
    }
}
