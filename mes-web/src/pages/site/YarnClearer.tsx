import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { getEquipments } from '@/api/plan'
import { pageCuts, saveCut, deleteCut } from '@/api/site'
import type { RepAutoWindingCut } from '@/api/site'

const CUT_TYPES = ['电容', '光电']

/** 自络筒切疵记录（自动络筒清纱器切除疵点） */
export default function YarnClearer() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<RepAutoWindingCut>('/api/site/yarn-clearer')
  const [form] = Form.useForm()
  const [open, setOpen] = useState(false)
  const [eqOptions, setEqOptions] = useState<{ label: string; value: number }[]>([])
  const eqName: Record<number, string> = {}
  eqOptions.forEach((e) => (eqName[e.value] = e.label))

  const openForm = () => { setOpen(true); form.resetFields() }

  const columns: ColumnsType<RepAutoWindingCut> = [
    { title: '批号', dataIndex: 'batchNo', width: 130 },
    { title: '原料', dataIndex: 'rawMaterial', width: 160, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 100 },
    { title: '支数', dataIndex: 'countSpec', width: 90 },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '机台', dataIndex: 'equipmentId', width: 110, render: (v) => eqName[v] ?? v ?? '-' },
    { title: '切疵类型', dataIndex: 'cutType', width: 90 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', width: 80,
      render: (_, r) => (
        <Popconfirm title="确认删除该切疵记录？" onConfirm={async () => { await deleteCut(r.id); message.success('已删除'); reload() }}>
          <a style={{ color: '#f56c6c' }}>删除</a>
        </Popconfirm>
      ),
    },
  ]

  const wsChange = (ws?: number) => {
    if (ws) getEquipments(ws).then((l) => setEqOptions(l.map((e) => ({ label: e.machineNo, value: e.id }))))
    else setEqOptions([])
  }

  return (
    <div className="page">
      <div className="sec-title">自络筒切疵记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} onChange={wsChange} />
          </Form.Item>
          <Form.Item name="batchNo" label="批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={openForm}>新增切疵记录</Button>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title="新增切疵记录" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveCut(v); message.success('保存成功'); setOpen(false); reload()
        }}>
          <Form.Item name="batchNo" label="批号" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="rawMaterial" label="原料"><Input /></Form.Item>
          <Form.Item name="colorNo" label="色号"><Input /></Form.Item>
          <Form.Item name="countSpec" label="支数"><Input placeholder="1/40" /></Form.Item>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} onChange={wsChange} />
          </Form.Item>
          <Form.Item name="equipmentId" label="机台">
            <Select allowClear showSearch optionFilterProp="label" options={eqOptions} />
          </Form.Item>
          <Form.Item name="cutType" label="切疵类型" rules={[{ required: true }]}>
            <Select options={CUT_TYPES.map((t) => ({ label: t, value: t }))} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
