import { get, post, del } from './request'
import type { BaseEquipment, BaseWorkshop, ProdSchedule, ProdTask } from './plan'
import type { RepBatchOn, RepOutput, RepStartStop } from './site'
import type { FrontReport } from './craft'
import type { PkBox, PkExecute, PkTask } from './pack'

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
