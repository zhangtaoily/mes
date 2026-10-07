import { useEffect, useState } from 'react'
import { getWorkshops, getProcesses } from '@/api/plan'
import type { BaseWorkshop, BaseProcess } from '@/api/plan'

/** 车间下拉选项 + id→名称映射 */
export function useWorkshops() {
  const [list, setList] = useState<BaseWorkshop[]>([])
  useEffect(() => {
    getWorkshops().then(setList).catch(() => setList([]))
  }, [])
  const options = list.map((w) => ({ label: w.workshopName, value: w.id }))
  const nameMap: Record<number, string> = {}
  list.forEach((w) => (nameMap[w.id] = w.workshopName))
  return { list, options, nameMap }
}

/** 工序下拉选项 + id→名称映射 */
export function useProcesses() {
  const [list, setList] = useState<BaseProcess[]>([])
  useEffect(() => {
    getProcesses().then(setList).catch(() => setList([]))
  }, [])
  const options = list.map((p) => ({ label: p.processName, value: p.id }))
  const nameMap: Record<number, string> = {}
  list.forEach((p) => (nameMap[p.id] = p.processName))
  return { list, options, nameMap }
}
