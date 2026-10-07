package com.xinao.mes.plugin.scale;

import com.xinao.mes.plugin.SPI;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 电子秤 mock 实现：模拟串口直读（梅特勒 IND 系列，连续输出协议）。
 *
 * <p>模拟语义：基准 25kg 物件，每次读数带 ±20g 随机波动与 5% 概率的未稳定帧，
 * 连续两次读数可见波动（验收"秤重直读"演示）。
 */
@SPI("scale")
public class MockScaleService implements ScaleService {

    private static final BigDecimal BASE = new BigDecimal("25.000");

    @Override
    public WeightReading read() {
        // 模拟串口帧等待稳定
        sleep(50);
        WeightReading reading = new WeightReading();
        double delta = ThreadLocalRandom.current().nextDouble(-0.020, 0.020);
        reading.setWeight(BASE.add(BigDecimal.valueOf(delta)).setScale(3, RoundingMode.HALF_UP));
        reading.setUnit("kg");
        reading.setStable(ThreadLocalRandom.current().nextInt(20) != 0);
        reading.setDevice("METTLER-IND226(COM3,9600)");
        reading.setReadAt(LocalDateTime.now());
        return reading;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
