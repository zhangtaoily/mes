package com.xinao.mes.plugin.erp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ERP 连接器 SPI（K3 插件实现，M5 联调）
 *
 * <p>所有同步经 sync_log 幂等（K3 接口 ID 唯一索引），失败重试走 XXL-Job。
 */
public interface ErpConnector {

    /**
     * 增量拉取 K3 生产订单
     *
     * @param startTime 增量起始时间（上次同步位点）
     * @return 拉取的订单列表（字段映射见 PRD 3.1 的 49 字段映射表）
     */
    List<Map<String, Object>> pullOrders(LocalDateTime startTime);

    /**
     * 回写 K3：引入量 / 关闭状态
     *
     * @param orderNo MES 生产任务单号
     * @param introducedQty 引入量
     * @param closed 是否关闭
     * @return 回写成功的 K3 单据内码
     */
    Long writeBack(String orderNo, java.math.BigDecimal introducedQty, boolean closed);
}
