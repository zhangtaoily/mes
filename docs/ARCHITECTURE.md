# MES 混合架构设计（DDD 稳定域 + 元数据易变层 + 插件 SPI）

> 依据：本项目逆向的 123 张表 / 10 大模块（docs/schema.sql）+ crm 元数据引擎模式（TableNode/Plugin/WorkFlow）
> 核心诉求：**业务深度用 DDD，变更频率用引擎，集成点用插件，重启最少化**

## 1. 总体架构

```
                    ┌───────────────┐   ┌──────────────┐
   PDA/一体机 ─────▶│  gateway 网关  │◀──│ PC Web (Vue) │
                    └───────┬───────┘   └──────────────┘
                            ▼
   ┌──────────────── mes-bootstrap (Spring Boot 3 启动器) ────────────────┐
   │                                                                    │
   │  ┌── 稳定域（DDD，强类型，月级发版）──────────────────────────┐      │
   │  │ plan 任务上下文   produce 执行上下文   pack 包装上下文     │      │
   │  │ （工艺单/任务/排程）（汇报/投料/追溯）（执行/装箱/组托）   │      │
   │  └───────────────────────┬───────────────────────────────┘      │
   │                          │ 领域事件                              │
   │  ┌── 易变层（引擎，周级变更不发版）────────────────────────┐      │
   │  │ script  JS脚本引擎(质检公式/报产公式)                    │      │
   │  │ meta    质检方案/工艺项/标识牌/字典（纵表配置）          │      │
   │  │ code    自动编码引擎(前缀+日期+流水)                     │      │
   │  │ flow    通用状态机(单据状态驱动，无审批流)               │      │
   │  └─────────────────────────────────────────────────────────┘      │
   │  ┌── 插件 SPI（集成点热插拔）──────────────────────────────┐      │
   │  │ k3-plugin  wms-plugin  printer-plugin  scale-plugin     │      │
   │  └─────────────────────────────────────────────────────────┘      │
   └────────────────────────────┬───────────────────────────────────────┘
                                ▼
                    MySQL 8（docs/schema.sql）+ Redis（编码流水/缓存）
```

## 2. 技术栈

| 层 | 选型 | 说明 |
|---|---|---|
| 运行时 | Java 17 + Spring Boot 3.2 | |
| 持久层 | MyBatis-Plus | 多表关联/报表用 XML |
| 脚本引擎 | **GraalVM JS（推荐）**/ Nashorn 兼容层 | 质检公式 `$BQ()`、报产公式，参照系统同款设计 |
| 状态机 | Spring StateMachine 或自研轻量 flow 模块 | 单据状态：编制中→已提交→生效→了批→已关闭 |
| 规则外置 | QLExpress（备用） | 剩余可排量校验等可配置化 |
| 定时任务 | XXL-Job | K3 增量同步、排程刷新、WMS 重试 |
| 消息 | 领域事件用 Spring Event 起步，峰值后迁 RocketMQ | 上批→包装联动解耦 |
| 热更新 | 脚本/配置存 DB（Redis 缓存+版本号失效） | **不发版四件套：脚本、方案、编码、字典** |

## 3. Maven 工程结构

```
mes-parent
├── mes-bootstrap          # 启动器：组装，不含业务
├── mes-domain             # DDD 稳定域（4 上下文）
│   └── com.xinao.mes.{plan|produce|pack|qc}
│       ├── domain         # 聚合根/实体/领域服务/领域事件
│       ├── application    # 用例服务（编排，无状态）
│       └── infrastructure # repository 实现、消息适配
├── mes-engine             # 易变层引擎（对标 CRM common/service）
│   └── com.xinao.mes.engine
│       ├── script         # ScriptEngine：JS 沙箱、$BQ 注入、白名单
│       ├── meta           # TableNode（借 CRM）+ 方案/工艺项纵表读写
│       ├── code           # CodeGenerator：Redis 流水，按日/月重置
│       └── flow           # BillStateMachine（单据状态迁移，无审批）
├── mes-plugin-api         # SPI 接口定义
└── mes-plugins            # 独立 jar，可单独发版
    ├── k3 / wms / printer / scale / pda
```

## 4. 稳定域：限界上下文与聚合根

| 上下文 | 聚合根 | 关键不变量（DDD 价值所在） | 对应表 |
|---|---|---|---|
| plan 计划 | `PlanCraft`(计划工艺单) / `ProductionTask`(生产任务) / `Schedule`(排程) | 工序路线完整性校验；任务引入量≤订单数量；排程数量≤剩余可排 | plan_craft*、prod_task*、sch_schedule* |
| produce 执行 | `BatchReport`(上批汇报) / `Feeding`(投料) / `OutputReport`(产量汇报) | 投料完毕才能了批；汇报产量≤排程余量；追溯批次链闭环 | report_*、mt_feed* |
| pack 包装 | `PackTask` / `PackExecute` | 五列重量恒等式：毛=皮+净；净→公重换算按回潮公式；箱号唯一 | pack_* |
| qc 质检 | `QcReport`(壳，DDD) | 报告状态机（作废/取消作废）；指标值越上下限→结论 | qc_report*（指标值本身走引擎） |

**划分原则**：聚合内强一致（本地事务），跨上下文只走领域事件（如 `BatchFinishedEvent` → 触发包装任务生成）。

## 5. 易变层引擎设计（不发版的核心）

### 5.1 脚本引擎（script）——复刻参照系统

```java
public interface FormulaEngine {
    /** ctx 暴露：$BQ(itemId) 取质检值、$SET(key,val) 写回衍生指标 */
    FormulaResult eval(String script, FormulaContext ctx);
    // 沙箱：超时 500ms、禁 IO/System、异常转 FormulaError 带行号
}
```
- 脚本存 `qc_formula.script_text`，**编辑即生效**（Redis 缓存按 version 失效）
- 适用：质检衍生指标（不匀率）、车间报产公式（折算产量）

### 5.2 元数据（meta）——借 CRM TableNode 模式
- 质检方案指标项（qc_scheme_item 纵表）、车间工艺项（craft_item）、标识牌、字典：全部配置增删，**加指标不改代码**
- EnhancedTableNode 提供列表页动态列渲染（Web 端配合）

### 5.3 编码引擎（code）
- 规则表 sys_code_rule（前缀/日期格式/流水位数），Redis `INCR` + 按日/月 key 过期重置
- 新单据类型 = 插一条配置

### 5.4 状态机（flow）
- BillStatusEnum 通用状态 + 每单据类型一张迁移表（可配置）：编制中→已提交→生效→了批→已关闭，含驳回/撤回/作废边（**仅状态迁移，无审批流**）
- 按钮可见性 = f(当前状态, 角色)，前端由接口下发（我们原型已按此实现）

## 6. 插件 SPI（集成即热插拔）

```java
@SPI("k3")
public interface ErpConnector {
    SyncResult pullOrders(SyncCmd cmd);      // K3 增量拉单
    WritebackResult writeBack(WritebackCmd cmd); // 引入量/关闭回写
}
```
- k3/wms/printer(筒贴/箱贴/A4制造卡)/scale(电子秤 RS232)/pda(手持机) 各自独立 jar
- 版本独立：WMS 接口升级只发 mes-plugin-wms-1.2.jar，核心不重启（SPI 加载器支持 jar 目录扫描+手动 reload）
- 所有同步经 sync_log（幂等：K3 接口 ID + 唯一索引），失败重试走 XXL-Job

## 7. 少重启手段清单（对需求逐条兑现）

| 变更场景 | 手段 | 是否重启 |
|---|---|---|
| 质检公式/报产公式调整 | JS 脚本 DB 存储，编辑生效 | ❌ |
| 加质检指标/改工艺项/换标识牌 | 纵表配置 | ❌ |
| 新单据编号规则 | code 规则配置 | ❌ |
| 状态迁移调整（如新增撤回边） | flow 迁移表配置 | ❌ |
| 列表加列/查询项 | TableNode 元数据 | ❌ |
| K3/WMS 对接变更 | 插件 jar 单独发 | 仅插件 |
| 核心业务逻辑（聚合不变量） | DDD 域代码发版 | ✅（月级，滚动发布） |

## 8. 落地顺序建议

1. **mes-engine 先行**（code + flow + meta）：所有单据从第一天走编码引擎与状态机，避免后补改造
2. plan 上下文（DDD 范式打样）：计划工艺单→任务→排程一条链
3. produce → pack → qc 域复制范式
4. script 引擎随 qc 域上线（公式先支持内建函数，JS 后置）
5. 插件层最后抽 SPI（初期直接 @Service 调用，接口先按 SPI 定义写）

> 详细表结构见 docs/schema.sql（123 张）；领域事件清单、聚合不变量明细可在此基础上细化。
