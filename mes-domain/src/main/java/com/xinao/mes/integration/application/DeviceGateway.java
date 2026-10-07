package com.xinao.mes.integration.application;

import com.xinao.mes.plugin.loader.PluginManager;
import com.xinao.mes.plugin.pda.PdaConnector;
import com.xinao.mes.plugin.pda.PdaReport;
import com.xinao.mes.plugin.printer.PrintRequest;
import com.xinao.mes.plugin.printer.PrintResult;
import com.xinao.mes.plugin.printer.PrinterService;
import com.xinao.mes.plugin.scale.ScaleService;
import com.xinao.mes.plugin.scale.WeightReading;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 外设网关：打印 / 秤重直读 / PDA 报文适配（薄封装，每次现取插件实例支持热替换） */
@Service
@RequiredArgsConstructor
public class DeviceGateway {

    private final PluginManager pluginManager;

    /** 发送打印任务（制造卡/箱贴/筒贴/吊牌，样式配置化） */
    public PrintResult print(PrintRequest request) {
        if (request.getCopies() == null || request.getCopies() <= 0) {
            request.setCopies(1);
        }
        return pluginManager.get("printer", PrinterService.class).print(request);
    }

    /** 秤重直读（包装称重一体机） */
    public WeightReading readScale() {
        return pluginManager.get("scale", ScaleService.class).read();
    }

    /** PDA 原始报文适配为标准结构 */
    public PdaReport adaptPda(String rawMessage) {
        return pluginManager.get("pda", PdaConnector.class).adapt(rawMessage);
    }
}
