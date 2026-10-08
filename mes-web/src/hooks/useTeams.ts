import { useEffect, useState } from 'react'
import { pageTeams } from '@/api/base'
import type { BaseTeam } from '@/api/base'

/** 班组下拉选项 + id→名称映射（全量取100条） */
export function useTeams() {
  const [list, setList] = useState<BaseTeam[]>([])
  useEffect(() => {
    pageTeams({ pageNum: 1, pageSize: 100 }).then((p) => setList(p.rows ?? [])).catch(() => setList([]))
  }, [])
  const options = list.map((t) => ({ label: t.teamName, value: t.id }))
  const nameMap: Record<number, string> = {}
  list.forEach((t) => (nameMap[t.id] = t.teamName))
  return { list, options, nameMap }
}
