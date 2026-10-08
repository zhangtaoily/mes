import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { getEquipments } from '@/api/plan'
import type { BaseEquipment } from '@/api/plan'
import { pageDutyMachines, saveDutyMachine, deleteDutyMachine } from '@/api/equip'
import type { EqDutyMachine } from '@/api/equip'

/** 责任机台：车间机台责任人划分 */
export default function DutyMachine() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<EqDutyMachine>('/api/equip/duty-machines')
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()
  const [editing, setEditing] = useState<EqDutyMachine | null>(null)
  const [open, setOpen] = useState(false)
  const [equipments, setEquipments] = useState<BaseEquipment[]>([])

  const loadEquipments = (ws?: number) => {
    if (ws) {
      getEquipments(ws).then(setEquipments).catch(() => setEquipments([]))
    } else {
      setEquipments([])
    }
  }

  const openForm = (row?: EqDutyMachine) => {
    setEditing(row ?? null)
    editForm.resetFields()
    if (row?.workshopId) loadEquipments(row.workshopId)
    editForm.setFieldsValue(row ?? {})
    setOpen(true)
  }

  const submit = async (v: Partial<EqDutyMachine>) => {
    await saveDutyMachine({ ...v, id: editing?.id })
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<EqDutyMachine> = [
    { title: '车间', dataIndex: 'workshopId', width: 130, fixed: 'left', render: (v) => wsName[v] ?? v ?? '-' },
    { title: '责任人', dataIndex: 'checkUser', width: 110 },
    { title: '工号', dataIndex: 'empNo', width: 100, render: (v) => v ?? '-' },
    { title: '机台', dataIndex: 'equipmentId', width: 110, render: (v) => (v != null ? equipments.find((e) => e.id === v)?.machineNo ?? `#${v}` : '-') },
    { title: '备注', dataIndex: 'remark', width: 220, ellipsis: true, render: (v) => v ?? '-' },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', width: 110, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除该责任机台？" onConfirm={async () => { await deleteDutyMachine(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">责任机台</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 140 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="checkUser" label="责任人"><Input allowClear placeholder="姓名" style={{ width: 110 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1000 }} />
      </Card>

      <Modal title={editing ? '编辑责任机台' : '新增责任机台'} open={open} onCancel={() => setOpen(false)} onOk={() => editForm.submit()} destroyOnClose>
        <Form form={editForm} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="workshopId" label="车间">
              <Select allowClear showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} onChange={loadEquipments} />
            </Form.Item>
            <Form.Item name="equipmentId" label="机台" rules={[{ required: true, message: '请选择机台' }]}>
              <Select showSearch optionFilterProp="label" placeholder="先选车间" options={equipments.map((e) => ({ label: e.machineNo, value: e.id }))} />
            </Form.Item>
            <Form.Item name="checkUser" label="责任人" rules={[{ required: true, message: '请输入责任人' }]}>
              <Input />
            </Form.Item>
            <Form.Item name="empNo" label="工号"><Input /></Form.Item>
          </div>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
