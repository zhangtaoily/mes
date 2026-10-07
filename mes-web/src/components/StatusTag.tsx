import { Tag } from 'antd'

/** 单据状态 → 颜色映射（对齐原型 tag 配色） */
const STATUS_COLOR: Record<string, string> = {
  编制中: 'orange',
  已提交: 'blue',
  生效: 'green',
  已暂停: 'orange',
  了批: 'purple',
  已了批: 'purple',
  已关闭: 'default',
  已作废: 'red',
  待确认: 'orange',
  已确认: 'green',
  已公示: 'cyan',
  计划执行: 'blue',
  工艺控制: 'purple',
  已打印: 'green',
  已撤回: 'default',
  已完毕: 'green',
}

export default function StatusTag({ status }: { status?: string | number | null }) {
  if (status === null || status === undefined || status === '') return <span className="muted">-</span>
  const s = String(status)
  return <Tag color={STATUS_COLOR[s] ?? 'blue'}>{s}</Tag>
}

/** 0/1 布尔字段显示 */
export function BoolTag({ value, labels = ['否', '是'] }: { value?: number | null; labels?: [string, string] | string[] }) {
  if (value === null || value === undefined) return <span className="muted">-</span>
  return <Tag color={value ? 'green' : 'default'}>{value ? labels[1] : labels[0]}</Tag>
}
