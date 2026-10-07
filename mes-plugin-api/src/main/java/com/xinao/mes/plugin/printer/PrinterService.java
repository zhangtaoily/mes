package com.xinao.mes.plugin.printer;

/**
 * 打印服务 SPI（printer 插件实现）
 *
 * <p>制造卡 / 箱贴 / 筒贴 / 吊牌统一走打印插件，模板与样式配置化（base_print_template）。
 */
public interface PrinterService {

    /**
     * 发送打印任务
     *
     * @param request 打印请求（模板类型 + 业务号 + 数据）
     * @return 打印回执（任务号 + 打印机队列）
     */
    PrintResult print(PrintRequest request);
}
