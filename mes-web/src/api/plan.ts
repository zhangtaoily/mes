import { get, post, put, del } from './request'
import type { PageResult } from './request'

/** 基础数据（车间/产线/工序/批号标识/设备） */
export interface BaseWorkshop {
  id: number
  workshopCode: string
  workshopName: string
  workshopType: number
  status: number
}
export interface BaseProdLine { id: number; workshopId: number; lineName: string; status: number }
export interface BaseProcess { id: number; processCode: string; processName: string; processType: string; sortNo: number; status: number }
export interface BaseBatchFlag { id: number; flagName: string; alias: string; processId: number; status: number }
export interface BaseEquipment { id: number; workshopId: number; machineModel: string; machineNo: string; status: number }

export const getWorkshops = () => get<BaseWorkshop[]>('/api/plan/base/workshops')
export const getProdLines = (workshopId?: number) => get<BaseProdLine[]>('/api/plan/base/prod-lines', { workshopId })
export const getProcesses = () => get<BaseProcess[]>('/api/plan/base/processes')
export const getBatchFlags = () => get<BaseBatchFlag[]>('/api/plan/base/batch-flags')
export const getEquipments = (workshopId?: number) => get<BaseEquipment[]>('/api/plan/base/equipments', { workshopId })

/** 通用分页参数 */
export interface PageParams { pageNum?: number; pageSize?: number }

// ============ 生产管理（plan） ============

export interface ErpK3Order {
  id: number
  orderType?: string
  orderNo: string
  orderDate?: string
  workshopName?: string
  batchNo?: string
  k3Qty?: number
  mergeFeedQty?: number
  importedQty?: number
  productName?: string
  colorNo?: string
  spinCount?: string
  customerName?: string
  saleDept?: string
  materialDetail?: string
  deliveryDate?: string
  planEndDate?: string
  isClosed?: number
  orderStatus?: string
}

export function pageErpOrders(params: PageParams & { orderNo?: string; batchNo?: string; productName?: string; isClosed?: number }) {
  return get<PageResult<ErpK3Order>>('/api/plan/erp-orders', params)
}
export function getErpOrder(id: number) {
  return get<ErpK3Order>(`/api/plan/erp-orders/${id}`)
}

export interface ProdTask {
  id: number
  taskNo: string
  planCraftId?: number
  taskType?: string
  docStatus: string
  execStatus?: string
  batchNo?: string
  workshopId?: number
  rawMaterial?: string
  colorNo?: string
  productNo?: string
  productSpec?: string
  actualCount?: string
  mergeFeedQty?: number
  planFeedQty?: number
  planQty?: number
  unit?: string
  k3OrderNo?: string
  orderDate?: string
  deliveryDate?: string
  isFinished?: number
  isClosed?: number
}

export function pageTasks(params: PageParams & { workshopId?: number; batchNo?: string; actualCount?: string; rawMaterial?: string; docStatus?: string }) {
  return get<PageResult<ProdTask>>('/api/plan/tasks', params)
}
export function getTask(id: number) {
  return get<ProdTask>(`/api/plan/tasks/${id}`)
}
/** 批量状态操作：submit / withdraw / finish / close / void */
export function taskBatchOp(op: 'submit' | 'withdraw' | 'finish' | 'close' | 'void', ids: number[]) {
  return post<void>(`/api/plan/tasks/${op}`, { ids })
}
export function deleteTask(id: number) {
  return del<void>(`/api/plan/tasks/${id}`)
}

export interface ProdSchedule {
  id: number
  scheduleNo: string
  workOrderNo?: string
  workshopId?: number
  taskId?: number
  batchNo?: string
  traceBatchNo?: string
  processId?: number
  machineModel?: string
  equipmentId?: number
  scheduleDate?: string
  planStart?: string
  planEnd?: string
  scheduleQty?: number
  seq?: number
}

export function pageSchedules(params: PageParams & { scheduleDate?: string; workshopId?: number; batchNo?: string; processId?: number }) {
  return get<PageResult<ProdSchedule>>('/api/plan/schedules', params)
}

export interface PlanCraft {
  id: number
  craftNo: string
  docStatus: string
  workshopId?: number
  batchNo?: string
  traceBatchNo?: string
  rawMaterial?: string
  rawSpec?: string
  colorNo?: string
  actualCount?: string
  feedQty?: number
  mergeFeedQty?: number
  k3OrderNo?: string
  orderDate?: string
  deliveryDate?: string
  planQty?: number
}

export function pageCrafts(params: PageParams & { workshopId?: number; batchNo?: string; actualCount?: string; rawMaterial?: string; docStatus?: string }) {
  return get<PageResult<PlanCraft>>('/api/plan/crafts', params)
}
export function submitCrafts(ids: number[]) {
  return post<void>('/api/plan/crafts/submit', { ids })
}
export function withdrawCrafts(ids: number[]) {
  return post<void>('/api/plan/crafts/withdraw', { ids })
}
export function voidCraft(id: number) {
  return put<void>(`/api/plan/crafts/${id}/void`)
}
export function generateSchedule(id: number) {
  return post<Record<string, unknown>>(`/api/plan/crafts/${id}/generate-schedule`)
}

export interface MachineCardRow {
  card: {
    id: number
    cardNo: string
    scheduleId?: number
    batchNo?: string
    workshopId?: number
    processId?: number
    machineModel?: string
    status?: string
    countSpec?: string
    productSpec?: string
    rawMaterial?: string
    speed?: string
    fixedLength?: string
    quota?: string
    tension?: string
    printTime?: string
    withdrawTime?: string
  }
  workshopName?: string
  processName?: string
  machineNo?: string
  scheduleNo?: string
}

export function pageCards(params: PageParams & { status?: string; batchNo?: string; cardNo?: string }) {
  return get<PageResult<MachineCardRow>>('/api/plan/cards', params)
}
export function printCard(id: number) {
  return put<void>(`/api/plan/cards/${id}/print`)
}
export function withdrawCard(id: number) {
  return put<void>(`/api/plan/cards/${id}/withdraw`)
}
