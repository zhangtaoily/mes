package com.xinao.mes.integration.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.integration.domain.model.SyncLog;
import com.xinao.mes.integration.infrastructure.mapper.SyncLogMapper;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.infrastructure.mapper.ErpK3OrderMapper;
import com.xinao.mes.plugin.erp.ErpConnector;
import com.xinao.mes.plugin.loader.PluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * K3 订单同步（k3 插件编排 + log_sync 幂等）。
 *
 * <p>幂等模型：逐单以 K3 内码（iface_id）占位 log_sync——
 * 唯一索引命中且已成功 → 重推跳过；命中但失败 → 重放该单；
 * erp_k3_order 侧按 order_no upsert（保留引入进度与关闭标记）。
 *
 * <p>字段映射：插件报文 key = erp_k3_order 属性名（PRD 3.1 的 49 字段映射），
 * Jackson 一步转实体；日期格式 yyyy-MM-dd[ HH:mm:ss]。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class K3SyncService {

    public static final String SYNC_TYPE_K3 = "K3订单";
    public static final String SYNC_TYPE_K3_WRITEBACK = "K3回写";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(javaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final PluginManager pluginManager;
    private final SyncLogMapper syncLogMapper;
    private final ErpK3OrderMapper k3Mapper;

    /** 拉取并同步 K3 订单（重推幂等：同 K3 内码只落一次） */
    public Map<String, Object> syncOrders() {
        ErpConnector k3 = pluginManager.get("k3", ErpConnector.class);
        long begin = System.currentTimeMillis();
        List<Map<String, Object>> orders = k3.pullOrders(null);
        int inserted = 0, updated = 0, skipped = 0, failed = 0;
        for (Map<String, Object> raw : orders) {
            ErpK3Order order = MAPPER.convertValue(raw, ErpK3Order.class);
            if (order.getK3OrderId() == null || order.getK3OrderId().isBlank()) {
                throw new BizException("K3订单缺少接口幂等键 k3OrderId: " + order.getOrderNo());
            }
            // 占位：唯一索引 (sync_type, iface_id) 命中 = 重推
            SyncLog syncLog = newLog(order);
            boolean isRetry = false;
            try {
                syncLogMapper.insert(syncLog);
            } catch (DuplicateKeyException dup) {
                SyncLog exist = findByIface(SYNC_TYPE_K3, order.getK3OrderId());
                if (exist != null && Integer.valueOf(1).equals(exist.getSyncResult())) {
                    skipped++;
                    continue;
                }
                syncLog = exist;
                isRetry = true;
            }
            long t0 = System.currentTimeMillis();
            try {
                if (upsertOrder(order)) {
                    updated++;
                } else {
                    inserted++;
                }
                syncLog.setSyncResult(1);
                syncLog.setCostSeconds(cost(t0));
                syncLog.setRespParam(isRetry ? "重试成功" : "ok");
                if (isRetry) {
                    syncLog.setRetryCount((syncLog.getRetryCount() == null ? 0 : syncLog.getRetryCount()) + 1);
                }
                syncLogMapper.updateById(syncLog);
            } catch (Exception e) {
                log.warn("K3订单同步失败: {} {}", order.getOrderNo(), e.getMessage());
                syncLog.setSyncResult(0);
                syncLog.setCostSeconds(cost(t0));
                syncLog.setRespParam(truncate("失败: " + e.getMessage()));
                syncLogMapper.updateById(syncLog);
                failed++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pulled", orders.size());
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("failed", failed);
        result.put("costMs", System.currentTimeMillis() - begin);
        return result;
    }

    /** 回写 K3：引入量 / 关闭状态（可重复动作，ifaceId 不占幂等键） */
    public Map<String, Object> writeBack(String orderNo, BigDecimal introducedQty, boolean closed) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BizException("订单号不能为空");
        }
        ErpConnector k3 = pluginManager.get("k3", ErpConnector.class);
        long t0 = System.currentTimeMillis();
        Long k3DocId = k3.writeBack(orderNo, introducedQty, closed);
        // 本地镜像联动关闭标记（引入量在 M2 引入动作已 CAS 回写）
        ErpK3Order mirror = k3Mapper.selectOne(Wrappers.<ErpK3Order>lambdaQuery()
                .eq(ErpK3Order::getOrderNo, orderNo));
        if (mirror != null) {
            int target = closed ? 1 : 0;
            int current = mirror.getIsClosed() == null ? 0 : mirror.getIsClosed();
            if (current != target) {
                ErpK3Order patch = new ErpK3Order();
                patch.setId(mirror.getId());
                patch.setIsClosed(target);
                k3Mapper.updateById(patch);
            }
        }
        SyncLog syncLog = new SyncLog();
        syncLog.setSyncType(SYNC_TYPE_K3_WRITEBACK);
        syncLog.setOpName("引入量回写");
        syncLog.setBizNo(orderNo);
        syncLog.setSyncResult(1);
        syncLog.setCostSeconds(cost(t0));
        syncLog.setReqParam(truncate("qty=" + introducedQty + ", closed=" + closed));
        syncLog.setRespParam("K3单据内码=" + k3DocId);
        syncLogMapper.insert(syncLog);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("k3DocId", k3DocId);
        result.put("introducedQty", introducedQty);
        result.put("closed", closed);
        return result;
    }

    /** 重试单条失败记录（SyncRetryJob 路由调用，重放 reqParam 报文） */
    public boolean replay(SyncLog failedLog) {
        try {
            ErpK3Order order = MAPPER.readValue(failedLog.getReqParam(), ErpK3Order.class);
            upsertOrder(order);
            SyncLog patch = new SyncLog();
            patch.setId(failedLog.getId());
            patch.setSyncResult(1);
            patch.setRetryCount((failedLog.getRetryCount() == null ? 0 : failedLog.getRetryCount()) + 1);
            patch.setRespParam("重试成功(报文重放)");
            syncLogMapper.updateById(patch);
            return true;
        } catch (Exception e) {
            log.warn("K3订单重试失败: {} {}", failedLog.getBizNo(), e.getMessage());
            SyncLog patch = new SyncLog();
            patch.setId(failedLog.getId());
            patch.setRetryCount((failedLog.getRetryCount() == null ? 0 : failedLog.getRetryCount()) + 1);
            patch.setRespParam(truncate("重试失败: " + e.getMessage()));
            syncLogMapper.updateById(patch);
            return false;
        }
    }

    /** upsert 镜像：新单插入 / 已有单更新（保留引入进度 importedQty 与 isClosed） */
    private boolean upsertOrder(ErpK3Order order) {
        LocalDateTime now = LocalDateTime.now();
        ErpK3Order exist = k3Mapper.selectOne(Wrappers.<ErpK3Order>lambdaQuery()
                .eq(ErpK3Order::getOrderNo, order.getOrderNo()));
        if (exist == null) {
            order.setMesCreateTime(now);
            k3Mapper.insert(order);
            return false;
        }
        order.setId(exist.getId());
        order.setImportedQty(exist.getImportedQty());
        order.setIsClosed(exist.getIsClosed());
        order.setMesCreateTime(exist.getMesCreateTime());
        order.setMesUpdateTime(now);
        k3Mapper.updateById(order);
        return true;
    }

    private SyncLog newLog(ErpK3Order order) {
        SyncLog syncLog = new SyncLog();
        syncLog.setSyncType(SYNC_TYPE_K3);
        syncLog.setOpName("订单同步");
        syncLog.setBizNo(order.getOrderNo());
        syncLog.setIfaceId(order.getK3OrderId());
        try {
            syncLog.setReqParam(truncate(MAPPER.writeValueAsString(order)));
        } catch (Exception ignored) {
            syncLog.setReqParam(truncate(String.valueOf(order.getOrderNo())));
        }
        return syncLog;
    }

    public SyncLog findByIface(String syncType, String ifaceId) {
        return syncLogMapper.selectOne(Wrappers.<SyncLog>lambdaQuery()
                .eq(SyncLog::getSyncType, syncType)
                .eq(SyncLog::getIfaceId, ifaceId));
    }

    private static JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME));
        return module;
    }

    private static BigDecimal cost(long t0) {
        return BigDecimal.valueOf(System.currentTimeMillis() - t0, 3);
    }

    private static String truncate(String s) {
        return s != null && s.length() > 2000 ? s.substring(0, 2000) : s;
    }
}
