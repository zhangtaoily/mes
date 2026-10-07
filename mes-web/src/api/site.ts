import { get, post, del } from './request'
import type { PageResult } from './request'
import type { PageParams } from './plan'

// ============ 现场管理（produce） ============

export interface RepBatchOn {
  id: number
  batchOnNo: string
  scheduleId?: number
  workshopId?: number
  teamId?: number
  shift?: string
  batchNo?: string
  traceBatchNo?: string
  processId?: number
  productCode?: string
  productName?: string
  equipmentId?: number
  isBatchOff?: number
  isFinished?: number
  batchOnTime?: string
  batchOffTime?: string
  batchOffUser?: string
  planQty?: number
  source?: number
}

export function pageBatchOn(params: PageParams & { workshopId?: number; batchNo?: string; batchOnNo?: string; isBatchOff?: number }) {
  return get<PageResult<RepBatchOn>>('/api/site/batch-on', params)
}
/** 手动下批 */
export function batchOff(id: number) {
  return post<void>(`/api/site/batch-on/${id}/off`)
}

export interface MtFeed {
  id: number
  feedNo: string
  taskNo?: string
  workshopId?: number
  teamId?: number
  batchNo?: string
  rawDetail?: string
  feedQty?: number
  isFinish?: number
  feedBy?: string
  feedTime?: string
}

export function pageFeeds(params: PageParams & { feedNo?: string; batchNo?: string; taskNo?: string; workshopId?: number; isFinish?: number }) {
  return get<PageResult<MtFeed>>('/api/site/feed-record', params)
}

export interface RepOutput {
  id: number
  outputNo: string
  scheduleId?: number
  taskId?: number
  teamId?: number
  reportUser?: string
  prodUsers?: string
  batchNo?: string
  processId?: number
  productName?: string
  equipmentId?: number
  outputQty?: number
  reworkQty?: number
  returnWoolQty?: number
  returnWoolRate?: number
  woolQty?: number
  reportType?: string
  unit?: string
  reportDate?: string
}

export function pageOutputs(params: PageParams & { batchNo?: string; outputNo?: string; teamId?: number; scheduleId?: number; reportDateBegin?: string; reportDateEnd?: string }) {
  return get<PageResult<RepOutput>>('/api/site/output', params)
}
