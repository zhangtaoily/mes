import { useEffect, useMemo, useRef, useState } from 'react'
import { get } from '@/api/request'
import type { PageResult } from '@/api/request'

/**
 * 通用分页列表 Hook：按 url + 查询参数自动加载，
 * search 变化回到第 1 页，page 变化保持查询条件。
 */
export function usePagedList<T>(
  url: string,
  initialSearch: Record<string, unknown> = {},
  opts?: { pageSize?: number },
) {
  const [search, setSearch] = useState<Record<string, unknown>>(initialSearch)
  const [page, setPage] = useState({ pageNum: 1, pageSize: opts?.pageSize ?? 20 })
  const [rows, setRows] = useState<T[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const reqSeq = useRef(0)

  // search 变化时回到第一页（首轮跳过，避免双请求）
  useEffect(() => {
    setPage((p) => (p.pageNum === 1 ? p : { ...p, pageNum: 1 }))
  }, [search])

  useEffect(() => {
    const seq = ++reqSeq.current
    setLoading(true)
    get<PageResult<T>>(url, { ...search, ...page })
      .then((r) => {
        if (seq !== reqSeq.current) return
        setRows(r?.rows ?? [])
        setTotal(r?.total ?? 0)
      })
      .catch(() => {
        if (seq === reqSeq.current) {
          setRows([])
          setTotal(0)
        }
      })
      .finally(() => {
        if (seq === reqSeq.current) setLoading(false)
      })
  }, [url, search, page])

  const pagination = useMemo(
    () => ({
      current: page.pageNum,
      pageSize: page.pageSize,
      total,
      showSizeChanger: true,
      showTotal: (t: number) => `共 ${t} 条`,
      onChange: (pageNum: number, pageSize: number) => setPage({ pageNum, pageSize }),
    }),
    [page, total],
  )

  return { rows, total, loading, pagination, search, setSearch, page, setPage, reload: () => setPage({ ...page }) }
}
