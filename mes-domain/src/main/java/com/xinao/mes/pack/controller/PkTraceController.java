package com.xinao.mes.pack.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.pack.application.PkTraceAppService;
import com.xinao.mes.pack.domain.model.PkErpIn;
import com.xinao.mes.pack.domain.model.PkLabelReprint;
import com.xinao.mes.pack.domain.model.PkTubePrint;
import com.xinao.mes.pack.domain.model.PkWmsIn;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 包装追溯（箱贴补打 / 筒贴打印 / WMS / ERP 入库链路） */
@RestController
@RequestMapping("/api/pack/trace")
@RequiredArgsConstructor
public class PkTraceController {

    private final PkTraceAppService traceAppService;

    /** 箱贴补打列表 */
    @GetMapping("/label-reprints")
    @RequiresPerm("pack:trace:list")
    public R<PageResult<PkLabelReprint>> labelReprints(@RequestParam(required = false) String docNo,
                                                       @RequestParam(required = false) String warehouseBatchNo,
                                                       @RequestParam(defaultValue = "1") int pageNum,
                                                       @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(traceAppService.labelReprints(docNo, warehouseBatchNo, pageNum, pageSize));
    }

    /** 箱贴补打留痕 */
    @PostMapping("/label-reprints")
    @RequiresPerm("pack:trace:list")
    @OperLog(title = "箱贴补打")
    public R<Long> reprint(@RequestBody PkLabelReprint reprint) {
        return R.ok(traceAppService.reprint(reprint));
    }

    /** 筒贴打印记录 */
    @GetMapping("/tube-prints")
    @RequiresPerm("pack:trace:list")
    public R<PageResult<PkTubePrint>> tubePrints(@RequestParam(required = false) String batchNo,
                                                 @RequestParam(required = false) String printUser,
                                                 @RequestParam(defaultValue = "1") int pageNum,
                                                 @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(traceAppService.tubePrints(batchNo, printUser, pageNum, pageSize));
    }

    /** WMS 入库记录 */
    @GetMapping("/wms-in")
    @RequiresPerm("pack:trace:list")
    public R<PageResult<PkWmsIn>> wmsIn(@RequestParam(required = false) String warehouseBatchNo,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(traceAppService.wmsIn(warehouseBatchNo, pageNum, pageSize));
    }

    /** ERP 入库记录 */
    @GetMapping("/erp-in")
    @RequiresPerm("pack:trace:list")
    public R<PageResult<PkErpIn>> erpIn(@RequestParam(required = false) String warehouseBatchNo,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(traceAppService.erpIn(warehouseBatchNo, pageNum, pageSize));
    }
}
