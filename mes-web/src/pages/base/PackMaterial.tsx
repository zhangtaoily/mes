import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { BoolTag } from '@/components/StatusTag'
import {
  pagePackMaterials, savePackMaterial, updatePackMaterial, deletePackMaterial,
  pagePrintTemplates, savePrintTemplate, updatePrintTemplate, deletePrintTemplate, TEMPLATE_TYPES,
} from '@/api/base'
import type { BasePackMaterial, BasePrintTemplate } from '@/api/base'

/** 包装与打印：包装材质 + 打印模板 */
export default function PackMaterial() {
  const { message } = App.useApp()
  const mat = usePagedList<BasePackMaterial>('/api/base/pack-materials')
  const tpl = usePagedList<BasePrintTemplate>('/api/base/print-templates')
  const [matForm] = Form.useForm()
  const [tplForm] = Form.useForm()
  const [matOpen, setMatOpen] = useState(false)
  const [tplOpen, setTplOpen] = useState(false)
  const [editingMat, setEditingMat] = useState<BasePackMaterial | null>(null)
  const [editingTpl, setEditingTpl] = useState<BasePrintTemplate | null>(null)

  const openMat = (r?: BasePackMaterial) => {
    setEditingMat(r ?? null); setMatOpen(true); matForm.resetFields()
    if (r) matForm.setFieldsValue(r)
  }
  const openTpl = (r?: BasePrintTemplate) => {
    setEditingTpl(r ?? null); setTplOpen(true); tplForm.resetFields()
    if (r) tplForm.setFieldsValue(r)
  }

  const matColumns: ColumnsType<BasePackMaterial> = [
    { title: '包装材质', dataIndex: 'materialName', width: 180 },
    { title: '包装规格(长×宽×高)', dataIndex: 'spec', width: 180 },
    { title: '纸箱重量(KG)', dataIndex: 'weight', width: 110, align: 'right' },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openMat(r)}>编辑</a>
          <Popconfirm title="确认删除该材质？" onConfirm={async () => { await deletePackMaterial(r.id); message.success('已删除'); mat.reload() }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]
  const tplColumns: ColumnsType<BasePrintTemplate> = [
    { title: '模板名称', dataIndex: 'templateName', width: 220 },
    { title: '模板类型', dataIndex: 'templateType', width: 100, render: (v) => (v && TEMPLATE_TYPES[v - 1]) || '-' },
    { title: '纸张类型', dataIndex: 'paperType', width: 140 },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openTpl(r)}>编辑</a>
          <Popconfirm title="确认删除该模板？" onConfirm={async () => { await deletePrintTemplate(r.id); message.success('已删除'); tpl.reload() }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">包装与打印</div>

      <Card size="small" style={{ marginBottom: 12 }}>
        <div style={{ marginBottom: 10 }}><Button type="primary" onClick={() => openMat()}>新增材质</Button></div>
        <Form layout="inline" onFinish={(v) => mat.setSearch(cleanParams(v))}>
          <Form.Item name="materialName" label="包装材质"><Input allowClear placeholder="内销纸箱(标准)" style={{ width: 160 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => mat.setSearch({})}>重置</Button></Form.Item>
        </Form>
        <Table rowKey="id" size="small" style={{ marginTop: 10 }} columns={matColumns} dataSource={mat.rows} loading={mat.loading} pagination={mat.pagination} />
      </Card>

      <Card size="small">
        <div style={{ marginBottom: 10 }}><Button type="primary" onClick={() => openTpl()}>新增模板</Button></div>
        <Form layout="inline" onFinish={(v) => tpl.setSearch(cleanParams(v))}>
          <Form.Item name="templateName" label="模板名称"><Input allowClear placeholder="筒贴-标准带RWS(英文)" style={{ width: 200 }} /></Form.Item>
          <Form.Item name="templateType" label="模板类型">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={TEMPLATE_TYPES.map((n, i) => ({ label: n, value: i + 1 }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => tpl.setSearch({})}>重置</Button></Form.Item>
        </Form>
        <Table rowKey="id" size="small" style={{ marginTop: 10 }} columns={tplColumns} dataSource={tpl.rows} loading={tpl.loading} pagination={tpl.pagination} />
      </Card>

      <Modal title={editingMat ? `编辑材质 - ${editingMat.materialName}` : '新增包装材质'} open={matOpen} onCancel={() => setMatOpen(false)} onOk={() => matForm.submit()} destroyOnClose>
        <Form form={matForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editingMat) { await updatePackMaterial(editingMat.id, v); message.success('修改成功') }
          else { await savePackMaterial(v); message.success('新增成功') }
          setMatOpen(false); mat.reload()
        }}>
          <Form.Item name="materialName" label="材质名称" rules={[{ required: true }]}><Input placeholder="内销纸箱(标准)" /></Form.Item>
          <Form.Item name="spec" label="包装规格(长×宽×高)"><Input placeholder="620×420×580" /></Form.Item>
          <Form.Item name="weight" label="纸箱重量(KG)"><InputNumber style={{ width: '100%' }} min={0} precision={3} /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={editingTpl ? `编辑模板 - ${editingTpl.templateName}` : '新增打印模板'} open={tplOpen} onCancel={() => setTplOpen(false)} onOk={() => tplForm.submit()} destroyOnClose width={520}>
        <Form form={tplForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editingTpl) { await updatePrintTemplate(editingTpl.id, v); message.success('修改成功') }
          else { await savePrintTemplate(v); message.success('新增成功') }
          setTplOpen(false); tpl.reload()
        }}>
          <Form.Item name="templateName" label="模板名称" rules={[{ required: true }]}><Input placeholder="筒贴-标准带RWS(英文)" /></Form.Item>
          <Form.Item name="templateType" label="模板类型" rules={[{ required: true }]}>
            <Select options={TEMPLATE_TYPES.map((n, i) => ({ label: n, value: i + 1 }))} />
          </Form.Item>
          <Form.Item name="paperType" label="纸张类型"><Input placeholder="热敏纸76×36 / A4" /></Form.Item>
          <Form.Item name="content" label="打印参数模板"><Input.TextArea rows={3} placeholder="模板内容(JSON/指令)" /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
