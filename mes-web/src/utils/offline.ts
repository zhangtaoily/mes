import { post, BizError } from '@/api/request'

/**
 * PDA 离线暂存（M4 轻量方案）
 *
 * 网络异常（无法连接后端，BizError.code === -1）时把提交体存入 localStorage，
 * 恢复联网后在「我的-离线待提交」逐条重放（后端重新取号，无幂等冲突）；
 * 业务校验失败（code=400）不入队——重放必然再失败。
 */
export interface OfflineItem {
  id: string
  url: string
  body: unknown
  label: string
  createTime: string
}

const KEY = 'mes_pda_offline'

function load(): OfflineItem[] {
  try {
    const raw = localStorage.getItem(KEY)
    const arr = raw ? (JSON.parse(raw) as OfflineItem[]) : []
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function save(items: OfflineItem[]) {
  localStorage.setItem(KEY, JSON.stringify(items))
}

/** 是否网络异常（可离线暂存：无 HTTP 响应，如断网/超时/服务不可达） */
export function isNetworkError(e: unknown): boolean {
  return e instanceof BizError && e.code === -1
}

/** 当前待提交队列 */
export function offlineList(): OfflineItem[] {
  return load()
}

/** 入队（网络异常时调用） */
export function offlineEnqueue(url: string, body: unknown, label: string) {
  const items = load()
  items.unshift({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    url,
    body,
    label,
    createTime: new Date().toLocaleString(),
  })
  save(items)
}

/** 单条出队 */
export function offlineRemove(id: string) {
  save(load().filter((i) => i.id !== id))
}

/** 清空 */
export function offlineClear() {
  save([])
}

/** 逐条重试：成功即出队；返回 {ok, fail}；登录过期（401）中断并抛出 */
export async function offlineRetryAll(): Promise<{ ok: number; fail: number }> {
  let ok = 0
  let fail = 0
  for (const it of load()) {
    try {
      await post(it.url, it.body)
      offlineRemove(it.id)
      ok++
    } catch (e) {
      if (e instanceof BizError && e.code === 401) {
        throw e
      }
      fail++
    }
  }
  return { ok, fail }
}
