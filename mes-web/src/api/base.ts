import { get, post, put, del } from './request'
import type { PageResult } from './request'
import type { PageParams } from './plan'

// ============ 基础信息管理（/api/base/*） ============

export interface BaseWorkshopFull {
  id: number
  workshopCode: string
  workshopName: string
  workshopType?: number
  k3WorkshopId?: string
  status?: number
}
export interface BaseTeam {
  id: number
  workshopId: number
  teamName: string
  leaderUserId?: number
  status?: number
}
export interface BaseEquipmentFull {
  id: number
  workshopId: number
  eqTypeId?: number
  machineModel?: string
  machineNo: string
  hourCapacity?: number
  status?: number
  createTime?: string
}
export interface BaseBatchFlagFull {
  id: number
  flagName: string
  alias?: string
  previewUrl?: string
  processId?: number
  status?: number
}
export interface BasePackMaterial {
  id: number
  materialCode?: string
  materialName: string
  spec?: string
  weight?: number
  status?: number
}
export interface BasePrintTemplate {
  id: number
  templateCode?: string
  templateName: string
  templateType?: number
  paperType?: string
  logoUrl?: string
  content?: string
  status?: number
}

// ---------- 车间 ----------
export function pageWorkshops(params: PageParams & { workshopName?: string; workshopType?: number; status?: number }) {
  return get<PageResult<BaseWorkshopFull>>('/api/base/workshops', params)
}
export function saveWorkshop(body: Partial<BaseWorkshopFull>) { return post<void>('/api/base/workshops', body) }
export function updateWorkshop(id: number, body: Partial<BaseWorkshopFull>) { return put<void>(`/api/base/workshops/${id}`, body) }
export function deleteWorkshop(id: number) { return del<void>(`/api/base/workshops/${id}`) }

// ---------- 班组 ----------
export function pageTeams(params: PageParams & { workshopId?: number; teamName?: string; status?: number }) {
  return get<PageResult<BaseTeam>>('/api/base/teams', params)
}
export function saveTeam(body: Partial<BaseTeam>) { return post<void>('/api/base/teams', body) }
export function updateTeam(id: number, body: Partial<BaseTeam>) { return put<void>(`/api/base/teams/${id}`, body) }
export function deleteTeam(id: number) { return del<void>(`/api/base/teams/${id}`) }

// ---------- 设备 ----------
export function pageMachines(params: PageParams & { workshopId?: number; machineNo?: string; machineModel?: string; status?: number }) {
  return get<PageResult<BaseEquipmentFull>>('/api/base/machines', params)
}
export function saveMachine(body: Partial<BaseEquipmentFull>) { return post<void>('/api/base/machines', body) }
export function updateMachine(id: number, body: Partial<BaseEquipmentFull>) { return put<void>(`/api/base/machines/${id}`, body) }
export function deleteMachine(id: number) { return del<void>(`/api/base/machines/${id}`) }

// ---------- 标识牌 ----------
export function pageBatchFlags(params: PageParams & { flagName?: string; processId?: number; status?: number }) {
  return get<PageResult<BaseBatchFlagFull>>('/api/base/batch-flags', params)
}
export function saveBatchFlag(body: Partial<BaseBatchFlagFull>) { return post<void>('/api/base/batch-flags', body) }
export function updateBatchFlag(id: number, body: Partial<BaseBatchFlagFull>) { return put<void>(`/api/base/batch-flags/${id}`, body) }
export function deleteBatchFlag(id: number) { return del<void>(`/api/base/batch-flags/${id}`) }

// ---------- 包装材质 ----------
export function pagePackMaterials(params: PageParams & { materialName?: string; status?: number }) {
  return get<PageResult<BasePackMaterial>>('/api/base/pack-materials', params)
}
export function savePackMaterial(body: Partial<BasePackMaterial>) { return post<void>('/api/base/pack-materials', body) }
export function updatePackMaterial(id: number, body: Partial<BasePackMaterial>) { return put<void>(`/api/base/pack-materials/${id}`, body) }
export function deletePackMaterial(id: number) { return del<void>(`/api/base/pack-materials/${id}`) }

// ---------- 打印模板 ----------
export const TEMPLATE_TYPES = ['筒贴', '箱单', '吊牌', '制造卡', '唛架', '流程单', '工艺单']
export function pagePrintTemplates(params: PageParams & { templateName?: string; templateType?: number; status?: number }) {
  return get<PageResult<BasePrintTemplate>>('/api/base/print-templates', params)
}
export function savePrintTemplate(body: Partial<BasePrintTemplate>) { return post<void>('/api/base/print-templates', body) }
export function updatePrintTemplate(id: number, body: Partial<BasePrintTemplate>) { return put<void>(`/api/base/print-templates/${id}`, body) }
export function deletePrintTemplate(id: number) { return del<void>(`/api/base/print-templates/${id}`) }

// ---------- 质检方案 ----------
export interface BaseQcScheme {
  id: number
  schemeCode: string
  schemeName: string
  workshopId?: number
  sampleCount?: number
  status?: number
  createBy?: string
  createTime?: string
}
export interface BaseQcSchemeItem {
  id: number
  schemeId: number
  indicatorId: number
  standardValue?: string
  lowerLimit?: string
  upperLimit?: string
  sortNo?: number
}
export interface BaseQcIndicator {
  id: number
  indicatorCode: string
  indicatorName: string
  unit?: string
  indicatorType?: number
  formulaId?: number
  status?: number
}

export function pageQcSchemes(params: PageParams & { schemeCode?: string; schemeName?: string; workshopId?: number; status?: number }) {
  return get<PageResult<BaseQcScheme>>('/api/base/qc-schemes', params)
}
export function saveQcScheme(body: Partial<BaseQcScheme>) { return post<void>('/api/base/qc-schemes', body) }
export function updateQcScheme(id: number, body: Partial<BaseQcScheme>) { return put<void>(`/api/base/qc-schemes/${id}`, body) }
export function changeQcSchemeStatus(id: number, status: number) { return put<void>(`/api/base/qc-schemes/${id}/status/${status}`) }
export function deleteQcScheme(id: number) { return del<void>(`/api/base/qc-schemes/${id}`) }
export function getQcSchemeItems(id: number) { return get<BaseQcSchemeItem[]>(`/api/base/qc-schemes/${id}/items`) }
export function saveQcSchemeItem(id: number, body: Partial<BaseQcSchemeItem>) { return post<void>(`/api/base/qc-schemes/${id}/items`, body) }
export function updateQcSchemeItem(id: number, itemId: number, body: Partial<BaseQcSchemeItem>) { return put<void>(`/api/base/qc-schemes/${id}/items/${itemId}`, body) }
export function deleteQcSchemeItem(id: number, itemId: number) { return del<void>(`/api/base/qc-schemes/${id}/items/${itemId}`) }
export function getQcIndicators() { return get<BaseQcIndicator[]>('/api/base/qc-indicators') }
