import { App, Button, Card, Form, Input, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pagePackTasks, finishPackTasks, cancelFinishPackTasks } from '@/api/pack'
import type { PkTask } from '@/api/pack'

/** 倒毛包装任务（taskVariant=倒毛 固定过滤） */
export default function TaskDm() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<PkTask>('/api/pack/tasks', { taskVariant: '倒毛' })
  const [form] = Form.useForm()

  const columns: ColumnsType<PkTask> = [
    { title: '任务编号', dataIndex: 'taskNo', width: 150, fixed: 'left' },
    { title: '生产批号', dataIndex: 'batchNo', width: 125 },
    { title: 'K3订单号', dataIndex: 'k3OrderNo', width: 130 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 85 },
    { title: '入库批号', dataIndex: 'warehouseBatchNo', width: 115 },
    { title: '产品名称', dataIndex: 'productName', width: 130, ellipsis: true },
    { title: '原料', dataIndex: 'rawMaterial', width: 110, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 80 },
    { title: '规格', dataIndex: 'productSpec', width: 95 },
    { title: '客户', dataIndex: 'customerName', width: 110, ellipsis: true },
    { title: '交期', dataIndex: 'deliveryDate', width: 105 },
    {
      title: '状态', dataIndex: 'isFinished', width: 80,
      render: (v) => (v === 1 ? <span style={{ color: '#52c41a' }}>已了批</span> : '进行中'),
    },
    {
      title: '操作', width: 130, fixed: 'right',
      render: (_, r) => (
        <>
          {r.isFinished !== 1 && (
            <Popconfirm title="确认了批该倒毛任务？" onConfirm={async () => { await finishPackTasks([r.id]); message.success('已了批'); reload() }}>
              <a style={{ color: '#52c41a' }}>了批</a>
            </Popconfirm>
          )}
          {r.isFinished === 1 && (
            <Popconfirm title="取消了批？" onConfirm={async () => { await cancelFinishPackTasks([r.id]); message.success('已取消'); reload() }}>
              <a>取消了批</a>
            </Popconfirm>
          )}
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">倒毛包装任务</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams({ ...v, taskVariant: '倒毛' }))}>
          <Form.Item name="taskNo" label="任务编号"><Input allowClear placeholder="PS..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="isFinished" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={[{ label: '进行中', value: 0 }, { label: '已了批', value: 1 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({ taskVariant: '倒毛' }) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1500 }} />
      </Card>
    </div>
  )
}
