package com.xinao.mes.integration;

import com.xinao.mes.integration.application.K3SyncService;
import com.xinao.mes.integration.domain.model.SyncLog;
import com.xinao.mes.integration.infrastructure.mapper.SyncLogMapper;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.infrastructure.mapper.ErpK3OrderMapper;
import com.xinao.mes.plugin.SPI;
import com.xinao.mes.plugin.erp.ErpConnector;
import com.xinao.mes.plugin.loader.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** K3 同步幂等单测：首推入库 / 重推跳过 / 失败重放 */
@ExtendWith(MockitoExtension.class)
class K3SyncIdempotentTest {

    private final PluginManager pluginManager = PluginManager.getInstance();

    @Mock
    private SyncLogMapper syncLogMapper;
    @Mock
    private ErpK3OrderMapper k3Mapper;

    private K3SyncService service;

    @SPI("k3")
    static class OneOrderK3 implements ErpConnector {
        @Override
        public List<Map<String, Object>> pullOrders(LocalDateTime startTime) {
            return List.of(Map.of(
                    "k3OrderId", "K3-T1",
                    "orderNo", "FBDC772350",
                    "orderDate", "2026-10-01",
                    "k3Qty", new BigDecimal("5000"),
                    "workshopName", "纺十车间"));
        }

        @Override
        public Long writeBack(String orderNo, BigDecimal introducedQty, boolean closed) {
            return 1L;
        }
    }

    @BeforeEach
    void setUp() {
        pluginManager.clear();
        pluginManager.register(new OneOrderK3());
        service = new K3SyncService(pluginManager, syncLogMapper, k3Mapper);
    }

    @AfterEach
    void tearDown() {
        pluginManager.clear();
    }

    private SyncLog logWithId(long id, Integer result, Integer retryCount) {
        SyncLog log = new SyncLog();
        log.setId(id);
        log.setSyncType(K3SyncService.SYNC_TYPE_K3);
        log.setIfaceId("K3-T1");
        log.setBizNo("FBDC772350");
        log.setSyncResult(result);
        log.setRetryCount(retryCount);
        return log;
    }

    @Test
    void firstSyncInsertsNewOrder() {
        when(syncLogMapper.insert(any(SyncLog.class))).thenAnswer(inv -> {
            inv.getArgument(0, SyncLog.class).setId(1L);
            return 1;
        });
        when(k3Mapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.syncOrders();

        assertEquals(1, result.get("pulled"));
        assertEquals(1, result.get("inserted"));
        assertEquals(0, result.get("skipped"));
        verify(k3Mapper).insert(any(ErpK3Order.class));
        // 落库成功后回写 log 状态
        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getSyncResult());
    }

    @Test
    void duplicatePushSkipsSyncedOrder() {
        // 唯一索引命中 = K3 重推同一单
        when(syncLogMapper.insert(any(SyncLog.class))).thenThrow(new DuplicateKeyException("dup"));
        when(syncLogMapper.selectOne(any())).thenReturn(logWithId(1, 1, 0));

        Map<String, Object> result = service.syncOrders();

        assertEquals(1, result.get("pulled"));
        assertEquals(0, result.get("inserted"));
        assertEquals(0, result.get("updated"));
        assertEquals(1, result.get("skipped"));
        // 重推被拦截：不碰订单镜像
        verify(k3Mapper, never()).insert(any(ErpK3Order.class));
        verify(k3Mapper, never()).updateById(any(ErpK3Order.class));
    }

    @Test
    void failedOrderIsReplayedOnNextPush() {
        // 首推失败：唯一键冲突但已有记录是失败态
        when(syncLogMapper.insert(any(SyncLog.class))).thenThrow(new DuplicateKeyException("dup"));
        when(syncLogMapper.selectOne(any())).thenReturn(logWithId(1, 0, 1));
        ErpK3Order existing = new ErpK3Order();
        existing.setId(9L);
        existing.setOrderNo("FBDC772350");
        existing.setImportedQty(new BigDecimal("100"));
        when(k3Mapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> result = service.syncOrders();

        assertEquals(1, result.get("updated"));
        assertEquals(0, result.get("skipped"));
        // 失败记录被救活：result=1 且 retry_count 累加
        ArgumentCaptor<SyncLog> captor = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getSyncResult());
        assertEquals(2, captor.getValue().getRetryCount());
        assertTrue(captor.getValue().getRespParam().contains("重试"));
        // upsert 保留引入进度
        ArgumentCaptor<ErpK3Order> orderCaptor = ArgumentCaptor.forClass(ErpK3Order.class);
        verify(k3Mapper).updateById(orderCaptor.capture());
        assertEquals(new BigDecimal("100"), orderCaptor.getValue().getImportedQty());
    }
}
