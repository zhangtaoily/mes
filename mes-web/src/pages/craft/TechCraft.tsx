import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useProcesses } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pageTechCrafts, saveTechCraft, updateTechCraft, deleteTechCraft, techParamApi } from '@/api/craft'
import type { TechCraft, CraftParam } from '@/api/craft'
import { BoolTag } from '@/components/StatusTag'

const CRAFT_LEVELS = ['A', 'B', 'C'].map((s) => ({ label: s, value: s }))
const DYE_MODES = ['条染', '绞染', '散毛染', '筒子染'].map((s) => ({ label: s, value: s }))

/** 技术工艺库（基础工艺模板，工艺配置引用源1：工艺项机型为空） */
export default function TechCraft() {
  const { message } = App.useApp()
  const { nameMap: procName, options: procOptions } = useProcesses()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<TechCraft>('/api/craft/tech-crafts')
  const [form] = Form.useForm()
  const [editing, setEditing] = useState<TechCraft | null>(null)
  const [open, setOpen] = useState(false)
  const [paramRow, setParamRow] = useState<TechCraft | null>(null)
  const [params, setParams] = useState<CraftParam[]>([])
  const [paramOpen, setParamOpen] = useState(false)
  const [paramForm] = Form.useForm()

  const openForm = (row?: TechCraft) => {
    setEditing(row ?? null)
    form.resetFields()
    form.setFieldsValue(row ?? { enabled: 1 })
    setOpen(true)
  }

  const submit = async (v: Partial<TechCraft>) => {
    if (editing) await updateTechCraft(editing.id, v)
    else await saveTechCraft(v)
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const openParams = async (row: TechCraft) => {
    setParamRow(row)
    setParams(await techParamApi.list(row.id))
    paramForm.resetFields()
    setParamOpen(true)
  }

  const columns: ColumnsType<TechCraft> = [
    { title: '工艺编号', dataIndex: 'craftCode', width: 110, fixed: 'left' },
    { title: '工艺名称', dataIndex: 'craftName', width: 170, ellipsis: true },
    { title: '成分', dataIndex: 'component', width: 130, ellipsis: true, render: (v) => v ?? '-' },
    { title: '实纺支数', dataIndex: 'actualCount', width: 85, align: 'right' },
    { title: '工艺等级', dataIndex: 'craftLevel', width: 80, render: (v) => v ?? '-' },
    { title: '染色方式', dataIndex: 'dyeMode', width: 85, render: (v) => v ?? '-' },
    { title: '捻度', dataIndex: 'twist', width: 80, align: 'right' },
    { title: '启用', dataIndex: 'enabled', width: 70, render: (v) => <BoolTag value={v} /> },
    { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true, render: (v) => v ?? '-' },
    { title: '更新时间', dataIndex: 'updateTime', width: 160 },
    {
      title: '操作', width: 180, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openParams(r)}>工艺参数</a>
          <a style={{ marginLeft: 8 }} onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除后引用该工艺的配置将无法带出参数，确认？" onConfirm={async () => { await deleteTechCraft(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const paramColumns: ColumnsType<CraftParam> = [
    { title: '工序', dataIndex: 'processId', width: 100, render: (v) => procName[v] ?? '-' },
    { title: '工艺项', dataIndex: 'paramName', width: 110 },
    { title: '工艺值', dataIndex: 'paramValue', width: 100 },
    { title: '下限', dataIndex: 'lowerLimit', width: 80, render: (v) => v ?? '-' },
    { title: '上限', dataIndex: 'upperLimit', width: 80, render: (v) => v ?? '-' },
    { title: '单位', dataIndex: 'unit', width: 70, render: (v) => v ?? '-' },
    { title: '排序', dataIndex: 'sortNo', width: 60, align: 'right' },
    {
      title: '操作', width: 70,
      render: (_, p) => (
        <Popconfirm title="删除该工艺参数？" onConfirm={async () => { await techParamApi.remove(paramRow!.id, p.id); setParams(await techParamApi.list(paramRow!.id)) }}>
          <a style={{ color: '#f56c6c' }}>删除</a>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">技术工艺库</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="craftCode" label="工艺编号"><Input allowClear placeholder="CT..." style={{ width: 110 }} /></Form.Item>
          <Form.Item name="craftName" label="工艺名称"><Input allowClear placeholder="名称" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="enabled" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 90 }} options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增工艺</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1300 }} />
      </Card>

      <Modal
        title={editing ? `编辑技术工艺 - ${editing.craftName}` : '新增技术工艺'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        width={620}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="craftCode" label="工艺编号" rules={[{ required: true, message: '请输入工艺编号' }]}>
              <Input placeholder="如 CT000006" />
            </Form.Item>
            <Form.Item name="craftName" label="工艺名称" rules={[{ required: true, message: '请输入工艺名称' }]}>
              <Input placeholder="如 全毛细纱标准工艺" />
            </Form.Item>
            <Form.Item name="component" label="成分"><Input placeholder="如 100%澳毛" /></Form.Item>
            <Form.Item name="actualCount" label="实纺支数"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="craftLevel" label="工艺等级"><Select allowClear options={CRAFT_LEVELS} /></Form.Item>
            <Form.Item name="dyeMode" label="染色方式"><Select allowClear options={DYE_MODES} /></Form.Item>
            <Form.Item name="twist" label="捻度"><InputNumber style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="enabled" label="状态">
              <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
            </Form.Item>
          </div>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`工艺参数 - ${paramRow?.craftName ?? ''}（技术库）`}
        open={paramOpen}
        onCancel={() => setParamOpen(false)}
        footer={null}
        width={820}
        destroyOnClose
      >
        <Form
          form={paramForm}
          layout="inline"
          style={{ marginBottom: 10 }}
          onFinish={async (v) => {
            await techParamApi.add(paramRow!.id, v)
            message.success('已添加')
            paramForm.resetFields()
            setParams(await techParamApi.list(paramRow!.id))
          }}
        >
          <Form.Item name="processId" rules={[{ required: true, message: '选工序' }]}>
            <Select showSearch optionFilterProp="label" placeholder="工序" style={{ width: 110 }} options={procOptions} />
          </Form.Item>
          <Form.Item name="paramName" rules={[{ required: true, message: '输入工艺项' }]}>
            <Input placeholder="工艺项：车速/定量..." style={{ width: 130 }} />
          </Form.Item>
          <Form.Item name="paramValue"><Input placeholder="工艺值" style={{ width: 90 }} /></Form.Item>
          <Form.Item name="lowerLimit"><Input placeholder="下限" style={{ width: 70 }} /></Form.Item>
          <Form.Item name="upperLimit"><Input placeholder="上限" style={{ width: 70 }} /></Form.Item>
          <Form.Item name="unit"><Input placeholder="单位" style={{ width: 70 }} /></Form.Item>
          <Form.Item name="sortNo"><InputNumber placeholder="排序" style={{ width: 75 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">添加</Button></Form.Item>
        </Form>
        <Table rowKey="id" size="small" columns={paramColumns} dataSource={params} pagination={false} scroll={{ x: 700 }} />
      </Modal>
    </div>
  )
}
