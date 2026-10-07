import { Button, Card, DatePicker, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops, useProcesses } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { ProdSchedule } from '@/api/plan'

/** 生产排程（只读查看） */
export default function Schedule() {
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch } = usePagedList<ProdSchedule>('/api/plan/schedules')
  const [form] = Form.useForm()

  const columns: ColumnsType<ProdSchedule> = [
    { title: '排程单号', dataIndex: 'scheduleNo', width: 150, fixed: 'left' },
    { title: '工单号', dataIndex: 'workOrderNo', width: 140 },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '追溯批号', dataIndex: 'traceBatchNo', width: 115 },
    { title: '工序', dataIndex: 'processId', width: 90, render: (v) => procName[v] ?? v ?? '-' },
    { title: '机型', dataIndex: 'machineModel', width: 110 },
    { title: '排程日期', dataIndex: 'scheduleDate', width: 100 },
    { title: '计划开始', dataIndex: 'planStart', width: 140, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '计划结束', dataIndex: 'planEnd', width: 140, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '排产量', dataIndex: 'scheduleQty', width: 90, align: 'right' },
    { title: '顺序', dataIndex: 'seq', width: 60 },
  ]

  return (
    <div className="page">
      <div className="sec-title">生产排程</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form
          form={form}
          layout="inline"
          onFinish={(v) => setSearch(cleanParams({ ...v, scheduleDate: v.scheduleDate ? v.scheduleDate.format('YYYY-MM-DD') : undefined }))}
        >
          <Form.Item name="scheduleDate" label="排程日期"><DatePicker allowClear style={{ width: 130 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="processId" label="工序">
            <Select allowClear placeholder="全部" style={{ width: 120 }} options={Object.entries(procName).map(([k, n]) => ({ label: n, value: Number(k) }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>
    </div>
  )
}
