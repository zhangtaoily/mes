package com.xinao.mes.qc.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.qc.application.QcPackageReportAppService;
import com.xinao.mes.qc.domain.model.QcPackageReport;
import com.xinao.mes.qc.dto.QcPackageReportSaveDTO;
import com.xinao.mes.qc.vo.QcPackageReportDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 包装质检报告（按入库批次录入 15 类疵纱分类统计） */
@RestController
@RequestMapping("/api/qc/package-reports")
@RequiredArgsConstructor
public class QcPackageReportController {

    private final QcPackageReportAppService packageReportAppService;

    /** 分页查询 */
    @GetMapping
    @RequiresPerm("qc:packageReport:list")
    public R<PageResult<QcPackageReport>> page(@RequestParam(required = false) String warehouseBatchNo,
                                               @RequestParam(required = false) String batchNo,
                                               @RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(packageReportAppService.page(warehouseBatchNo, batchNo, pageNum, pageSize));
    }

    /** 详情（主表 + 疵纱明细） */
    @GetMapping("/{id}")
    @RequiresPerm("qc:packageReport:list")
    public R<QcPackageReportDetailVO> detail(@PathVariable Long id) {
        return R.ok(packageReportAppService.detail(id));
    }

    /** 保存（疵纱明细整表保存，合格率服务端计算） */
    @PostMapping
    @RequiresPerm("qc:packageReport:list")
    @OperLog(title = "保存包装质检报告")
    public R<Long> save(@Valid @RequestBody QcPackageReportSaveDTO dto) {
        return R.ok(packageReportAppService.save(dto));
    }
}
