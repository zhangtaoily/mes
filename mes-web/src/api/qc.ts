import { get, post } from './request'
import type { PageResult } from './request'
import type { PageParams } from './plan'

// ============ 质检管理（qc） ============

export interface QcSample {
  id: number
  batchNo?: string
  processId?: number
  equipmentId?: number
  workshopId?: number
  feedQty?: number
  reportTime?: string
  isRegistered?: number
  registerTime?: string
  isBatchOff?: number
  batchOffTime?: string
  taskFinished?: number
}

export function pageSamples(params: PageParams & { batchNo?: string; workshopId?: number; isRegistered?: number }) {
  return get<PageResult<QcSample>>('/api/qc/samples', params)
}
/** 批量登记生成检验报告 */
export function registerSamples(ids: number[]) {
  return post<void>('/api/qc/samples/register', { ids })
}

export interface QcReport {
  id: number
  qcNo: string
  workshopId?: number
  qcType?: string
  equipmentId?: number
  teamId?: number
  batchNo?: string
  dyeVatNo?: string
  inspector?: string
  inspectDate?: string
  sampleQty?: number
  retainQty?: number
  rawMaterial?: string
  spinCount?: string
  mergeFeedQty?: number
  docStatus: string
  isPublic?: number
}

export function pageQcReports(params: PageParams & { qcNo?: string; batchNo?: string; workshopId?: number; qcType?: string; docStatus?: string; publicOnly?: number }) {
  return get<PageResult<QcReport>>('/api/qc/reports', params)
}
export function getQcStatistics(workshopId?: number) {
  return get<{ total: number; overLimitCount?: number; avgByl?: number }>('/api/qc/reports/statistics', { workshopId })
}
export function calcQcReport(id: number) {
  return post<void>(`/api/qc/reports/${id}/calc`)
}
export function submitQcReport(id: number) {
  return post<void>(`/api/qc/reports/${id}/submit`)
}
export function voidQcReport(id: number) {
  return post<void>(`/api/qc/reports/${id}/void`)
}
export function unvoidQcReport(id: number) {
  return post<void>(`/api/qc/reports/${id}/unvoid`)
}
export function publicQcReport(id: number) {
  return post<void>(`/api/qc/reports/${id}/public`)
}
