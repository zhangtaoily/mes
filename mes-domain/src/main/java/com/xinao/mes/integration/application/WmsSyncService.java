package com.xinao.mes.integration.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinao.mes.common.exception.BizException;
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
import com.xinao.mes.plugin.loader.PluginManager;
import com.xinao.mes.plugin.wms.WmsConnector;
import com.xinao.mes.plugin.wms.WmsInboundMessage;
import com.xinao.mes.plugin.wms.WmsInboundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WMS 入库推送（wms 插件编排 + 五列重量报文 + log_sync 幂等）。
 *
 * <p>组托完成后按托推送：托内箱子五列重量（毛/皮/净/公 + 标重）汇总成报文，
 * 以托盘号为幂等键——同托重推命中唯一索引跳过；推送失败落 log_sync 等待重试，
 * 重试成功补落 pk_wms_in。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WmsSyncService {

    public static final String SYNC_TYPE_WMS = "WMS入库";

    private final PluginManager pluginManager;
    private final SyncLogMapper syncLogMapper;
    private final PkPalletMapper palletMapper;
    private final PkBoxMapper boxMapper;
    private final PkExecuteMapper executeMapper;
    private final PkWmsInMapper wmsInMapper;

    /** 推送一托入库报文（幂等：同托只推一次成功） */
    public Map<String, Object> pushPallet(String palletCode) {
        if (StringUtils.isBlank(palletCode)) {
            throw new BizException("托盘号不能为空");
        }
        WmsInboundMessage message = buildMessage(palletCode);
        long t0 = System.currentTimeMillis();
        // 占位：唯一索引 (sync_type=WMS入库, iface_id=托盘号) 命中 = 重推
        SyncLog syncLog = newSyncLog(message);
        boolean isRetry = false;
        try {
            syncLogMapper.insert(syncLog);
        } catch (DuplicateKeyException dup) {
            SyncLog exist = syncLogMapper.selectOne(Wrappers.<SyncLog>lambdaQuery()
                    .eq(SyncLog::getSyncType, SYNC_TYPE_WMS)
                    .eq(SyncLog::getIfaceId, palletCode));
            if (exist != null && Integer.valueOf(1).equals(exist.getSyncResult())) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("palletCode", palletCode);
                result.put("skipped", true);
                result.put("message", "该托已推送成功，重推被幂等拦截");
                return result;
            }
            syncLog = exist;
            isRetry = true;
        }
        WmsConnector wms = pluginManager.get("wms", WmsConnector.class);
        try {
            WmsInboundResult inbound = wms.pushInbound(message);
            // 受理成功 → 落 WMS 入库记录
            saveWmsIn(message, inbound);
            syncLog.setSyncResult(1);
            syncLog.setCostSeconds(cost(t0));
            syncLog.setRespParam(truncate(inbound.getRespPayload()));
            syncLog.setReqParam(truncate(inbound.getReqPayload()));
            if (isRetry) {
                syncLog.setRetryCount((syncLog.getRetryCount() == null ? 0 : syncLog.getRetryCount()) + 1);
            }
            syncLogMapper.updateById(syncLog);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("palletCode", palletCode);
            result.put("skipped", false);
            result.put("success", inbound.isSuccess());
            result.put("provider", inbound.getProvider());
            result.put("wmsDocNo", inbound.getWmsDocNo());
            result.put("message", inbound.getMessage());
            return result;
        } catch (Exception e) {
            log.warn("WMS推送失败: {} {}", palletCode, e.getMessage());
            syncLog.setSyncResult(0);
            syncLog.setCostSeconds(cost(t0));
            syncLog.setRespParam(truncate("推送失败: " + e.getMessage()));
            syncLogMapper.updateById(syncLog);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("palletCode", palletCode);
            result.put("skipped", false);
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /** 按托组装五列重量报文（箱子明细 + 托级汇总） */
    private WmsInboundMessage buildMessage(String palletCode) {
        List<PkPallet> pallets = palletMapper.selectList(Wrappers.<PkPallet>lambdaQuery()
                .eq(PkPallet::getPalletCode, palletCode)
                .orderByAsc(PkPallet::getBoxCode));
        if (pallets.isEmpty()) {
            throw new BizException("托盘不存在或未组托: " + palletCode);
        }
        List<PkBox> boxes = boxMapper.selectList(Wrappers.<PkBox>lambdaQuery()
                .eq(PkBox::getPalletCode, palletCode)
                .orderByAsc(PkBox::getSeqNo));
        if (boxes.isEmpty()) {
            throw new BizException("托盘下无箱子: " + palletCode);
        }
        PkExecute execute = executeMapper.selectById(boxes.get(0).getPkExecuteId());
        WmsInboundMessage message = new WmsInboundMessage();
        message.setPalletCode(palletCode);
        message.setWarehouseBatchNo(execute == null ? null : execute.getWarehouseBatchNo());
        message.setBatchNo(pallets.get(0).getBatchNo());
        message.setDyeVatNo(pallets.get(0).getDyeVatNo());
        message.setBoxCount(boxes.size());
        message.setStandardWeight(pallets.get(0).getStandardWeight());
        message.setTotalGrossWeight(sum(boxes, PkBox::getGrossWeight));
        message.setTotalTareWeight(sum(boxes, PkBox::getTareWeight));
        message.setTotalNetWeight(sum(boxes, PkBox::getNetWeight));
        message.setTotalPublicWeight(sum(boxes, PkBox::getPublicWeight));
        message.setBoxes(boxes.stream().map(b -> {
            WmsInboundMessage.BoxItem item = new WmsInboundMessage.BoxItem();
            item.setBoxCode(b.getBoxCode());
            item.setGrossWeight(b.getGrossWeight());
            item.setTareWeight(b.getTareWeight());
            item.setNetWeight(b.getNetWeight());
            item.setPublicWeight(b.getPublicWeight());
            return item;
        }).toList());
        return message;
    }

    private void saveWmsIn(WmsInboundMessage message, WmsInboundResult inbound) {
        // 重试成功路径可能已有失败前未落库记录：成功才落库，故无重复
        PkWmsIn wmsIn = new PkWmsIn();
        wmsIn.setWarehouseBatchNo(message.getWarehouseBatchNo());
        wmsIn.setBatchNo(message.getBatchNo());
        wmsIn.setDyeVatNo(message.getDyeVatNo());
        wmsIn.setPalletCode(message.getPalletCode());
        wmsIn.setBoxCode("共" + message.getBoxCount() + "箱");
        wmsIn.setPublicWeight(message.getTotalPublicWeight());
        wmsIn.setGrossWeight(message.getTotalGrossWeight());
        wmsIn.setTareWeight(message.getTotalTareWeight());
        wmsIn.setNetWeight(message.getTotalNetWeight());
        wmsIn.setInTime(LocalDateTime.now());
        wmsIn.setWmsStatus("已同步[" + inbound.getProvider() + "]");
        wmsInMapper.insert(wmsIn);
    }

    private SyncLog newSyncLog(WmsInboundMessage message) {
        SyncLog syncLog = new SyncLog();
        syncLog.setSyncType(SYNC_TYPE_WMS);
        syncLog.setOpName("组托入库推送");
        syncLog.setBizNo(message.getPalletCode());
        syncLog.setIfaceId(message.getPalletCode());
        syncLog.setPublicWeight(message.getTotalPublicWeight());
        syncLog.setGrossWeight(message.getTotalGrossWeight());
        syncLog.setTareWeight(message.getTotalTareWeight());
        syncLog.setNetWeight(message.getTotalNetWeight());
        return syncLog;
    }

    private BigDecimal sum(List<PkBox> boxes, java.util.function.Function<PkBox, BigDecimal> getter) {
        return boxes.stream()
                .map(getter)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal cost(long t0) {
        return BigDecimal.valueOf(System.currentTimeMillis() - t0, 3);
    }

    private static String truncate(String s) {
        return s != null && s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}
