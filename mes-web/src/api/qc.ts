import { get, post, put, del } from './request'
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

// ============ 质检公式 ============

export interface QcFormula {
  id: number
  formulaName: string
  processId?: number
  workshopType?: string
  formulaType?: string
  targetIndicatorId?: number
  affectIndicators?: string
  sortNo?: number
  script?: string
  updateTime?: string
}

export function pageFormulas(params: PageParams & { formulaName?: string; formulaType?: string; processId?: number }) {
  return get<PageResult<QcFormula>>('/api/qc/formulas', params)
}
export function saveFormula(body: Partial<QcFormula>) { return post<number>('/api/qc/formulas', body) }
export function updateFormula(id: number, body: Partial<QcFormula>) { return put<void>(`/api/qc/formulas/${id}`, body) }
export function deleteFormula(id: number) { return del<void>(`/api/qc/formulas/${id}`) }

// ============ 包装质检报告 ============

export interface QcPackageReport {
  id: number
  warehouseBatchNo: string
  batchNo?: string
  workshopId?: number
  qcWorkshopId?: number
  docDate?: string
  firstGradeQty?: number
  passRate?: number
  remark?: string
}
export interface QcPackageDefect {
  id: number
  packageQcId?: number
  defectType?: string
  value?: number
  remark?: string
}

export const DEFECT_TYPES = ['正纱', 'AA BB纱', '尾纱', '辫子纱', '松捻纱', '紧捻纱', '重纱', '轻纱', '断纱', '多股纱', '油纱', '大小肚', '乱纱', '圈圈纱', '其它']

export function pagePackageReports(params: PageParams & { warehouseBatchNo?: string; batchNo?: string }) {
  return get<PageResult<QcPackageReport>>('/api/qc/package-reports', params)
}
export function getPackageReportDetail(id: number) {
  return get<{ report: QcPackageReport; defects: QcPackageDefect[] }>(`/api/qc/package-reports/${id}`)
}
export function savePackageReport(body: Partial<QcPackageReport> & { defects?: { defectType?: string; value?: number; remark?: string }[] }) {
  return post<number>('/api/qc/package-reports', body)
}
