package com.xinao.mes.plugin.pda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinao.mes.plugin.SPI;

/**
 * PDA 报文适配实现：优博讯 MF919 上报格式。
 *
 * <p>原始报文形如：
 * {@code {"device":"MF919","cmd":"SCAN","user":"011365","data":{"code":"XJP2610456-12","type":"上批"}}}
 * 适配为标准 {@link PdaReport} 后走与 PC 共用的 application 层。
 */
@SPI("pda")
public class UrovoPdaConnector implements PdaConnector {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public PdaReport adapt(String rawMessage) {
        try {
            JsonNode root = mapper.readTree(rawMessage);
            PdaReport report = new PdaReport();
            report.setDeviceModel(root.path("device").asText("UNKNOWN"));
            report.setUserCode(root.path("user").asText(null));
            JsonNode data = root.path("data");
            report.setScanCode(data.path("code").asText(null));
            report.setReportType(data.path("type").asText("上批"));
            report.setRaw(rawMessage);
            if (report.getScanCode() == null || report.getScanCode().isBlank()) {
                throw new IllegalArgumentException("PDA报文缺少扫码内容: " + rawMessage);
            }
            return report;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("PDA报文解析失败: " + rawMessage, e);
        }
    }
}
