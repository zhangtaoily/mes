import { get, post, del } from './request'
import type { PageResult } from './request'
import type { PageParams, ProdTask } from './plan'

// ============ 生产管理扩展（/api/prod/*） ============

// ---------- 领退料确认 ----------

export interface MtPick {
  id: number
  orderNo?: string
  batchNo?: string
  pickNo?: string
  pickType?: number
  workshopId?: number
  taskNo?: string
  pickTime?: string
  picker?: string
  warehouse?: string
  location?: string
  warehouseKeeper?: string
  productNo?: string
  productName?: string
  materialNo?: string
  materialName?: string
  materialSpec?: string
  auxAttr?: string
  productEn?: string
  colorNo?: string
  unit?: string
  qty?: number
  pickDate?: string
  docStatus: string
  createBy?: string
  updateTime?: string
}

export const PICK_TYPES = [{ label: '领料单', value: 1 }, { label: '退料单', value: 2 }]

export function pagePicks(params: PageParams & { orderNo?: string; batchNo?: string; workshopId?: number; pickType?: number; docStatus?: string }) {
  return get<PageResult<MtPick>>('/api/prod/pick', params)
}
/** 保存（id 空=补录新建） */
export function savePick(body: Partial<MtPick>) { return post<number>('/api/prod/pick', body) }
/** 批量确认（待确认→已确认） */
export function confirmPicks(ids: number[]) { return post<void>('/api/prod/pick/confirm', ids) }
/** 删除（仅待确认） */
export function deletePick(id: number) { return del<void>(`/api/prod/pick/${id}`) }

// ---------- 任务变更 ----------

export interface TaskChange {
  id: number
  changeNo: string
  taskId: number
  docStatus: string
  changeContent?: string
  newPlanFeedQty?: number
  newPlanQty?: number
  newDeliveryDate?: string
  createBy?: string
  createTime?: string
}

export function pageTaskChanges(params: PageParams & { changeNo?: string; taskId?: number; docStatus?: string }) {
  return get<PageResult<TaskChange>>('/api/prod/task-changes', params)
}
export function saveTaskChange(body: Partial<TaskChange>) { return post<number>('/api/prod/task-changes', body) }
export function submitTaskChange(id: number) { return post<void>(`/api/prod/task-changes/${id}/submit`) }
export function deleteTaskChange(id: number) { return del<void>(`/api/prod/task-changes/${id}`) }
/** 变更基准：原任务当前值 */
export function getTaskBaseline(taskId: number) { return get<ProdTask>(`/api/prod/task-changes/task/${taskId}/baseline`) }

// ---------- 任务其它信息（1:1 挂任务） ----------

export interface TaskExtra {
  id: number
  taskId: number
  batchNo?: string
  workshopName?: string
  temperature?: number
  humidity?: number
  rawOil?: number
  productOil?: number
  machineModel?: string
  wrapNeedle?: number
  wrapRing?: number
  wrapBalloon?: number
  wrapRoller?: number
  wrapRoller2?: number
  talcPowder?: number
  extraType?: string
  remark?: string
}

export function pageTaskExtras(params: PageParams & { taskId?: number; batchNo?: string }) {
  return get<PageResult<TaskExtra>>('/api/prod/task-extras', params)
}
export function saveTaskExtra(body: Partial<TaskExtra>) { return post<number>('/api/prod/task-extras', body) }
