import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { getEquipments } from '@/api/plan'
import { pageEnergy, saveEnergy, submitEnergy, withdrawEnergy, deleteEnergy, getEnergyItems, addEnergyItems, deleteEnergyItem } from '@/api/site'
import type { StEnergy, StEnergyItem } from '@/api/site'

/** 车间能耗（月度：编制中→已提交，机台明细子表） */
export default function Energy() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<StEnergy>('/api/site/energy')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const [energyId, setEnergyId] = useState<number | null>(null)
  const [items, setItems] = useState<StEnergyItem[]>([])
  const [itemsLoading, setItemsLoading] = useState(false)
  const [itemForm] = Form.useForm()
  const [eqOptions, setEqOptions] = useState<{ label: string; value: number }[]>([])

  const loadItems = async (r: StEnergy) => {
    setEnergyId(r.id)
    setItemsLoading(true)
    try {
      setItems(await getEnergyItems(r.id))
      if (r.workshopId) {
        getEquipments(r.workshopId).then((l) => setEqOptions(l.map((e) => ({ label: e.machineNo, value: e.id }))))
      }
    } finally {
      setItemsLoading(false)
    }
  }

  const columns: ColumnsType<StEnergy> = [
    { title: '期间', dataIndex: 'periodMonth', width: 90 },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '用水(吨)', dataIndex: 'waterUsage', width: 100, align: 'right' },
    { title: '用电(KWh)', dataIndex: 'powerUsage', width: 110, align: 'right' },
    { title: '用气(m³)', dataIndex: 'gasUsage', width: 100, align: 'right' },
    {
      title: '状态', dataIndex: 'docStatus', width: 80,
      render: (v) => v === '已提交' ? <span style={{ color: '#1677ff' }}>{v}</span> : v ?? '-',
    },
    {
      title: '操作', width: 250,
      render: (_, r) => (
        <>
          <a onClick={() => { setOpen(true); form.resetFields(); form.setFieldsValue(r) }}>编辑</a>
          <a style={{ margin: '0 8px' }} onClick={() => loadItems(r)}>明细</a>
          {r.docStatus === '编制中' && (
            <a style={{ marginRight: 8 }} onClick={async () => { await submitEnergy(r.id); message.success('已提交'); reload() }}>提交</a>
          )}
          {r.docStatus === '已提交' && (
            <a style={{ marginRight: 8 }} onClick={async () => { await withdrawEnergy(r.id); message.success('已撤回'); reload() }}>撤回</a>
          )}
          <Popconfirm title="确认删除该能耗单（含明细）？" onConfirm={async () => { await deleteEnergy(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const itemColumns: ColumnsType<StEnergyItem> = [
    { title: '机台', dataIndex: 'equipmentId', width: 110, render: (v) => eqOptions.find((e) => e.value === v)?.label ?? v ?? '-' },
    { title: '用电(KWh)', dataIndex: 'powerUsage', width: 110, align: 'right' },
    { title: '用水(吨)', dataIndex: 'waterUsage', width: 100, align: 'right' },
    { title: '用气(m³)', dataIndex: 'gasUsage', width: 100, align: 'right' },
    {
      title: '操作', width: 80,
      render: (_, it) => (
        <Popconfirm title="删除该明细？" onConfirm={async () => { await deleteEnergyItem(it.id); message.success('已删除'); if (energyId) setItems(await getEnergyItems(energyId)) }}>
          <a style={{ color: '#f56c6c' }}>删除</a>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间能耗</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="periodMonth" label="期间"><Input allowClear placeholder="YYYY-MM" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={['编制中', '已提交', '审核通过', '已撤回'].map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => { setOpen(true); form.resetFields(); form.setFieldsValue({ periodMonth: dayjs().format('YYYY-MM') }) }}>新增能耗</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>期间+车间唯一</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title={form.getFieldValue('id') ? '编辑能耗' : '新增能耗'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveEnergy(v)
          message.success('保存成功')
          setOpen(false); reload()
        }}>
          <Form.Item name="id" hidden><InputNumber /></Form.Item>
          <Form.Item name="periodMonth" label="期间(月)" rules={[{ required: true }]}><Input placeholder="2026-10" /></Form.Item>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="waterUsage" label="用水(吨)"><InputNumber style={{ width: '100%' }} min={0} precision={3} /></Form.Item>
          <Form.Item name="powerUsage" label="用电(KWh)"><InputNumber style={{ width: '100%' }} min={0} precision={3} /></Form.Item>
          <Form.Item name="gasUsage" label="用气(m³)"><InputNumber style={{ width: '100%' }} min={0} precision={3} /></Form.Item>
        </Form>
      </Modal>

      <Modal title="机台能耗明细" open={energyId !== null} onCancel={() => setEnergyId(null)} footer={null} destroyOnClose>
        <Form form={itemForm} layout="inline" style={{ marginBottom: 10 }} onFinish={async (v) => {
          if (!energyId) return
          await addEnergyItems(energyId, [v])
          message.success('已添加')
          itemForm.resetFields()
          setItems(await getEnergyItems(energyId))
        }}>
          <Form.Item name="equipmentId" rules={[{ required: true }]} style={{ marginBottom: 0 }}>
            <Select showSearch optionFilterProp="label" placeholder="机台" style={{ width: 130 }} options={eqOptions} />
          </Form.Item>
          <Form.Item name="powerUsage" style={{ marginBottom: 0, marginLeft: 8 }}>
            <InputNumber placeholder="用电" min={0} precision={3} style={{ width: 100 }} />
          </Form.Item>
          <Form.Item name="waterUsage" style={{ marginBottom: 0, marginLeft: 8 }}>
            <InputNumber placeholder="用水" min={0} precision={3} style={{ width: 100 }} />
          </Form.Item>
          <Form.Item name="gasUsage" style={{ marginBottom: 0, marginLeft: 8 }}>
            <InputNumber placeholder="用气" min={0} precision={3} style={{ width: 100 }} />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, marginLeft: 8 }}>
            <Button type="primary" htmlType="submit">添加</Button>
          </Form.Item>
        </Form>
        <Table rowKey="id" size="small" columns={itemColumns} dataSource={items} loading={itemsLoading} pagination={false} />
      </Modal>
    </div>
  )
}
