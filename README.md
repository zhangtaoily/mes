# 毛纺 MES（精梳毛纺·纱线纺织流程制造生产执行系统）

面向**纱线纺织（精梳毛纺）**行业的**流程制造（Process Manufacturing）**生产执行系统；混合架构：**DDD 稳定域 + 引擎易变层 + 插件 SPI**。

- 需求与设计：[docs/PRD.md](docs/PRD.md) · [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) · [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md)
- 数据库：[docs/schema.sql](docs/schema.sql)（123 表，经 Flyway 管理，禁止手改库）
- 交互基准：[prototype/](prototype/index.html)

## 业务定位：纱线纺织 · 流程制造

本系统服务精梳毛纺（纱线）工厂，属典型的**流程制造**场景——以**批号**为主线、按**工序路线**在机台间连续流转（而非离散件装配），投料/产出按工序累计，全链批次可追溯。

- **工序链**：毛条 → 前纺（混条/针梳/粗纱）→ 后纺（细纱/并线/倍捻/摇绞）→ 蒸纱定捻 → 包装入库；纺织特有现场单据：启停、切疵、翻批、前纺情况
- **批次追溯**：扫制造卡上批（批号标识 SR）→ 投料（领退料单确认）→ 产量按工序汇报累计 → 了批（强校验"投料完毕 + 批次链闭环"，通过后自动下批、释放批号标识）
- **工艺配方快照下发**：技术/车间工艺库 → 计划工艺单 → 上机制造卡（车速/定长/张力等逐机台参数），工艺库调整不影响已下发单据
- **流程计量**：逐箱称重五列恒等式（毛=皮+净）、公定回潮换算公重、组托入库、用毛率/条干 CV/不匀率等纺织口径统计
- **质检体系**：原料检验、前纺质检方案、疵纱 15 类分级（正纱/油纱/断纱…）、物理指标越限判定（公式 GraalJS 脚本外置可改）
- **八大业务闭环**：计划链（K3/手工订单→工艺单→任务→排程→制造卡）→ 生产执行 → 包装入库 → 质检 → 设备维保 → 现场物流（周转车/蒸纱）→ PDA 离线 → 报表统计

## 技术栈

Java 17 · Spring Boot 3.2 · MyBatis-Plus · MySQL 8 · Redis · Flyway · JWT（jjwt）

## 工程结构

```
mes-parent
├── mes-common        技术基础设施：统一返回体 / 全局异常 / JWT鉴权 / 按钮级权限 / 操作日志 / MP配置
├── mes-engine        易变层引擎（M1）：code自动编码 / flow状态机 / meta元数据 / script脚本
├── mes-domain        DDD 稳定域：system(用户/角色/组织/菜单，M0) → plan / produce / pack / qc
├── mes-plugin-api    插件 SPI 框架：@SPI 注解 + PluginManager 加载器（目录扫描/热替换）+ 五契约接口
├── mes-plugins       插件实现（独立 jar 放运行目录 plugins/ 加载）：k3 / wms(V1) / wms-b(V2 热替换演示) / printer / scale / pda
└── mes-bootstrap     启动器：组装 + Flyway 迁移(db/migration) + 验收接口
```

## 构建与启动

```bash
# 构建（首次需 JDK17，本仓库 JAVA_HOME 指向 JDK8 时请切换）
mvn package

# 启动（默认连开发库 192.168.0.75/mes，配置在 application.yml，可用环境变量覆盖）
java -jar mes-bootstrap/target/mes-bootstrap-1.0.0-SNAPSHOT.jar

# 覆盖数据库/Redis 连接
MES_DB_URL="jdbc:mysql://<host>:3306/mes?..." MES_DB_USER=xxx MES_DB_PASSWORD=xxx \
MES_REDIS_HOST=<host> MES_REDIS_PASSWORD=xxx \
java -jar mes-bootstrap/target/mes-bootstrap-1.0.0-SNAPSHOT.jar
```

首次启动 Flyway 自动执行 `V1__init.sql`（123 表）与 `V2__seed_sys.sql`（种子数据），
并初始化种子用户密码为 `mes.init-password`（默认 `admin@2026`）。

**初始账号**：`admin / admin@2026`（超管，权限集通配 `*`）；`zhangt / admin@2026`（工艺员+调度员，验证按钮级权限）。

## M0 验收清单

| 项 | 接口 | 说明 |
|---|---|---|
| 连通性 | `GET /api/hello` | 公开，验证服务/DB |
| 登录 | `POST /api/auth/login` | `{"loginName":"admin","password":"admin@2026"}` |
| 当前用户 | `GET /api/auth/info` | 携带 `Authorization: Bearer <token>` |
| 菜单树 | `GET /api/auth/menus` | 超管全量，普通用户按角色过滤 |
| 鉴权拦截 | `GET /api/hello/me` | 无 token 返回 401 |
| 按钮级权限 | `GET /api/hello/perm` | 无 `sys:user:list` 权限返回 403（zhangt 验证） |
| 用户管理 | `GET/POST/PUT/DELETE /api/sys/users` | 注解 `@RequiresPerm("sys:user:*")` |
| 操作日志 | AOP `@OperLog` | 标注接口自动落 `sys_oper_log` |

## M1 验收清单（引擎层，均已通过）

启动前置：本机 Redis（`redis/redis-server.exe --port 6379 --requirepass mes123456`）。首次启动自动执行 `V3__engine.sql`（flow/meta/sys_code_log 表 + 种子）。

| 项 | 接口 | 验收结果 |
|---|---|---|
| 编码-单号 | `GET /api/engine/code/next/{codeName}` | `PI20260930000001`（前缀+日+5位流水） |
| 编码-并发 | `GET /api/engine/code/concurrent/{codeName}?threads=100&loops=1` | 100 线程 0 重号 |
| 编码-断号追溯 | `sys_code_log` | 每号留痕（102 号连续可查），`sys_auto_code` 回写 current_serial/period |
| 状态机-按钮 | `GET /api/engine/flow/buttons?billType=&status=` | 按角色过滤下发 |
| 状态机-迁移 | `POST /api/engine/flow/fire` | `submit: 编制中→已提交` |
| 状态机-配置化 | `POST /api/engine/flow/transitions` | 新增"生效→已暂停(pause)"配置后按钮即时变化，**不改代码** |
| 元数据-动态列 | `GET /api/engine/meta/columns/qc_report_item` | 3 固定列 + 6 指标动态列 |
| 元数据-增删指标 | `POST/DELETE /api/engine/meta/indicators` | 加"条干CV值"→10 列，删→9 列 |
| 脚本-公式计算 | `POST /api/engine/script/eval`（formulaId=1 不匀率） | value=13.33，$SET 写回 BYL |
| 脚本-超时杀 | `POST /api/engine/script/eval-raw`（while(true)，timeoutMs=500） | 500ms 强杀且报行号:2 |
| 脚本-沙箱 | eval-raw `Java.type('java.io.File')` | 拒绝访问宿主类 |
| 脚本-编辑即生效 | `PUT /api/engine/script/formulas/{id}/script` | 改脚本后立即生效（无重启） |
| 事件总线 | `POST /api/engine/event/publish` | 监听器同步回执 handledBy |

M1 关键实现：GraalVM Polyglot JS 沙箱（`HostAccess.EXPLICIT` + 禁 IO/线程/本地访问 + watchdog `Context.interrupt` 超时强杀）；`$BQ/$SET` 以 `ProxyExecutable` 注入；Redis INCR 取号 + DB 进度回写（NULL 期间需 isNull 兜底，SQL 三值逻辑坑）。

## M2 验收清单（plan 稳定域，均已通过）

首次启动自动执行 `V4__plan_domain.sql`（plan_craft_craft 表 + prod_task 流转重配 + 工单/制造卡编码规则 + 工序/机台/工艺库/K3订单种子）。聚合根 PlanCraft / ProdTask / ProdSchedule，23/23 验收项通过，单测 13 个（不变量 8 + 排程服务 5）。

| 项 | 接口 | 验收结果 |
|---|---|---|
| K3 订单镜像 | `GET /api/plan/erp-orders` | 只读分页 + 详情，无操作按钮 |
| 不变量-引入量 | `POST /api/plan/crafts/import-k3` | 超订单剩余拒绝（剩余3000 引99999 拦截）；引入后 imported_qty CAS 回写 3000；重复引入拦截 |
| 引入 K3 订单 | 同上 | 生成工艺单（车间名匹配纺十车间，字段自动带出），craftNo=PI…0112 |
| 工序路线 | `PUT /api/plan/crafts/{id}/routes` | 整表替换 + seq 重排（粗纱/倍捻两道） |
| 工艺引用-技术库 | `POST /api/plan/crafts/{id}/apply-craft` | refSource=1 全工序带出 5 项（快照，引用后库调整不影响单据） |
| 工艺引用-车间库 | 同上 | refSource=2 按工序+机型（SINCRO B 306）覆盖倍捻行 3 项，refSchemeNo=CJ1 |
| 提交 | `POST /api/plan/crafts/submit` | flow fire 编制中→已提交 |
| 自动生成任务 | 事件 `PlanCraftSubmittedEvent` | 同步监听生成生效任务 PT…0005（planQty=3000，planCraftId 回链），幂等 |
| 不变量-路线完整 | `POST /api/plan/crafts/{id}/generate-schedule` | 空路线/编制中工艺单拒绝"请先提交后再生成排程" |
| 生成排程 | 同上 | 逐工序生成（粗纱/倍捻各一条，同工单号 TPC…0006，机台自动分配），末道工序占用任务产量 3000 |
| 不变量-剩余可排 | `POST /api/plan/schedules/task-details` | 排程量超剩余拒绝（计划3000 已排3000 剩余0，本次1 拦截） |
| 制造卡 | `POST /api/plan/cards/generate`、`PUT /{id}/print` | 按排程快照生成（车速/定长/张力等），打印留痕 flow_card_print_log |
| 制造卡撤回 | `PUT /api/plan/cards/{id}/withdraw` | 2 张全部撤回 → 排程释放（现存0）→ **重排成功**（再生成2条）；已撤回卡重复撤回被拦截 |

M2 关键实现：任务自动生成走 Spring 事件同步监听（同事务、幂等：存在未作废关联任务则跳过）；排程占用模型 = 末道工序插 prod_schedule_task，剩余可排实时 SUM；制造卡撤回 = 排程逻辑删 + 子表物理删释放占用；引入量/状态更新全部 CAS（eq 旧值，NULL 与显式 0 两种口径分别 isNull/eq——显式 0 时 isNull 匹配不到的坑）。

## M3 验收清单（produce 稳定域，均已通过）

首次启动自动执行 `V5__produce_domain.sql`（rep_batch_on 补下批人/车间列 + 上批/产量/投料/领退料/启停 6 条编码规则 + mt_pick 确认流 + 班组/领退料种子）与 `V6__fix_feed_code_rule.sql`（清理 V1 与 V5 重复的投料单编码规则）。聚合根 MtFeed / RepBatchOn / RepOutput，PDA 与 PC 共用 application 层；单测 22 个（plan 不变量 8 + 排程 5 + produce 不变量 9）。验收 demo：任务 PT…0005 一票批次"上批→投料→汇报→了批"全链路走通。

| 项 | 接口 | 验收结果 |
|---|---|---|
| PDA 扫码 | `POST /api/pda/scan` | 制造卡二维码→排程/任务/工艺项/活动上批（无活动上批返回 null，支持排程单号兜底） |
| PDA 上批 | `POST /api/pda/batch-on` | source=2 PDA 来源；重复上批拦截；再扫码显示活动上批 |
| PDA 产量 | `POST /api/pda/output` | 产量汇报 + route.output_qty 事件累计 |
| PDA 投料任务 | `GET /api/pda/tasks` | 已了批任务不再下发 |
| PC 上批/下批 | `POST /api/site/batch-on`、`POST /api/site/batch-on/{id}/off` | 与 PDA 共用 application 层，来源自动区分 |
| 投料-保存 | `POST /api/site/feed-record` | 明细超剩余 pickQty 拦截；未完毕单可删并回退累计 |
| 投料-完毕封账 | 同上（isFinish=1） | 完毕封账后不可再投不可删 |
| 不变量-投料完毕才能了批 | `POST /api/plan/tasks/finish` | 无投料记录 / 无完毕单均拒绝 |
| 不变量-断链拒了批 | 同上 | 排程无上批记录拒绝（追溯批次链闭环，多排程逐张校验） |
| 了批联动 | 同上 | 全部排程上批自动下批 + 批号标识释放 + 工序路线标记了批 |
| 不变量-了批封账 | 上批 / 产量接口 | 了批后上批、报产量均拦截 |
| 领退料确认 | `POST /api/prod/pick/confirm` | flow confirm 待确认→已确认 |
| 周转车 | `POST /api/site/truck/move`、`/split` | 移包并包 / 拆包分卡 |
| 启停/翻批/前纺/切疵/蒸纱 | `/api/site/stop`、`/api/site/rebatch`、`/api/craft/front-report`、`/api/site/yarn-clearer`、`/api/site/steam` | 五类现场单据 + 蒸纱打印/接收闭环 |

M3 关键实现：TaskFinishRule 作为 produce 对 plan 的了批校验/联动挂点（投料完毕 + 批次链闭环两不变量，通过后自动下批闭环 + 释放批号标识）；上批/产量/投料三服务对"任务须生效"做对称防护。验收中发现并修复三处：① 投料单编码规则 V1/V5 重名导致 CodeEngine.next selectOne 报错（V6 清理，规则名必须唯一）；② 了批后任务仍可上批/报产量污染 route 累计量（补任务生效校验）；③ scheduleIdsOfTask 误用 prod_schedule_task（末道占用表）漏非末道排程，断链校验与了批联动均不完整（改按 prod_schedule.task_id 直查）。

## M4 验收清单（pack/qc 稳定域，均已通过）

首次启动自动执行 `V7__pack_qc_domain.sql`（qc_report 补 is_public / qc_report_item 补 is_over 两列 + 包装箱 BX/箱贴补打 FG 编码规则 + 公重指标与"公重换算-回潮公式"（formula_type=包装称重，脚本外置）+ 不匀率公式升级集合版 + 前纺质检方案（5 试样×6 指标）与抽样/包装材质种子）。pack 域 9 实体 + qc 域 7 实体，7 应用服务 + 7 Controller；单测 31 个（新增 pack/qc 不变量 9 个）。验收 demo：包装任务 PS…0003 → 逐箱称重（五列重量恒等式 + 公重公式）→ 组托 TP…0003 → 检验报告 QC…0003（$BQ 公式算不匀率 33.33 并越限判定）→ 公示 → 作废/恢复。

| 项 | 接口 | 验收结果 |
|---|---|---|
| 包装任务 | `POST /api/pack/tasks` | 入库批号/计重方式必填；生成 PS…；了批任务不可改 |
| 包装执行 | `POST /api/pack/tasks/{id}/execute` | 生成 PC…，产品信息/回潮/筒子数从任务带出 |
| 逐箱称重 | `POST /api/pack/executes/{id}/boxes` | 生成 BX…+seqNo 递增；公重=净重×(1+公定回潮)/(1+实测回潮)=6.304/6.525（脚本外置算） |
| 不变量-五列重量恒等式 | 同上 | 毛10.00≠皮2.50+净6.28 拒绝（容差 0.01） |
| 汇总重算 | `GET /api/pack/executes/{id}` | boxCount=2 / sumNet=12.78 / sumPublic=12.829 / sumGross=17.78 / sumTube=48 随增删箱实时重算 |
| 了包封账 | `POST /api/pack/executes/{id}/finish` | 无箱拒了包；了包后称重/删箱均拦截 |
| 删箱审计 | `DELETE /api/pack/executes/boxes/{boxId}` | pk_box_delete 留痕（删箱人/时间/重量）+ 逻辑删 + 汇总回退 |
| 组托 | `POST /api/pack/pallets/palletize` | 生成 TP…；箱子回写托号；已组托箱子拒绝重复组托 |
| 检验报告-方案矩阵 | `POST /api/qc/reports` | 选方案1 生成 5×6=30 行矩阵，快照上下限 |
| $BQ 公式计算 | `POST /api/qc/reports/{id}/calc` | 集合版不匀率=(28-20)/24×100=33.33 写回指标6，衍生指标参与越限判定（33.33>15 置 is_over） |
| 越限判定 | 同上 | 超上限（28>26）与低于下限（20<22）均置位；范围内指标不受影响 |
| 提交/公示 | `POST /{id}/submit`、`/{id}/public` | flow 编制中→已提交；仅已提交可公示（is_public=1） |
| 作废恢复 | `POST /{id}/void`、`/{id}/unvoid` | 作废→已作废且公示联动取消；取消作废→已提交重新计入统计 |
| 不变量-作废不计入统计 | `GET /api/qc/reports/statistics` | 作废后 total/overLimitCount/avgByl 全部回到基线；unvoid 后恢复 |

M4 关键实现：script 引擎在 qc 正式上线——`$BQ(指标ID)` 取该指标全部试样值集合、`$SET` 写衍生值，公重/不匀率公式全部存 qc_formula 编辑即生效（TTL 缓存+主动失效）；五列重量恒等式（毛=皮+净，容差0.01）作为 addBox 聚合校验；作废走 doc_status=已作废（flow 驱动）而非逻辑删，统计口径统一 ne(已作废)。验收中发现并修复两处：① GraalVM JS 桥接对宿主 BigDecimal 直接透传导致脚本 Number() 得 NaN（ScriptBridge 统一转 double），宿主 List 在 HostAccess.EXPLICIT 下无导出成员 a.length 为 undefined（包装为 ProxyArray）；② V7 公重指标 id=7 与 V1 条干CV值种子主键冲突（改 id=8，Flyway 失败记录清理后重放）。

## M5 验收清单（集成收口·插件 SPI 化，均已通过）

首次启动自动执行 `V8__integration.sql`（log_sync 补 iface_id 接口幂等键 + retry_count 列 + `(sync_type, iface_id)` 唯一索引——NULL 不受唯一约束，回写等每次留痕动作不受影响）。插件体系：`mes-plugin-api`（@SPI 注解 + PluginManager 加载器 + Erp/Wms/Printer/Scale/Pda 五契约）+ `mes-plugins` 六子模块独立 jar（k3 / wms V1 / wms-b V2 热替换演示 / printer / scale / pda）——插件是纯协议适配器（不碰 Spring/DB），运行时放 `plugins/` 目录由 URLClassLoader 隔离加载，**与 fat jar 完全分离**（bootstrap 不依赖 plugins 模块）。失败重试用 sync_log 状态驱动 + `@Scheduled` 每分钟扫描兜底（无外部调度中心的 XXL-Job 最简替代，retry_count<3）。验收 demo：`K3重推幂等 / 换wms插件不重启 / 秤重直读`，21/21 项通过。

| 项 | 接口 | 验收结果 |
|---|---|---|
| 插件清单 | `GET /api/plugins` | 启动扫描 plugins/ 加载 5 实现（name/class/jar/loadTime） |
| K3 同步-首次 | `POST /api/integration/k3/sync` | pulled=4：3 新单插入 + 种子单 FBDC772348 走 upsert 更新（保留引入进度） |
| K3 重推幂等 | 同上（第二次调用） | skipped=4 / inserted=0——`(K3订单, K3内码)` 唯一索引命中即跳过，erp_k3_order 5 行不重复 |
| 秤重直读 | `GET /api/integration/scale/read` | 重量≈25kg±20g 波动、unit=kg、readAt 有值；连续两次读数不同（模拟串口实时帧） |
| 打印插件 | `POST /api/integration/print` | 箱贴模板回执 jobId=PRN-… + ZPL 指令（^XA…^XZ 含二维码） |
| PDA 报文适配 | `POST /api/integration/pda/report` | 优博讯 MF919 原始报文→标准结构（scanCode=XJP2610456-12/type=上批） |
| WMS 推送-失败落账 | `POST /api/integration/wms/push-pallet` | V1 推 TP…000001 触发模拟超时→log_sync 落 result=0 + 五列重量留账 |
| 热替换 wms 插件 | `POST /api/plugins/reload?jar=plugins/mes-plugin-wms-b-…jar` | 拷入 V2 jar 后 reload，loaded=1；wms 实现 V1→V2，**核心服务不重启**（k3 插件 loadTime 不变为证） |
| 失败重试 | `POST /api/integration/sync/retry` | 按类型路由重放：000001 重推成功（V2 受理），log 行救活 result=1/retry_count=1，补落 pk_wms_in |
| WMS 推送-V2 直推 | 同 push-pallet | TP…000002 provider=WMS-MOCK-V2（嵌套报文 header/payload） |
| WMS 重推幂等 | 同 push-pallet（已成功托再推） | skipped=true——同托唯一索引拦截 |
| K3 回写 | `POST /api/integration/k3/writeback` | 引入量/关闭状态回写 K3（回执单据内码）+ 本地镜像 is_closed 联动 |
| 同步日志 | `GET /api/integration/sync/logs` | WMS 同步日志页面：按类型/单号/结果过滤，req/resp 报文可视 |

M5 关键实现：PluginManager 单例（Spring @Bean 暴露）每 jar 独立 URLClassLoader（parent=应用类加载器，插件可见契约接口与 Jackson 等公共库），同名 @SPI 后加载覆盖前者并 close 旧 loader——宿主每次 `get(name, type)` 现取实例故热替换即生效；幂等模型统一为"逐条占位 log_sync→命中唯一键且已成功则跳过，命中失败行则重放"；K3 49 字段映射走 Jackson `convertValue`（Map→ErpK3Order，日期格式 yyyy-MM-dd[ HH:mm:ss]，FAIL_ON_UNKNOWN 关闭）；WmsConnectorV1/V2 演示协议演进（扁平 JSON→嵌套报文）不重启切换。联调替换真实 K3/WMS 时仅需替换 plugins/ 下对应 jar 实现，契约不动。

## M6 验收清单（报表与上线，均已通过）

首次启动自动执行 `V9__report_seed.sql`（恢复指标/报表菜单种子 + 真实台账（2026-09/10 检验报告/产量/入库/用毛率/原料检验，供手工台账核对）+ 量级压测数据（7 万检验明细 / 5 万产量行 / 2 万装箱，落在 2026-01~08 与真实台账时间隔离））与 `V10__fix_raw_qc_craft_ref.sql`（原料检验按批次号重挂工艺单引用）。新增 report 域为**只读跨域聚合**：ReportMapper（@Select 跨域 SQL）+ ReportAppService（口径集中）+ ReportController（catalog + 12 端点，权限统一 `report:list`）。单测新增 12 个（全量 53/53），接口验收 51/51，压测 4/4。

| 项 | 接口 | 验收结果 |
|---|---|---|
| 报表目录 | `GET /api/reports/catalog` | 8 分类 12 张 P0 已实现，P1/P2 后置灰显（前端据此渲染导航树） |
| 1 车间质量检验日报 | `GET /api/reports/qc-daily` | 车间×日期聚合：检验数/合格率/越限数（作废报告不计入） |
| 2 疵纱统计 | `GET /api/reports/defect-stats` | 批次台账 + 类型合计（正纱2+油纱1+断纱3=6） |
| 3 筒子称重产量 | `GET /api/reports/tube-weight-output` | 2 万箱聚合，五列恒等式复验（毛8.78=皮2.50+净6.28） |
| 4 包装批次入库汇总 | `GET /api/reports/pack-inbound` | 按入库批号汇总箱数/毛净公重 |
| 5 生产进度跟踪 | `GET /api/reports/progress-tracking` | 末道报产工序累计产出 + 状态推导（已完成/逾期/进行中） |
| 6 纺纱生产日报 | `GET /api/reports/spinning-daily` | 后纺 7-10 工序（细纱/并线/倍捻/摇绞）日产量 |
| 7 细毛产量日报 | `GET /api/reports/wool-top-daily` | 毛条线 1-5 工序日产量 |
| 8 前纺产量统计 | `GET /api/reports/fore-spin-stats` | 前纺 1-6 工序（+粗纱）日产量 |
| 9 用毛率统计 | `GET /api/reports/wool-rate` | 四查询合并：实际 107.14% vs 标准 105% → 差异 +2.14 |
| 10 物理指标统计 | `GET /api/reports/yarn-physical-stats` | 均值/最大/最小/合格率（纵表指标 CAST 兼容） |
| 11 条干CV统计 | `GET /api/reports/cv-stats` | 15 试样均值 13.97（209.5/15） |
| 12 原料检验汇总 | `GET /api/reports/raw-qc-summary` | 中心实验室批次台账（V10 修正工艺单引用后返回真实行） |
| 压测-日报表 P95 | 20 并发×10 轮×4 报表端点 | **P95=1975ms < 3000ms（验收标准）**，800 请求 0 失败 |
| 压测-产量峰值 | 10 并发×5 次 `POST /api/pda/output` | 50/50 成功，并发取号无重号（OR26100700001~00050） |
| 压测-排程刷新 | `POST /api/plan/schedules/refresh` | 21ms（同步生产实绩重算已排产量） |
| 压测-K3 批量幂等 | `POST /api/integration/k3/sync` | 58ms，pulled=4 / skipped=4（重推不重复） |

M6 关键实现：报表口径全部集中在 AppService（percent/进度状态推导/用毛率四查询合并/疵纱组装），Mapper 纯 @Select SQL 无业务逻辑；纵表指标以 `CAST(NULLIF(value_str,'') AS DECIMAL(12,4))` 兼容空串；产量日报车间取自 `rep_output JOIN prod_task`（rep_output 无车间列）；量级数据 `task_id=NULL` 实现隔离（产量日报可见、用毛率 JOIN 自动排除，不污染真实台账核对）。验收中修复两处：① V9 原料检验误按 plan_craft_id=1/2 挂载（实际工艺单 id=10/11）→ V10 按批次号重挂；② PowerShell 5.1 嵌套数组下标语法限制（压测脚本百分位计算拆临时变量）。压测数据已全部回滚（50 行产量删除、route 累计恢复 1500、任务状态恢复了批）。

### 数据迁移与试运行说明

- **数据初始化分三类，均无需旧系统历史迁移**：基础数据（车间/机台/工序/质检方案/编码规则/公式）走 Flyway 种子 + 页面维护；K3 订单由集成层实时拉取（幂等 upsert，M5 已验证重推不重复）；历史检验/产量台账如需补录走各业务页面录入（与报表同源口径）。
- **量级导入演练**：V9 以 7 万检验明细 / 5 万产量行 / 2 万装箱规模验证大批量数据下报表性能——最重的 qc-daily 聚合 P95=1975ms，满足 <3s 验收标准；上线后数据量增长可按此基线监控。
- **试运行与并行核对**：试运行期以 2026-09/10 真实手工台账为基准，12 张 P0 报表逐张与手工核对一致（51/51 断言通过，含用毛率 107.14%、CV 均值 13.97、疵纱合计 6 等口径复验）；上线后报表口径调整仅改 ReportMapper SQL 与 AppService，不动稳定域。
- **P1/P2 报表后置**：出勤/能耗/费用类报表依赖现场管理页面数据积累，灰显于目录中，待试运行数据沉淀后按同一模式扩展（新增 Mapper 方法 + VO 即可）。

## PDA-M1 验收清单（手持终端一期，均已通过）

范围：移动端壳（PdaLayout 手机壳 + PDA 登录页 + 工作台/我的）+ 生产执行增强（启停/前纺 BFF 化）。设计原则：PDA 仅 BFF 薄委托（参数补齐：汇报人/日期/机型缺省），业务校验全部复用 PC application 层同源校验链；扫码驱动一切操作；少输入多确认；无审批流；PDA 页面不进系统菜单，直接 URL `/#/pda` 进入。与 PC 共享 JWT（localStorage `mes_token`），401 时按 hash 前缀分流回 `/pda/login`。

| 项 | 入口/接口 | 验收结果 |
|---|---|---|
| PDA 登录 | `/#/pda/login` → `POST /api/auth/login` | 手机壳居中 max420；已有 token 自动跳工作台（按 localStorage 实时值判断，规避 401 清 store 后 React state 旧值回环） |
| 工作台 | `/#/pda/home` | 问候卡 + 扫码大卡 + 5 模块卡（上批/产量/投料/启停/前纺）+ 底部三 Tab |
| 扫码作业 | `POST /api/pda/scan` | 输入 PSC20261007000004 识别 → 排程/任务/当前批次三卡 + 按状态出"上批"按钮（hasPerm 控制） |
| 上批（同源校验） | `POST /api/pda/batch-on` | 了批任务提交被校验链拦截："任务状态为[了批]，仅生效任务可上批"（BFF 复用 BatchOnAppService 校验链核心验证点） |
| 产量汇报 | `/#/pda/output` | 无扫码上下文时空态保护"请先扫码"+去扫码按钮；表单大字号 InputNumber |
| 投料 | `GET /api/pda/tasks` | 任务下拉"暂无数据"正确（库中无生效未封账任务）；提交按钮正确禁用 |
| 启停汇报 | `POST /api/pda/stop` + `GET /api/pda/{workshops,teams,machines}` | 车间→机台级联（workshopId 过滤）；提交成功"停机汇报成功（1），编号自动取号"，机型/汇报人/日期自动补齐，取号 ST26101000002 |
| 前纺情况 | `POST /api/pda/front-report` | 绕毛五项×三档大按钮（严重红色）；提交成功（车间选填，workshop_id 可空） |
| 我的 | `/#/pda/mine` | 用户卡 + 返回 PC 端 + 退出登录（按 PDA 上下文分流回 `/pda/login`） |
| 下拉端点 | `GET /api/pda/workshops|teams|machines` | 登录即可（不依赖 `base:workshop:list`，操作工无基础信息权限也能用） |

PDA-M1 关键实现：后端 PdaController 仅 5 个新端点（stop/front-report/workshops/teams/machines），全部薄委托 SiteReportAppService；前端 9 页 + PdaLayout + api/pda.ts，antd Select 在 CDP 自动化下用"type 过滤 + Enter"选择。验收中修复两处表结构缺陷：① rep_start_stop 缺 update_by/update_time 审计列（V1 建表遗漏，BaseEntity INSERT_UPDATE 填充导致插入报错）→ V12 补列；② rep_front_report.workshop_id NOT NULL 与"车间选填"页面语义冲突 → V13 改可空。两条测试数据（rep_start_stop id=1 / rep_front_report id=1）已验证后清理。

## PDA-M2 验收通过（手持终端二期·包装线，统一验收已完成）

范围：包装线三大作业 PDA 化——装箱（逐箱称重）、组托（扫箱绑托）、打印（筒贴留痕/箱贴补打）。延续 M1 架构原则：BFF 薄委托 + 同源校验链（五列重量恒等式、公重回潮公式、重复组托拦截、了包不可再称重/删箱等不变量全部复用 pack application 层）；扫码驱动、少输入多确认、无审批流。

后端新增 `PdaPackController`（`/api/pda/pack/*`，11 个端点）：

| 端点 | 权限 | 说明 |
|---|---|---|
| `GET /api/pda/pack/executes?keyword=` | pack:execute:list | 执行单查询（单号/包装批号/入库批号模糊，未了包在前，LIMIT 50） |
| `GET /api/pda/pack/executes/{id}` | pack:execute:list | 执行单详情（主表+箱子明细，复用 detail） |
| `POST /api/pda/pack/boxes` | pack:execute:list | 逐箱称重（packUser 缺省当前用户，OperLog"PDA逐箱称重"） |
| `DELETE /api/pda/pack/boxes/{boxId}` | pack:execute:list | 删箱（审计留痕+汇总回退） |
| `POST /api/pda/pack/executes/{id}/finish` | pack:execute:list | 了包 |
| `POST /api/pda/pack/scan-box` | pack:pallet:list | 组托扫箱（返回箱子+执行单，已组托提前拦截） |
| `GET /api/pda/pack/box-info?boxCode=` | pack:trace:list | 箱子信息查询（补打用，已组托不拦截） |
| `POST /api/pda/pack/palletize` | pack:pallet:list | 组托（executeId 可空，按第一个箱码自动解析） |
| `GET /api/pda/pack/tasks?keyword=` | pack:task:list | 包装任务查询（筒贴打印选任务用） |
| `POST /api/pda/pack/print-tube/{taskId}?qty=` | pack:task:list | 筒贴打印留痕 |
| `POST /api/pda/pack/label-reprint` | pack:trace:list | 箱贴补打（扫箱码，箱子/执行/任务信息自动带出，FG 取号） |

前端新增 3 页 + 工作台"包装作业"模块区（绿色图标区分产线）：

| 页面 | 路由 | 要点 |
|---|---|---|
| 装箱作业 | `/#/pda/pack` | 查执行单→称重表单：净/皮/毛输任意两项自动算第三项；装箱后保留皮重/筒数清净重（连续称重）；箱子明细倒序+删箱；了包二次确认 |
| 组托作业 | `/#/pda/pallet` | 逐箱扫码加入清单（去重+执行单一致性前端预校验）；标准筒数选填；组托取号 TP 大字回显 |
| 打印作业 | `/#/pda/print` | Tab1 筒贴：查任务→数量→留痕；Tab2 箱贴补打：扫箱码回显→登记补打（后端带出全部信息，无需手填） |

状态：统一运行时验收已通过（2026-10-10 八大闭环 75 用例：60 通过/0 失败/15 SKIP，留档 [TEST-PLAN.md](TEST-PLAN.md) / [TEST-CASES.md](TEST-CASES.md)）；操作工角色权限点已在统一验收补配（V15）。

## PDA-M3 验收通过（手持终端三期·质检与物流，统一验收已完成）

范围：质检与现场物流四大作业 PDA 化——包装质检（15 类疵纱分类录入）、领退料确认（待确认单据批量确认）、毛球周转车（绑定/称重/入库/移库/出库 + 轨迹查看）、蒸纱信息（登记 + 打印/接收闭环）。延续 M1/M2 架构原则：BFF 薄委托 + 同源校验链（合格率服务端计算、flow 迁移 + CAS 确认、车号唯一、未打印不可接收等不变量全部复用 application 层）；扫码驱动、少输入多确认、无审批流。

后端新增 2 个 BFF Controller：

| 端点 | 权限 | 说明 |
|---|---|---|
| `GET /api/pda/qc/package-reports?keyword=` | qc:packageReport:list | 包装质检报告查询（入库/生产批次模糊，LIMIT 50） |
| `GET /api/pda/qc/package-reports/{id}` | qc:packageReport:list | 报告详情（主表+疵纱明细） |
| `POST /api/pda/qc/package-reports` | qc:packageReport:list | 保存报告（疵纱明细整表保存，合格率服务端计算；新建缺省今天） |
| `GET /api/pda/site/picks?keyword=&docStatus=` | prod:feed:list | 领退料单查询（单号/订单号/批号模糊，待确认在前，LIMIT 50） |
| `POST /api/pda/site/picks/confirm` | prod:feed:list | 批量确认（flow 迁移+CAS，确认人/时间回填） |
| `GET /api/pda/site/trucks?keyword=` | site:truck:list | 周转车查询（车号/批号模糊） |
| `GET /api/pda/site/trucks/{id}` | site:truck:list | 周转车详情（主表+移动轨迹） |
| `POST /api/pda/site/trucks` | site:truck:list | 新增空车（车号唯一校验同 PC） |
| `POST /api/pda/site/trucks/move` | site:truck:list | 移动操作（scanWorkshop 缺省按车辆车间带出） |
| `GET /api/pda/site/steams?keyword=` | site:steam:list | 蒸纱查询（批号/车号模糊，未打印在前、未接收在前） |
| `POST /api/pda/site/steams` | site:steam:list | 新增蒸纱信息 |
| `POST /api/pda/site/steams/{id}/print` | site:steam:list | 蒸纱打印（已打印拦截同 PC） |
| `POST /api/pda/site/steams/{id}/receive` | site:steam:list | 蒸纱接收（未打印不可接收） |

前端新增 4 页 + 工作台"质检与物流"模块区（橙色图标区分）：

| 页面 | 路由 | 要点 |
|---|---|---|
| 包装质检 | `/#/pda/qc-pack` | Tab1 录入：入库批次扫码 + 15 类疵纱数量网格（正纱高亮，一等品数留空自动取正纱数）；Tab2 查询：合格率 Tag 卡片，点击回显编辑 |
| 领退料确认 | `/#/pda/pick` | 待确认/已确认/全部切换；卡片勾选 → 底部悬浮批量确认；确认人/时间回显 |
| 周转车作业 | `/#/pda/truck` | 扫车号（未找到现场登记空车）→ 车辆信息卡 → 按操作类型（绑定/称重/入库/移库/出库）动态切换字段；移动轨迹列表 |
| 蒸纱作业 | `/#/pda/steam` | Tab1 登记：车号扫码+粒数/重量/车数；Tab2 打印/接收：状态按打印/接收时间推导（未打印/已打印/已接收） |

状态：统一运行时验收已通过（同上，疵纱录入/领退料确认/周转车轨迹/蒸纱打印接收闭环均实测）。

## PDA-M4 验收通过（手持终端四期·设备点检维护+离线，统一验收已完成）

范围：设备点检执行（扫机台→逐项正常/异常）、报修登记与维修闭环（登记取号→修复回填）两大作业 PDA 化，外加轻量离线暂存能力（网络异常提交入队、恢复后重放）。消息中心经确认砍掉（系统无消息域，待有真实需求再做）。与 PC 共表同源台账（eq_check_record/eq_repair_record），记录编码由 CodeEngine 自动取号（DJ/WX，V14 迁移补规则种子，操作工免手输），点检人/维修人/日期/车间缺省自动补齐。

后端新增 1 个 BFF Controller（`equip/controller/PdaEquipController`）：

| 端点 | 权限 | 说明 |
|---|---|---|
| `GET /api/pda/equip/scan-machine?keyword=` | 登录即可 | 扫机台（机台号精确匹配，带出车间名+点检责任人） |
| `GET /api/pda/equip/check-items` | 登录即可 | 点检项目字典（启用） |
| `GET /api/pda/equip/fault-types` | 登录即可 | 故障分类字典（启用） |
| `POST /api/pda/equip/checks` | equip:checkRecord:list | 提交点检（编码取号 DJ；点检人/日期缺省补齐） |
| `GET /api/pda/equip/repairs?keyword=&equipmentId=&unsolved=` | equip:repairRecord:list | 报修单查询（未解决在前，LIMIT 50） |
| `POST /api/pda/equip/repairs` | equip:repairRecord:list | 报修登记（编码取号 WX；故障时间缺省现在、车间按机台带出、solveTime 留空待维修） |
| `POST /api/pda/equip/repairs/{id}/solve` | equip:repairRecord:list | 维修完成（回填解决时间+维修人；已修复拦截） |

前端新增 2 页 + 离线能力 + 工作台"设备点检维护"模块区（紫色 #722ed1 图标区分）：

| 页面 | 路由 | 要点 |
|---|---|---|
| 点检执行 | `/#/pda/check` | 扫机台 → 机台卡（车间/型号/责任人）→ 点检项目逐项 正常/异常 切换 → 异常说明（选填）→ 提交（describe 自动拼装台账） |
| 报修登记 | `/#/pda/repair` | Tab1 登记：扫机台 → 该机台未解决报修提示 → 故障分类下拉 + 详情 → 提交；Tab2 维修处理：报修单卡片（未解决在前）→ 修复完成 |
| 离线暂存 | `/#/pda/mine` | 提交遇网络异常自动入队（localStorage `mes_pda_offline`）；「我的-离线待提交」支持全部重试/单条删除/清空；重放后端重新取号无幂等冲突；业务校验失败（400）不入队 |

状态：统一运行时验收已通过（离线暂存闭环浏览器实测：断网入队→重放→DB 落库 DJ…00002；request.ts 网关错误判定缺陷已修复）。

## 开发约定

- 单据状态机：编制中→已提交→生效→了批→已关闭（**无审批流**），由 M1 flow 引擎配置化驱动
- 不发版四件套：脚本 / 方案 / 编码 / 字典，全部 DB 配置化（M1 落地）
- 数据库变更一律走 Flyway（`mes-bootstrap/src/main/resources/db/migration/V*.sql`）
- CI：提交/PR 跑 `mvn package`（`.github/workflows/ci.yml`）
