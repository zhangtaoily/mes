package com.xinao.mes.plugin.k3;

import com.xinao.mes.plugin.SPI;
import com.xinao.mes.plugin.erp.ErpConnector;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * K3 连接器 mock 实现：模拟金蝶 K3 WebService（联调前占位）。
 *
 * <p>每次 pullOrders 返回同一批订单（模拟 K3 侧重推/重复拉取——幂等由宿主
 * log_sync 的 (sync_type, iface_id) 唯一索引兜底）；其中 FBDC772348 为
 * V4 种子已有订单，验证 upsert 更新路径不重复插入。
 *
 * <p>报文字段 = erp_k3_order 属性名（PRD 3.1 生产信息 49 字段映射，camelCase），
 * 宿主用 Jackson convertValue 直接转实体。
 */
@SPI("k3")
public class MockK3Connector implements ErpConnector {

    @Override
    public List<Map<String, Object>> pullOrders(LocalDateTime startTime) {
        return List.of(
                order("K3-100101", "FBDC772350", "2026-10-01", "纺十车间", "纺纱一线", "26100101",
                        "5000.000", "2026-10-12", "2026-10-30", "HY-M63635", "澳毛精纺纱(绞染)",
                        "2/60 NM", "1.0000", "绞纱", "4.1.1.003", "澳毛条", "100%澳毛",
                        "浙江纺织", "国内销售部", "王芳", "梭织", "70% 澳毛条 HY-M63635-浅灰", "浅灰"),
                order("K3-100102", "FBDC772351", "2026-10-03", "纺五车间", "纺纱二线", "26100302",
                        "2200.000", "2026-10-15", "2026-11-05", "TD-88125", "全毛针织纱(条染)",
                        "2/48 NM", "1.0000", "条染", "4.1.1.002", "澳毛条", "100%澳毛",
                        "上海毛麻进出口", "外贸销售部", "李强", "横机", "100% 澳毛条 TD-88125-藏青", "藏青"),
                order("K3-100103", "FBDC772352", "2026-10-05", "纺十车间", "纺纱一线", "26100503",
                        "3600.000", "2026-10-18", "2026-11-10", "GS-2026-77", "丝毛混纺纱(绞染)",
                        "1/45 NM", "1.0000", "绞纱", "4.1.1.004", "桑蚕丝条", "55%羊毛45%桑蚕丝",
                        "江苏悦达家纺", "国内销售部", "赵敏", "梭织", "55% 澳毛条 GS-2026-77-米白", "米白"),
                // 已存在于 V4 种子的订单：重推验证 upsert（按 order_no 更新而非重复插入）
                order("K3-100001", "FBDC772348", "2026-09-20", "纺十车间", "纺纱一线", "26091340",
                        "3000.000", "2026-10-08", "2026-10-25", "HY-M63634", "莱赛尔桑蚕丝纤维(绞染)",
                        "2/60 NM", "1.0000", "绞纱", "4.1.1.001", "澳毛条", "70%澳毛30%桑蚕丝",
                        "浙江纺织", "国内销售部", "王芳", "梭织", "70% 澳毛条 HY-M63634-中灰", "中灰"));
    }

    @Override
    public Long writeBack(String orderNo, BigDecimal introducedQty, boolean closed) {
        // 模拟 K3 销售订单下推/回写接口成功，返回 K3 单据内码
        return 900_001L + Math.abs(orderNo == null ? 0 : orderNo.hashCode() % 1000);
    }

    private Map<String, Object> order(String k3OrderId, String orderNo, String orderDate,
                                      String workshop, String prodLine, String batchNo, String qty,
                                      String planStart, String planEnd, String colorNo, String productName,
                                      String spinCount, String convertRate, String dyeType,
                                      String materialNo, String materialName, String materialSpec,
                                      String customer, String saleDept, String saleUser, String purpose,
                                      String auxAttr, String color) {
        return Map.ofEntries(
                Map.entry("k3OrderId", k3OrderId),
                Map.entry("orderType", "调入"),
                Map.entry("orderNo", orderNo),
                Map.entry("orderDate", orderDate),
                Map.entry("workshopName", workshop),
                Map.entry("prodLine", prodLine),
                Map.entry("batchNo", batchNo),
                Map.entry("k3Qty", new BigDecimal(qty)),
                Map.entry("planStartDate", planStart),
                Map.entry("planEndDate", planEnd),
                Map.entry("colorNo", colorNo),
                Map.entry("productName", productName),
                Map.entry("spinCount", spinCount),
                Map.entry("convertRate", new BigDecimal(convertRate)),
                Map.entry("dyeType", dyeType),
                Map.entry("materialNo", materialNo),
                Map.entry("materialName", materialName),
                Map.entry("materialSpec", materialSpec),
                Map.entry("unit", "KG"),
                Map.entry("customerName", customer),
                Map.entry("saleDept", saleDept),
                Map.entry("saleUser", saleUser),
                Map.entry("purpose", purpose),
                Map.entry("auxAttr", auxAttr),
                Map.entry("color", color),
                Map.entry("isFluorescent", 0),
                Map.entry("k3VersionTime", "2026-10-06 18:00:00"),
                Map.entry("createBy", "k3-sync"));
    }
}
