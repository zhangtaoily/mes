package com.xinao.mes.integration.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.integration.domain.model.SyncLog;
import com.xinao.mes.integration.infrastructure.mapper.SyncLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步失败重试（sync_log 状态驱动，替代 XXL-Job 的最简实现——无外部调度中心）。
 *
 * <p>每分钟扫描 sync_result=0 且 retry_count<3 的记录，按 sync_type 路由重放：
 * K3订单 → 报文重放；WMS入库 → 重新组报文推送（插件可能已热替换修复）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SyncRetryJob {

    private static final int MAX_RETRY = 3;

    private final SyncLogMapper syncLogMapper;
    private final K3SyncService k3SyncService;
    private final WmsSyncService wmsSyncService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 120_000)
    public void autoRetry() {
        Map<String, Object> stats = retryAll();
        if (!Integer.valueOf(0).equals(stats.get("total"))) {
            log.info("同步重试完成: {}", stats);
        }
    }

    /** 手动/自动重试入口：返回统计 */
    public Map<String, Object> retryAll() {
        List<SyncLog> failed = syncLogMapper.selectList(Wrappers.<SyncLog>lambdaQuery()
                .eq(SyncLog::getSyncResult, 0)
                .lt(SyncLog::getRetryCount, MAX_RETRY)
                .orderByAsc(SyncLog::getId));
        int success = 0;
        int remain = 0;
        for (SyncLog syncLog : failed) {
            Boolean ok = switch (syncLog.getSyncType() == null ? "" : syncLog.getSyncType()) {
                case K3SyncService.SYNC_TYPE_K3 -> k3SyncService.replay(syncLog);
                case WmsSyncService.SYNC_TYPE_WMS -> isPushSuccess(wmsSyncService.pushPallet(syncLog.getBizNo()));
                default -> null;
            };
            if (Boolean.TRUE.equals(ok)) {
                success++;
            } else {
                remain++;
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", failed.size());
        stats.put("success", success);
        stats.put("remain", remain);
        return stats;
    }

    private boolean isPushSuccess(Map<String, Object> pushResult) {
        return Boolean.TRUE.equals(pushResult.get("success"));
    }
}
