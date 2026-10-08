import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { BoolTag } from '@/components/StatusTag'
import { pageMachines, saveMachine, updateMachine, deleteMachine } from '@/api/base'
import type { BaseEquipmentFull } from '@/api/base'

/** 设备信息（机台）维护 */
export default function MachineInfo() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<BaseEquipmentFull>('/api/base/machines')
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<BaseEquipmentFull | null>(null)

  const openForm = (r?: BaseEquipmentFull) => {
    setEditing(r ?? null)
    setOpen(true)
    form.resetFields()
    if (r) form.setFieldsValue(r)
  }

  const columns: ColumnsType<BaseEquipmentFull> = [
    { title: '所属车间', dataIndex: 'workshopId', width: 180, render: (v) => wsName[v] ?? v },
    { title: '机型', dataIndex: 'machineModel', width: 130 },
    { title: '机台名称', dataIndex: 'machineNo', width: 140 },
    { title: '小时产能(KG/h)', dataIndex: 'hourCapacity', width: 120, align: 'right' },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="确认删除该设备？" onConfirm={async () => { await deleteMachine(r.id); message.success('已删除'); reload() }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">设备信息</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="所属车间">
            <Select allowClear placeholder="全部" style={{ width: 180 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="machineNo" label="机台名称"><Input allowClear placeholder="110倍捻12" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="machineModel" label="机型"><Input allowClear placeholder="VTS-08" style={{ width: 110 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => openForm()}>新增设备</Button>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title={editing ? `编辑设备 - ${editing.machineNo}` : '新增设备'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editing) { await updateMachine(editing.id, v); message.success('修改成功') }
          else { await saveMachine(v); message.success('新增成功') }
          setOpen(false); reload()
        }}>
          <Form.Item name="workshopId" label="所属车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="machineModel" label="机型"><Input placeholder="VTS-08" /></Form.Item>
          <Form.Item name="machineNo" label="机台名称" rules={[{ required: true }]}><Input placeholder="110倍捻12" /></Form.Item>
          <Form.Item name="hourCapacity" label="小时产能(KG/h)"><InputNumber style={{ width: '100%' }} min={0} precision={3} /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
