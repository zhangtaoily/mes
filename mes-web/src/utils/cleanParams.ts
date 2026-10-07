/** 清理查询参数：去掉空串/null/undefined，避免后端空条件干扰 */
export function cleanParams<T extends Record<string, unknown>>(v: T): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const [k, val] of Object.entries(v)) {
    if (val !== '' && val !== null && val !== undefined) out[k] = val
  }
  return out
}
