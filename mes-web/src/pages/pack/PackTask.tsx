import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { finishPackTasks, cancelFinishPackTasks } from '@/api/pack'
import type { PkTask } from '@/api/pack'
import { BoolTag } from '@/components/StatusTag'

/** 包装任务：了批/取消了批 */
export default function PackTask() {
  const { message } = App.useApp()
  const [sel, setSel] = useState<PkTask[]>([])
  const { rows, loading, pagination, setSearch, reload } = usePagedList<PkTask>('/api/pack/tasks')
  const [form] = Form.useForm()

  const columns: ColumnsType<PkTask> = [
    { title: '包装任务号', dataIndex: 'taskNo', width: 140, fixed: 'left' },
    { title: '任务类型', dataIndex: 'taskVariant', width: 90 },
    { title: '入库批号', dataIndex: 'warehouseBatchNo', width: 120 },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 90 },
    { title: '产品名称', dataIndex: 'productName', width: 150, ellipsis: true },
    { title: '原料', dataIndex: 'rawMaterial', width: 140, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 85 },
    { title: '规格', dataIndex: 'productSpec', width: 100 },
    { title: '客户', dataIndex: 'customerName', width: 120, ellipsis: true },
    { title: '计重方式', dataIndex: 'weightMode', width: 90 },
    { title: '了批', dataIndex: 'isFinished', width: 75, render: (v) => <BoolTag value={v} /> },
    { title: '交期', dataIndex: 'deliveryDate', width: 95 },
  ]

  return (
    <div className="page">
      <div className="sec-title">包装任务</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="taskNo" label="任务号"><Input allowClear placeholder="PS..." style={{ width: 125 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="warehouseBatchNo" label="入库批号"><Input allowClear placeholder="入库批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="isFinished" label="了批状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={[{ label: '进行中', value: 0 }, { label: '已了批', value: 1 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
          <Button
            type="primary"
            disabled={!sel.length}
            onClick={() => Modal.confirm({
              title: '包装了批',
              content: `已选 ${sel.length} 条。了批后包装任务封账。确认？`,
              onOk: async () => { await finishPackTasks(sel.map((r) => r.id)); message.success('了批成功'); setSel([]); reload() },
            })}
          >
            了批
          </Button>
          <Button
            disabled={!sel.length}
            onClick={() => Modal.confirm({
              title: '取消了批',
              content: `已选 ${sel.length} 条。取消后任务恢复进行中。确认？`,
              onOk: async () => { await cancelFinishPackTasks(sel.map((r) => r.id)); message.success('已取消了批'); setSel([]); reload() },
            })}
          >
            取消了批
          </Button>
          <span className="muted" style={{ alignSelf: 'center' }}>勾选 {sel.length} 条</span>
        </div>
        <Table
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={rows}
          loading={loading}
          pagination={pagination}
          scroll={{ x: 1550 }}
          rowSelection={{ selectedRowKeys: sel.map((r) => r.id), onChange: (_, rows2) => setSel(rows2) }}
        />
      </Card>
    </div>
  )
}
