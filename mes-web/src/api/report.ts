import { get } from './request'

// ============ 报表中心（report） ============

export interface ReportItem {
  code: string
  name: string
  priority: string
  implemented: boolean
}
export interface ReportCatalog {
  category: string
  reports: ReportItem[]
}

export interface ReportQuery {
  dateBegin?: string
  dateEnd?: string
  workshopId?: number
  batchNo?: string
  warehouseBatchNo?: string
  periodMonth?: string
  indicatorId?: number
}

export function getReportCatalog() {
  return get<ReportCatalog[]>('/api/reports/catalog')
}

/** 报表 code → 端点映射（键与后端 catalog code 一致，连字符格式） */
export const REPORT_ENDPOINTS: Record<string, string> = {
  'qc-daily': '/api/reports/qc-daily',
  'defect-stats': '/api/reports/defect-stats',
  'tube-weight-output': '/api/reports/tube-weight-output',
  'pack-inbound': '/api/reports/pack-inbound',
  'progress-tracking': '/api/reports/progress-tracking',
  'spinning-daily': '/api/reports/spinning-daily',
  'wool-top-daily': '/api/reports/wool-top-daily',
  'fore-spin-stats': '/api/reports/fore-spin-stats',
  'wool-rate': '/api/reports/wool-rate',
  'yarn-physical-stats': '/api/reports/yarn-physical-stats',
  'cv-stats': '/api/reports/cv-stats',
  'raw-qc-summary': '/api/reports/raw-qc-summary',
}

/** 通用报表查询：返回行集合（列动态） */
export function queryReport(code: string, params: ReportQuery) {
  const endpoint = REPORT_ENDPOINTS[code]
  if (!endpoint) return Promise.reject(new Error(`未知报表 ${code}`))
  return get<Record<string, unknown>[]>(endpoint, params as Record<string, unknown>)
}
