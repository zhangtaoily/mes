package com.xinao.mes.plugin.wms;

import lombok.Data;

/** WMS 入库回执 */
@Data
public class WmsInboundResult {

    /** 是否受理成功 */
    private boolean success;
    /** 插件提供方标识（如 WMS-MOCK-V1 / WMS-MOCK-V2，用于验证热替换生效） */
    private String provider;
    /** WMS 侧单据号 */
    private String wmsDocNo;
    /** 说明（失败原因等） */
    private String message;
    /** 实际发送的协议报文（落 log_sync.req_param） */
    private String reqPayload;
    /** WMS 返回的协议报文（落 log_sync.resp_param） */
    private String respPayload;
}
