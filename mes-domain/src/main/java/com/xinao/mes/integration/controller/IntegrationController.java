package com.xinao.mes.integration.controller;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.common.result.R;
import com.xinao.mes.integration.application.DeviceGateway;
import com.xinao.mes.integration.application.K3SyncService;
import com.xinao.mes.integration.application.SyncRetryJob;
import com.xinao.mes.integration.application.WmsSyncService;
import com.xinao.mes.integration.domain.model.SyncLog;
import com.xinao.mes.integration.infrastructure.mapper.SyncLogMapper;
import com.xinao.mes.plugin.pda.PdaReport;
import com.xinao.mes.plugin.printer.PrintRequest;
import com.xinao.mes.plugin.printer.PrintResult;
import com.xinao.mes.plugin.scale.WeightReading;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/** 集成收口：K3 同步 / WMS 推送 / 打印 / 秤重直读 / PDA 适配 / 同步日志与重试 */
@RestController
@RequestMapping("/api/integration")
@RequiresPerm("integration:sync:list")
@RequiredArgsConstructor
public class IntegrationController {

    private final K3SyncService k3SyncService;
    private final WmsSyncService wmsSyncService;
    private final DeviceGateway deviceGateway;
    private final SyncRetryJob syncRetryJob;
    private final SyncLogMapper syncLogMapper;

    /** K3 订单同步（重推幂等：同 K3 内码只落一次） */
    @PostMapping("/k3/sync")
    @RequiresPerm("integration:sync:operate")
    @OperLog(title = "K3订单同步")
    public R<Map<String, Object>> syncK3() {
        return R.ok(k3SyncService.syncOrders());
    }

    /** 回写 K3：引入量 / 关闭状态 */
    @PostMapping("/k3/writeback")
    @RequiresPerm("integration:sync:operate")
    @OperLog(title = "K3引入量回写")
    public R<Map<String, Object>> writeBackK3(@RequestBody Map<String, Object> body) {
        String orderNo = (String) body.get("orderNo");
        BigDecimal qty = body.get("introducedQty") == null ? null : new BigDecimal(String.valueOf(body.get("introducedQty")));
        boolean closed = Boolean.TRUE.equals(body.get("closed"));
        return R.ok(k3SyncService.writeBack(orderNo, qty, closed));
    }

    /** 推送一托 WMS 入库报文（箱号/托盘号/五列重量；同托重推幂等） */
    @PostMapping("/wms/push-pallet")
    @RequiresPerm("integration:sync:operate")
    @OperLog(title = "WMS入库推送")
    public R<Map<String, Object>> pushPallet(@RequestParam String palletCode) {
        return R.ok(wmsSyncService.pushPallet(palletCode));
    }

    /** 秤重直读（包装称重一体机） */
    @GetMapping("/scale/read")
    public R<WeightReading> readScale() {
        return R.ok(deviceGateway.readScale());
    }

    /** 发送打印任务（制造卡/箱贴/筒贴/吊牌） */
    @PostMapping("/print")
    @OperLog(title = "外设打印")
    public R<PrintResult> print(@RequestBody PrintRequest request) {
        return R.ok(deviceGateway.print(request));
    }

    /** PDA 原始报文适配（型号差异由插件屏蔽） */
    @PostMapping("/pda/report")
    public R<PdaReport> pdaReport(@RequestBody String rawMessage) {
        return R.ok(deviceGateway.adaptPda(rawMessage));
    }

    /** 同步日志查询（WMS同步日志页面） */
    @GetMapping("/sync/logs")
    public R<PageResult<SyncLog>> syncLogs(@RequestParam(required = false) String syncType,
                                           @RequestParam(required = false) String bizNo,
                                           @RequestParam(required = false) Integer syncResult,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        Page<SyncLog> page = Page.of(pageNum, pageSize);
        Page<SyncLog> result = syncLogMapper.selectPage(page, Wrappers.<SyncLog>lambdaQuery()
                .eq(StringUtils.isNotBlank(syncType), SyncLog::getSyncType, syncType)
                .like(StringUtils.isNotBlank(bizNo), SyncLog::getBizNo, bizNo)
                .eq(syncResult != null, SyncLog::getSyncResult, syncResult)
                .orderByDesc(SyncLog::getId));
        return R.ok(PageResult.of(result.getTotal(), result.getRecords()));
    }

    /** 手动重试失败同步（每分钟亦有定时扫描兜底） */
    @PostMapping("/sync/retry")
    @RequiresPerm("integration:sync:operate")
    @OperLog(title = "同步失败重试")
    public R<Map<String, Object>> retrySync() {
        return R.ok(syncRetryJob.retryAll());
    }
}
