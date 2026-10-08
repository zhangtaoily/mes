import { Button, Card, DatePicker, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useProcesses, useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { ProdSchedule } from '@/api/plan'

/** 生产排程查看（只读视图，复用 /api/plan/schedules） */
export default function ScheduleView() {
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { options: procOptions, nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch } = usePagedList<ProdSchedule>('/api/plan/schedules')
  const [form] = Form.useForm()

  const columns: ColumnsType<ProdSchedule> = [
    { title: '排程单号', dataIndex: 'scheduleNo', width: 150, fixed: 'left' },
    { title: '工单号', dataIndex: 'workOrderNo', width: 140, render: (v) => v ?? '-' },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '追溯批号', dataIndex: 'traceBatchNo', width: 115, render: (v) => v ?? '-' },
    { title: '工序', dataIndex: 'processId', width: 100, render: (v) => procName[v] ?? v ?? '-' },
    { title: '机型', dataIndex: 'machineModel', width: 110, render: (v) => v ?? '-' },
    { title: '机台', dataIndex: 'equipmentId', width: 90, render: (v) => (v != null ? `机台${v}` : '-') },
    { title: '排程日期', dataIndex: 'scheduleDate', width: 100 },
    { title: '计划开始', dataIndex: 'planStart', width: 100, render: (v) => v ?? '-' },
    { title: '计划结束', dataIndex: 'planEnd', width: 100, render: (v) => v ?? '-' },
    { title: '排程量', dataIndex: 'scheduleQty', width: 90, align: 'right' },
    { title: '顺序', dataIndex: 'seq', width: 60, align: 'right' },
  ]

  return (
    <div className="page">
      <div className="sec-title">生产排程查看</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams({ ...v, scheduleDate: v.scheduleDate ? v.scheduleDate.format('YYYY-MM-DD') : undefined }))}>
          <Form.Item name="scheduleDate" label="排程日期"><DatePicker style={{ width: 130 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 140 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="processId" label="工序">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 120 }} options={procOptions} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1500 }} />
        <div className="muted" style={{ marginTop: 8 }}>只读视图：排程刷新与调整请使用「生产排程」菜单。</div>
      </Card>
    </div>
  )
}
