package com.xinao.mes.plugin.pda;

/**
 * PDA 报文适配 SPI（pda 插件实现）
 *
 * <p>不同型号 PDA 的上报报文格式不同，经插件适配为标准结构后走共用 application 层（PRD：PDA 与 PC 仅 BFF 差异）。
 */
public interface PdaConnector {

    /**
     * 适配 PDA 原始上报报文
     *
     * @param rawMessage PDA 原始报文（JSON）
     * @return 标准上报结构
     */
    PdaReport adapt(String rawMessage);
}
