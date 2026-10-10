package com.xinao.mes.qc.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.qc.application.QcPackageReportAppService;
import com.xinao.mes.qc.domain.model.QcPackageReport;
import com.xinao.mes.qc.dto.QcPackageReportSaveDTO;
import com.xinao.mes.qc.infrastructure.mapper.QcPackageReportMapper;
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

import java.util.List;

/**
 * PDA 质检 BFF（M3：包装质检报告录入）
 *
 * <p>与 PC 共用 qc application 层（入库批次必填、疵纱明细整表保存、
 * 一等品数缺省取正纱数、合格率服务端计算同源），仅做 PDA 查询差异：
 * 关键词模糊 + LIMIT 50（无分页翻页）。
 */
@RestController
@RequestMapping("/api/pda/qc")
@RequiredArgsConstructor
public class PdaQcController {

    private final QcPackageReportAppService packageReportAppService;
    private final QcPackageReportMapper reportMapper;

    /** 包装质检报告查询（入库批次/生产批次模糊，最多50条） */
    @GetMapping("/package-reports")
    @RequiresPerm("qc:packageReport:list")
    public R<List<QcPackageReport>> reports(@RequestParam(required = false) String keyword) {
        String kw = StringUtils.isBlank(keyword) ? null : keyword.trim();
        return R.ok(reportMapper.selectList(Wrappers.<QcPackageReport>lambdaQuery()
                .and(kw != null, w -> w.like(QcPackageReport::getWarehouseBatchNo, kw)
                        .or().like(QcPackageReport::getBatchNo, kw))
                .orderByDesc(QcPackageReport::getId)
                .last("LIMIT 50")));
    }

    /** 报告详情（主表 + 疵纱明细） */
    @GetMapping("/package-reports/{id}")
    @RequiresPerm("qc:packageReport:list")
    public R<QcPackageReportDetailVO> detail(@PathVariable Long id) {
        return R.ok(packageReportAppService.detail(id));
    }

    /** PDA 保存包装质检报告（15 类疵纱分类，合格率服务端计算；新建缺省今天） */
    @PostMapping("/package-reports")
    @RequiresPerm("qc:packageReport:list")
    @OperLog(title = "PDA保存包装质检报告")
    public R<Long> save(@Valid @RequestBody QcPackageReportSaveDTO dto) {
        return R.ok(packageReportAppService.save(dto));
    }
}
