package com.xinao.mes.integration.controller;

import com.xinao.mes.common.auth.RequiresPerm;
import com.xinao.mes.common.log.OperLog;
import com.xinao.mes.common.result.R;
import com.xinao.mes.plugin.loader.PluginManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** 插件管理：清单查询 + 热替换（替换 jar 不重启核心服务） */
@RestController
@RequestMapping("/api/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginManager pluginManager;

    /** 已加载插件清单（name/class/jar/loadTime——loadTime 不变即服务未重启的证据） */
    @GetMapping
    @RequiresPerm("integration:plugin:list")
    public R<?> list() {
        return R.ok(pluginManager.list());
    }

    /** 热替换单个插件 jar（同 jar 内 @SPI 类重新注册，同名旧插件被卸载） */
    @PostMapping("/reload")
    @RequiresPerm("integration:plugin:manage")
    @OperLog(title = "插件热替换")
    public R<?> reload(@RequestParam String jar) {
        Path path = Path.of(jar);
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir"), jar);
        }
        if (!path.toString().endsWith(".jar") || !Files.isRegularFile(path)) {
            return R.fail("插件 jar 不存在: " + path);
        }
        int loaded = pluginManager.loadJar(path);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jar", path.toString());
        result.put("loadedImplementations", loaded);
        result.put("plugins", pluginManager.list());
        return R.ok(result);
    }
}
