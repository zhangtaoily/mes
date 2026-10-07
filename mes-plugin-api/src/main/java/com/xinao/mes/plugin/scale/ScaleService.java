package com.xinao.mes.plugin.scale;

/**
 * 电子秤服务 SPI（scale 插件实现）
 *
 * <p>包装称重一体机直读秤重（串口/TCP 适配由插件屏蔽），宿主拿稳定读数回填包装执行。
 */
public interface ScaleService {

    /** 读取一次称重读数（等待稳定） */
    WeightReading read();
}
