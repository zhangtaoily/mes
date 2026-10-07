# 数据库设计文档（DB Design）

数据库：MySQL 8.0，utf8mb4，InnoDB。约120张表，按域前缀组织。

## 1. 命名与通用约定

- 前缀：`sys_`系统、`base_`基础数据、`erp_`K3订单、`plan_`计划工艺单、`prod_`生产任务/排程、`rep_`现场汇报、`qc_`质检、`eq_`设备、`pk_`包装、`mt_`物料、`st_`现场统计、`log_`同步日志
- 所有表含公共字段：`id BIGINT PK AUTO_INCREMENT`、`create_by/create_time/update_by/update_time`，重要主数据含 `deleted`（软删）
- 单据编号由 `sys_auto_code`（自动编码引擎：前缀+日期+流水）生成，业务表存编号冗余字段
- 重量 DECIMAL(12,3)，百分比 DECIMAL(7,3)，回潮/含油 DECIMAL(7,3)

## 2. 域与表清单

### 2.1 系统域（10表）
| 表 | 说明 |
|---|---|
| sys_org | 组织机构（树） |
| sys_user | 用户 |
| sys_role | 角色 |
| sys_user_role | 用户-角色 |
| sys_role_menu | 角色-菜单 |
| sys_menu | 菜单 |
| sys_dict / sys_dict_item | 字典 |
| sys_auto_code | 自动编码规则 |
| sys_oper_log | 操作日志 |

### 2.2 基础数据域（16表）
base_workshop车间、base_team班组、base_equipment_type设备类型、base_equipment设备(机台)、base_process生产工序、base_process_standard工序标准、base_piece_price计件单价、base_batch_flag标识牌(批号标识)、base_craft_param工艺参数、base_craft基础工艺(技术工艺库)、base_craft_workshop车间工艺库、base_pack_material包装材质、base_print_template打印样式、base_qc_scheme质检方案、base_qc_indicator质检指标、base_machine_efficiency机台效率。

### 2.3 K3订单域（1表）
erp_k3_order：40+业务字段+K3各ID映射+数据版本时间。

### 2.4 计划与任务域（15表）
plan_craft计划工艺单主表；子表：plan_craft_route工序路线、plan_craft_material用料汇总、plan_craft_prod生产信息、plan_craft_req工序要求、plan_craft_raw_qc原料检验报告；prod_task生产任务；子表：prod_task_order订单明细、prod_task_material用料汇总、prod_task_extra其它信息、prod_task_route工序路线（倒筒15列，工序如倒毛）、prod_task_craft工艺配置（引用技术/车间工艺库，ref_source+ref_scheme_no溯源）；prod_task_change任务变更；rep_front_report前纺生产情况汇报（挂工艺管理菜单）。

### 2.5 排程域（4表）
prod_schedule排程主表（含工单单号）、prod_schedule_craft上机工艺项、prod_schedule_material用料、prod_schedule_task任务明细（从PI带出，剩余量=计划-已排校验）；prod_machine_card上机制造卡。

### 2.6 现场汇报域（10表）
rep_batch_on上批汇报（含下批/了批标志）、rep_batch_on_flag工序标识、rep_batch_on_craft工艺项、rep_output产量汇报、rep_start_stop启停汇报、rep_rebatch_log翻批日志、rep_rebatch_detail翻批明细、rep_labor_hour人员工时、rep_auto_winding_cut自络筒切疵、rep_truck毛球周转车+明细。

### 2.7 质检域（8表）
qc_sample抽样登记、qc_report检验报告、qc_report_item检验报告指标值、qc_formula质检公式、qc_package_report包装质检报告、qc_package_defect疵纱明细、qc_workshop_scheme车间质检方案、qc_wool_rate指标用毛率。

### 2.8 设备域（11表）
eq_fault_type故障分类、eq_maintain_item保养项目、eq_check_item点检项目、eq_maintain_method维保方式、eq_part_model配件型号、eq_maintain_plan保养计划、eq_maintain_record保养记录、eq_check_plan点检计划、eq_check_record点检记录、eq_repair_record维保记录、eq_duty_machine责任机台。

### 2.9 包装域（12表）
pk_task包装任务、pk_execute包装执行、pk_box箱子明细（逐箱称重）、pk_box_record装箱记录、pk_pallet组托记录、pk_label_reprint箱贴补打、pk_tube_print筒贴打印、pk_box_delete箱子删除记录、pk_wms_in WMS入库记录、pk_erp_in ERP入库记录、pk_yarn_steam蒸纱信息、pk_silk_truck丝车吊牌。

### 2.10 物料域（5表）
mt_pick领退料确认（订单+批号维度，18列：领退料单编码JDSC1L/JDSC1T、领退料人确认回填、产品/物料/辅助属性/色号）、mt_feed投料记录主表、mt_feed_item投料明细、mt_receive原料接收记录、mt_return_wool回毛登记。

### 2.11 现场统计域（6表）
st_attendance车间出勤、st_shift车间排班、st_shift_person排班人员、st_energy车间能耗+明细、st_cost车间生产费用、st_issue现场问题反馈。

### 2.12 参考系统表结构补全域（22表，2024-06-20版docx对齐，见analysis/11）

按参考系统（db/MES数据结构设计文档2024620094530.docx，157表）补齐的缺失域：

- **批号标识管理**：ws_tag车间标识库（图形/色系等级/别标识名，"批号标识"字段的底层主档）、ws_tag_occupy标识占用记录（批次占用/了批释放，防重复分配）
- **报产公式**：ws_report_formula车间报产公式（折算产量计算引擎）、ws_report_formula_rel工序关联
- **批次主档**：batch_pro生产批次（全生命周期）、batch_pack包装批次
- **粗纱库存**：rove_in_store粗纱入库、rove_out_store粗纱出库（支撑"粗纱库存表"报表）
- **质检链路**：qc_task质检任务+qc_task_item质检项、qc_sample质检样品登记（取样→登记→检验，含纱支）
- **产存**：pack_pd_notice产存通知+detail明细
- **倒筒**：pack_dm_coefficient倒筒系数表
- **设备**：eq_error设备异常、eq_accessory_inv配件库存台账、eq_mp_tip维保知识库
- **现场**：ws_ap_record奖惩记录、ws_person_skill人员技能（按技能匹配机台排班）、base_holiday节假日（排班日历）
- **其它**：base_mat_use_lib用料库（标准用料模板）、flow_card_print_log工艺流转卡打印记录（补打留痕）

结构启示：参考系统将排程拆9表/任务拆11表（含SCH_TASK冗余表避免联查）——当前schema按适度冗余设计，性能优化时可参考其拆表思路；枚举统一走sys_dict数据字典（与参考系统TYPE_DIC_ID体系一致）。

### 2.13 同步日志域（2表）
log_sync_interface接口同步日志（WMS/K3/原料接收，含请求返回参数）、log_sync_retry重试。

## 3. 核心ER关系

```
erp_k3_order 1─N plan_craft（按批号引入）
plan_craft 1─N prod_task（生成任务/主子批）
prod_task 1─N prod_schedule（生成排程：机台×工序×日期）
prod_schedule 1─N prod_schedule_task（任务明细，N─1 prod_task，剩余量校验）
prod_task 1─N prod_task_route / prod_task_craft（工序路线 / 工艺配置引用）
prod_schedule 1─1 prod_machine_card（制造卡打印）
prod_schedule 1─N rep_batch_on（上批）→ 1─N rep_output（逐班产量）
rep_batch_on 1─N qc_sample → qc_report → qc_report_item
prod_task 1─N pk_task（包装任务）→ pk_execute → pk_box（箱）→ pk_pallet（托）→ pk_wms_in → pk_erp_in
prod_task 1─N mt_feed（投料）；mt_pick关联K3领料
base_equipment N─1 base_workshop；prod_schedule N─1 base_equipment
```

## 4. 关键设计决策

1. **批号双轨**：`batch_no`生产批号 + `trace_batch_no`追溯批次，全链路冗余存储便于追溯查询
2. **重量组**：包装箱统一 毛重/皮重/净重/公重/标重 五列；公重=净重×(1+公定回潮)/(1+实测回潮) 由应用层计算
3. **质检方案矩阵**：qc_report_item采用 纵表（report_id×indicator_id×sample_no×value），适配任意方案指标×试样数
4. **工艺三级**：base_craft(技术库)→base_craft_workshop(车间库:工序>机型>工艺项，item表存工艺值+上下限)
5. **任务类型/产线变体**：prod_task.task_type区分 普通/复精梳/倒筒，避免多套同构表
6. **状态机字段**：单据状态(doc_status)与执行状态(exec_status)分离，编码见各表注释
7. **K3 ID映射**：erp_k3_order冗余全部K3主键ID列，同步以 K3数据版本时间 做增量与幂等
8. **软删+审计**：包装箱删除单独留痕表（pk_box_delete），业务表软删

## 5. DDL

完整建表语句见同目录 [schema.sql](schema.sql)。
