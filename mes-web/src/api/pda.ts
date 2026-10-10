import { get, post } from './request'
import type { BaseEquipment, BaseWorkshop, ProdSchedule, ProdTask } from './plan'
import type { RepBatchOn, RepOutput, RepStartStop } from './site'
import type { FrontReport } from './craft'

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
