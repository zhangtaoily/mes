-- ============================================================
-- 精梳毛纺 MES 数据库 DDL（MySQL 8.0, utf8mb4, InnoDB）
-- 基于现有系统截图逆向设计 V1.0  2026-09-29
-- ============================================================
SET NAMES utf8mb4;

-- ============================================================
-- 一、系统域
-- ============================================================
CREATE TABLE sys_org (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父组织ID',
  org_name VARCHAR(64) NOT NULL COMMENT '组织名称',
  org_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型:1公司2部门3车间',
  sort_no INT DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0停用',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0
) COMMENT '组织机构';

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT COMMENT '组织ID',
  emp_no VARCHAR(32) COMMENT '工号',
  user_name VARCHAR(64) NOT NULL COMMENT '姓名',
  login_name VARCHAR(64) NOT NULL COMMENT '登录名',
  password VARCHAR(128) NOT NULL,
  phone VARCHAR(20),
  status TINYINT NOT NULL DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_login (login_name)
) COMMENT '用户';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_name VARCHAR(64) NOT NULL COMMENT '角色名',
  role_key VARCHAR(64) NOT NULL,
  data_scope TINYINT DEFAULT 1 COMMENT '数据权限范围',
  status TINYINT DEFAULT 1,
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '角色';

CREATE TABLE sys_user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
  UNIQUE KEY uk_ur (user_id, role_id)
) COMMENT '用户角色';

CREATE TABLE sys_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  menu_name VARCHAR(64) NOT NULL,
  menu_type TINYINT COMMENT '1目录2菜单3按钮',
  path VARCHAR(128), component VARCHAR(128), perms VARCHAR(128),
  visible TINYINT DEFAULT 1, status TINYINT DEFAULT 1,
  stopped TINYINT DEFAULT 0 COMMENT '已停用标记',
  sort_no INT DEFAULT 0,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '菜单(截图含"每日排班(已停用)"等停用标记)';

CREATE TABLE sys_role_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL, menu_id BIGINT NOT NULL,
  UNIQUE KEY uk_rm (role_id, menu_id)
) COMMENT '角色菜单';

CREATE TABLE sys_dict (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(64) NOT NULL COMMENT '字典类型:stop_reason停机原因/shift班次/bag_color袋子颜色/tube_color筒管颜色/defect_type疵纱分类/report_type报工类型等',
  dict_name VARCHAR(64), status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_type (dict_type)
) COMMENT '字典类型';

CREATE TABLE sys_dict_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(64) NOT NULL, item_label VARCHAR(64) NOT NULL, item_value VARCHAR(64) NOT NULL,
  sort_no INT DEFAULT 0, status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_item (dict_type, item_value)
) COMMENT '字典项';

CREATE TABLE sys_auto_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code_name VARCHAR(64) NOT NULL COMMENT '编号名',
  prefix VARCHAR(16) NOT NULL COMMENT '前缀:PI/PSC/TPC/SR/OR/QC/PS/PC/MF/PT等',
  date_format VARCHAR(16) DEFAULT 'yyyyMMdd',
  serial_len INT DEFAULT 6 COMMENT '流水位数',
  reset_type TINYINT DEFAULT 2 COMMENT '1不重置2按日',
  current_serial BIGINT DEFAULT 0, current_period VARCHAR(16) COMMENT '当前流水期间',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '自动编码规则(前缀+日期+流水,如PSC260927000030)';

CREATE TABLE sys_oper_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128), method VARCHAR(255), url VARCHAR(255),
  oper_name VARCHAR(32), oper_ip VARCHAR(64), oper_param TEXT,
  json_result TEXT, status TINYINT, error_msg TEXT,
  oper_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '操作日志';

-- ============================================================
-- 二、基础数据域
-- ============================================================
CREATE TABLE base_workshop (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workshop_code VARCHAR(32) NOT NULL,
  workshop_name VARCHAR(64) NOT NULL COMMENT '车间名:纺十车间/总厂复精梳车间(二)/总厂倒筒车间等',
  workshop_type TINYINT COMMENT '1纺纱2复精梳3倒筒4包装',
  k3_workshop_id VARCHAR(64) COMMENT 'K3车间ID',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (workshop_code)
) COMMENT '车间';

CREATE TABLE base_team (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workshop_id BIGINT NOT NULL,
  team_name VARCHAR(64) NOT NULL COMMENT '班组:甲班/乙班/丙班(白班)',
  leader_user_id BIGINT,
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '班组';

CREATE TABLE base_equipment_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL, type_name VARCHAR(64) NOT NULL COMMENT '设备类型',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '设备类型';

CREATE TABLE base_equipment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workshop_id BIGINT NOT NULL COMMENT '所属车间',
  eq_type_id BIGINT COMMENT '设备类型',
  machine_model VARCHAR(64) COMMENT '机型:VTS-08/SINCRO B 306/CSN等',
  machine_no VARCHAR(64) NOT NULL COMMENT '机台名称:110倍捻12/F9粗纱1/F10并线1',
  hour_capacity DECIMAL(12,3) COMMENT '小时产能',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '设备信息(机台)';

CREATE TABLE base_process (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_code VARCHAR(32) NOT NULL COMMENT '工序编号:PP000001',
  process_name VARCHAR(64) NOT NULL COMMENT '工序名:混条(一)/头针/二针/三针/四针/粗纱/细纱/并线/倍捻/摇绞/倒毛/倒丝',
  process_type VARCHAR(32) COMMENT '工序类型:前纺/后纺/复精梳',
  sort_no INT DEFAULT 0,
  material_unit VARCHAR(16) DEFAULT 'KG' COMMENT '物料单位',
  prod_unit VARCHAR(16) COMMENT '生产单位', count_unit VARCHAR(16) COMMENT '计数单位',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_pcode (process_code)
) COMMENT '生产工序';

CREATE TABLE base_process_standard (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_id BIGINT NOT NULL,
  standard_name VARCHAR(128), content TEXT COMMENT '工序标准内容',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '工序标准';

CREATE TABLE base_piece_price (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  process_id BIGINT, workshop_id BIGINT,
  spec VARCHAR(64) COMMENT '支数/规格区间',
  unit_price DECIMAL(12,4) COMMENT '计件单价',
  effective_date DATE,
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '计件单价';

CREATE TABLE base_batch_flag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  flag_name VARCHAR(32) NOT NULL COMMENT '标识名:紫叶子/三角/菱形/粉色',
  alias VARCHAR(32) COMMENT '别名',
  preview_url VARCHAR(255) COMMENT '标识预览图',
  process_id BIGINT COMMENT '适用工序',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '标识牌(批号标识色块)';

CREATE TABLE base_craft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  craft_code VARCHAR(32) NOT NULL COMMENT '工艺编号:CT000006',
  craft_name VARCHAR(64) NOT NULL COMMENT '工艺名称:倒丝等',
  component VARCHAR(255) COMMENT '成分',
  actual_count DECIMAL(10,2) COMMENT '实纺支数',
  craft_level VARCHAR(32) COMMENT '工艺等级',
  dye_mode VARCHAR(32) COMMENT '染色方式',
  twist DECIMAL(10,2) COMMENT '捻度',
  enabled TINYINT DEFAULT 1 COMMENT '启用/停用',
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '技术工艺库(基础工艺模板)';

CREATE TABLE base_craft_param (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  craft_id BIGINT NOT NULL,
  process_id BIGINT COMMENT '所属工序',
  param_name VARCHAR(64) NOT NULL COMMENT '工艺项:车速/定长/定量/张力/并合根数/牵伸倍数/喂入单重等',
  param_value VARCHAR(128) COMMENT '工艺值',
  lower_limit VARCHAR(64) COMMENT '下限', upper_limit VARCHAR(64) COMMENT '上限',
  sort_no INT DEFAULT 0,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '工艺参数(技术/车间工艺库-工艺项)';

CREATE TABLE base_craft_workshop (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  craft_name VARCHAR(64) NOT NULL COMMENT '车间工艺名称',
  workshop_id BIGINT NOT NULL, prod_line VARCHAR(64) COMMENT '车间产线:复二4组/纺十车间2路',
  raw_material VARCHAR(255) COMMENT '原料明细',
  actual_count DECIMAL(10,2), twist DECIMAL(10,2), dye_mode VARCHAR(32), craft_level VARCHAR(32),
  process_id BIGINT COMMENT '工序', equipment_id BIGINT COMMENT '机型',
  hour_capacity DECIMAL(12,3),
  enabled TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '车间工艺库(工序>机型>工艺项三级,工艺项复用base_craft_param)';

CREATE TABLE base_pack_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_code VARCHAR(32), material_name VARCHAR(64) COMMENT '包装材质:内销纸箱(标准)/内胆纸箱(实重)',
  spec VARCHAR(128) COMMENT '包装规格:默认纸箱规格',
  weight DECIMAL(12,3) COMMENT '纸箱重量',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '包装材质';

CREATE TABLE base_print_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(32), template_name VARCHAR(128) NOT NULL COMMENT '标准带RWS认证(英文)/标准',
  template_type TINYINT COMMENT '1筒贴2箱单3吊牌4制造卡5唛架6流程单7工艺单',
  paper_type VARCHAR(32) COMMENT '纸张类型',
  logo_url VARCHAR(255),
  content TEXT COMMENT '打印参数模板',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '打印样式';

CREATE TABLE base_qc_indicator (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  indicator_code VARCHAR(32) NOT NULL,
  indicator_name VARCHAR(64) NOT NULL COMMENT '每米重量/含油/回潮/长度/离散/短毛率/疵点/不匀率/毛粒50克/强力/毛羽',
  unit VARCHAR(16), indicator_type TINYINT COMMENT '1原始2衍生(公式计算)',
  formula_id BIGINT COMMENT '衍生指标关联公式',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '质检指标';

CREATE TABLE base_qc_scheme (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scheme_code VARCHAR(32) NOT NULL, scheme_name VARCHAR(64) NOT NULL,
  workshop_id BIGINT, sample_count INT COMMENT '试样数',
  status TINYINT DEFAULT 1 COMMENT '启用/停用',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '质检方案';

CREATE TABLE base_qc_scheme_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  scheme_id BIGINT NOT NULL, indicator_id BIGINT NOT NULL,
  standard_value VARCHAR(64) COMMENT '标准值', lower_limit VARCHAR(64), upper_limit VARCHAR(64),
  sort_no INT DEFAULT 0
) COMMENT '质检方案-指标项';

CREATE TABLE base_machine_efficiency (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  equipment_id BIGINT NOT NULL, workshop_id BIGINT,
  efficiency DECIMAL(7,3) COMMENT '机台效率%',
  period_month VARCHAR(7) COMMENT '年月',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '机台效率';

CREATE TABLE base_prod_line (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workshop_id BIGINT NOT NULL, line_name VARCHAR(64) NOT NULL COMMENT '产线:纺十车间2路',
  k3_line_id VARCHAR(64) COMMENT 'K3生产线ID',
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '车间产线';

-- ============================================================
-- 三、K3订单域
-- ============================================================
CREATE TABLE erp_k3_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_type VARCHAR(32) COMMENT '订单类型(调入)',
  order_no VARCHAR(64) NOT NULL COMMENT '订单号:FBDC772348',
  order_date DATE,
  workshop_name VARCHAR(64), prod_line VARCHAR(64),
  batch_no VARCHAR(64) COMMENT '批号:26091340',
  k3_qty DECIMAL(12,3) COMMENT 'K3数量', merge_feed_qty DECIMAL(12,3) COMMENT '合并投料量',
  plan_start_date DATE, plan_end_date DATE,
  color_no VARCHAR(64) COMMENT '色号', product_name VARCHAR(128) COMMENT '品名',
  spin_count VARCHAR(32) COMMENT '纺纱支数', count_num VARCHAR(16) COMMENT '支数', ply_num VARCHAR(16) COMMENT '股数',
  convert_rate DECIMAL(10,4) COMMENT '折标率', imported_qty DECIMAL(12,3) COMMENT '已引入数量',
  dye_type VARCHAR(32) COMMENT '染色类别:条染/绞纱/胚纱',
  material_detail VARCHAR(255) COMMENT '物料明细',
  material_no VARCHAR(64) COMMENT '物料号:4.1.1.001', material_name VARCHAR(128), material_spec VARCHAR(128),
  unit VARCHAR(16) DEFAULT 'KG',
  name_en VARCHAR(255) COMMENT '英文品名', name_cn VARCHAR(255) COMMENT '中文品名',
  print_code VARCHAR(64) COMMENT '打印编号', company_color VARCHAR(64) COMMENT '公司色号',
  issue_date DATE COMMENT '纺纱下达日期',
  customer_name VARCHAR(128), sale_dept VARCHAR(64), sale_user VARCHAR(32),
  purpose VARCHAR(64) COMMENT '用途:横机/梭织',
  combed_batch_no VARCHAR(64) COMMENT '纺纱用精梳批号', recombed_batch_no VARCHAR(64) COMMENT '复精梳批号',
  raw_batch_no VARCHAR(64) COMMENT '原料批号', raw_sample_no VARCHAR(64) COMMENT '原料样板编号(OCR不确定):001.030',
  raw_material VARCHAR(255) COMMENT '原料', customer_color VARCHAR(64) COMMENT '客户色号',
  dye_vat_no VARCHAR(64) COMMENT '缸号', print_vat_no VARCHAR(64) COMMENT '打印缸号',
  process_unit VARCHAR(128) COMMENT '加工单位', process_name VARCHAR(64) COMMENT '工序名称',
  version_name VARCHAR(32) COMMENT '版本名称',
  td_proof_color VARCHAR(64) COMMENT '条染订版色号:HY-M63634', jd_proof_color VARCHAR(64) COMMENT '绞染订版色号',
  color VARCHAR(64) COMMENT '颜色:中灰',
  aux_attr VARCHAR(255) COMMENT '辅助属性:70% 澳毛条 HY-M63634-中灰',
  bom_version VARCHAR(32) COMMENT 'BOM版本',
  is_fluorescent TINYINT DEFAULT 0 COMMENT '是否荧光',
  -- 工艺要求
  winding_req TEXT COMMENT '络筒工艺要求', tech_spin_req TEXT COMMENT '技术工艺纺纱要求',
  plan_spin_req TEXT COMMENT '计划工艺纺纱要求', craft_twist_req TEXT COMMENT '工艺及倍捻要求',
  product_req TEXT COMMENT '成品要求', label_print_req TEXT COMMENT '标签打印要求',
  pack_req TEXT COMMENT '包装要求', recombed_remark TEXT COMMENT '复精梳纺纱备注',
  -- 审批
  order_status VARCHAR(32), audit_user VARCHAR(32), audit_time DATETIME,
  void_status TINYINT DEFAULT 0, void_time DATETIME, del_flag TINYINT DEFAULT 0,
  -- K3 ID映射
  k3_order_no_mes VARCHAR(64) COMMENT 'K3订单号(MES)', k3_order_id VARCHAR(64), k3_material_id VARCHAR(64),
  k3_workshop_id VARCHAR(64), k3_line_id VARCHAR(64), batch_id VARCHAR(64), aux_attr_id VARCHAR(64),
  customer_id VARCHAR(64), order_detail_id VARCHAR(64), process_unit_id VARCHAR(64), owner_id VARCHAR(64),
  print_template_id VARCHAR(64), measure_type_id VARCHAR(64),
  mes_create_time DATETIME, mes_update_time DATETIME, k3_version_time DATETIME COMMENT 'K3数据版本时间(增量同步)',
  is_closed TINYINT DEFAULT 0 COMMENT '是否关闭(引入完毕)',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_order (order_no),
  KEY idx_batch (batch_no), KEY idx_k3ver (k3_version_time)
) COMMENT 'K3订单(同步只读镜像)';

-- ============================================================
-- 四、计划工艺单与生产任务域
-- ============================================================
CREATE TABLE plan_craft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  craft_no VARCHAR(64) NOT NULL COMMENT '计划工艺单号',
  doc_status VARCHAR(32) DEFAULT '编制中' COMMENT '编制中/已提交/已审核/已撤回',
  workshop_id BIGINT, prod_line_id BIGINT,
  batch_no VARCHAR(64) COMMENT '生产批次', trace_batch_no VARCHAR(64) COMMENT '追溯批次',
  batch_flag_id BIGINT COMMENT '批号标识',
  raw_material VARCHAR(255) COMMENT '原料明细', raw_spec VARCHAR(255) COMMENT '原料规格',
  color_no VARCHAR(64), actual_count VARCHAR(32) COMMENT '实纺支数',
  feed_qty DECIMAL(12,3) COMMENT '投料量', merge_feed_qty DECIMAL(12,3) COMMENT '合并投料量',
  k3_order_no VARCHAR(64) COMMENT '生产订单(K3)',
  order_date DATE COMMENT '下单日期', delivery_date DATE COMMENT '预计交期',
  prod_flag VARCHAR(64) COMMENT '生产标识',
  plan_qty DECIMAL(12,3) COMMENT '计划产量',
  special_req TEXT COMMENT '特殊工艺要求', plan_spin_req TEXT, tech_spin_req TEXT,
  craft_twist_req TEXT, product_req TEXT, label_req TEXT, pack_req TEXT,
  fore_remark TEXT COMMENT '前纺备注', groove_remark TEXT COMMENT '翻槽纺纱备注', after_remark TEXT COMMENT '后纺备注',
  remark TEXT,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_craft (craft_no), KEY idx_batch (batch_no)
) COMMENT '计划工艺单';

CREATE TABLE plan_craft_route (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_craft_id BIGINT NOT NULL,
  seq INT COMMENT '排序', line_type TINYINT COMMENT '类型',
  process_id BIGINT NOT NULL COMMENT '工序',
  is_feed TINYINT DEFAULT 0 COMMENT '投料', is_report TINYINT DEFAULT 0 COMMENT '报产',
  is_load TINYINT DEFAULT 0 COMMENT '上下料', line_side_warehouse VARCHAR(64) COMMENT '线边仓',
  material_unit VARCHAR(16), prod_unit VARCHAR(16),
  plan_qty DECIMAL(12,3) COMMENT '计划产量', scheduled_qty DECIMAL(12,3) COMMENT '排程量',
  output_qty DECIMAL(12,3) COMMENT '产出量', workshop_id BIGINT,
  is_finished TINYINT DEFAULT 0 COMMENT '了批',
  process_flag_id BIGINT COMMENT '工序标识', equipment_id BIGINT COMMENT '机型',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '计划工艺单-工序路线';

CREATE TABLE plan_craft_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_craft_id BIGINT NOT NULL,
  raw_batch_no VARCHAR(64) COMMENT '原料批号', raw_name VARCHAR(128) COMMENT '原料名称',
  workshop_qty DECIMAL(12,3) COMMENT '车间用料量', mix_qty DECIMAL(12,3) COMMENT '配料量',
  feed_qty DECIMAL(12,3) COMMENT '投料量', unit VARCHAR(16)
) COMMENT '计划工艺单-用料汇总';

CREATE TABLE plan_craft_prod (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_craft_id BIGINT NOT NULL,
  product_no VARCHAR(64) COMMENT '产品编号:5.1.2.004.099',
  product_name VARCHAR(128), print_abbr VARCHAR(64) COMMENT '打印缩写:W100',
  name_en VARCHAR(255), product_spec VARCHAR(128) COMMENT '2/60 NM',
  aux_attr VARCHAR(255) COMMENT '辅助属性:90%防缩短毛条(阳离子)',
  color_no VARCHAR(64), company_color VARCHAR(64),
  td_proof_color VARCHAR(64), jd_proof_color VARCHAR(64),
  actual_count VARCHAR(32), recombed_batch_no VARCHAR(64) COMMENT '复精梳批号',
  recombed_no VARCHAR(64) COMMENT '复精梳编号', fine_count VARCHAR(32) COMMENT '细纱支数',
  spin_count VARCHAR(32) COMMENT '纺纱支数', unit VARCHAR(16),
  print_en VARCHAR(255) COMMENT '英文打印', print_cn VARCHAR(255) COMMENT '中文打印',
  purpose VARCHAR(64), customer_name VARCHAR(128), sale_dept VARCHAR(64), sale_user VARCHAR(32),
  is_fluorescent TINYINT DEFAULT 0 COMMENT '是否荧光', ply_count VARCHAR(32) COMMENT '股数/支数:2/60'
) COMMENT '计划工艺单-生产信息';

CREATE TABLE plan_craft_raw_qc (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_craft_id BIGINT NOT NULL,
  sample_batch VARCHAR(64) COMMENT '取样批次', sample_product VARCHAR(128), remark VARCHAR(255),
  weight_per_meter DECIMAL(10,3), uneven_rate DECIMAL(10,3) COMMENT '不匀率',
  neps_50g DECIMAL(10,3) COMMENT '毛粒50克', impurity DECIMAL(10,3) COMMENT '杂质/毛屑不清',
  moisture DECIMAL(7,3) COMMENT '回潮', wool_piece DECIMAL(10,3) COMMENT '毛片',
  alien_color DECIMAL(10,3) COMMENT '异色毛', alien_fiber DECIMAL(10,3) COMMENT '异纤',
  neps_large DECIMAL(10,3), neps_mid DECIMAL(10,3), neps_small DECIMAL(10,3),
  oil DECIMAL(7,3) COMMENT '含油', grass_50g DECIMAL(10,3) COMMENT '草屑50克',
  length DECIMAL(10,3), dispersion DECIMAL(10,3) COMMENT '离散'
) COMMENT '计划工艺单-原料检验报告';

CREATE TABLE prod_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL COMMENT '任务编号:PI...',
  task_type VARCHAR(32) DEFAULT '主任务' COMMENT '主任务/子任务',
  line_variant VARCHAR(32) DEFAULT '普通' COMMENT '普通/复精梳/倒筒',
  doc_status VARCHAR(32) DEFAULT '编制中' COMMENT '编制中/生效',
  exec_status VARCHAR(32) COMMENT '计划执行/工艺控制',
  batch_no VARCHAR(64) NOT NULL COMMENT '生产批号:J2609988-10',
  workshop_id BIGINT, prod_line_id BIGINT,
  batch_flag_id BIGINT, raw_material VARCHAR(255), color_no VARCHAR(64),
  product_no VARCHAR(64), product_spec VARCHAR(128), actual_count VARCHAR(32),
  merge_feed_qty DECIMAL(12,3), plan_feed_qty DECIMAL(12,3) COMMENT '计划投料量',
  plan_qty DECIMAL(12,3) COMMENT '计划产量', unit VARCHAR(16) DEFAULT 'KG',
  k3_order_no VARCHAR(64) COMMENT '生产订单号', order_date DATE, delivery_date DATE,
  spin_method VARCHAR(64) COMMENT '纺纱方法:进口紧密纺',
  raw_variety VARCHAR(64) COMMENT '原料品种:长毛', raw_type VARCHAR(64) COMMENT '原料类型:全毛',
  ply_count VARCHAR(32) COMMENT '股数/支数',
  is_finished TINYINT DEFAULT 0 COMMENT '了批', is_closed TINYINT DEFAULT 0 COMMENT '关闭',
  remark TEXT,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_task (task_no), KEY idx_batch (batch_no)
) COMMENT '生产任务';

CREATE TABLE prod_task_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  spin_batch_no VARCHAR(64) COMMENT '纺纱批号', product_name VARCHAR(128),
  raw_material VARCHAR(255), ply_count VARCHAR(32),
  origin_plan_qty DECIMAL(12,3) COMMENT '原计划量', plan_qty DECIMAL(12,3),
  feed_qty DECIMAL(12,3), output_qty DECIMAL(12,3), unit VARCHAR(16)
) COMMENT '生产任务-订单明细';

CREATE TABLE prod_task_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  raw_batch_no VARCHAR(64), raw_name VARCHAR(128),
  plan_feed_qty DECIMAL(12,3) COMMENT '计划投料量', 
  workshop_qty DECIMAL(12,3) COMMENT '车间用料量',
  fed_qty DECIMAL(12,3) COMMENT '已投料量', unit VARCHAR(16)
) COMMENT '生产任务-用料汇总';

CREATE TABLE prod_task_extra (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  batch_no VARCHAR(64), workshop_name VARCHAR(64),
  temperature DECIMAL(6,2) COMMENT '温度',
   humidity DECIMAL(6,2) COMMENT '湿度',
  raw_oil DECIMAL(7,3) COMMENT '原料含油',
   product_oil DECIMAL(7,3) COMMENT '成品含油',
  machine_model VARCHAR(64) COMMENT '机型',
  wrap_needle TINYINT DEFAULT 0 COMMENT '绕针板', 
  wrap_ring TINYINT DEFAULT 0 COMMENT '绕圆梳',
  wrap_balloon TINYINT DEFAULT 0 COMMENT '绕气泡', 
  wrap_roller TINYINT DEFAULT 0 COMMENT '绕皮辊',
  wrap_roller2 TINYINT DEFAULT 0 COMMENT '绕罗拉',
  talc_powder TINYINT DEFAULT 0 COMMENT '撒滑石粉',
  extra_type VARCHAR(32) COMMENT '类型', remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '生产任务其它信息';

CREATE TABLE rep_front_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workshop_id BIGINT NOT NULL, batch_no VARCHAR(64) NOT NULL COMMENT '生产批号',
  report_date DATE,
  temperature DECIMAL(6,2) COMMENT '温度', humidity DECIMAL(6,2) COMMENT '湿度',
  raw_oil DECIMAL(6,3) COMMENT '原料含油', product_oil DECIMAL(6,3) COMMENT '成品含油',
  machine_model VARCHAR(64) COMMENT '机型',
  wrap_comber VARCHAR(16) COMMENT '绕针板(无/轻微/严重)',
  wrap_circle_comber VARCHAR(16) COMMENT '绕圆梳',
  wrap_bubble VARCHAR(16) COMMENT '绕气泡',
  wrap_apron VARCHAR(16) COMMENT '绕皮辊',
  wrap_roller VARCHAR(16) COMMENT '绕罗拉',
  use_talcum VARCHAR(16) COMMENT '撒滑石粉',
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_batch (batch_no)
) COMMENT '前纺生产情况汇报(挂工艺管理菜单)';

CREATE TABLE prod_task_route (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  seq INT COMMENT '序号', type_name VARCHAR(32) COMMENT '类型',
  process_id BIGINT COMMENT '工序', process_name VARCHAR(64),
  is_feed TINYINT DEFAULT 0 COMMENT '投料(红绿图标)',
  is_report TINYINT DEFAULT 0 COMMENT '报产',
  is_batch_change TINYINT DEFAULT 0 COMMENT '上下批',
  material_unit VARCHAR(16) COMMENT '物料单位', prod_unit VARCHAR(16) COMMENT '生产单位',
  plan_qty DECIMAL(12,3) COMMENT '计划产量', feed_qty DECIMAL(12,3) COMMENT '投料量', output_qty DECIMAL(12,3) COMMENT '产出量',
  workshop_id BIGINT COMMENT '车间',
  is_finish_batch TINYINT DEFAULT 0 COMMENT '了批',
  process_flag VARCHAR(64) COMMENT '工序标识',
  KEY idx_task (task_id)
) COMMENT '生产任务-工序路线(倒筒任务15列结构,工序如倒毛)';

CREATE TABLE prod_task_craft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  process_name VARCHAR(64) NOT NULL COMMENT '工序',
  param_name VARCHAR(64) NOT NULL COMMENT '工艺项', param_value VARCHAR(128) COMMENT '工艺值',
  lower_limit VARCHAR(64), upper_limit VARCHAR(64), unit VARCHAR(16),
  ref_source TINYINT COMMENT '引用来源:1技术工艺库2车间工艺库',
  ref_scheme_no VARCHAR(64) COMMENT '引用方案编号:TJ.../CJ...',
  KEY idx_task (task_id)
) COMMENT '生产任务-工艺配置(引用技术/车间工艺库带出)';

CREATE TABLE prod_task_change (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  change_no VARCHAR(64) NOT NULL COMMENT '任务变更单号',
  task_id BIGINT NOT NULL COMMENT '原任务',
  doc_status VARCHAR(32) COMMENT '编制中/已提交/已审核',
  change_content TEXT,
  new_plan_feed_qty DECIMAL(12,3), new_plan_qty DECIMAL(12,3), new_delivery_date DATE,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_change (change_no)
) COMMENT '任务变更';

-- ============================================================
-- 五、排程与制造卡域
-- ============================================================
CREATE TABLE prod_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schedule_no VARCHAR(64) NOT NULL COMMENT '排程编号:PSC260927000030',
  work_order_no VARCHAR(64) COMMENT '工单单号:TPC2026091600005(自动生成)',
  workshop_id BIGINT NOT NULL, task_id BIGINT NOT NULL,
  batch_no VARCHAR(64) COMMENT '批次信息', trace_batch_no VARCHAR(64) COMMENT '追溯批次',
  batch_flag_id BIGINT,
  process_id BIGINT NOT NULL COMMENT '工序',
  machine_model VARCHAR(64) COMMENT '机型', change_model VARCHAR(64) COMMENT '换机型',
  equipment_id BIGINT NOT NULL COMMENT '机台',
  schedule_date DATE NOT NULL, plan_start DATETIME, plan_end DATETIME,
  schedule_qty DECIMAL(12,3) COMMENT '排程数量', seq INT COMMENT '序号',
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_sch (schedule_no), KEY idx_date (schedule_date), KEY idx_batch (batch_no)
) COMMENT '生产排程';

CREATE TABLE prod_schedule_craft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schedule_id BIGINT NOT NULL,
  param_name VARCHAR(64) NOT NULL COMMENT '工艺项', param_value VARCHAR(128) COMMENT '工艺值',
  upper_limit VARCHAR(64), lower_limit VARCHAR(64), sort_no INT
) COMMENT '排程-上机工艺(项/值/上下限)';

CREATE TABLE prod_schedule_material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schedule_id BIGINT NOT NULL,
  raw_batch_no VARCHAR(64), raw_name VARCHAR(128),
  plan_qty DECIMAL(12,3) COMMENT '计划用量', actual_qty DECIMAL(12,3) COMMENT '实投数量', unit VARCHAR(16)
) COMMENT '排程-用料汇总';

CREATE TABLE prod_schedule_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  schedule_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL COMMENT '生产任务(PI)',
  product_no VARCHAR(64), product_name VARCHAR(128), product_pno VARCHAR(64) COMMENT '产品编号',
  spec VARCHAR(64) COMMENT '产品规格', aux_attr VARCHAR(255) COMMENT '辅助属性',
  raw_detail VARCHAR(255) COMMENT '原料明细',
  process_name VARCHAR(64) COMMENT '排产工序',
  plan_qty DECIMAL(12,3) COMMENT '计划产量', scheduled_qty DECIMAL(12,3) COMMENT '已排产量',
  schedule_qty DECIMAL(12,3) COMMENT '排程数量(校验:不可超计划-已排)', unit VARCHAR(16),
  KEY idx_sch (schedule_id), KEY idx_task (task_id)
) COMMENT '排程-任务明细(从生产任务带出,剩余量校验)';

CREATE TABLE prod_machine_card (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  card_no VARCHAR(64) COMMENT '制造卡号',
  schedule_id BIGINT NOT NULL,
  task_id BIGINT, batch_no VARCHAR(64), batch_flag_id BIGINT,
  workshop_id BIGINT, process_id BIGINT, machine_model VARCHAR(64), equipment_id BIGINT,
  status VARCHAR(32) DEFAULT '已排在做' COMMENT '已排在做/了机',
  count_spec VARCHAR(64) COMMENT '支数:45.4/2', product_spec VARCHAR(128) COMMENT '品名规格:MwU5545 2/48 NM',
  raw_material VARCHAR(255),
  speed VARCHAR(64) COMMENT '车速:350', fixed_length VARCHAR(64) COMMENT '定长:20500',
  quota VARCHAR(64) COMMENT '定量:0.9kg/只', tension VARCHAR(128) COMMENT '张力:防缩:22/20',
  merge_count VARCHAR(32) COMMENT '并合根数:2根',
  craft_desc TEXT COMMENT '工艺说明', qr_code VARCHAR(255) COMMENT '二维码',
  print_time DATETIME, withdraw_time DATETIME COMMENT '撤回时间',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_status (status)
) COMMENT '上机制造卡(A4打印+二维码)';

-- ============================================================
-- 六、现场汇报域
-- ============================================================
CREATE TABLE rep_batch_on (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_on_no VARCHAR(64) NOT NULL COMMENT '上批编号:SR26092900442',
  schedule_id BIGINT NOT NULL COMMENT '排程号',
  team_id BIGINT COMMENT '班组', shift VARCHAR(32) COMMENT '班次',
  batch_no VARCHAR(64), trace_batch_no VARCHAR(64),
  process_id BIGINT, process_flag VARCHAR(64) COMMENT '工序标识:浅三粗纱管',
  product_code VARCHAR(64), product_name VARCHAR(128),
  batch_flag_id BIGINT, equipment_id BIGINT NOT NULL,
  is_batch_off TINYINT DEFAULT 0 COMMENT '是否下批',
  is_finished TINYINT DEFAULT 0 COMMENT '是否了批',
  batch_on_time DATETIME COMMENT '上批时间', batch_off_time DATETIME COMMENT '下批时间',
  plan_qty DECIMAL(12,3), remark VARCHAR(255),
  source TINYINT DEFAULT 1 COMMENT '1PC 2PDA',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_bon (batch_on_no)
) COMMENT '上批汇报';

CREATE TABLE rep_batch_on_flag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_on_id BIGINT NOT NULL,
  flag_id BIGINT NOT NULL COMMENT '工序标识(标识牌)',
  task_no VARCHAR(64)
) COMMENT '上批汇报-工序标识';

CREATE TABLE rep_batch_on_craft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_on_id BIGINT NOT NULL,
  param_name VARCHAR(64) COMMENT '工艺项', param_value VARCHAR(128) COMMENT '工艺值',
  check_value VARCHAR(128) COMMENT '检验值'
) COMMENT '上批汇报-工艺项';

CREATE TABLE rep_output (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  output_no VARCHAR(64) NOT NULL COMMENT '编码:OR26092901064',
  schedule_id BIGINT NOT NULL, task_id BIGINT,
  team_id BIGINT COMMENT '班组', report_user VARCHAR(32) COMMENT '汇报人员',
  prod_users VARCHAR(255) COMMENT '生产人员(多选)',
  batch_no VARCHAR(64), process_id BIGINT,
  product_name VARCHAR(128), equipment_id BIGINT,
  output_qty DECIMAL(12,3) NOT NULL COMMENT '汇报产量',
  rework_qty DECIMAL(12,3) COMMENT '返工数量',
  return_wool_qty DECIMAL(12,3) COMMENT '回毛数量',
  return_wool_rate DECIMAL(7,3) COMMENT '正常回毛上报比例',
  wool_qty DECIMAL(12,3) COMMENT '毛料数量',
  bag_color VARCHAR(32) COMMENT '袋子颜色', tube_color VARCHAR(32) COMMENT '筒管颜色:网蓝',
  report_type VARCHAR(32) COMMENT '报工类型', unit VARCHAR(16),
  check_qty DECIMAL(12,3) COMMENT '检验产量',
  report_date DATE NOT NULL COMMENT '报产日期',
  is_warehouse TINYINT DEFAULT 0 COMMENT '是否入库',
  remark VARCHAR(255) COMMENT '备注:倒毛自动报程',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_out (output_no), KEY idx_batch (batch_no)
) COMMENT '产量汇报';

CREATE TABLE rep_start_stop (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stop_no VARCHAR(64) NOT NULL COMMENT '停机编号',
  workshop_id BIGINT, equipment_id BIGINT, machine_model VARCHAR(64),
  team_id BIGINT COMMENT '汇报班组', report_user VARCHAR(32) COMMENT '汇报人',
  report_date DATE, stop_duration DECIMAL(10,2) COMMENT '停机时长(小时)',
  stop_reason VARCHAR(64) COMMENT '停机原因:撞车/落料/停机/其他',
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_stop (stop_no)
) COMMENT '启停汇报';

CREATE TABLE rep_rebatch_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL COMMENT '【纺八车间】【丙班】【时段】翻批记录',
  workshop_id BIGINT, team_id BIGINT,
  start_time DATETIME, end_time DATETIME,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '翻批日志';

CREATE TABLE rep_rebatch_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rebatch_log_id BIGINT NOT NULL,
  workshop_id BIGINT, team_id BIGINT,
  batch_no VARCHAR(64), trace_batch_no VARCHAR(64),
  raw_material VARCHAR(255), product_no VARCHAR(64), product_spec VARCHAR(128),
  process_id BIGINT, equipment_id BIGINT,
  is_batch_off TINYINT, is_finished TINYINT,
  batch_on_time DATETIME, batch_on_user VARCHAR(32),
  batch_off_time DATETIME, batch_off_user VARCHAR(32)
) COMMENT '翻批记录明细';

CREATE TABLE rep_labor_hour (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  period_month VARCHAR(7) COMMENT '年月',
  prod_line VARCHAR(64) COMMENT '工时产线',
  user_id BIGINT COMMENT '人员', work_hour DECIMAL(10,2) COMMENT '工时',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '人员工时';

CREATE TABLE rep_auto_winding_cut (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(64), raw_material VARCHAR(255), color_no VARCHAR(64),
  count_spec VARCHAR(32) COMMENT '支数', workshop_id BIGINT, equipment_id BIGINT,
  cut_type VARCHAR(32) COMMENT '电容/光电',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '自络筒切疵记录';

CREATE TABLE rep_truck (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  truck_no VARCHAR(64) NOT NULL COMMENT '车号',
  status VARCHAR(32) COMMENT '状态',
  workshop_id BIGINT COMMENT '所在车间', batch_no VARCHAR(64) COMMENT '批号',
  raw_material VARCHAR(255), location VARCHAR(64) COMMENT '库位',
  bag_color VARCHAR(32) COMMENT '袋子颜色',
  ball_count INT COMMENT '毛球数',
  gross_weight DECIMAL(12,3) COMMENT '毛重', net_weight DECIMAL(12,3) COMMENT '净重',
  standard_weight DECIMAL(12,3) COMMENT '标重',
  moisture_standard DECIMAL(7,3) COMMENT '公定回潮', moisture_actual DECIMAL(7,3) COMMENT '实测回潮',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '复精梳毛球周转车';

CREATE TABLE rep_truck_move (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  truck_id BIGINT NOT NULL,
  from_location VARCHAR(64) COMMENT '移出库位', to_location VARCHAR(64) COMMENT '移入库位',
  scan_workshop VARCHAR(64) COMMENT '扫描车间',
  recombed_batch_no VARCHAR(64) COMMENT '复精梳批号', spin_batch_no VARCHAR(64) COMMENT '纺纱批号',
  bag_color VARCHAR(32), ball_count INT, gross_weight DECIMAL(12,3), net_weight DECIMAL(12,3),
  standard_weight DECIMAL(12,3), moisture_standard DECIMAL(7,3), moisture_actual DECIMAL(7,3),
  op_name VARCHAR(64) COMMENT '操作名称:绑定/称重/入库/移库/车辆拆分/出库',
  move_net_weight DECIMAL(12,3) COMMENT '移出净重', move_ball_count INT COMMENT '移出毛球数',
  is_first TINYINT COMMENT '先排', is_finished TINYINT COMMENT '了批',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '周转车移动明细';

CREATE TABLE rep_yarn_steam (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  print_code VARCHAR(64) COMMENT '打印编码',
  status VARCHAR(32) COMMENT '状态', process_id BIGINT,
  truck_no VARCHAR(64) COMMENT '车号', grain_count INT COMMENT '颗数',
  weight DECIMAL(12,3) COMMENT '重量', truck_count INT COMMENT '车数',
  batch_no VARCHAR(64),
  print_user VARCHAR(32), print_time DATETIME,
  receive_user VARCHAR(32), receive_time DATETIME,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '蒸纱信息';

CREATE TABLE rep_silk_truck (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL COMMENT '任务号:PT26092900057',
  batch_no VARCHAR(64), product_no VARCHAR(64), product_model VARCHAR(64) COMMENT '产品型号:MwNw8713',
  workshop_id BIGINT, pack_workshop_id BIGINT COMMENT '包装车间',
  product_name VARCHAR(128), name_en VARCHAR(255), raw_material VARCHAR(255),
  company_color VARCHAR(64), color_no VARCHAR(64), product_spec VARCHAR(128),
  plan_qty DECIMAL(12,3), actual_qty DECIMAL(12,3), unit VARCHAR(16),
  spin_count VARCHAR(32), print_cn VARCHAR(255), print_en VARCHAR(255),
  delivery_date DATE, fore_remark TEXT, after_remark TEXT COMMENT '后纺备注:精梳棉,上蜡,留尾纱...',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '丝车吊牌任务';

CREATE TABLE rep_silk_truck_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  silk_truck_id BIGINT NOT NULL,
  status VARCHAR(32), print_code VARCHAR(64) COMMENT '打印编码',
  equipment_id BIGINT, weight DECIMAL(12,3), grain_count INT COMMENT '颗数', truck_count INT COMMENT '车数',
  print_user VARCHAR(32), print_time DATETIME,
  receive_user VARCHAR(32), receive_time DATETIME, receive_count INT COMMENT '接收颗数',
  receive_weight DECIMAL(12,3) COMMENT '接收重量'
) COMMENT '丝车吊牌明细(打印/接收闭环)';

-- ============================================================
-- 七、质检域
-- ============================================================
CREATE TABLE qc_sample (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(64), process_id BIGINT, equipment_id BIGINT,
  workshop_id BIGINT, feed_qty DECIMAL(12,3) COMMENT '投料量',
  report_time DATETIME COMMENT '上报时间',
  is_registered TINYINT DEFAULT 0 COMMENT '是否登记', register_time DATETIME,
  is_batch_off TINYINT DEFAULT 0, batch_off_time DATETIME,
  task_finished TINYINT DEFAULT 0 COMMENT '任务了批',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '抽样登记';

CREATE TABLE qc_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  qc_no VARCHAR(64) NOT NULL COMMENT '质检单号:QC2026092901254',
  workshop_id BIGINT NOT NULL, prod_line_id BIGINT,
  qc_type VARCHAR(32) NOT NULL COMMENT '类型:原料检/工序检/成品检',
  lab_type TINYINT DEFAULT 0 COMMENT '0车间1中心实验室',
  equipment_id BIGINT, team_id BIGINT,
  batch_no VARCHAR(64) COMMENT '生产批次/取样批次',
  dye_vat_no VARCHAR(64) COMMENT '缸号',
  inspector VARCHAR(32) NOT NULL COMMENT '检验员',
  inspect_date DATE NOT NULL,
  sample_qty DECIMAL(12,3) COMMENT '取样数量', retain_qty DECIMAL(12,3) COMMENT '留样数量',
  scheme_id BIGINT COMMENT '质检方案',
  raw_material VARCHAR(255) COMMENT '原料信息(中心实验室)',
  spin_count_drop VARCHAR(32) COMMENT '落纺次数:60/1',
  spin_count VARCHAR(32) COMMENT '纺纱支数',
  raw_batch_no VARCHAR(64), merge_feed_qty DECIMAL(12,3),
  doc_status VARCHAR(32) DEFAULT '编制中' COMMENT '编制中/已提交',
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_qc (qc_no)
) COMMENT '检验报告';

CREATE TABLE qc_report_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  qc_report_id BIGINT NOT NULL,
  indicator_id BIGINT NOT NULL COMMENT '指标',
  sample_no INT NOT NULL DEFAULT 1 COMMENT '试样号1..n',
  value_str VARCHAR(128) COMMENT '检验值',
  standard_value VARCHAR(64), upper_limit VARCHAR(64), lower_limit VARCHAR(64)
) COMMENT '检验报告-指标值(方案矩阵纵表)';

CREATE TABLE qc_formula (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  formula_name VARCHAR(128) NOT NULL,
  process_id BIGINT, workshop_type VARCHAR(32) COMMENT '整精梳车间/复精梳车间/前纺/后纺',
  formula_type VARCHAR(32) DEFAULT '工序检',
  target_indicator_id BIGINT NOT NULL COMMENT '目标指标',
  affect_indicators VARCHAR(255) COMMENT '影响的质检项(多选)',
  sort_no INT DEFAULT 0,
  script TEXT NOT NULL COMMENT 'JS公式脚本:var calculateBYL=function(){var zlObj=$BQ(质检项ID);...}',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '质检公式(JS引擎,$BQ取值计算衍生指标)';

CREATE TABLE qc_package_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_batch_no VARCHAR(64) NOT NULL COMMENT '入库批次:2607933-4',
  batch_no VARCHAR(64) COMMENT '生产批次',
  workshop_id BIGINT COMMENT '生产车间', qc_workshop_id BIGINT COMMENT '质检车间:质检五组',
  doc_date DATE, first_grade_qty INT COMMENT '一等品数',
  pass_rate DECIMAL(7,3) COMMENT '合格率%', remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '包装质检报告';

CREATE TABLE qc_package_defect (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  package_qc_id BIGINT NOT NULL,
  defect_type VARCHAR(32) NOT NULL COMMENT '正纱/AA BB纱/尾纱/辫子纱/松捻纱/紧捻纱/重纱/轻纱/断纱/多股纱/油纱/大小肚/乱纱/圈圈纱/其它',
  value DECIMAL(12,3) COMMENT '指标值', remark VARCHAR(255)
) COMMENT '包装质检-疵纱明细';

CREATE TABLE qc_wool_rate (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  period_month VARCHAR(7) NOT NULL COMMENT '年月',
  workshop_id BIGINT NOT NULL,
  standard_rate DECIMAL(7,3) COMMENT '标准用毛率%',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_wm (period_month, workshop_id)
) COMMENT '指标用毛率';

-- ============================================================
-- 八、设备域
-- ============================================================
CREATE TABLE eq_fault_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fault_code VARCHAR(32) NOT NULL, fault_name VARCHAR(64) NOT NULL,
  fault_desc VARCHAR(255), status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '故障分类';

CREATE TABLE eq_maintain_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_code VARCHAR(32) NOT NULL, item_name VARCHAR(64) NOT NULL,
  maintain_type VARCHAR(32) COMMENT '保养类型', item_desc VARCHAR(255),
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '保养项目';

CREATE TABLE eq_check_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  item_code VARCHAR(32) NOT NULL, item_name VARCHAR(64) NOT NULL,
  check_type VARCHAR(32) COMMENT '点检类型', item_desc VARCHAR(255),
  status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '点检项目';

CREATE TABLE eq_maintain_method (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  method_code VARCHAR(32) NOT NULL, method_name VARCHAR(64) NOT NULL,
  method_desc VARCHAR(255), status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '维保方式';

CREATE TABLE eq_part_model (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  part_no VARCHAR(32) NOT NULL COMMENT '件号', part_name VARCHAR(64) NOT NULL COMMENT '配件名称',
  part_desc VARCHAR(255), status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '配件型号';

CREATE TABLE eq_maintain_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_code VARCHAR(64) NOT NULL COMMENT '计划编码',
  workshop_id BIGINT NOT NULL, plan_name VARCHAR(128) COMMENT '保养名称',
  cycle_type TINYINT COMMENT '周期:日/周/月', cycle_value INT,
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT '保养计划';

CREATE TABLE eq_maintain_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_code VARCHAR(64) COMMENT '记录编码',
  maintain_plan_id BIGINT,
  workshop_id BIGINT, equipment_id BIGINT NOT NULL,
  maintain_type VARCHAR(32), maintain_item VARCHAR(128),
  expect_date DATE COMMENT '预计保养日期',
  maintain_user VARCHAR(32) COMMENT '保养人', finish_date DATE COMMENT '保养完成日期',
  is_finish TINYINT DEFAULT 0 COMMENT '是否完成',
  detail TEXT COMMENT '保养详情', remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '保养记录';

CREATE TABLE eq_check_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  plan_code VARCHAR(64) NOT NULL,
  workshop_id BIGINT, plan_name VARCHAR(128),
  cycle_type TINYINT, cycle_value INT, status TINYINT DEFAULT 1,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '点检计划';

CREATE TABLE eq_check_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_code VARCHAR(64) NOT NULL COMMENT '记录编码',
  check_plan_id BIGINT, workshop_id BIGINT,
  check_user VARCHAR(32) COMMENT '点检人', check_date DATE,
  `describe` VARCHAR(255) COMMENT '描述', remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '点检记录';

CREATE TABLE eq_repair_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_code VARCHAR(64) NOT NULL COMMENT '维修编码',
  workshop_id BIGINT, equipment_id BIGINT NOT NULL,
  fault_time DATETIME COMMENT '故障发生时间', fault_type_id BIGINT COMMENT '故障分类',
  repair_user VARCHAR(32) COMMENT '维修人', solve_time DATETIME COMMENT '故障解决时间',
  repair_method_id BIGINT COMMENT '维修方式', detail TEXT COMMENT '维修详情', remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '维保记录';

CREATE TABLE eq_duty_machine (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workshop_id BIGINT NOT NULL,
  check_user VARCHAR(32) NOT NULL COMMENT '点检人员', emp_no VARCHAR(32) COMMENT '人员工号',
  equipment_id BIGINT NOT NULL COMMENT '机台',
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '责任机台(人员-机台点检责任)';

-- ============================================================
-- 九、包装域
-- ============================================================
CREATE TABLE pk_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL COMMENT '任务编号:PS20260929092',
  task_variant VARCHAR(32) DEFAULT '普通' COMMENT '普通/委外/倒毛',
  batch_no VARCHAR(64) COMMENT '生产批号', k3_order_no VARCHAR(64) COMMENT '生产订单号',
  dye_vat_no VARCHAR(64) COMMENT '缸号',
  warehouse_batch_no VARCHAR(64) NOT NULL COMMENT '入库批号',
  workshop_id BIGINT COMMENT '生产车间', pack_workshop_id BIGINT COMMENT '包装车间:质检五组',
  product_name VARCHAR(128), name_en VARCHAR(255), raw_material VARCHAR(255),
  color_no VARCHAR(64), product_spec VARCHAR(128) COMMENT '1/72 NM',
  customer_name VARCHAR(128), sale_dept VARCHAR(64), sale_user VARCHAR(32),
  pack_require VARCHAR(255) COMMENT '包装要求:内销纸箱(标准)/内胆纸箱(实重)',
  pack_spec_id BIGINT COMMENT '包装规格', box_list_copies INT COMMENT '箱单份数',
  delivery_date DATE COMMENT '交期', gross_weight DECIMAL(12,3) COMMENT '毛重',
  template_id BIGINT COMMENT '打印模板:标准带RWS认证(英文)',
  delivery_mode VARCHAR(64) COMMENT '交货方式:简装交货/绞纱交货',
  weight_mode VARCHAR(32) NOT NULL COMMENT '计重方式:标重交货/实磅交货',
  is_hank TINYINT DEFAULT 0 COMMENT '是否绞纱', is_blended TINYINT DEFAULT 0 COMMENT '是否混纤/混托',
  customer_order_no VARCHAR(64), customer_batch_no VARCHAR(64),
  tube_count INT COMMENT '筒子数/绞数:24',
  paper_tube_weight DECIMAL(12,3) COMMENT '纸管重量:0.046',
  carton_weight DECIMAL(12,3) COMMENT '纸箱重量:1.8',
  film_bag_weight DECIMAL(12,3) COMMENT '薄膜袋重量',
  moisture_standard DECIMAL(7,3) COMMENT '公定回潮%:13.32',
  moisture_actual DECIMAL(7,3) COMMENT '实测回潮%:12.88',
  wool_remove_weight DECIMAL(12,3) COMMENT '去毛重量:0.1',
  box_weight_per DECIMAL(12,3) COMMENT '每箱重量(倒毛)',
  box_no_type VARCHAR(32) COMMENT '箱号类型(倒毛)',
  box_model VARCHAR(64) COMMENT '箱型号(倒毛)',
  weight_front10 DECIMAL(12,3) COMMENT '做前10块箱重量(倒毛)',
  weight_back10 DECIMAL(12,3) COMMENT '做后10块箱重量(倒毛)',
  temperature DECIMAL(6,2) COMMENT '温度(倒毛)',
  per_tube_weight DECIMAL(12,3) COMMENT '每管重量(倒毛)',
  is_finished TINYINT DEFAULT 0 COMMENT '任务了批',
  finish_time DATETIME COMMENT '了批时间',
  vat_finished TINYINT DEFAULT 0 COMMENT '缸号了批',
  vat_finish_time DATETIME COMMENT '缸号了批时间',
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_ptask (task_no), KEY idx_whbatch (warehouse_batch_no)
) COMMENT '包装任务(含倒毛包装任务)';

CREATE TABLE pk_execute (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  execute_no VARCHAR(64) NOT NULL COMMENT '包装编号:PC2606929174',
  pk_task_id BIGINT NOT NULL,
  pack_batch_no VARCHAR(64) COMMENT '包装批号:XJP2610456-12',
  team_id BIGINT COMMENT '包装班组',
  warehouse_batch_no VARCHAR(64) COMMENT '入库批号', dye_vat_no VARCHAR(64) COMMENT '缸号',
  pack_name VARCHAR(128) COMMENT '包装名称',
  product_name VARCHAR(128), name_en VARCHAR(255), raw_material VARCHAR(255),
  color_no VARCHAR(64), product_spec VARCHAR(128), pack_require VARCHAR(255),
  tube_count INT COMMENT '筒子数', pack_weight DECIMAL(12,3) COMMENT '包装重量',
  standard_weight DECIMAL(12,3) COMMENT '标重',
  moisture_standard DECIMAL(7,3) COMMENT '公定回潮', moisture_actual DECIMAL(7,3) COMMENT '实测回潮',
  is_packed TINYINT DEFAULT 0 COMMENT '是否了包',
  sum_public_weight DECIMAL(14,3) COMMENT '公重总和', sum_gross_weight DECIMAL(14,3) COMMENT '毛重总和',
  sum_net_weight DECIMAL(14,3) COMMENT '净重总和', sum_tube INT COMMENT '筒子数总和',
  box_count INT COMMENT '包装箱数',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pex (execute_no)
) COMMENT '包装执行';

CREATE TABLE pk_box (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pk_execute_id BIGINT NOT NULL,
  box_code VARCHAR(64) NOT NULL COMMENT '箱子编码:260928066002',
  box_status VARCHAR(32) DEFAULT '新建' COMMENT '箱子状态',
  is_stock_notice TINYINT DEFAULT 0 COMMENT '是否生成库存通知',
  seq_no INT COMMENT '正序/箱序号',
  grain_count INT COMMENT '颗数:26',
  public_weight DECIMAL(12,3) COMMENT '公重:6.4',
  net_weight DECIMAL(12,3) COMMENT '净重:6.28',
  gross_weight DECIMAL(12,3) COMMENT '毛重:8.79',
  tare_weight DECIMAL(12,3) COMMENT '去皮重量:7.48',
  pack_spec VARCHAR(64), tie_line_color VARCHAR(32) COMMENT '扎绞/线颜色',
  pallet_code VARCHAR(64) COMMENT '托盘码',
  line_type VARCHAR(16) COMMENT '自动线/手动线',
  machine_code VARCHAR(64) COMMENT '一体机编码',
  pack_user VARCHAR(32) COMMENT '上机/装箱人员',
  box_time DATETIME COMMENT '装箱时间',
  actual_tube INT COMMENT '实际筒数',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_box (box_code), KEY idx_ex (pk_execute_id)
) COMMENT '包装箱子明细(逐箱称重)';

CREATE TABLE pk_pallet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pallet_code VARCHAR(64) NOT NULL COMMENT '托号',
  box_code VARCHAR(64) NOT NULL COMMENT '箱号',
  pack_workshop_id BIGINT, batch_no VARCHAR(64), dye_vat_no VARCHAR(64),
  standard_weight DECIMAL(12,3), gross_weight DECIMAL(12,3), net_weight DECIMAL(12,3),
  standard_tube INT COMMENT '标准筒数',
  pallet_user VARCHAR(32) COMMENT '组托人', pallet_time DATETIME COMMENT '组托时间',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_pallet (pallet_code)
) COMMENT '组托记录';

CREATE TABLE pk_label_reprint (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doc_no VARCHAR(64) NOT NULL COMMENT '单据编号:FG20260929082',
  pk_task_id BIGINT,
  batch_no VARCHAR(64), k3_order_no VARCHAR(64), dye_vat_no VARCHAR(64),
  warehouse_batch_no VARCHAR(64),
  product_name VARCHAR(128), name_en VARCHAR(255), product_spec VARCHAR(128),
  customer_name VARCHAR(128), doc_date DATE,
  biz_type VARCHAR(32) COMMENT '业务类型', raw_desc VARCHAR(255) COMMENT '原料说明',
  customer_batch_no VARCHAR(64), confirm_status VARCHAR(32) COMMENT '确认状态',
  box_code VARCHAR(64), box_seq INT COMMENT '箱子序号',
  public_weight DECIMAL(12,3), net_weight DECIMAL(12,3),
  gross_weight DECIMAL(12,3), tare_weight DECIMAL(12,3),
  pack_spec VARCHAR(64), pallet_code VARCHAR(64),
  print_time DATETIME,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '箱贴补打';

CREATE TABLE pk_tube_print (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pack_workshop_id BIGINT, batch_no VARCHAR(64), warehouse_batch_no VARCHAR(64),
  dye_vat_no VARCHAR(64), product_spec VARCHAR(128),
  print_user VARCHAR(32) COMMENT '包装人员/工号',
  qty INT COMMENT '数量',
  print_date DATE,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '筒贴打印数量/筒贴详情';

CREATE TABLE pk_box_delete (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  box_code VARCHAR(64) NOT NULL, pallet_code VARCHAR(64),
  seq_no INT, tare_weight DECIMAL(12,3), gross_weight DECIMAL(12,3),
  tube_count INT, paper_tube_weight DECIMAL(12,3),
  delete_user VARCHAR(32) COMMENT '删除人', delete_time DATETIME COMMENT '删除时间'
) COMMENT '箱子删除记录(审计)';

CREATE TABLE pk_wms_in (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_batch_no VARCHAR(64) COMMENT '入库批次',
  batch_no VARCHAR(64), dye_vat_no VARCHAR(64),
  box_code VARCHAR(64), pallet_code VARCHAR(64),
  public_weight DECIMAL(12,3), gross_weight DECIMAL(12,3),
  tare_weight DECIMAL(12,3), net_weight DECIMAL(12,3),
  in_time DATETIME, wms_status VARCHAR(32) COMMENT '同步状态',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT 'WMS入库记录';

CREATE TABLE pk_erp_in (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  warehouse_batch_no VARCHAR(64), batch_no VARCHAR(64),
  k3_order_no VARCHAR(64), in_qty DECIMAL(12,3),
  in_time DATETIME, erp_status VARCHAR(32),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT 'ERP入库记录';

-- ============================================================
-- 十、物料域
-- ============================================================
CREATE TABLE mt_pick (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) COMMENT '订单号', batch_no VARCHAR(64) COMMENT '批号',
  pick_no VARCHAR(64) NOT NULL COMMENT '领退料单编码:领料JDSC1L.../退料JDSC1T...',
  pick_type TINYINT NOT NULL COMMENT '1领料单2退料单',
  workshop_id BIGINT COMMENT '车间', task_no VARCHAR(64) COMMENT '生产任务号',
  pick_time DATETIME COMMENT '领退料时间', picker VARCHAR(32) COMMENT '领退料人(确认后回填)',
  warehouse VARCHAR(64) COMMENT '仓库', location VARCHAR(64) COMMENT '库位',
  warehouse_keeper VARCHAR(32) COMMENT '仓管',
  product_no VARCHAR(64) COMMENT '产品编码', product_name VARCHAR(128) COMMENT '产品名称',
  material_no VARCHAR(64) NOT NULL, material_name VARCHAR(128), material_spec VARCHAR(128),
  aux_attr VARCHAR(255) COMMENT '辅助属性', product_en VARCHAR(128) COMMENT '产品英文名', color_no VARCHAR(64) COMMENT '色号',
  unit VARCHAR(16), qty DECIMAL(12,3),
  pick_date DATE, doc_status VARCHAR(32) COMMENT '待确认/已确认',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pick (pick_no), KEY idx_order (order_no, batch_no)
) COMMENT '领退料确认';

CREATE TABLE mt_feed (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feed_no VARCHAR(64) NOT NULL COMMENT '投料单号:MF...',
  task_no VARCHAR(64) NOT NULL COMMENT '生产任务号:PI...',
  workshop_id BIGINT COMMENT '生产车间', craft_workshop_id BIGINT COMMENT '工艺单车间',
  team_id BIGINT COMMENT '班组', shift_id BIGINT COMMENT '班次',
  batch_no VARCHAR(64), raw_detail VARCHAR(255) COMMENT '原料明细',
  feed_qty DECIMAL(12,3) COMMENT '投料量',
  is_finish TINYINT DEFAULT 0 COMMENT '投料完毕',
  feed_by VARCHAR(32) COMMENT '投料人', feed_time DATETIME COMMENT '投料时间',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_feed (feed_no)
) COMMENT '投料记录';

CREATE TABLE mt_feed_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feed_id BIGINT NOT NULL,
  raw_batch_no VARCHAR(64) COMMENT '原料批号', material_no VARCHAR(64) COMMENT '物料号',
  material_name VARCHAR(128) COMMENT '物料名称', material_spec VARCHAR(64) COMMENT '物料规格',
  pick_qty DECIMAL(12,3) COMMENT '领料数量',
  this_feed_qty DECIMAL(12,3) COMMENT '本次投料量',
  fed_qty DECIMAL(12,3) COMMENT '已投料量'
) COMMENT '投料记录明细';

CREATE TABLE mt_receive (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_no VARCHAR(64) COMMENT '整经通知单',
  ball_count INT COMMENT '毛球数',
  moisture_standard DECIMAL(7,3) COMMENT '标准回潮', moisture_actual DECIMAL(7,3) COMMENT '实际回潮',
  gross_weight DECIMAL(12,3), tare_weight DECIMAL(12,3), net_weight DECIMAL(12,3) COMMENT '净重',
  sync_status TINYINT DEFAULT 0 COMMENT '同步状态(同步/取消同步)',
  receive_date DATE,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '原料接收记录';

CREATE TABLE mt_return_wool (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(64), workshop_id BIGINT, equipment_id BIGINT,
  weight DECIMAL(12,3) COMMENT '回毛重量',
  bag_color VARCHAR(32), register_date DATE,
  remark VARCHAR(255),
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '回毛登记';

-- ============================================================
-- 十一、现场统计域
-- ============================================================
CREATE TABLE st_attendance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  period_month VARCHAR(7) NOT NULL COMMENT '期间(月)',
  workshop_id BIGINT NOT NULL,
  work_days DECIMAL(5,1) COMMENT '开工天数',
  manager_count INT COMMENT '车间管理人员',
  day_aux_count INT COMMENT '常日班辅助',
  frontline_count INT COMMENT '一线员工',
  qc_count INT COMMENT '质检组出勤',
  total_count INT COMMENT '总出勤',
  convert_count DECIMAL(10,2) COMMENT '折算出勤',
  doc_status VARCHAR(32) DEFAULT '编制中' COMMENT '编制中/已提交/审核通过/已撤回',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_att (period_month, workshop_id)
) COMMENT '车间出勤';

CREATE TABLE st_shift (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  workshop_id BIGINT NOT NULL, team_id BIGINT NOT NULL,
  shift_name VARCHAR(32) COMMENT '班次:常日班/甲乙丙班',
  on_duty_time VARCHAR(16) COMMENT '上班时间', off_duty_time VARCHAR(16) COMMENT '下班时间',
  start_date DATE, end_date DATE,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '车间排班';

CREATE TABLE st_shift_person (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shift_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL, user_name VARCHAR(32), emp_no VARCHAR(32),
  equipment_id BIGINT COMMENT '机台'
) COMMENT '排班人员';

CREATE TABLE st_energy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  period_month VARCHAR(7) NOT NULL, workshop_id BIGINT NOT NULL,
  water_usage DECIMAL(12,3) COMMENT '用水(吨)', power_usage DECIMAL(12,3) COMMENT '用电(KWh)',
  gas_usage DECIMAL(12,3) COMMENT '用气(m³)',
  doc_status VARCHAR(32) DEFAULT '编制中',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_energy (period_month, workshop_id)
) COMMENT '车间能耗';

CREATE TABLE st_energy_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  energy_id BIGINT NOT NULL,
  equipment_id BIGINT NOT NULL COMMENT '机台',
  power_usage DECIMAL(12,3), water_usage DECIMAL(12,3), gas_usage DECIMAL(12,3)
) COMMENT '车间能耗-机台明细';

CREATE TABLE st_cost (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  period_month VARCHAR(7) NOT NULL, workshop_id BIGINT NOT NULL,
  water_usage DECIMAL(12,3) COMMENT '用水量', water_cost DECIMAL(12,2) COMMENT '用水费用',
  power_usage DECIMAL(12,3) COMMENT '用电量', power_cost DECIMAL(12,2) COMMENT '用电费用',
  machine_material_cost DECIMAL(12,2) COMMENT '机物料',
  pack_cost DECIMAL(12,2) COMMENT '包装费用',
  month_depreciation DECIMAL(12,2) COMMENT '月折旧',
  process_cost_share DECIMAL(12,2) COMMENT '加工费分摊',
  salary DECIMAL(12,2) COMMENT '工资',
  month_kpi DECIMAL(12,2) COMMENT '月度考核指标',
  convert_output DECIMAL(12,3) COMMENT '折算产量',
  wool_rate DECIMAL(7,3) COMMENT '用毛率',
  doc_status VARCHAR(32) DEFAULT '编制中' COMMENT '编制中/已提交/审核通过/驳回',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cost (period_month, workshop_id)
) COMMENT '车间生产费用';

CREATE TABLE st_issue (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  check_user VARCHAR(32) COMMENT '检查人', check_date DATE,
  issue_location VARCHAR(128) COMMENT '问题所在',
  issue_desc VARCHAR(512) COMMENT '问题描述',
  rectify_status VARCHAR(32) COMMENT '整改状态',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(32), update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '车间现场问题反馈';

-- ============================================================
-- 十二、同步日志域
-- ============================================================
CREATE TABLE log_sync (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sync_type VARCHAR(32) COMMENT '同步类型:K3订单/领退料/WMS入库/原料接收/纱线物理指标',
  op_name VARCHAR(64) COMMENT '操作名称',
  biz_no VARCHAR(64) COMMENT '业务单号:箱号/托盘号/订单号',
  sync_result TINYINT COMMENT '1成功0失败',
  cost_seconds DECIMAL(10,2) COMMENT '耗时(秒)',
  req_param TEXT COMMENT '请求参数', resp_param TEXT COMMENT '返回参数',
  public_weight DECIMAL(12,3), gross_weight DECIMAL(12,3),
  tare_weight DECIMAL(12,3), net_weight DECIMAL(12,3),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_type (sync_type), KEY idx_time (create_time)
) COMMENT '接口同步日志(K3/WMS/原料接收)';

-- ============================================================
-- 初始化字典(节选)
-- ============================================================
INSERT INTO sys_dict (dict_type, dict_name) VALUES
('stop_reason','停机原因'),('shift','班次'),('bag_color','袋子颜色'),
('tube_color','筒管颜色'),('defect_type','疵纱分类'),('report_type','报工类型'),
('weight_mode','计重方式'),('dye_type','染色类别'),('doc_status','单据状态');

INSERT INTO sys_dict_item (dict_type, item_label, item_value) VALUES
('stop_reason','撞车','撞车'),('stop_reason','落料','落料'),('stop_reason','停机','停机'),('stop_reason','其他','其他'),
('defect_type','正纱','正纱'),('defect_type','AA BB纱','AA_BB'),('defect_type','尾纱','尾纱'),
('defect_type','辫子纱','辫子纱'),('defect_type','松捻纱','松捻纱'),('defect_type','紧捻纱','紧捻纱'),
('defect_type','重纱','重纱'),('defect_type','轻纱','轻纱'),('defect_type','断纱','断纱'),
('defect_type','多股纱','多股纱'),('defect_type','油纱','油纱'),('defect_type','大小肚','大小肚'),
('defect_type','乱纱','乱纱'),('defect_type','圈圈纱','圈圈纱'),('defect_type','其它','其它'),
('weight_mode','标重交货','标重交货'),('weight_mode','实磅交货','实磅交货'),
('dye_type','条染','条染'),('dye_type','绞纱','绞纱'),('dye_type','胚纱','胚纱');

-- ============================================================
-- 参考系统表结构补全（来源: db/MES数据结构设计文档2024620094530.docx, 2024-06-20版157表,
--   见 docs/analysis/11-参考系统表结构.md; 命名风格统一为当前schema小写下划线）
-- ============================================================

-- ── 批号标识管理（MES_WS_TAG / MES_WS_TAG_OCCUPY）──
CREATE TABLE ws_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL COMMENT '标识编码', name VARCHAR(64) NOT NULL COMMENT '标识名称',
  alias VARCHAR(64) COMMENT '别标识名', color_grade VARCHAR(32) COMMENT '色系等级',
  shape VARCHAR(32) COMMENT '图形(倒三角/圆/方/叶子等)', color VARCHAR(16) COMMENT '颜色',
  type TINYINT COMMENT '1生产标识2工序标识',
  status TINYINT DEFAULT 1 COMMENT '是否启用',
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '车间标识库(批号标识主档)';

CREATE TABLE ws_tag_occupy (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag_id BIGINT NOT NULL, batch_no VARCHAR(64) NOT NULL COMMENT '占用批次',
  workshop_id BIGINT, occupy_time DATETIME, release_time DATETIME COMMENT '释放时间(了批后释放)',
  KEY idx_tag (tag_id), KEY idx_batch (batch_no)
) COMMENT '车间标识占用记录(防重复分配)';

-- ── 车间报产公式（MES_WS_REPORT / _PROC_REL）──
CREATE TABLE ws_report_formula (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL COMMENT '公式名称', workshop_id BIGINT,
  formula TEXT COMMENT '折算产量计算公式', status TINYINT DEFAULT 1
) COMMENT '车间报产公式(折算产量引擎)';

CREATE TABLE ws_report_formula_rel (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  formula_id BIGINT NOT NULL, process_id BIGINT COMMENT '关联车间工序',
  KEY idx_f (formula_id)
) COMMENT '车间报产公式.车间工序关联';

-- ── 批次主档（MES_BATCH_PRO / MES_BATCH_PACK）──
CREATE TABLE batch_pro (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(64) NOT NULL COMMENT '生产批号', trace_batch_no VARCHAR(64) COMMENT '追溯批次',
  order_no VARCHAR(64), task_no VARCHAR(64), workshop_id BIGINT, tag_id BIGINT COMMENT '批号标识',
  product_no VARCHAR(64), raw_detail VARCHAR(255), color_no VARCHAR(64),
  status VARCHAR(32) COMMENT '进行中/了批/关闭',
  start_time DATETIME, end_time DATETIME,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_batch (batch_no)
) COMMENT '生产批次主档(全生命周期)';

CREATE TABLE batch_pack (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pack_batch_no VARCHAR(64) NOT NULL COMMENT '包装批次号', batch_no VARCHAR(64) COMMENT '生产批号',
  pack_task_id BIGINT, box_count INT COMMENT '箱数', total_net DECIMAL(12,3) COMMENT '总净重',
  status VARCHAR(32),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pb (pack_batch_no)
) COMMENT '包装批次主档';

-- ── 粗纱库存（MES_PRO_ROVE_IN_STORE / OUT_STORE）──
CREATE TABLE rove_in_store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  batch_no VARCHAR(64) NOT NULL, workshop_id BIGINT, machine VARCHAR(64) COMMENT '机台',
  rove_qty DECIMAL(12,3) COMMENT '粗纱数量', unit VARCHAR(16),
  in_time DATETIME COMMENT '入库时间', operator VARCHAR(32),
  KEY idx_batch (batch_no)
) COMMENT '粗纱入库';

CREATE TABLE rove_out_store (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  in_id BIGINT COMMENT '关联入库', batch_no VARCHAR(64) NOT NULL,
  out_qty DECIMAL(12,3) COMMENT '出库数量(供细纱)', out_time DATETIME, operator VARCHAR(32),
  KEY idx_batch (batch_no)
) COMMENT '粗纱出库';

-- ── 质检任务与取样（MES_QA_QC_TASK / MES_QC_SAMPLE）──
CREATE TABLE qc_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL COMMENT '质检任务号', qc_plan_id BIGINT COMMENT '质检方案',
  batch_no VARCHAR(64), process_id BIGINT, workshop_id BIGINT,
  sample_status VARCHAR(32) COMMENT '待取样/已取样/检验中/已完成',
  plan_date DATE, finish_time DATETIME,
  create_by VARCHAR(32), create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_task (task_no)
) COMMENT '质检任务';

CREATE TABLE qc_task_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL, qc_index_id BIGINT NOT NULL COMMENT '质检指标',
  KEY idx_task (task_id)
) COMMENT '质检任务.质检项';

-- 质检样品登记（与质检域 qc_sample 抽样登记语义不同，原表 MES_QC_SAMPLE，改名避免冲突）
CREATE TABLE qc_specimen (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT COMMENT '质检任务', batch_no VARCHAR(64) NOT NULL,
  z_g_count VARCHAR(32) COMMENT '纱支', sample_time DATETIME COMMENT '取样时间',
  sampler VARCHAR(32) COMMENT '取样人', status VARCHAR(32) COMMENT '待检/已检',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_batch (batch_no)
) COMMENT '质检样品登记表';

-- ── 产存通知（MES_PACK_PD_NOTICE / _DETAIL）──
CREATE TABLE pack_pd_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_no VARCHAR(64) NOT NULL COMMENT '产存通知单号', batch_no VARCHAR(64),
  workshop_id BIGINT, type TINYINT COMMENT '1入库2出库', status VARCHAR(32),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_notice (notice_no)
) COMMENT '产存通知';

CREATE TABLE pack_pd_notice_detail (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notice_id BIGINT NOT NULL, box_no VARCHAR(64) COMMENT '箱号',
  qty DECIMAL(12,3), unit VARCHAR(16),
  KEY idx_notice (notice_id)
) COMMENT '产存通知明细';

-- ── 倒筒系数（MES_PACK_DM_COEFFICIENT）──
CREATE TABLE pack_dm_coefficient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  spec VARCHAR(64) COMMENT '规格/支数', coefficient DECIMAL(10,4) COMMENT '换算系数',
  remark VARCHAR(255), status TINYINT DEFAULT 1
) COMMENT '倒筒系数表';

-- ── 设备补全（MES_DEV_ERROR / MES_DEV_ACCESS_INV / MES_DEV_MP_TIP）──
CREATE TABLE eq_error (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  machine_id BIGINT NOT NULL, error_time DATETIME, error_desc VARCHAR(255),
  handler VARCHAR(32), handle_result VARCHAR(255), status VARCHAR(32) COMMENT '待处理/已处理',
  KEY idx_machine (machine_id)
) COMMENT '设备异常记录';

CREATE TABLE eq_accessory_inv (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  accessory_id BIGINT NOT NULL COMMENT '配件型号', warehouse VARCHAR(64),
  in_qty DECIMAL(12,3), out_qty DECIMAL(12,3), stock_qty DECIMAL(12,3) COMMENT '库存量',
  change_time DATETIME, ref_id BIGINT COMMENT '关联单据(维修领用等)',
  KEY idx_acc (accessory_id)
) COMMENT '配件库存台账';

CREATE TABLE eq_mp_tip (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(128) NOT NULL, fault_type_id BIGINT COMMENT '故障分类',
  content TEXT COMMENT '处理经验', author VARCHAR(32),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '维保知识库';

-- ── 现场补全（MES_WS_AP_REC / MES_WS_PER_SKILL / MES_BASIC_HOLIDAY）──
CREATE TABLE ws_ap_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  person_id BIGINT NOT NULL, record_date DATE,
  type TINYINT COMMENT '1奖励2惩罚', amount DECIMAL(10,2) COMMENT '金额/分值',
  reason VARCHAR(255), approver VARCHAR(32),
  KEY idx_person (person_id, record_date)
) COMMENT '奖惩记录';

CREATE TABLE ws_person_skill (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  person_id BIGINT NOT NULL, machine_id BIGINT NOT NULL COMMENT '掌握机台',
  skill_level TINYINT COMMENT '技能等级',
  KEY idx_person (person_id)
) COMMENT '人员技能(排班按技能匹配机台)';

CREATE TABLE base_holiday (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  old_date DATE COMMENT '原日期', old_week VARCHAR(16) COMMENT '原周几',
  change_date DATE COMMENT '改日期', change_week VARCHAR(16),
  is_default TINYINT, remark VARCHAR(255)
) COMMENT '节假日详情(排班日历)';

-- ── 用料库与打印留痕（MES_PRO_MAT_USE_LIB / MES_PRO_FLOW_CARD_PRINT）──
CREATE TABLE base_mat_use_lib (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_no VARCHAR(64) COMMENT '产品编号', material_no VARCHAR(64) COMMENT '物料编号',
  material_name VARCHAR(128), ratio DECIMAL(8,4) COMMENT '配比', unit VARCHAR(16),
  status TINYINT DEFAULT 1
) COMMENT '用料库(标准用料模板)';

CREATE TABLE flow_card_print_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source_id BIGINT NOT NULL, source_table VARCHAR(64) COMMENT '来源(制造卡/工艺单)',
  print_time DATETIME, printer VARCHAR(32), print_count INT DEFAULT 1,
  KEY idx_src (source_table, source_id)
) COMMENT '工艺流转卡打印记录(补打留痕)';
