import { get, post, del } from './request'
import type { BaseEquipment, BaseWorkshop, ProdSchedule, ProdTask } from './plan'
import type { RepBatchOn, RepOutput, RepStartStop, RepTruck, RepTruckMove, RepYarnSteam } from './site'
import type { FrontReport } from './craft'
import type { PkBox, PkExecute, PkTask } from './pack'
import type { MtPick } from './prod'
import type { QcPackageReport } from './qc'

// ============ PDA 手持终端（/api/pda/*，BFF 薄委托 PC application 层） ============

export interface PdaTeam {
  id: number
  workshopId?: number
  teamName?: string
  status?: number
}

export interface ProdMachineCard {
  id: number
  cardNo: string
  scheduleId?: number
  batchNo?: string
  machineModel?: string
  status?: string
  productSpec?: string
  rawMaterial?: string
  qrCode?: string
}

export interface ProdScheduleCraftItem {
  id: number
  paramName?: string
  paramValue?: string
  upperLimit?: string
  lowerLimit?: string
  sortNo?: number
}

/** 扫码结果：排程+制造卡+任务+工艺项+活动上批 */
export interface PdaScanVO {
  schedule: ProdSchedule
  card?: ProdMachineCard | null
  task?: ProdTask | null
  crafts?: ProdScheduleCraftItem[]
  activeBatchOn?: RepBatchOn | null
  processName?: string | null
  machineNo?: string | null
}

/** 上批提交（source 由后端置 PDA） */
export function pdaScan(content: string) {
  return post<PdaScanVO>('/api/pda/scan', { content })
}

export function pdaBatchOn(body: { batchOn: Partial<RepBatchOn>; flags?: unknown[]; crafts?: unknown[] }) {
  return post<number>('/api/pda/batch-on', body)
}

export function pdaBatchOff(id: number) {
  return post<void>(`/api/pda/batch-off/${id}`)
}

export function pdaOutput(body: Partial<RepOutput>) {
  return post<number>('/api/pda/output', body)
}

/** 任务用料清单（复用 PC 端点，权限与 /api/pda/feed 一致） */
export interface ProdTaskMaterial {
  id: number
  taskId?: number
  rawBatchNo?: string
  rawName?: string
  planFeedQty?: number
  workshopQty?: number
  fedQty?: number
  unit?: string
}

export function pdaFeed(body: {
  feed: Partial<{ taskNo: string; teamId: number; isFinish: number; feedQty: number }>
  items: Partial<{ rawBatchNo: string; thisFeedQty: number }>[]
}) {
  return post<number>('/api/pda/feed', body)
}

export function pdaTasks() {
  return get<ProdTask[]>('/api/pda/tasks')
}

export function taskMaterials(taskNo: string) {
  return get<ProdTaskMaterial[]>('/api/site/feed-record/materials', { taskNo })
}

// ---------- 启停/前纺汇报（M1） ----------

export function pdaStop(body: Partial<RepStartStop>) {
  return post<number>('/api/pda/stop', body)
}

export function pdaFrontReport(body: Partial<FrontReport>) {
  return post<number>('/api/pda/front-report', body)
}

// ---------- PDA 常用下拉（登录即可） ----------

export function pdaWorkshops() {
  return get<BaseWorkshop[]>('/api/pda/workshops')
}

export function pdaTeams() {
  return get<PdaTeam[]>('/api/pda/teams')
}

export function pdaMachines(workshopId?: number) {
  return get<BaseEquipment[]>('/api/pda/machines', { workshopId })
}

// ---------- 包装线（M2：/api/pda/pack/*，BFF 薄委托 pack application 层） ----------

/** 执行单详情（主表 + 箱子明细） */
export interface PkExecuteDetail {
  execute: PkExecute
  boxes: PkBox[]
}

/** 组托扫箱结果：箱子 + 所属执行单 */
export interface PdaBoxScanVO {
  box: PkBox
  execute: PkExecute
}

/** 包装执行单查询（单号/包装批号/入库批号模糊，未了包在前） */
export function pdaPackExecutes(keyword?: string) {
  return get<PkExecute[]>('/api/pda/pack/executes', { keyword })
}

export function pdaPackExecuteDetail(id: number) {
  return get<PkExecuteDetail>(`/api/pda/pack/executes/${id}`)
}

/** 逐箱称重（前端算齐净/皮/毛三项提交，恒等式与公重公式由后端校验） */
export function pdaAddBox(body: {
  executeId: number
  grainCount?: number
  netWeight: number
  grossWeight: number
  tareWeight: number
  packSpec?: string
  tieLineColor?: string
  lineType?: string
  machineCode?: string
  packUser?: string
  actualTube?: number
}) {
  return post<PkBox>('/api/pda/pack/boxes', body)
}

export function pdaDeleteBox(boxId: number) {
  return del<void>(`/api/pda/pack/boxes/${boxId}`)
}

export function pdaFinishExecute(id: number) {
  return post<void>(`/api/pda/pack/executes/${id}/finish`)
}

/** 组托扫箱（已组托的箱子会被拦截） */
export function pdaScanBox(boxCode: string) {
  return post<PdaBoxScanVO>('/api/pda/pack/scan-box', { boxCode })
}

/** 箱子信息查询（箱贴补打用，已组托也可查） */
export function pdaBoxInfo(boxCode: string) {
  return get<PdaBoxScanVO>('/api/pda/pack/box-info', { boxCode })
}

/** 组托（executeId 由后端按第一个箱码自动解析） */
export function pdaPalletize(body: { boxCodes: string[]; standardTube?: number }) {
  return post<string>('/api/pda/pack/palletize', body)
}

/** 包装任务查询（筒贴打印选任务用） */
export function pdaPackTasks(keyword?: string) {
  return get<PkTask[]>('/api/pda/pack/tasks', { keyword })
}

/** 筒贴打印留痕 */
export function pdaPrintTube(taskId: number, qty?: number) {
  return post<void>(`/api/pda/pack/print-tube/${taskId}`, undefined, { qty })
}

/** 箱贴补打（扫箱码，信息由后端自动带出） */
export function pdaLabelReprint(boxCode: string) {
  return post<number>('/api/pda/pack/label-reprint', { boxCode })
}

// ---------- 质检与物流（M3：/api/pda/qc|site/*，BFF 薄委托同源校验链） ----------

/** 包装质检报告保存入参（15 类疵纱分类，合格率服务端计算） */
export interface QcPackSaveBody {
  id?: number
  warehouseBatchNo: string
  batchNo?: string
  workshopId?: number
  qcWorkshopId?: number
  docDate?: string
  firstGradeQty?: number
  remark?: string
  defects?: { defectType: string; value?: number; remark?: string }[]
}

/** 报告详情（主表 + 疵纱明细） */
export interface QcPackDetail {
  report: QcPackageReport
  defects: { defectType?: string; value?: number; remark?: string }[]
}

/** 周转车详情（主表 + 移动轨迹） */
export interface TruckDetail {
  truck: RepTruck
  moves: RepTruckMove[]
}

/** 包装质检报告查询（入库批次/生产批次模糊） */
export function pdaQcReports(keyword?: string) {
  return get<QcPackageReport[]>('/api/pda/qc/package-reports', { keyword })
}

export function pdaQcReportDetail(id: number) {
  return get<QcPackDetail>(`/api/pda/qc/package-reports/${id}`)
}

export function pdaSaveQcReport(body: QcPackSaveBody) {
  return post<number>('/api/pda/qc/package-reports', body)
}

/** 领退料单查询（单号/订单号/批号模糊；docStatus 可选，待确认在前） */
export function pdaPicks(keyword?: string, docStatus?: string) {
  return get<MtPick[]>('/api/pda/site/picks', { keyword, docStatus })
}

/** 领退料批量确认（待确认→已确认，确认人/时间回填） */
export function pdaConfirmPicks(ids: number[]) {
  return post<void>('/api/pda/site/picks/confirm', ids)
}

/** 周转车查询（车号/批号模糊） */
export function pdaTrucks(keyword?: string) {
  return get<RepTruck[]>('/api/pda/site/trucks', { keyword })
}

export function pdaTruckDetail(id: number) {
  return get<TruckDetail>(`/api/pda/site/trucks/${id}`)
}

/** 新增周转车（空车登记） */
export function pdaSaveTruck(truck: Partial<RepTruck>) {
  return post<number>('/api/pda/site/trucks', { truck })
}

/** 周转车移动（绑定/称重/入库/移库/出库；scanWorkshop 后端按车辆车间补齐） */
export function pdaMoveTruck(body: Partial<{
  truckId: number
  opName: string
  toLocation: string
  recombedBatchNo: string
  spinBatchNo: string
  bagColor: string
  ballCount: number
  grossWeight: number
  netWeight: number
  standardWeight: number
  moistureStandard: number
  moistureActual: number
  moveNetWeight: number
  moveBallCount: number
}>) {
  return post<void>('/api/pda/site/trucks/move', body)
}

/** 蒸纱信息查询（批号/车号模糊；未打印在前、未接收在前） */
export function pdaSteams(keyword?: string) {
  return get<RepYarnSteam[]>('/api/pda/site/steams', { keyword })
}

/** 新增蒸纱信息 */
export function pdaSaveSteam(body: Partial<RepYarnSteam>) {
  return post<number>('/api/pda/site/steams', body)
}

/** 蒸纱打印（回填打印人/时间） */
export function pdaPrintSteam(id: number) {
  return post<void>(`/api/pda/site/steams/${id}/print`)
}

/** 蒸纱接收（未打印不可接收） */
export function pdaReceiveSteam(id: number) {
  return post<void>(`/api/pda/site/steams/${id}/receive`)
}

// ---------- 设备点检维护（M4：/api/pda/equip/*，与 PC 共表同源台账） ----------

/** 扫机台结果：机台 + 车间名 + 点检责任人（点检/报修页共用） */
export interface PdaMachineVO {
  equipment: BaseEquipment
  workshopName?: string | null
  dutyUsers?: string | null
}

/** 点检项目（设备基础字典） */
export interface PdaCheckItem {
  id: number
  itemCode?: string
  itemName?: string
  checkType?: string
  itemDesc?: string
  status?: number
}

/** 故障分类（设备基础字典） */
export interface PdaFaultType {
  id: number
  faultCode?: string
  faultName?: string
  faultDesc?: string
  status?: number
}

/** 点检记录提交体（recordCode 后端取号 DJ；checkUser/checkDate 缺省后端补齐） */
export interface PdaCheckSaveBody {
  workshopId?: number
  describe: string
  remark?: string
}

/** 维保记录（报修单） */
export interface PdaRepairRecord {
  id: number
  repairCode?: string
  workshopId?: number
  equipmentId?: number
  faultTime?: string
  faultTypeId?: number
  repairUser?: string
  solveTime?: string
  repairMethodId?: number
  detail?: string
  remark?: string
  createTime?: string
}

/** 扫机台（机台号精确匹配，带出车间名与点检责任人） */
export function pdaScanMachine(keyword: string) {
  return get<PdaMachineVO>('/api/pda/equip/scan-machine', { keyword })
}

/** 点检项目字典（启用；登录即可） */
export function pdaCheckItems() {
  return get<PdaCheckItem[]>('/api/pda/equip/check-items')
}

/** 故障分类字典（启用；登录即可） */
export function pdaFaultTypes() {
  return get<PdaFaultType[]>('/api/pda/equip/fault-types')
}

/** 提交点检记录（编码自动取号，点检人/日期缺省补齐） */
export function pdaSaveCheck(body: PdaCheckSaveBody) {
  return post<number>('/api/pda/equip/checks', body)
}

/** 报修单查询（编码模糊/机台过滤；unsolved=true 只看未解决，未解决在前） */
export function pdaRepairs(params?: { keyword?: string; equipmentId?: number; unsolved?: boolean }) {
  return get<PdaRepairRecord[]>('/api/pda/equip/repairs', params)
}

/** 报修登记（编码自动取号 WX；faultTime 缺省现在、车间按机台带出） */
export function pdaSaveRepair(body: Partial<PdaRepairRecord>) {
  return post<number>('/api/pda/equip/repairs', body)
}

/** 维修完成（回填解决时间与维修人；已修复拦截） */
export function pdaSolveRepair(id: number) {
  return post<void>(`/api/pda/equip/repairs/${id}/solve`)
}
