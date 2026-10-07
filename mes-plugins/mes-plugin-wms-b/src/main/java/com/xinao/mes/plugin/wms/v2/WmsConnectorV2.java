package com.xinao.mes.plugin.wms.v2;

import com.xinao.mes.plugin.SPI;
import com.xinao.mes.plugin.wms.WmsConnector;
import com.xinao.mes.plugin.wms.WmsInboundMessage;
import com.xinao.mes.plugin.wms.WmsInboundResult;

import java.util.stream.Collectors;

/**
 * WMS 连接器 V2：嵌套报文（header/payload 包裹）+ 消息头校验。
 *
 * <p>与 V1 同名注册（@SPI("wms")），经 /api/plugins/reload 热替换后生效——
 * 宿主只依赖 {@link WmsConnector} 契约，协议变化不重启核心服务。
 * V2 已修复 V1 的超时故障，历史失败记录重试即成功。
 */
@SPI("wms")
public class WmsConnectorV2 implements WmsConnector {

    @Override
    public WmsInboundResult pushInbound(WmsInboundMessage message) {
        if (message.getPalletCode() == null || message.getPalletCode().isBlank()) {
            throw new IllegalStateException("V2校验失败：托盘号缺失");
        }
        WmsInboundResult result = new WmsInboundResult();
        result.setSuccess(true);
        result.setProvider("WMS-MOCK-V2");
        result.setWmsDocNo("WMS2-" + System.currentTimeMillis());
        result.setMessage("新版接口受理成功(嵌套报文+头部校验)");
        result.setReqPayload(nestedJson(message));
        result.setRespPayload(String.format(
                "{\"header\":{\"version\":\"2.0\",\"receiver\":\"WMS\"},\"body\":{\"code\":0,\"docNo\":\"%s\"}}",
                result.getWmsDocNo()));
        return result;
    }

    private String nestedJson(WmsInboundMessage m) {
        String boxes = m.getBoxes() == null ? "" : m.getBoxes().stream()
                .map(b -> String.format("{\"box\":\"%s\",\"w\":{\"g\":%s,\"t\":%s,\"n\":%s,\"p\":%s}}",
                        b.getBoxCode(), b.getGrossWeight(), b.getTareWeight(), b.getNetWeight(), b.getPublicWeight()))
                .collect(Collectors.joining(","));
        return String.format(
                "{\"header\":{\"version\":\"2.0\",\"sender\":\"MES\",\"pallet\":\"%s\"},"
                        + "\"payload\":{\"whBatch\":\"%s\",\"batch\":\"%s\",\"vat\":\"%s\",\"boxCount\":%d,"
                        + "\"weight\":{\"gross\":%s,\"tare\":%s,\"net\":%s,\"public\":%s},\"boxes\":[%s]}}",
                m.getPalletCode(), m.getWarehouseBatchNo(), m.getBatchNo(), m.getDyeVatNo(),
                m.getBoxCount() == null ? 0 : m.getBoxCount(),
                m.getTotalGrossWeight(), m.getTotalTareWeight(), m.getTotalNetWeight(), m.getTotalPublicWeight(),
                boxes);
    }
}
