package com.xinao.mes.plugin.wms.v1;

import com.xinao.mes.plugin.SPI;
import com.xinao.mes.plugin.wms.WmsConnector;
import com.xinao.mes.plugin.wms.WmsInboundMessage;
import com.xinao.mes.plugin.wms.WmsInboundResult;

import java.util.stream.Collectors;

/**
 * WMS 连接器 V1：扁平 JSON 报文（模拟旧版 WMS HTTP 接口）。
 *
 * <p>内置故障规则：托盘号以 000001 结尾的推送抛异常（模拟接口连接超时），
 * 用于验收"失败落 log_sync → 热替换 V2 插件 → 重试成功"闭环。
 */
@SPI("wms")
public class WmsConnectorV1 implements WmsConnector {

    @Override
    public WmsInboundResult pushInbound(WmsInboundMessage message) {
        if (message.getPalletCode() != null && message.getPalletCode().endsWith("000001")) {
            throw new IllegalStateException("WMS接口连接超时(模拟)：POST /wms/api/v1/inbound 失败");
        }
        WmsInboundResult result = new WmsInboundResult();
        result.setSuccess(true);
        result.setProvider("WMS-MOCK-V1");
        result.setWmsDocNo("WMS-IN-" + System.currentTimeMillis());
        result.setMessage("旧版接口受理成功");
        result.setReqPayload(flatJson(message));
        result.setRespPayload("{\"code\":200,\"msg\":\"ok\",\"version\":\"1.0\"}");
        return result;
    }

    private String flatJson(WmsInboundMessage m) {
        String boxes = m.getBoxes() == null ? "" : m.getBoxes().stream()
                .map(b -> String.format("{\"boxCode\":\"%s\",\"gross\":%s,\"tare\":%s,\"net\":%s,\"public\":%s}",
                        b.getBoxCode(), b.getGrossWeight(), b.getTareWeight(), b.getNetWeight(), b.getPublicWeight()))
                .collect(Collectors.joining(","));
        return String.format(
                "{\"palletCode\":\"%s\",\"warehouseBatchNo\":\"%s\",\"batchNo\":\"%s\",\"dyeVatNo\":\"%s\","
                        + "\"boxCount\":%d,\"totalGross\":%s,\"totalTare\":%s,\"totalNet\":%s,\"totalPublic\":%s,"
                        + "\"boxes\":[%s]}",
                m.getPalletCode(), m.getWarehouseBatchNo(), m.getBatchNo(), m.getDyeVatNo(),
                m.getBoxCount() == null ? 0 : m.getBoxCount(),
                m.getTotalGrossWeight(), m.getTotalTareWeight(), m.getTotalNetWeight(), m.getTotalPublicWeight(),
                boxes);
    }
}
