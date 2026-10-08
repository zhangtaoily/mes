import { get, post, del } from './request'
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

// ---------- 装箱记录（执行单 + 箱子明细） ----------

export interface PkBox {
  id: number
  pkExecuteId?: number
  boxCode?: string
  boxStatus?: string
  seqNo?: number
  grainCount?: number
  publicWeight?: number
  netWeight?: number
  grossWeight?: number
  tareWeight?: number
  packSpec?: string
  tieLineColor?: string
  palletCode?: string
  packUser?: string
  boxTime?: string
  actualTube?: number
}

export function getPackExecuteDetail(id: number) {
  return get<{ execute: PkExecute; boxes: PkBox[] }>(`/api/pack/executes/${id}`)
}
export function deletePackBox(boxId: number) { return del<void>(`/api/pack/executes/boxes/${boxId}`) }
export function finishPackExecute(id: number) { return post<void>(`/api/pack/executes/${id}/finish`) }

// ---------- 组托记录 ----------

export interface PkPallet {
  id: number
  palletCode?: string
  boxCode?: string
  packWorkshopId?: number
  batchNo?: string
  dyeVatNo?: string
  standardWeight?: number
  grossWeight?: number
  netWeight?: number
  standardTube?: number
  palletUser?: string
  palletTime?: string
}

export function pagePallets(params: PageParams & { palletCode?: string; batchNo?: string }) {
  return get<PageResult<PkPallet>>('/api/pack/pallets', params)
}
export function palletize(body: { executeId?: number; boxCodes: string[]; standardTube?: number }) {
  return post<string>('/api/pack/pallets/palletize', body)
}

// ---------- 箱贴补打 ----------

export interface PkLabelReprint {
  id: number
  docNo?: string
  pkTaskId?: number
  batchNo?: string
  k3OrderNo?: string
  dyeVatNo?: string
  warehouseBatchNo?: string
  productName?: string
  productSpec?: string
  customerName?: string
  docDate?: string
  bizType?: string
  confirmStatus?: string
  boxCode?: string
  boxSeq?: number
  publicWeight?: number
  netWeight?: number
  grossWeight?: number
  tareWeight?: number
  packSpec?: string
  palletCode?: string
  printTime?: string
  createTime?: string
}

export function pageLabelReprints(params: PageParams & { docNo?: string; warehouseBatchNo?: string }) {
  return get<PageResult<PkLabelReprint>>('/api/pack/trace/label-reprints', params)
}
export function reprintLabel(body: Partial<PkLabelReprint>) {
  return post<number>('/api/pack/trace/label-reprints', body)
}

// ---------- WMS 入库记录 ----------

export interface PkWmsIn {
  id: number
  warehouseBatchNo?: string
  batchNo?: string
  dyeVatNo?: string
  boxCode?: string
  palletCode?: string
  publicWeight?: number
  grossWeight?: number
  tareWeight?: number
  netWeight?: number
  inTime?: string
  wmsStatus?: string
}

export function pageWmsIn(params: PageParams & { warehouseBatchNo?: string }) {
  return get<PageResult<PkWmsIn>>('/api/pack/trace/wms-in', params)
}

// ---------- 箱子删除记录 ----------

export interface PkBoxDelete {
  id: number
  boxCode?: string
  palletCode?: string
  seqNo?: number
  tareWeight?: number
  grossWeight?: number
  tubeCount?: number
  paperTubeWeight?: number
  deleteUser?: string
  deleteTime?: string
}

export function pageBoxDeletes(params: PageParams & { boxCode?: string; palletCode?: string; deleteUser?: string }) {
  return get<PageResult<PkBoxDelete>>('/api/pack/box-deletes', params)
}
