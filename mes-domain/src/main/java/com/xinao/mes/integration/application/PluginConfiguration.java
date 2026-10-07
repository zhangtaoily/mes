package com.xinao.mes.integration.application;

import com.xinao.mes.plugin.loader.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * 插件装配：启动时扫描 plugins 目录加载全部插件 jar。
 *
 * <p>插件 jar 与 fat jar 完全分离（bootstrap 不依赖 mes-plugins 子模块），
 * 替换 plugins 目录中的 jar 并调 /api/plugins/reload 即热替换，核心服务不重启。
 */
@Configuration
@Slf4j
public class PluginConfiguration {

    @Bean(destroyMethod = "shutdown")
    public PluginManager pluginManager(@Value("${mes.plugins.dir:plugins}") String pluginsDir) {
        PluginManager manager = PluginManager.getInstance();
        int count = manager.loadAll(Path.of(pluginsDir));
        log.info("插件加载完成: {} 个实现, 目录: {}", count, Path.of(pluginsDir).toAbsolutePath());
        return manager;
    }
}
