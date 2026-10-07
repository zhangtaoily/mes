package com.xinao.mes.integration;

import com.xinao.mes.plugin.SPI;
import com.xinao.mes.plugin.erp.ErpConnector;
import com.xinao.mes.plugin.loader.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** PluginManager 单测：注册路由 / 类型检查 / 同名覆盖（热替换语义） */
class PluginManagerTest {

    private final PluginManager manager = PluginManager.getInstance();

    @BeforeEach
    @AfterEach
    void clean() {
        manager.clear();
    }

    @SPI("k3")
    static class StubK3 implements ErpConnector {
        @Override
        public List<Map<String, Object>> pullOrders(LocalDateTime startTime) {
            return List.of();
        }

        @Override
        public Long writeBack(String orderNo, java.math.BigDecimal introducedQty, boolean closed) {
            return 1L;
        }
    }

    @SPI("k3")
    static class StubK3V2 implements ErpConnector {
        @Override
        public List<Map<String, Object>> pullOrders(LocalDateTime startTime) {
            return List.of();
        }

        @Override
        public Long writeBack(String orderNo, java.math.BigDecimal introducedQty, boolean closed) {
            return 2L;
        }
    }

    @SPI("not-erp")
    static class NotAnErp {
    }

    @Test
    void registerAndGet() {
        manager.register(new StubK3());
        ErpConnector connector = manager.get("k3", ErpConnector.class);
        assertInstanceOf(StubK3.class, connector);
        assertEquals(1L, connector.writeBack("x", null, false));
    }

    @Test
    void getUnloadedPluginThrows() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> manager.get("wms", ErpConnector.class));
        assertEquals("插件未加载: wms（检查 plugins 目录与 /api/plugins）", ex.getMessage());
    }

    @Test
    void typeMismatchThrows() {
        manager.register(new NotAnErp());
        assertThrows(IllegalStateException.class, () -> manager.get("not-erp", ErpConnector.class));
    }

    @Test
    void registerOverridesSameNamePlugin() {
        manager.register(new StubK3());
        manager.register(new StubK3V2());
        // 热替换语义：同名后注册者生效，宿主无需缓存失效
        ErpConnector connector = manager.get("k3", ErpConnector.class);
        assertInstanceOf(StubK3V2.class, connector);
        assertEquals(2L, connector.writeBack("x", null, false));
        assertEquals(1, manager.list().size());
    }

    @Test
    void registerWithoutSpiAnnotationThrows() {
        assertThrows(IllegalArgumentException.class, () -> manager.register(new Object()));
    }
}
