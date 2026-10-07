import { get, post } from './request'
import type { PageResult } from './request'
import type { PageParams } from './plan'

// ============ 包装管理（pack） ============

export interface PkTask {
  id: number
  taskNo: string
  taskVariant?: string
  batchNo?: string
  k3OrderNo?: string
  dyeVatNo?: string
  warehouseBatchNo?: string
  workshopId?: number
  packWorkshopId?: number
  productName?: string
  nameEn?: string
  rawMaterial?: string
  colorNo?: string
  productSpec?: string
  customerName?: string
  packSpecId?: number
  deliveryDate?: string
  weightMode?: string
  isFinished?: number
  finishTime?: string
  vatFinished?: number
}

export function pagePackTasks(params: PageParams & { taskNo?: string; batchNo?: string; warehouseBatchNo?: string; taskVariant?: string; isFinished?: number }) {
  return get<PageResult<PkTask>>('/api/pack/tasks', params)
}
export function finishPackTasks(ids: number[]) {
  return post<void>('/api/pack/tasks/finish', { ids })
}
export function cancelFinishPackTasks(ids: number[]) {
  return post<void>('/api/pack/tasks/cancel-finish', { ids })
}

export interface PkExecute {
  id: number
  executeNo: string
  pkTaskId?: number
  packBatchNo?: string
  teamId?: number
  warehouseBatchNo?: string
  dyeVatNo?: string
  packName?: string
  productName?: string
  nameEn?: string
  rawMaterial?: string
  colorNo?: string
  productSpec?: string
  tubeCount?: number
  packWeight?: number
  standardWeight?: number
  moistureStandard?: number
  moistureActual?: number
  isPacked?: number
  sumPublicWeight?: number
  sumGrossWeight?: number
  sumNetWeight?: number
  sumTube?: number
  boxCount?: number
}

export function pagePackExecutes(params: PageParams & { executeNo?: string; packBatchNo?: string; warehouseBatchNo?: string; pkTaskId?: number }) {
  return get<PageResult<PkExecute>>('/api/pack/executes', params)
}
