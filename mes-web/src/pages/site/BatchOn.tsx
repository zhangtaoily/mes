import { App, Button, Card, Form, Input, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops, useProcesses } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { batchOff } from '@/api/site'
import type { RepBatchOn } from '@/api/site'
import { BoolTag } from '@/components/StatusTag'
import dayjs from 'dayjs'

/** 上批汇报：查看批次活动状态，支持手动下批 */
export default function BatchOn() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<RepBatchOn>('/api/site/batch-on')
  const [form] = Form.useForm()

  const columns: ColumnsType<RepBatchOn> = [
    { title: '上批单号', dataIndex: 'batchOnNo', width: 150, fixed: 'left' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '追溯批号', dataIndex: 'traceBatchNo', width: 115 },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '工序', dataIndex: 'processId', width: 90, render: (v) => procName[v] ?? v ?? '-' },
    { title: '产品名称', dataIndex: 'productName', width: 150, ellipsis: true },
    { title: '班次', dataIndex: 'shift', width: 70 },
    { title: '上批时间', dataIndex: 'batchOnTime', width: 145, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '是否下批', dataIndex: 'isBatchOff', width: 90, render: (v) => <BoolTag value={v} /> },
    { title: '下批时间', dataIndex: 'batchOffTime', width: 145, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '来源', dataIndex: 'source', width: 80, render: (v) => (v === 2 ? 'PDA' : v === 1 ? 'PC' : v ?? '-') },
    {
      title: '操作', width: 90, fixed: 'right',
      render: (_, r) => !r.isBatchOff ? (
        <Popconfirm title="确认手动下批？" onConfirm={async () => { await batchOff(r.id); message.success('下批成功'); reload() }}>
          <a>下批</a>
        </Popconfirm>
      ) : <span className="muted">-</span>,
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">上批汇报</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="batchOnNo" label="上批单号"><Input allowClear placeholder="单号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="isBatchOff" label="下批状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={[{ label: '活动中', value: 0 }, { label: '已下批', value: 1 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1500 }} />
      </Card>
    </div>
  )
}
