package com.xinao.mes.bootstrap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MES 启动器：组装 common / domain / engine / plugin 各模块
 *
 * <p>@EnableScheduling：M5 同步失败重试定时扫描（sync_log 状态驱动，XXL-Job 兜底的最简实现）。
 */
@SpringBootApplication(scanBasePackages = "com.xinao.mes")
@MapperScan("com.xinao.mes.**.mapper")
@EnableScheduling
public class MesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MesApplication.class, args);
    }
}
