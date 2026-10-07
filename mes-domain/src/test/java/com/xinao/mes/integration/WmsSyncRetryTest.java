package com.xinao.mes.integration;

import com.xinao.mes.integration.application.WmsSyncService;
import com.xinao.mes.integration.domain.model.SyncLog;
import com.xinao.mes.integration.infrastructure.mapper.SyncLogMapper;
import com.xinao.mes.pack.domain.model.PkBox;
import com.xinao.mes.pack.domain.model.PkExecute;
import com.xinao.mes.pack.domain.model.PkPallet;
import com.xinao.mes.pack.domain.model.PkWmsIn;
import com.xinao.mes.pack.infrastructure.mapper.PkBoxMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkExecuteMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkPalletMapper;
import com.xinao.mes.pack.infrastructure.mapper.PkWmsInMapper;
import com.xinao.mes.plugin.SPI;
import com.xinao.mes.plugin.loader.PluginManager;
import com.xinao.mes.plugin.wms.WmsConnector;
import com.xinao.mes.plugin.wms.WmsInboundMessage;
import com.xinao.mes.plugin.wms.WmsInboundResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** WMS 推送单测：五列重量报文 / 失败落账 / 热替换插件后重试成功闭环 */
@ExtendWith(MockitoExtension.class)
class WmsSyncRetryTest {

    private static final String PALLET = "TP20261007000001";

    private final PluginManager pluginManager = PluginManager.getInstance();

    @Mock
    private SyncLogMapper syncLogMapper;
    @Mock
    private PkPalletMapper palletMapper;
    @Mock
    private PkBoxMapper boxMapper;
    @Mock
    private PkExecuteMapper executeMapper;
    @Mock
    private PkWmsInMapper wmsInMapper;

    private WmsSyncService service;

    /** 故障版插件（模拟旧版 WMS 接口超时） */
    @SPI("wms")
    static class BrokenWms implements WmsConnector {
        @Override
        public WmsInboundResult pushInbound(WmsInboundMessage message) {
            throw new IllegalStateException("WMS接口连接超时(模拟)");
        }
    }

    /** 修复版插件（热替换后生效） */
    @SPI("wms")
    static class FixedWms implements WmsConnector {
        @Override
        public WmsInboundResult pushInbound(WmsInboundMessage message) {
            WmsInboundResult result = new WmsInboundResult();
            result.setSuccess(true);
            result.setProvider("WMS-MOCK-V2");
            result.setWmsDocNo("WMS2-1");
            result.setMessage("ok");
            return result;
        }
    }

    @BeforeEach
    void setUp() {
        pluginManager.clear();
        service = new WmsSyncService(pluginManager, syncLogMapper, palletMapper, boxMapper, executeMapper, wmsInMapper);
    }

    @AfterEach
    void tearDown() {
        pluginManager.clear();
    }

    private void mockPalletData() {
        PkPallet pallet = new PkPallet();
        pallet.setPalletCode(PALLET);
        pallet.setBoxCode("BX20261007000001");
        pallet.setBatchNo("B-2601");
        pallet.setDyeVatNo("V-01");
        pallet.setStandardWeight(new BigDecimal("25.000"));
        when(palletMapper.selectList(any())).thenReturn(List.of(pallet));

        PkBox box = new PkBox();
        box.setBoxCode("BX20261007000001");
        box.setPalletCode(PALLET);
        box.setPkExecuteId(1L);
        box.setSeqNo(1);
        box.setGrossWeight(new BigDecimal("26.100"));
        box.setTareWeight(new BigDecimal("1.100"));
        box.setNetWeight(new BigDecimal("25.000"));
        box.setPublicWeight(new BigDecimal("25.010"));
        when(boxMapper.selectList(any())).thenReturn(List.of(box));

        PkExecute execute = new PkExecute();
        execute.setId(1L);
        execute.setWarehouseBatchNo("FG20261007000003");
        when(executeMapper.selectById(1L)).thenReturn(execute);
    }

    @Test
    void pushFailureFallsLogAndRetrySucceedsAfterPluginSwap() {
        pluginManager.register(new BrokenWms());
        mockPalletData();
        when(syncLogMapper.insert(any(SyncLog.class))).thenAnswer(inv -> {
            inv.getArgument(0, SyncLog.class).setId(5L);
            return 1;
        });

        // 第一次推送：插件故障 → 失败落 log_sync（result=0）
        Map<String, Object> first = service.pushPallet(PALLET);
        assertFalse((Boolean) first.get("success"));
        ArgumentCaptor<SyncLog> failLog = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogMapper).updateById(failLog.capture());
        assertEquals(0, failLog.getValue().getSyncResult());
        // 五列重量已落 log 便于对账
        assertEquals(new BigDecimal("26.100"), failLog.getValue().getGrossWeight());
        assertEquals(new BigDecimal("25.010"), failLog.getValue().getPublicWeight());
        // 失败时未落 WMS 入库记录
        verify(wmsInMapper, org.mockito.Mockito.never()).insert(any(PkWmsIn.class));

        // 热替换插件（同名覆盖 = reload 语义）
        pluginManager.register(new FixedWms());

        // 重推：唯一索引命中失败行 → 走重试路径 → 成功
        SyncLog failedRow = new SyncLog();
        failedRow.setId(5L);
        failedRow.setSyncType(WmsSyncService.SYNC_TYPE_WMS);
        failedRow.setIfaceId(PALLET);
        failedRow.setBizNo(PALLET);
        failedRow.setSyncResult(0);
        failedRow.setRetryCount(1);
        org.mockito.Mockito.reset(syncLogMapper);
        when(syncLogMapper.insert(any(SyncLog.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
        when(syncLogMapper.selectOne(any())).thenReturn(failedRow);

        Map<String, Object> second = service.pushPallet(PALLET);
        assertTrue((Boolean) second.get("success"));
        assertEquals("WMS-MOCK-V2", second.get("provider"));

        // 成功后：log 救活 + 补落 WMS 入库记录（五列重量 + 入库批次）
        ArgumentCaptor<SyncLog> savedLog = ArgumentCaptor.forClass(SyncLog.class);
        verify(syncLogMapper).updateById(savedLog.capture());
        assertEquals(1, savedLog.getValue().getSyncResult());
        assertEquals(2, savedLog.getValue().getRetryCount());
        ArgumentCaptor<PkWmsIn> savedIn = ArgumentCaptor.forClass(PkWmsIn.class);
        verify(wmsInMapper).insert(savedIn.capture());
        assertEquals(PALLET, savedIn.getValue().getPalletCode());
        assertEquals("FG20261007000003", savedIn.getValue().getWarehouseBatchNo());
        assertEquals(new BigDecimal("25.000"), savedIn.getValue().getNetWeight());
    }

    @Test
    void duplicatePushOfSyncedPalletIsSkipped() {
        pluginManager.register(new FixedWms());
        mockPalletData();
        when(syncLogMapper.insert(any(SyncLog.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
        SyncLog synced = new SyncLog();
        synced.setId(5L);
        synced.setSyncResult(1);
        when(syncLogMapper.selectOne(any())).thenReturn(synced);

        Map<String, Object> result = service.pushPallet(PALLET);

        assertTrue((Boolean) result.get("skipped"));
        verify(wmsInMapper, org.mockito.Mockito.never()).insert(any(PkWmsIn.class));
    }
}
