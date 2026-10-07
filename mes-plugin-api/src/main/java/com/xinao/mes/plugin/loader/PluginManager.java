package com.xinao.mes.plugin.loader;

import com.xinao.mes.plugin.SPI;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件 SPI 加载器：目录扫描 + 手动 reload（替换插件 jar 不重启核心服务）。
 *
 * <p>加载模型：每个插件 jar 一个独立 URLClassLoader（parent 指向应用类加载器，
 * 插件可见 mes-plugin-api 契约接口与宿主公共库）；jar 内标注 {@link SPI} 的类
 * 按注解值注册，同名插件后加载者覆盖前者（覆盖时关闭旧 loader）。
 *
 * <p>热替换路径：POST /api/plugins/reload?jar=xxx → {@link #loadJar(Path)} 显式
 * 重新加载 → 宿主每次通过 {@link #get(String, Class)} 取实例（不缓存引用），
 * 新实例即刻生效。
 *
 * <p>单例：宿主 Spring 容器以 @Bean 暴露（见 integration 包 PluginConfiguration）。
 */
public final class PluginManager {

    private static final System.Logger LOG = System.getLogger(PluginManager.class.getName());
    private static final PluginManager INSTANCE = new PluginManager();

    public static PluginManager getInstance() {
        return INSTANCE;
    }

    private PluginManager() {
    }

    /** 已注册插件条目（name → 实例/加载器/来源） */
    private record Entry(Object instance, URLClassLoader loader, String jarPath, LocalDateTime loadTime) {
    }

    private final Map<String, Entry> plugins = new ConcurrentHashMap<>();

    /** 插件清单项 */
    public static final class PluginInfo {
        private final String name;
        private final String className;
        private final String jarPath;
        private final LocalDateTime loadTime;

        PluginInfo(String name, String className, String jarPath, LocalDateTime loadTime) {
            this.name = name;
            this.className = className;
            this.jarPath = jarPath;
            this.loadTime = loadTime;
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }

        public String getJarPath() {
            return jarPath;
        }

        public LocalDateTime getLoadTime() {
            return loadTime;
        }
    }

    /**
     * 扫描目录加载全部插件 jar。
     * 同名 @SPI 冲突时取文件修改时间较新的 jar（按 mtime 升序加载，后加载覆盖）。
     *
     * @return 加载的插件实现总数
     */
    public synchronized int loadAll(Path dir) {
        if (!Files.isDirectory(dir)) {
            LOG.log(System.Logger.Level.INFO, "插件目录不存在，跳过加载: {0}", dir);
            return 0;
        }
        try (var jars = Files.list(dir)) {
            List<Path> sorted = jars.filter(p -> p.toString().endsWith(".jar"))
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .toList();
            int count = 0;
            for (Path jar : sorted) {
                count += loadJar(jar);
            }
            return count;
        } catch (IOException e) {
            throw new IllegalStateException("扫描插件目录失败: " + dir, e);
        }
    }

    /**
     * 加载/热替换单个插件 jar：注册其中全部 @SPI 类，同名旧插件被覆盖（旧 loader 关闭）。
     *
     * @return 该 jar 中加载的插件实现数
     */
    public synchronized int loadJar(Path jar) {
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("插件 jar 不存在: " + jar);
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{toUrl(jar)}, PluginManager.class.getClassLoader());
        List<Class<?>> spiClasses = scanSpiClasses(loader, jar);
        int count = 0;
        for (Class<?> clazz : spiClasses) {
            SPI anno = clazz.getAnnotation(SPI.class);
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                Entry old = plugins.put(anno.value(), new Entry(instance, loader, jar.toString(), LocalDateTime.now()));
                if (old != null) {
                    closeQuietly(old.loader());
                    LOG.log(System.Logger.Level.INFO, "插件[{0}]已替换: {1} -> {2}",
                            anno.value(), old.instance().getClass().getName(), clazz.getName());
                } else {
                    LOG.log(System.Logger.Level.INFO, "插件[{0}]已加载: {1}", anno.value(), clazz.getName());
                }
                count++;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("插件实例化失败: " + clazz.getName(), e);
            }
        }
        if (count == 0) {
            closeQuietly(loader);
            LOG.log(System.Logger.Level.WARNING, "jar 中未发现 @SPI 插件: {0}", jar);
        }
        return count;
    }

    /** 编程式注册（测试用；同名覆盖） */
    public synchronized void register(Object instance) {
        SPI anno = instance.getClass().getAnnotation(SPI.class);
        if (anno == null) {
            throw new IllegalArgumentException("缺少 @SPI 注解: " + instance.getClass().getName());
        }
        plugins.put(anno.value(), new Entry(instance, null, "(registered)", LocalDateTime.now()));
    }

    /** 清空全部插件（测试用） */
    public synchronized void clear() {
        for (Entry e : plugins.values()) {
            closeQuietly(e.loader());
        }
        plugins.clear();
    }

    /** 关闭全部插件加载器（应用停机） */
    public synchronized void shutdown() {
        clear();
    }

    /** 取插件实例（每次调用现取，热替换后即新实例） */
    public <T> T get(String name, Class<T> type) {
        Entry entry = plugins.get(name);
        if (entry == null) {
            throw new IllegalStateException("插件未加载: " + name + "（检查 plugins 目录与 /api/plugins）");
        }
        if (!type.isInstance(entry.instance())) {
            throw new IllegalStateException("插件[" + name + "]实现 " + entry.instance().getClass().getName()
                    + " 不是期望的 " + type.getName());
        }
        return type.cast(entry.instance());
    }

    /** 已加载插件清单（按名排序） */
    public List<PluginInfo> list() {
        return plugins.entrySet().stream()
                .map(e -> new PluginInfo(e.getKey(), e.getValue().instance().getClass().getName(),
                        e.getValue().jarPath(), e.getValue().loadTime()))
                .sorted(Comparator.comparing(PluginInfo::getName))
                .toList();
    }

    private List<Class<?>> scanSpiClasses(URLClassLoader loader, Path jar) {
        List<Class<?>> result = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (!entryName.endsWith(".class")) {
                    continue;
                }
                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                Class<?> clazz;
                try {
                    clazz = Class.forName(className, false, loader);
                } catch (Throwable t) {
                    continue;
                }
                if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }
                if (clazz.isAnnotationPresent(SPI.class)) {
                    result.add(clazz);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取插件 jar 失败: " + jar, e);
        }
        return result;
    }

    private static URL toUrl(Path jar) {
        try {
            return jar.toUri().toURL();
        } catch (IOException e) {
            throw new IllegalStateException("插件路径转换失败: " + jar, e);
        }
    }

    private static void closeQuietly(URLClassLoader loader) {
        if (loader != null) {
            try {
                loader.close();
            } catch (IOException ignored) {
                // 尽力而为：旧实例仍在使用时关闭失败不影响新插件生效
            }
        }
    }
}
