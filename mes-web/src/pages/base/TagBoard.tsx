import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useProcesses } from '@/hooks/useOptions'
import { BoolTag } from '@/components/StatusTag'
import { pageBatchFlags, saveBatchFlag, updateBatchFlag, deleteBatchFlag } from '@/api/base'
import type { BaseBatchFlagFull } from '@/api/base'

/** 标识名→颜色（筒贴/箱单打印与现场周转识别） */
const FLAG_COLORS: Record<string, string> = {
  紫: 'purple', 蓝: 'blue', 橙: 'orange', 粉: 'magenta', 红: 'red', 绿: 'green', 黄: 'gold',
}
const flagColor = (name?: string) => {
  if (!name) return 'default'
  const key = Object.keys(FLAG_COLORS).find((c) => name.includes(c))
  return key ? FLAG_COLORS[key] : 'default'
}

/** 标识牌（批号标识色块）维护 */
export default function TagBoard() {
  const { message } = App.useApp()
  const { nameMap: procName, options: procOptions } = useProcesses()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<BaseBatchFlagFull>('/api/base/batch-flags')
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<BaseBatchFlagFull | null>(null)

  const openForm = (r?: BaseBatchFlagFull) => {
    setEditing(r ?? null)
    setOpen(true)
    form.resetFields()
    if (r) form.setFieldsValue(r)
  }

  const columns: ColumnsType<BaseBatchFlagFull> = [
    { title: '标识名', dataIndex: 'flagName', width: 120, render: (v) => <Tag color={flagColor(v)}>{v}</Tag> },
    { title: '别名', dataIndex: 'alias', width: 120 },
    { title: '适用工序', dataIndex: 'processId', width: 120, render: (v) => procName[v] ?? (v ?? '-') },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    { title: '预览', dataIndex: 'flagName', width: 90, render: (v) => <Tag color={flagColor(v)}>色块</Tag> },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="确认删除该标识牌？" onConfirm={async () => { await deleteBatchFlag(r.id); message.success('已删除'); reload() }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">标识牌</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="flagName" label="标识名"><Input allowClear placeholder="紫叶子" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="processId" label="适用工序">
            <Select allowClear placeholder="全部" style={{ width: 130 }} showSearch optionFilterProp="label" options={procOptions} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => openForm()}>新增标识牌</Button>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
        <div className="muted" style={{ fontSize: 12, padding: '6px 2px' }}>
          批号标识色块用于筒贴/箱单打印与现场周转识别；同车间内并行批次分配不同标识防混淆
        </div>
      </Card>

      <Modal title={editing ? `编辑标识牌 - ${editing.flagName}` : '新增标识牌'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editing) { await updateBatchFlag(editing.id, v); message.success('修改成功') }
          else { await saveBatchFlag(v); message.success('新增成功') }
          setOpen(false); reload()
        }}>
          <Form.Item name="flagName" label="标识名" rules={[{ required: true }]}><Input placeholder="紫叶子" /></Form.Item>
          <Form.Item name="alias" label="别名"><Input placeholder="紫叶标" /></Form.Item>
          <Form.Item name="processId" label="适用工序">
            <Select allowClear showSearch optionFilterProp="label" options={procOptions} />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
