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

/** 报表 code → 端点映射 */
export const REPORT_ENDPOINTS: Record<string, string> = {
  qc_daily: '/api/reports/qc-daily',
  defect_stats: '/api/reports/defect-stats',
  tube_weight_output: '/api/reports/tube-weight-output',
  pack_inbound: '/api/reports/pack-inbound',
  progress_tracking: '/api/reports/progress-tracking',
  spinning_daily: '/api/reports/spinning-daily',
  wool_top_daily: '/api/reports/wool-top-daily',
  fore_spin_stats: '/api/reports/fore-spin-stats',
  wool_rate: '/api/reports/wool-rate',
  yarn_physical_stats: '/api/reports/yarn-physical-stats',
  cv_stats: '/api/reports/cv-stats',
  raw_qc_summary: '/api/reports/raw-qc-summary',
}

/** 通用报表查询：返回行集合（列动态） */
export function queryReport(code: string, params: ReportQuery) {
  const endpoint = REPORT_ENDPOINTS[code]
  if (!endpoint) return Promise.reject(new Error(`未知报表 ${code}`))
  return get<Record<string, unknown>[]>(endpoint, params as Record<string, unknown>)
}
