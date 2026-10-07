import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, DatePicker, Form, Input, Table, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { getReportCatalog, queryReport } from '@/api/report'
import type { ReportCatalog, ReportQuery } from '@/api/report'

type Row = Record<string, unknown>

/** 报表中心：目录树 + 通用查询 + 动态列渲染 */
export default function ReportCenter() {
  const [catalog, setCatalog] = useState<ReportCatalog[]>([])
  const [active, setActive] = useState<string>('')
  const [rows, setRows] = useState<Row[]>([])
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')
  const [form] = Form.useForm()

  useEffect(() => {
    getReportCatalog()
      .then((c) => {
        setCatalog(c)
        // 默认选中第一个已实现报表
        for (const cat of c) {
          const impl = cat.reports.find((r) => r.implemented)
          if (impl) {
            setActive(impl.code)
            break
          }
        }
      })
      .catch(() => setCatalog([]))
  }, [])

  // 汇总所有分类下的报表
  const allReports = useMemo(() => catalog.flatMap((c) => c.reports), [catalog])
  const activeReport = allReports.find((r) => r.code === active)

  const doQuery = async (v: { range?: [dayjs.Dayjs, dayjs.Dayjs]; periodMonth?: dayjs.Dayjs; batchNo?: string; warehouseBatchNo?: string }) => {
    if (!active) return
    const params: ReportQuery = cleanQuery(v)
    setLoading(true)
    setErr('')
    try {
      const data = await queryReport(active, params)
      setRows(Array.isArray(data) ? data : [])
    } catch (e) {
      setRows([])
      setErr(e instanceof Error ? e.message : '查询失败')
    } finally {
      setLoading(false)
    }
  }

  // 切换报表时按默认日期范围自动查询一次
  useEffect(() => {
    if (!active) return
    form.resetFields()
    doQuery({ range: [dayjs().subtract(30, 'day'), dayjs()] })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active])

  // 动态列：取首行键（保持后端顺序）
  const columns: ColumnsType<Row> = useMemo(() => {
    if (!rows.length) return []
    return Object.keys(rows[0]).map((k) => ({
      title: k,
      dataIndex: k,
      width: Math.max(90, Math.min(160, k.length * 14 + 40)),
      render: (v: unknown) => (v === null || v === undefined ? '-' : typeof v === 'number' ? v.toLocaleString() : String(v)),
    }))
  }, [rows])

  const tabs = catalog.map((c) => ({
    key: c.category,
    label: c.category,
    children: (
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', padding: '6px 0' }}>
        {c.reports.map((r) => (
          <Button
            key={r.code}
            type={r.code === active ? 'primary' : 'default'}
            disabled={!r.implemented}
            onClick={() => setActive(r.code)}
            title={r.implemented ? r.name : 'P1/P2 后置，暂未实现'}
          >
            {r.name}{r.implemented ? '' : '（未实现）'}
          </Button>
        ))}
      </div>
    ),
  }))

  return (
    <div className="page">
      <div className="sec-title">报表中心</div>
      <Card size="small" style={{ marginBottom: 12 }} styles={{ body: { padding: '6px 14px' } }}>
        <Tabs items={tabs} />
      </Card>
      <Card size="small">
        <div style={{ fontWeight: 600, marginBottom: 10 }}>
          {activeReport?.name ?? '请选择报表'}
          {activeReport ? <span className="muted" style={{ fontWeight: 400, marginLeft: 10 }}>P{activeReport.priority}</span> : null}
        </div>
        <Form form={form} layout="inline" onFinish={doQuery} style={{ marginBottom: 10 }}>
          <Form.Item name="range" label="日期范围">
            <DatePicker.RangePicker allowClear style={{ width: 220 }} />
          </Form.Item>
          <Form.Item name="periodMonth" label="月份（用毛率）">
            <DatePicker picker="month" allowClear style={{ width: 110 }} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="warehouseBatchNo" label="入库批号"><Input allowClear placeholder="入库批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit" disabled={!active}>查询</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setRows([]) }}>重置</Button></Form.Item>
        </Form>
        {err ? <Alert type="error" message={err} style={{ marginBottom: 10 }} showIcon /> : null}
        <Table
          rowKey={(_, i) => String(i)}
          size="small"
          columns={columns}
          dataSource={rows}
          loading={loading}
          pagination={{ showSizeChanger: true, showTotal: (t) => `共 ${t} 条`, pageSize: 50 }}
          scroll={{ x: 'max-content' }}
        />
      </Card>
    </div>
  )
}

/** 表单值 → 查询参数（日期范围/月份格式化） */
function cleanQuery(v: { range?: [dayjs.Dayjs, dayjs.Dayjs]; periodMonth?: dayjs.Dayjs; batchNo?: string; warehouseBatchNo?: string }): ReportQuery {
  const out: ReportQuery = {}
  if (v.range?.[0]) out.dateBegin = v.range[0].format('YYYY-MM-DD')
  if (v.range?.[1]) out.dateEnd = v.range[1].format('YYYY-MM-DD')
  if (v.periodMonth) out.periodMonth = v.periodMonth.format('YYYY-MM')
  if (v.batchNo) out.batchNo = v.batchNo
  if (v.warehouseBatchNo) out.warehouseBatchNo = v.warehouseBatchNo
  return out
}
