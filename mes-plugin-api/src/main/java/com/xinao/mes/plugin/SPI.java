package com.xinao.mes.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 插件 SPI 标识注解
 *
 * <p>标注在插件实现类上，值对应插件名：k3 / wms / printer / scale / pda。
 * SPI 加载器按注解值路由到具体实现，支持 jar 目录扫描 + 手动 reload（替换插件不重启核心服务）。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {

    /** 插件名，如 "k3"、"wms" */
    String value();
}
