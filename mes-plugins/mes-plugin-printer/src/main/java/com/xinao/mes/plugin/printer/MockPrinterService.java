package com.xinao.mes.plugin.printer;

import com.xinao.mes.plugin.SPI;

/**
 * 打印服务 mock 实现：生成 ZPL 指令入打印队列（模拟斑马 ZT411 网络打印机）。
 *
 * <p>真实场景按 base_print_template 的样式配置渲染标签；本 mock 输出
 * 含二维码与关键数据的简化 ZPL，回执 spool 字段可直接查看指令内容。
 */
@SPI("printer")
public class MockPrinterService implements PrinterService {

    @Override
    public PrintResult print(PrintRequest request) {
        if (request.getTemplateType() == null || request.getTemplateType().isBlank()) {
            throw new IllegalArgumentException("打印模板类型缺失");
        }
        PrintResult result = new PrintResult();
        result.setJobId("PRN-" + System.currentTimeMillis());
        result.setStatus("QUEUED");
        result.setPrinter("ZEBRA-ZT411");
        result.setMessage("已发送至打印队列: " + request.getTemplateType() + " x" + (request.getCopies() == null ? 1 : request.getCopies()));
        result.setSpool(zpl(request));
        return result;
    }

    private String zpl(PrintRequest r) {
        String safeData = r.getDataJson() == null ? "" : r.getDataJson().replace("\"", "'");
        return "^XA\n"
                + "^CW1,E:TT0003M_.FNT\n"
                + "^FO50,50^A0N,40,40^FD" + r.getTemplateType() + "^FS\n"
                + "^FO50,110^A0N,30,30^FDNO:" + r.getBizNo() + "^FS\n"
                + "^FO50,170^A0N,24,24^FB700,3,0,L^FD" + abbreviate(safeData, 120) + "^FS\n"
                + "^FO560,320^BQN,2,6^FDMA," + r.getBizNo() + "^FS\n"
                + "^XZ";
    }

    private String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
