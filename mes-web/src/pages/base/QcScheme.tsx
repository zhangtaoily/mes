import { useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { BoolTag } from '@/components/StatusTag'
import {
  pageQcSchemes, saveQcScheme, updateQcScheme, changeQcSchemeStatus, deleteQcScheme,
  getQcSchemeItems, saveQcSchemeItem, updateQcSchemeItem, deleteQcSchemeItem, getQcIndicators,
} from '@/api/base'
import type { BaseQcScheme, BaseQcSchemeItem, BaseQcIndicator } from '@/api/base'

/** 质检方案维护：方案列表 + 选中方案显示指标项 */
export default function QcScheme() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<BaseQcScheme>('/api/base/qc-schemes')
  const [schemeForm] = Form.useForm()
  const [itemForm] = Form.useForm()
  const [schemeOpen, setSchemeOpen] = useState(false)
  const [itemOpen, setItemOpen] = useState(false)
  const [editing, setEditing] = useState<BaseQcScheme | null>(null)
  const [editingItem, setEditingItem] = useState<BaseQcSchemeItem | null>(null)
  const [selected, setSelected] = useState<BaseQcScheme | null>(null)
  const [items, setItems] = useState<BaseQcSchemeItem[]>([])
  const [indicators, setIndicators] = useState<BaseQcIndicator[]>([])
  const [itemsLoading, setItemsLoading] = useState(false)

  useEffect(() => { getQcIndicators().then(setIndicators).catch(() => setIndicators([])) }, [])
  useEffect(() => {
    if (!selected) { setItems([]); return }
    setItemsLoading(true)
    getQcSchemeItems(selected.id).then(setItems).catch(() => setItems([])).finally(() => setItemsLoading(false))
  }, [selected])

  const indName = (id?: number) => indicators.find((i) => i.id === id)?.indicatorName ?? id
  const indUnit = (id?: number) => indicators.find((i) => i.id === id)?.unit ?? '-'

  const openScheme = (r?: BaseQcScheme) => {
    setEditing(r ?? null); setSchemeOpen(true); schemeForm.resetFields()
    if (r) schemeForm.setFieldsValue(r)
  }
  const openItem = (r?: BaseQcSchemeItem) => {
    if (!selected) return
    setEditingItem(r ?? null); setItemOpen(true); itemForm.resetFields()
    if (r) itemForm.setFieldsValue(r)
  }

  const schemeColumns: ColumnsType<BaseQcScheme> = [
    { title: '方案编号', dataIndex: 'schemeCode', width: 110, render: (v, r) => <a onClick={() => setSelected(r)}>{v}</a> },
    { title: '方案名称', dataIndex: 'schemeName', width: 180, render: (v, r) => <a onClick={() => setSelected(r)}>{v}</a> },
    { title: '车间', dataIndex: 'workshopId', width: 150, render: (v) => wsName[v] ?? (v ?? '-') },
    { title: '试样数', dataIndex: 'sampleCount', width: 80, align: 'right' },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    { title: '创建人', dataIndex: 'createBy', width: 90 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', width: 180,
      render: (_, r) => (
        <>
          <a onClick={() => setSelected(r)}>指标项</a>
          <a style={{ marginLeft: 8 }} onClick={() => openScheme(r)}>编辑</a>
          <Popconfirm title={r.status ? '确认停用该方案？' : '确认启用该方案？'} onConfirm={async () => {
            await changeQcSchemeStatus(r.id, r.status ? 0 : 1); message.success('已更新'); reload()
          }}>
            <a style={{ marginLeft: 8, color: r.status ? '#e6a23c' : '#67c23a' }}>{r.status ? '停用' : '启用'}</a>
          </Popconfirm>
          <Popconfirm title="确认删除该方案（连带指标项）？" onConfirm={async () => {
            await deleteQcScheme(r.id); message.success('已删除')
            if (selected?.id === r.id) setSelected(null)
            reload()
          }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]
  const itemColumns: ColumnsType<BaseQcSchemeItem> = [
    { title: '指标', dataIndex: 'indicatorId', width: 130, render: indName },
    { title: '单位', dataIndex: 'indicatorId', width: 80, render: indUnit },
    { title: '标准值', dataIndex: 'standardValue', width: 100 },
    { title: '上限', dataIndex: 'upperLimit', width: 90 },
    { title: '下限', dataIndex: 'lowerLimit', width: 90 },
    { title: '排序', dataIndex: 'sortNo', width: 60 },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openItem(r)}>编辑</a>
          <Popconfirm title="确认删除该指标项？" onConfirm={async () => {
            if (selected) { await deleteQcSchemeItem(selected.id, r.id); message.success('已删除'); setItems(items.filter((i) => i.id !== r.id)) }
          }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">质检方案</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="生产车间">
            <Select allowClear placeholder="全部" style={{ width: 160 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="schemeCode" label="方案编号"><Input allowClear placeholder="FA-0012" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="schemeName" label="方案名称"><Input allowClear placeholder="原料检方案" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="status" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 90 }} options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small" style={{ marginBottom: 12 }}>
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => openScheme()}>新增方案</Button>
        </div>
        <Table rowKey="id" size="small" columns={schemeColumns} dataSource={rows} loading={loading} pagination={pagination}
          onRow={(r) => ({ onDoubleClick: () => setSelected(r) })} />
      </Card>

      <Card size="small">
        <div className="sec-title" style={{ marginBottom: 8 }}>
          方案指标项{selected ? ` - ${selected.schemeCode} ${selected.schemeName}` : '（点击上方方案编号查看）'}
        </div>
        {selected ? (
          <>
            <div style={{ marginBottom: 10 }}>
              <Button type="primary" onClick={() => openItem()}>新增指标</Button>
            </div>
            <Table rowKey="id" size="small" columns={itemColumns} dataSource={items} loading={itemsLoading} pagination={false} />
            <div className="muted" style={{ fontSize: 12, padding: '6px 2px' }}>
              指标项在检验报告中按方案带出，衍生指标（如不匀率）由质检公式（JS脚本 $BQ）计算
            </div>
          </>
        ) : null}
      </Card>

      <Modal title={editing ? `编辑方案 - ${editing.schemeName}` : '新增质检方案'} open={schemeOpen} onCancel={() => setSchemeOpen(false)} onOk={() => schemeForm.submit()} destroyOnClose>
        <Form form={schemeForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editing) { await updateQcScheme(editing.id, v); message.success('修改成功') }
          else { await saveQcScheme(v); message.success('新增成功') }
          setSchemeOpen(false); reload()
        }}>
          <Form.Item name="schemeCode" label="方案编号" rules={[{ required: true }]}><Input disabled={!!editing} placeholder="FA-0012" /></Form.Item>
          <Form.Item name="schemeName" label="方案名称" rules={[{ required: true }]}><Input placeholder="毛条原料检方案" /></Form.Item>
          <Form.Item name="workshopId" label="生产车间">
            <Select allowClear showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="sampleCount" label="试样数"><InputNumber style={{ width: '100%' }} min={1} max={10} /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={editingItem ? '编辑指标项' : '新增指标项'} open={itemOpen} onCancel={() => setItemOpen(false)} onOk={() => itemForm.submit()} destroyOnClose>
        <Form form={itemForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (!selected) return
          if (editingItem) {
            await updateQcSchemeItem(selected.id, editingItem.id, v); message.success('修改成功')
          } else {
            await saveQcSchemeItem(selected.id, v); message.success('新增成功')
          }
          setItemOpen(false)
          getQcSchemeItems(selected.id).then(setItems)
        }}>
          <Form.Item name="indicatorId" label="指标" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={indicators.map((i) => ({ label: `${i.indicatorName}(${i.unit ?? '-'})`, value: i.id }))} />
          </Form.Item>
          <Form.Item name="standardValue" label="标准值"><Input placeholder="19.25" /></Form.Item>
          <Form.Item name="upperLimit" label="上限"><Input placeholder="19.5" /></Form.Item>
          <Form.Item name="lowerLimit" label="下限"><Input placeholder="19.0" /></Form.Item>
          <Form.Item name="sortNo" label="排序"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
