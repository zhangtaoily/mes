import { get, post, put, del } from './request'
import type { PageResult } from './request'
import type { PageParams } from './plan'

// ============ 工艺管理（/api/craft/*） ============

// ---------- 技术工艺库 ----------

export interface TechCraft {
  id: number
  craftCode: string
  craftName: string
  component?: string
  actualCount?: number
  craftLevel?: string
  dyeMode?: string
  twist?: number
  enabled?: number
  remark?: string
  updateTime?: string
}

export function pageTechCrafts(params: PageParams & { craftCode?: string; craftName?: string; enabled?: number }) {
  return get<PageResult<TechCraft>>('/api/craft/tech-crafts', params)
}
export function saveTechCraft(body: Partial<TechCraft>) { return post<number>('/api/craft/tech-crafts', body) }
export function updateTechCraft(id: number, body: Partial<TechCraft>) { return put<void>(`/api/craft/tech-crafts/${id}`, body) }
export function deleteTechCraft(id: number) { return del<void>(`/api/craft/tech-crafts/${id}`) }

// ---------- 车间工艺库 ----------

export interface WsCraft {
  id: number
  craftName: string
  workshopId: number
  prodLine?: string
  rawMaterial?: string
  actualCount?: number
  twist?: number
  dyeMode?: string
  craftLevel?: string
  processId?: number
  equipmentId?: number
  hourCapacity?: number
  enabled?: number
  updateTime?: string
}

export function pageWsCrafts(params: PageParams & { craftName?: string; workshopId?: number; enabled?: number }) {
  return get<PageResult<WsCraft>>('/api/craft/ws-crafts', params)
}
export function saveWsCraft(body: Partial<WsCraft>) { return post<number>('/api/craft/ws-crafts', body) }
export function updateWsCraft(id: number, body: Partial<WsCraft>) { return put<void>(`/api/craft/ws-crafts/${id}`, body) }
export function deleteWsCraft(id: number) { return del<void>(`/api/craft/ws-crafts/${id}`) }

// ---------- 工艺参数（两库共用，技术库机型为空 / 车间库机型必填） ----------

export interface CraftParam {
  id: number
  craftId?: number
  processId?: number
  machineModel?: string
  paramName: string
  paramValue?: string
  lowerLimit?: string
  upperLimit?: string
  unit?: string
  sortNo?: number
}

function getTechParams(craftId: number) { return get<CraftParam[]>(`/api/craft/tech-crafts/${craftId}/params`) }
function addTechParam(craftId: number, body: Partial<CraftParam>) { return post<void>(`/api/craft/tech-crafts/${craftId}/params`, body) }
function deleteTechParam(craftId: number, paramId: number) { return del<void>(`/api/craft/tech-crafts/${craftId}/params/${paramId}`) }
function getWsParams(craftId: number) { return get<CraftParam[]>(`/api/craft/ws-crafts/${craftId}/params`) }
function addWsParam(craftId: number, body: Partial<CraftParam>) { return post<void>(`/api/craft/ws-crafts/${craftId}/params`, body) }
function deleteWsParam(craftId: number, paramId: number) { return del<void>(`/api/craft/ws-crafts/${craftId}/params/${paramId}`) }
export const techParamApi = { list: getTechParams, add: addTechParam, remove: deleteTechParam }
export const wsParamApi = { list: getWsParams, add: addWsParam, remove: deleteWsParam }

// ---------- 前纺生产情况汇报 ----------

/** 绕毛类字段三级枚举 */
export const WRAP_LEVELS = ['无', '轻微', '严重'].map((s) => ({ label: s, value: s }))

export interface FrontReport {
  id: number
  workshopId?: number
  batchNo?: string
  reportDate?: string
  temperature?: number
  humidity?: number
  rawOil?: number
  productOil?: number
  machineModel?: string
  wrapComber?: string
  wrapCircleComber?: string
  wrapBubble?: string
  wrapApron?: string
  wrapRoller?: string
  useTalcum?: string
  remark?: string
  createBy?: string
  updateTime?: string
}

export function pageFrontReports(params: PageParams & { workshopId?: number; batchNo?: string }) {
  return get<PageResult<FrontReport>>('/api/craft/front-report', params)
}
/** 保存（id 空=新建，有=更新） */
export function saveFrontReport(body: Partial<FrontReport>) { return post<number>('/api/craft/front-report', body) }
export function deleteFrontReport(id: number) { return del<void>(`/api/craft/front-report/${id}`) }
