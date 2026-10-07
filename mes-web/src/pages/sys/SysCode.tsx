import { useState } from 'react'
import { App, Button, Card, Select, Space, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { get } from '@/api/request'
import dayjs from 'dayjs'

/** 编码规则种子（sys_auto_code 规则名，中文） */
const CODE_RULES = [
  '计划工艺单', '生产任务', '生产排程', '制造卡', '上机制造卡',
  '上批汇报', '产量汇报', '投料记录', '领退料单', '启停汇报',
  '翻批单', '前纺汇报', '检验报告', '包装任务', '包装执行',
  '包装箱', '组托单', '箱贴补打',
].map((n) => ({ label: n, value: n }))

interface TakenCode {
  key: number
  codeName: string
  serialNo: string
  time: string
}

/** 自动编码：实时取号演示（Redis INCR + DB 断号留痕 sys_code_log） */
export default function SysCode() {
  const { message } = App.useApp()
  const [rule, setRule] = useState<string>('计划工艺单')
  const [bizRef, setBizRef] = useState('')
  const [history, setHistory] = useState<TakenCode[]>([])
  const [loading, setLoading] = useState(false)

  const takeCode = async () => {
    if (!rule) return
    setLoading(true)
    try {
      const enc = encodeURIComponent(rule)
      const r = await get<string>(`/api/engine/code/next/${enc}`, bizRef ? { bizRef } : undefined)
      message.success(`取号成功：${r}`)
      setHistory((h) => [
        { key: Date.now(), codeName: rule, serialNo: r, time: dayjs().format('HH:mm:ss.SSS') },
        ...h,
      ].slice(0, 15))
    } finally {
      setLoading(false)
    }
  }

  const columns: ColumnsType<TakenCode> = [
    { title: '规则名', dataIndex: 'codeName', width: 140 },
    { title: '号码', dataIndex: 'serialNo', width: 190, render: (v) => <Tag color="blue">{v}</Tag> },
    { title: '取号时间', dataIndex: 'time', width: 130 },
  ]

  return (
    <div className="page">
      <div className="sec-title">自动编码</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Space wrap>
          <span className="muted">编码规则（规则名中文，前缀+日期+流水）：</span>
          <Select style={{ width: 160 }} options={CODE_RULES} value={rule} onChange={setRule} showSearch />
          <Button type="primary" loading={loading} onClick={takeCode}>实时取号</Button>
          <span className="muted">每号在 sys_code_log 留痕（断号可追溯），并发取号零重号</span>
        </Space>
      </Card>
      <Card size="small" title="本次会话取号记录（最近 15 条）">
        <Table rowKey="key" size="small" columns={columns} dataSource={history} pagination={false} locale={{ emptyText: '点击"实时取号"体验编码引擎' }} />
      </Card>
    </div>
  )
}
