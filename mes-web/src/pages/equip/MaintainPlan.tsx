import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pageMaintainPlans, saveMaintainPlan, deleteMaintainPlan, CYCLE_TYPES } from '@/api/equip'
import type { EqMaintainPlan } from '@/api/equip'

/** 保养计划：周期规则配置（日/周/月） */
export default function MaintainPlan() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<EqMaintainPlan>('/api/equip/maintain-plans')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<EqMaintainPlan | null>(null)
  const [open, setOpen] = useState(false)

  const openForm = (row?: EqMaintainPlan) => {
    setEditing(row ?? null)
    form.resetFields()
    form.setFieldsValue(row ?? { cycleType: 1, cycleValue: 1 })
    setOpen(true)
  }

  const submit = async (v: Partial<EqMaintainPlan>) => {
    await saveMaintainPlan({ ...v, id: editing?.id })
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<EqMaintainPlan> = [
    { title: '计划编码', dataIndex: 'planCode', width: 130, fixed: 'left' },
    { title: '保养名称', dataIndex: 'planName', width: 200, ellipsis: true },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '周期', dataIndex: 'cycleType', width: 90, render: (v, r) => `${CYCLE_TYPES.find((c) => c.value === v)?.label ?? v}${r.cycleValue ?? ''}` },
    { title: '备注', dataIndex: 'remark', width: 200, ellipsis: true, render: (v) => v ?? '-' },
    { title: '更新时间', dataIndex: 'updateTime', width: 160 },
    {
      title: '操作', width: 110, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除该保养计划？" onConfirm={async () => { await deleteMaintainPlan(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">保养计划</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="planCode" label="计划编码"><Input allowClear placeholder="编码" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 140 }} options={wsOptions} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增计划</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1050 }} />
      </Card>

      <Modal title={editing ? `编辑保养计划 - ${editing.planCode}` : '新增保养计划'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="planCode" label="计划编码" rules={[{ required: true, message: '请输入计划编码' }]}>
            <Input placeholder="如 BYJH-001" />
          </Form.Item>
          <Form.Item name="planName" label="保养名称"><Input placeholder="如 细纱机月度保养" /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="cycleType" label="周期类型">
              <Select options={CYCLE_TYPES} />
            </Form.Item>
            <Form.Item name="cycleValue" label="周期值"><InputNumber style={{ width: '100%' }} min={1} /></Form.Item>
          </div>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
