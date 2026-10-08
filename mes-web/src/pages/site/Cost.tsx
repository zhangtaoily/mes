import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { pageCost, saveCost, submitCost, withdrawCost, deleteCost } from '@/api/site'
import type { StCost } from '@/api/site'

/** 车间生产费用（月度费用编制：编制中→已提交，可撤回） */
export default function Cost() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<StCost>('/api/site/cost')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const openForm = (r?: StCost) => {
    setOpen(true)
    form.resetFields()
    if (r) form.setFieldsValue({ ...r })
    else form.setFieldsValue({ periodMonth: dayjs().format('YYYY-MM') })
  }

  const columns: ColumnsType<StCost> = [
    { title: '期间', dataIndex: 'periodMonth', width: 85, fixed: 'left' },
    { title: '车间', dataIndex: 'workshopId', width: 110, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '用水量', dataIndex: 'waterUsage', width: 85, align: 'right' },
    { title: '水费', dataIndex: 'waterCost', width: 90, align: 'right' },
    { title: '用电量', dataIndex: 'powerUsage', width: 85, align: 'right' },
    { title: '电费', dataIndex: 'powerCost', width: 90, align: 'right' },
    { title: '机物料', dataIndex: 'machineMaterialCost', width: 90, align: 'right' },
    { title: '包装费', dataIndex: 'packCost', width: 90, align: 'right' },
    { title: '月折旧', dataIndex: 'monthDepreciation', width: 90, align: 'right' },
    { title: '加工费分摊', dataIndex: 'processCostShare', width: 100, align: 'right' },
    { title: '工资', dataIndex: 'salary', width: 90, align: 'right' },
    { title: '月度考核', dataIndex: 'monthKpi', width: 90, align: 'right' },
    { title: '折算产量', dataIndex: 'convertOutput', width: 90, align: 'right' },
    { title: '用毛率', dataIndex: 'woolRate', width: 85, align: 'right' },
    {
      title: '状态', dataIndex: 'docStatus', width: 80,
      render: (v) => v === '已提交' ? <span style={{ color: '#1677ff' }}>{v}</span> : v ?? '-',
    },
    {
      title: '操作', width: 190, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          {r.docStatus === '编制中' && (
            <a style={{ margin: '0 8px' }} onClick={async () => { await submitCost(r.id); message.success('已提交'); reload() }}>提交</a>
          )}
          {r.docStatus === '已提交' && (
            <a style={{ margin: '0 8px' }} onClick={async () => { await withdrawCost(r.id); message.success('已撤回'); reload() }}>撤回</a>
          )}
          <Popconfirm title="确认删除该费用单？" onConfirm={async () => { await deleteCost(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const num = (name: keyof StCost, label: string, precision = 2) => (
    <Form.Item name={name as string} label={label}>
      <InputNumber style={{ width: '100%' }} min={0} precision={precision} />
    </Form.Item>
  )

  return (
    <div className="page">
      <div className="sec-title">车间生产费用</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="periodMonth" label="期间"><Input allowClear placeholder="YYYY-MM" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={['编制中', '已提交', '审核通过', '驳回'].map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => openForm()}>新增费用单</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>期间+车间唯一</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1700 }} />
      </Card>

      <Modal title={form.getFieldValue('id') ? '编辑费用单' : '新增费用单'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose width={620}>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveCost(v)
          message.success('保存成功')
          setOpen(false); reload()
        }}>
          <Form.Item name="id" hidden><InputNumber /></Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="periodMonth" label="期间(月)" rules={[{ required: true }]}><Input placeholder="2026-10" /></Form.Item>
            <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
              <Select showSearch optionFilterProp="label" options={wsOptions} />
            </Form.Item>
            {num('waterUsage', '用水量', 3)}
            {num('waterCost', '用水费用')}
            {num('powerUsage', '用电量', 3)}
            {num('powerCost', '用电费用')}
            {num('machineMaterialCost', '机物料')}
            {num('packCost', '包装费用')}
            {num('monthDepreciation', '月折旧')}
            {num('processCostShare', '加工费分摊')}
            {num('salary', '工资')}
            {num('monthKpi', '月度考核指标')}
            {num('convertOutput', '折算产量', 3)}
            {num('woolRate', '用毛率', 3)}
          </div>
        </Form>
      </Modal>
    </div>
  )
}
