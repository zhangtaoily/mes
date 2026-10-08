import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useProcesses, useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pageWsCrafts, saveWsCraft, updateWsCraft, deleteWsCraft, wsParamApi } from '@/api/craft'
import type { WsCraft, CraftParam } from '@/api/craft'
import { BoolTag } from '@/components/StatusTag'

/** 车间工艺库（工序>机型>工艺项，工艺配置引用源2：工艺项机型必填） */
export default function WsCraft() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { nameMap: procName, options: procOptions } = useProcesses()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<WsCraft>('/api/craft/ws-crafts')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<WsCraft | null>(null)
  const [open, setOpen] = useState(false)
  const [paramRow, setParamRow] = useState<WsCraft | null>(null)
  const [params, setParams] = useState<CraftParam[]>([])
  const [paramOpen, setParamOpen] = useState(false)
  const [paramForm] = Form.useForm()

  const openForm = (row?: WsCraft) => {
    setEditing(row ?? null)
    form.resetFields()
    form.setFieldsValue(row ?? { enabled: 1 })
    setOpen(true)
  }

  const submit = async (v: Partial<WsCraft>) => {
    if (editing) await updateWsCraft(editing.id, v)
    else await saveWsCraft(v)
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const openParams = async (row: WsCraft) => {
    setParamRow(row)
    setParams(await wsParamApi.list(row.id))
    paramForm.resetFields()
    setParamOpen(true)
  }

  const columns: ColumnsType<WsCraft> = [
    { title: '车间工艺名称', dataIndex: 'craftName', width: 190, fixed: 'left', ellipsis: true },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '产线', dataIndex: 'prodLine', width: 130, ellipsis: true, render: (v) => v ?? '-' },
    { title: '原料明细', dataIndex: 'rawMaterial', width: 200, ellipsis: true, render: (v) => v ?? '-' },
    { title: '实纺支数', dataIndex: 'actualCount', width: 85, align: 'right' },
    { title: '捻度', dataIndex: 'twist', width: 75, align: 'right' },
    { title: '染色方式', dataIndex: 'dyeMode', width: 85, render: (v) => v ?? '-' },
    { title: '工艺等级', dataIndex: 'craftLevel', width: 80, render: (v) => v ?? '-' },
    { title: '小时产能', dataIndex: 'hourCapacity', width: 85, align: 'right' },
    { title: '启用', dataIndex: 'enabled', width: 65, render: (v) => <BoolTag value={v} /> },
    {
      title: '操作', width: 180, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openParams(r)}>工艺参数</a>
          <a style={{ marginLeft: 8 }} onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除后引用该工艺的配置将无法带出参数，确认？" onConfirm={async () => { await deleteWsCraft(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const paramColumns: ColumnsType<CraftParam> = [
    { title: '工序', dataIndex: 'processId', width: 100, render: (v) => procName[v] ?? '-' },
    { title: '机型', dataIndex: 'machineModel', width: 120 },
    { title: '工艺项', dataIndex: 'paramName', width: 100 },
    { title: '工艺值', dataIndex: 'paramValue', width: 95 },
    { title: '下限', dataIndex: 'lowerLimit', width: 75, render: (v) => v ?? '-' },
    { title: '上限', dataIndex: 'upperLimit', width: 75, render: (v) => v ?? '-' },
    { title: '单位', dataIndex: 'unit', width: 70, render: (v) => v ?? '-' },
    { title: '排序', dataIndex: 'sortNo', width: 60, align: 'right' },
    {
      title: '操作', width: 70,
      render: (_, p) => (
        <Popconfirm title="删除该工艺参数？" onConfirm={async () => { await wsParamApi.remove(paramRow!.id, p.id); setParams(await wsParamApi.list(paramRow!.id)) }}>
          <a style={{ color: '#f56c6c' }}>删除</a>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间工艺库</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="craftName" label="工艺名称"><Input allowClear placeholder="名称" style={{ width: 150 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="enabled" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 90 }} options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增工艺</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>

      <Modal
        title={editing ? `编辑车间工艺 - ${editing.craftName}` : '新增车间工艺'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <Form.Item name="craftName" label="车间工艺名称" rules={[{ required: true, message: '请输入工艺名称' }]}>
            <Input placeholder="如 纺十车间澳毛复精梳工艺" />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="workshopId" label="所属车间" rules={[{ required: true, message: '请选择车间' }]}>
              <Select showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} />
            </Form.Item>
            <Form.Item name="prodLine" label="产线"><Input placeholder="如 纺十车间2路" /></Form.Item>
            <Form.Item name="rawMaterial" label="原料明细"><Input placeholder="如 70% 澳毛条 HY-M63634-中灰" /></Form.Item>
            <Form.Item name="actualCount" label="实纺支数"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="twist" label="捻度"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="dyeMode" label="染色方式"><Input placeholder="如 条染" /></Form.Item>
            <Form.Item name="craftLevel" label="工艺等级"><Input placeholder="如 B" /></Form.Item>
            <Form.Item name="hourCapacity" label="小时产能"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
            <Form.Item name="enabled" label="状态">
              <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
            </Form.Item>
          </div>
        </Form>
      </Modal>

      <Modal
        title={`工艺参数 - ${paramRow?.craftName ?? ''}（车间库，按工序+机型）`}
        open={paramOpen}
        onCancel={() => setParamOpen(false)}
        footer={null}
        width={880}
        destroyOnClose
      >
        <Form
          form={paramForm}
          layout="inline"
          style={{ marginBottom: 10 }}
          onFinish={async (v) => {
            await wsParamApi.add(paramRow!.id, v)
            message.success('已添加')
            paramForm.resetFields()
            setParams(await wsParamApi.list(paramRow!.id))
          }}
        >
          <Form.Item name="processId" rules={[{ required: true, message: '选工序' }]}>
            <Select showSearch optionFilterProp="label" placeholder="工序" style={{ width: 105 }} options={procOptions} />
          </Form.Item>
          <Form.Item name="machineModel" rules={[{ required: true, message: '输入机型' }]}>
            <Input placeholder="机型：GC40..." style={{ width: 110 }} />
          </Form.Item>
          <Form.Item name="paramName" rules={[{ required: true, message: '输入工艺项' }]}>
            <Input placeholder="工艺项" style={{ width: 110 }} />
          </Form.Item>
          <Form.Item name="paramValue"><Input placeholder="工艺值" style={{ width: 85 }} /></Form.Item>
          <Form.Item name="lowerLimit"><Input placeholder="下限" style={{ width: 65 }} /></Form.Item>
          <Form.Item name="upperLimit"><Input placeholder="上限" style={{ width: 65 }} /></Form.Item>
          <Form.Item name="unit"><Input placeholder="单位" style={{ width: 65 }} /></Form.Item>
          <Form.Item name="sortNo"><InputNumber placeholder="排序" style={{ width: 70 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">添加</Button></Form.Item>
        </Form>
        <Table rowKey="id" size="small" columns={paramColumns} dataSource={params} pagination={false} scroll={{ x: 740 }} />
      </Modal>
    </div>
  )
}
