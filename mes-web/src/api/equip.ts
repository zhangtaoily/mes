import { get, post, put, del } from './request'
import type { PageResult } from './request'
import type { PageParams } from './plan'

// ============ 设备管理（/api/equip/*） ============

// ---------- 保养计划 ----------
export interface EqMaintainPlan {
  id: number
  planCode: string
  workshopId?: number
  planName?: string
  cycleType?: number
  cycleValue?: number
  remark?: string
  updateTime?: string
}
export const CYCLE_TYPES = [{ label: '日', value: 1 }, { label: '周', value: 2 }, { label: '月', value: 3 }]

export function pageMaintainPlans(params: PageParams & { planCode?: string; workshopId?: number }) {
  return get<PageResult<EqMaintainPlan>>('/api/equip/maintain-plans', params)
}
export function saveMaintainPlan(body: Partial<EqMaintainPlan>) { return post<number>('/api/equip/maintain-plans', body) }
export function deleteMaintainPlan(id: number) { return del<void>(`/api/equip/maintain-plans/${id}`) }

// ---------- 保养记录 ----------
export interface EqMaintainRecord {
  id: number
  recordCode?: string
  maintainPlanId?: number
  workshopId?: number
  equipmentId?: number
  maintainType?: string
  maintainItem?: string
  expectDate?: string
  maintainUser?: string
  finishDate?: string
  isFinish?: number
  detail?: string
  remark?: string
  updateTime?: string
}

export function pageMaintainRecords(params: PageParams & { recordCode?: string; workshopId?: number; equipmentId?: number; isFinish?: number }) {
  return get<PageResult<EqMaintainRecord>>('/api/equip/maintain-records', params)
}
export function saveMaintainRecord(body: Partial<EqMaintainRecord>) { return post<number>('/api/equip/maintain-records', body) }
export function deleteMaintainRecord(id: number) { return del<void>(`/api/equip/maintain-records/${id}`) }

// ---------- 点检记录 ----------
export interface EqCheckRecord {
  id: number
  recordCode: string
  checkPlanId?: number
  workshopId?: number
  checkUser?: string
  checkDate?: string
  describe?: string
  remark?: string
  createTime?: string
}

export function pageCheckRecords(params: PageParams & { recordCode?: string; workshopId?: number; checkUser?: string }) {
  return get<PageResult<EqCheckRecord>>('/api/equip/check-records', params)
}
export function saveCheckRecord(body: Partial<EqCheckRecord>) { return post<number>('/api/equip/check-records', body) }
export function deleteCheckRecord(id: number) { return del<void>(`/api/equip/check-records/${id}`) }

// ---------- 维保记录 ----------
export interface EqRepairRecord {
  id: number
  repairCode: string
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

export function pageRepairRecords(params: PageParams & { repairCode?: string; workshopId?: number; equipmentId?: number }) {
  return get<PageResult<EqRepairRecord>>('/api/equip/repair-records', params)
}
export function saveRepairRecord(body: Partial<EqRepairRecord>) { return post<number>('/api/equip/repair-records', body) }
export function deleteRepairRecord(id: number) { return del<void>(`/api/equip/repair-records/${id}`) }

// ---------- 责任机台 ----------
export interface EqDutyMachine {
  id: number
  workshopId?: number
  checkUser: string
  empNo?: string
  equipmentId?: number
  remark?: string
  createTime?: string
}

export function pageDutyMachines(params: PageParams & { workshopId?: number; checkUser?: string }) {
  return get<PageResult<EqDutyMachine>>('/api/equip/duty-machines', params)
}
export function saveDutyMachine(body: Partial<EqDutyMachine>) { return post<number>('/api/equip/duty-machines', body) }
export function deleteDutyMachine(id: number) { return del<void>(`/api/equip/duty-machines/${id}`) }

// ---------- 基础字典（5 类统一端点） ----------
export type EquipDictType = 'fault-types' | 'maintain-items' | 'check-items' | 'maintain-methods' | 'part-models'
export interface EquipDictItem {
  id: number
  status?: number
  [key: string]: unknown
}

export function listEquipDicts(type: EquipDictType, params?: { keyword?: string; status?: number }) {
  return get<EquipDictItem[]>(`/api/equip/dicts/${type}`, params)
}
export function saveEquipDict(type: EquipDictType, body: Record<string, unknown>) { return post<number>(`/api/equip/dicts/${type}`, body) }
export function updateEquipDict(type: EquipDictType, id: number, body: Record<string, unknown>) { return put<void>(`/api/equip/dicts/${type}/${id}`, body) }
export function deleteEquipDict(type: EquipDictType, id: number) { return del<void>(`/api/equip/dicts/${type}/${id}`) }
