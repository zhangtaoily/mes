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

// ============ 启停汇报 ============

export interface RepStartStop {
  id: number
  stopNo: string
  workshopId?: number
  equipmentId?: number
  machineModel?: string
  teamId?: number
  reportUser?: string
  reportDate?: string
  stopDuration?: number
  stopReason?: string
  remark?: string
  createTime?: string
}

export function pageStops(params: PageParams & { workshopId?: number; equipmentId?: number; stopNo?: string; reportDateBegin?: string; reportDateEnd?: string }) {
  return get<PageResult<RepStartStop>>('/api/site/stop', params)
}
export function saveStop(body: Partial<RepStartStop>) { return post<number>('/api/site/stop', body) }
export function deleteStop(id: number) { return del<void>(`/api/site/stop/${id}`) }

// ============ 翻批日志 ============

export interface RepRebatchLog {
  id: number
  title?: string
  workshopId?: number
  teamId?: number
  startTime?: string
  endTime?: string
  createBy?: string
  createTime?: string
}
export interface RepRebatchDetail {
  id: number
  rebatchLogId: number
  workshopId?: number
  teamId?: number
  batchNo?: string
  traceBatchNo?: string
  rawMaterial?: string
  productNo?: string
  productSpec?: string
  processId?: number
  equipmentId?: number
  isBatchOff?: number
  isFinished?: number
  batchOnTime?: string
  batchOnUser?: string
  batchOffTime?: string
  batchOffUser?: string
}

export function pageRebatchLogs(params: PageParams & { workshopId?: number; teamId?: number }) {
  return get<PageResult<RepRebatchLog>>('/api/site/rebatch', params)
}
export function getRebatchDetails(id: number) { return get<RepRebatchDetail[]>(`/api/site/rebatch/${id}/details`) }
export function generateRebatch(body: { workshopId: number; teamId: number; startTime: string; endTime: string }) {
  return post<number>('/api/site/rebatch/generate', body)
}

// ============ 自络筒切疵 ============

export interface RepAutoWindingCut {
  id: number
  batchNo?: string
  rawMaterial?: string
  colorNo?: string
  countSpec?: string
  workshopId?: number
  equipmentId?: number
  cutType?: string
  createTime?: string
}

export function pageCuts(params: PageParams & { workshopId?: number; batchNo?: string }) {
  return get<PageResult<RepAutoWindingCut>>('/api/site/yarn-clearer', params)
}
export function saveCut(body: Partial<RepAutoWindingCut>) { return post<number>('/api/site/yarn-clearer', body) }
export function deleteCut(id: number) { return del<void>(`/api/site/yarn-clearer/${id}`) }

// ============ 蒸纱信息 ============

export interface RepYarnSteam {
  id: number
  printCode?: string
  status?: string
  processId?: number
  truckNo?: string
  grainCount?: number
  weight?: number
  truckCount?: number
  batchNo?: string
  printUser?: string
  printTime?: string
  receiveUser?: string
  receiveTime?: string
}

export function pageSteams(params: PageParams & { workshopId?: number; batchNo?: string; truckNo?: string; status?: string }) {
  return get<PageResult<RepYarnSteam>>('/api/site/steam', params)
}
export function saveSteam(body: Partial<RepYarnSteam>) { return post<number>('/api/site/steam', body) }
export function printSteam(id: number) { return post<void>(`/api/site/steam/${id}/print`) }
export function receiveSteam(id: number) { return post<void>(`/api/site/steam/${id}/receive`) }
export function deleteSteam(id: number) { return del<void>(`/api/site/steam/${id}`) }

// ============ 毛球周转车 ============

export interface RepTruck {
  id: number
  truckNo: string
  status?: string
  workshopId?: number
  batchNo?: string
  rawMaterial?: string
  location?: string
  bagColor?: string
  ballCount?: number
  grossWeight?: number
  netWeight?: number
  standardWeight?: number
  moistureStandard?: number
  moistureActual?: number
}
export interface RepTruckMove {
  id: number
  truckNo?: string
  fromLocation?: string
  toLocation?: string
  recombedBatchNo?: string
  spinBatchNo?: string
  opName?: string
  moveNetWeight?: number
  moveBallCount?: number
  isFirst?: number
  isFinished?: number
  createTime?: string
}

export function pageTrucks(params: PageParams & { truckNo?: string; batchNo?: string; workshopId?: number; status?: string }) {
  return get<PageResult<RepTruck>>('/api/site/truck', params)
}
export function getTruckDetail(id: number) { return get<{ truck: RepTruck; moves: RepTruckMove[] }>(`/api/site/truck/${id}`) }
export function saveTruck(truck: Partial<RepTruck>) { return post<number>('/api/site/truck', { truck }) }
export function moveTruck(body: Record<string, unknown>) { return post<void>('/api/site/truck/move', body) }
export function splitTruck(body: { truckId: number; newTruckNo: string; moveNetWeight?: number; moveBallCount?: number; toLocation?: string }) {
  return post<number>('/api/site/truck/split', body)
}
export function deleteTruck(id: number) { return del<void>(`/api/site/truck/${id}`) }

// ============ 车间出勤 ============

export interface StAttendance {
  id: number
  periodMonth: string
  workshopId?: number
  workDays?: number
  managerCount?: number
  dayAuxCount?: number
  frontlineCount?: number
  qcCount?: number
  totalCount?: number
  convertCount?: number
  docStatus?: string
  createBy?: string
  createTime?: string
}

export function pageAttendance(params: PageParams & { periodMonth?: string; workshopId?: number; docStatus?: string }) {
  return get<PageResult<StAttendance>>('/api/site/attendance', params)
}
export function saveAttendance(body: Partial<StAttendance>) { return post<number>('/api/site/attendance', body) }
export function submitAttendance(id: number) { return post<void>(`/api/site/attendance/${id}/submit`) }
export function withdrawAttendance(id: number) { return post<void>(`/api/site/attendance/${id}/withdraw`) }
export function deleteAttendance(id: number) { return del<void>(`/api/site/attendance/${id}`) }

// ============ 车间排班 ============

export interface StShift {
  id: number
  workshopId?: number
  teamId?: number
  shiftName?: string
  onDutyTime?: string
  offDutyTime?: string
  startDate?: string
  endDate?: string
  createTime?: string
}
export interface StShiftPerson {
  id: number
  shiftId?: number
  userId?: number
  userName?: string
  empNo?: string
  equipmentId?: number
}

export function pageShifts(params: PageParams & { workshopId?: number; teamId?: number; shiftName?: string }) {
  return get<PageResult<StShift>>('/api/site/schedule', params)
}
export function saveShift(body: Partial<StShift>) { return post<number>('/api/site/schedule', body) }
export function deleteShift(id: number) { return del<void>(`/api/site/schedule/${id}`) }
export function getShiftPersons(id: number) { return get<StShiftPerson[]>(`/api/site/schedule/${id}/persons`) }
export function addShiftPersons(id: number, persons: Partial<StShiftPerson>[]) {
  return post<void>(`/api/site/schedule/${id}/persons`, persons)
}
export function deleteShiftPerson(personId: number) { return del<void>(`/api/site/schedule/persons/${personId}`) }

// ============ 人员工时 ============

export interface RepLaborHour {
  id: number
  periodMonth?: string
  prodLine?: string
  userId?: number
  workHour?: number
}

export function pageLaborHours(params: PageParams & { periodMonth?: string; prodLine?: string; userId?: number }) {
  return get<PageResult<RepLaborHour>>('/api/site/work-hours', params)
}
export function saveLaborHour(body: Partial<RepLaborHour>) { return post<number>('/api/site/work-hours', body) }
export function deleteLaborHour(id: number) { return del<void>(`/api/site/work-hours/${id}`) }

// ============ 车间能耗 ============

export interface StEnergy {
  id: number
  periodMonth: string
  workshopId?: number
  waterUsage?: number
  powerUsage?: number
  gasUsage?: number
  docStatus?: string
  createTime?: string
}
export interface StEnergyItem {
  id: number
  energyId?: number
  equipmentId?: number
  powerUsage?: number
  waterUsage?: number
  gasUsage?: number
}

export function pageEnergy(params: PageParams & { periodMonth?: string; workshopId?: number; docStatus?: string }) {
  return get<PageResult<StEnergy>>('/api/site/energy', params)
}
export function saveEnergy(body: Partial<StEnergy>) { return post<number>('/api/site/energy', body) }
export function submitEnergy(id: number) { return post<void>(`/api/site/energy/${id}/submit`) }
export function withdrawEnergy(id: number) { return post<void>(`/api/site/energy/${id}/withdraw`) }
export function deleteEnergy(id: number) { return del<void>(`/api/site/energy/${id}`) }
export function getEnergyItems(id: number) { return get<StEnergyItem[]>(`/api/site/energy/${id}/items`) }
export function addEnergyItems(id: number, items: Partial<StEnergyItem>[]) {
  return post<void>(`/api/site/energy/${id}/items`, items)
}
export function deleteEnergyItem(itemId: number) { return del<void>(`/api/site/energy/items/${itemId}`) }

// ============ 车间生产费用 ============

export interface StCost {
  id: number
  periodMonth: string
  workshopId?: number
  waterUsage?: number
  waterCost?: number
  powerUsage?: number
  powerCost?: number
  machineMaterialCost?: number
  packCost?: number
  monthDepreciation?: number
  processCostShare?: number
  salary?: number
  monthKpi?: number
  convertOutput?: number
  woolRate?: number
  docStatus?: string
  createTime?: string
}

export function pageCost(params: PageParams & { periodMonth?: string; workshopId?: number; docStatus?: string }) {
  return get<PageResult<StCost>>('/api/site/cost', params)
}
export function saveCost(body: Partial<StCost>) { return post<number>('/api/site/cost', body) }
export function submitCost(id: number) { return post<void>(`/api/site/cost/${id}/submit`) }
export function withdrawCost(id: number) { return post<void>(`/api/site/cost/${id}/withdraw`) }
export function deleteCost(id: number) { return del<void>(`/api/site/cost/${id}`) }

// ============ 现场问题反馈 ============

export interface StIssue {
  id: number
  checkUser?: string
  checkDate?: string
  issueLocation?: string
  issueDesc?: string
  rectifyStatus?: string
  createTime?: string
}

export function pageIssues(params: PageParams & { checkUser?: string; rectifyStatus?: string; issueLocation?: string }) {
  return get<PageResult<StIssue>>('/api/site/issue', params)
}
export function saveIssue(body: Partial<StIssue>) { return post<number>('/api/site/issue', body) }
export function deleteIssue(id: number) { return del<void>(`/api/site/issue/${id}`) }
