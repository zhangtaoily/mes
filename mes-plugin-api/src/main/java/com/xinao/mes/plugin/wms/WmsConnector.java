package com.xinao.mes.plugin.wms;

/**
 * WMS 连接器 SPI（wms 插件实现）
 *
 * <p>MES 在组托完成后按托推送入库报文（箱号/托盘号/五列重量），
 * 宿主侧经 log_sync 以托盘号为幂等键去重，重试由宿主调度。
 */
public interface WmsConnector {

    /**
     * 推送一托入库报文
     *
     * @param message 托级报文（含箱明细与五列重量汇总）
     * @return WMS 回执（含 provider 标识，便于区分插件版本）
     */
    WmsInboundResult pushInbound(WmsInboundMessage message);
}
