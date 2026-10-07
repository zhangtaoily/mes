import { Button, Card, DatePicker, Form, Input, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops, useProcesses } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import type { ProdSchedule } from '@/api/plan'

/** 后端 ScheduleListVO：排程主体 + 车间/工序/机台/任务名称 */
interface ScheduleRow {
  schedule: ProdSchedule
  workshopName?: string
  processName?: string
  machineNo?: string
  taskNo?: string
}

/** 生产排程（只读查看） */
export default function Schedule() {
  const { options: wsOptions } = useWorkshops()
  const { nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch } = usePagedList<ScheduleRow>('/api/plan/schedules')
  const [form] = Form.useForm()

  const columns: ColumnsType<ScheduleRow> = [
    { title: '排程单号', dataIndex: ['schedule', 'scheduleNo'], width: 150, fixed: 'left' },
    { title: '工单号', dataIndex: ['schedule', 'workOrderNo'], width: 140 },
    { title: '车间', dataIndex: 'workshopName', width: 130, render: (v) => v ?? '-' },
    { title: '生产批号', dataIndex: ['schedule', 'batchNo'], width: 115 },
    { title: '追溯批号', dataIndex: ['schedule', 'traceBatchNo'], width: 115 },
    { title: '工序', dataIndex: 'processName', width: 90, render: (v) => v ?? '-' },
    { title: '机型', dataIndex: ['schedule', 'machineModel'], width: 110 },
    { title: '排程日期', dataIndex: ['schedule', 'scheduleDate'], width: 100 },
    { title: '计划开始', dataIndex: ['schedule', 'planStart'], width: 140, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '计划结束', dataIndex: ['schedule', 'planEnd'], width: 140, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '排产量', dataIndex: ['schedule', 'scheduleQty'], width: 90, align: 'right' },
    { title: '顺序', dataIndex: ['schedule', 'seq'], width: 60 },
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
        <Table rowKey={(r) => r.schedule?.id ?? r.schedule?.scheduleNo ?? ''} size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>
    </div>
  )
}
