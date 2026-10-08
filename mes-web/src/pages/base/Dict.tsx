import { useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { BoolTag } from '@/components/StatusTag'
import { pageDicts, saveDict, updateDict, deleteDict, getDictItems, saveDictItem, updateDictItem, deleteDictItem } from '@/api/sys'
import type { SysDict, SysDictItem } from '@/api/sys'

/** 数据字典：字典类型 + 字典项（/base/dict 与 /sys/dict 共用） */
export default function Dict() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<SysDict>('/api/sys/dicts')
  const [dictForm] = Form.useForm()
  const [itemForm] = Form.useForm()
  const [dictOpen, setDictOpen] = useState(false)
  const [itemOpen, setItemOpen] = useState(false)
  const [editing, setEditing] = useState<SysDict | null>(null)
  const [editingItem, setEditingItem] = useState<SysDictItem | null>(null)
  const [selected, setSelected] = useState<SysDict | null>(null)
  const [items, setItems] = useState<SysDictItem[]>([])
  const [itemsLoading, setItemsLoading] = useState(false)

  useEffect(() => {
    if (!selected) { setItems([]); return }
    setItemsLoading(true)
    getDictItems(selected.id).then(setItems).catch(() => setItems([])).finally(() => setItemsLoading(false))
  }, [selected])

  const openDict = (r?: SysDict) => {
    setEditing(r ?? null); setDictOpen(true); dictForm.resetFields()
    if (r) dictForm.setFieldsValue(r)
  }
  const openItem = (r?: SysDictItem) => {
    if (!selected) return
    setEditingItem(r ?? null); setItemOpen(true); itemForm.resetFields()
    if (r) itemForm.setFieldsValue(r)
  }

  const dictColumns: ColumnsType<SysDict> = [
    { title: '字典类型', dataIndex: 'dictType', width: 150, render: (v, r) => <a onClick={() => setSelected(r)}>{v}</a> },
    { title: '类型名称', dataIndex: 'dictName', width: 160, render: (v, r) => <a onClick={() => setSelected(r)}>{v ?? r.dictType}</a> },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    {
      title: '操作', width: 170,
      render: (_, r) => (
        <>
          <a onClick={() => setSelected(r)}>字典项</a>
          <a style={{ marginLeft: 8 }} onClick={() => openDict(r)}>编辑</a>
          <Popconfirm title="确认删除该类型（连带字典项）？" onConfirm={async () => {
            await deleteDict(r.id); message.success('已删除')
            if (selected?.id === r.id) setSelected(null)
            reload()
          }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]
  const itemColumns: ColumnsType<SysDictItem> = [
    { title: '字典项', dataIndex: 'itemLabel', width: 160 },
    { title: '值', dataIndex: 'itemValue', width: 100 },
    { title: '排序', dataIndex: 'sortNo', width: 70 },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openItem(r)}>编辑</a>
          <Popconfirm title="确认删除该字典项？" onConfirm={async () => {
            if (selected) { await deleteDictItem(selected.id, r.id); message.success('已删除'); setItems(items.filter((i) => i.id !== r.id)) }
          }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">数据字典</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="dictName" label="字典类型"><Input allowClear placeholder="停机原因" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="dictType" label="类型编码"><Input allowClear placeholder="stop_reason" style={{ width: 140 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small" style={{ marginBottom: 12 }}>
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => openDict()}>新增字典类型</Button>
        </div>
        <Table rowKey="id" size="small" columns={dictColumns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Card size="small">
        <div className="sec-title" style={{ marginBottom: 8 }}>
          字典项{selected ? ` - ${selected.dictName ?? selected.dictType}` : '（点击上方字典类型查看）'}
        </div>
        {selected ? (
          <>
            <div style={{ marginBottom: 10 }}>
              <Button type="primary" onClick={() => openItem()}>新增字典项</Button>
            </div>
            <Table rowKey="id" size="small" columns={itemColumns} dataSource={items} loading={itemsLoading} pagination={false} />
          </>
        ) : null}
      </Card>

      <Modal title={editing ? `编辑字典类型 - ${editing.dictType}` : '新增字典类型'} open={dictOpen} onCancel={() => setDictOpen(false)} onOk={() => dictForm.submit()} destroyOnClose>
        <Form form={dictForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editing) { await updateDict(editing.id, v); message.success('修改成功') }
          else { await saveDict(v); message.success('新增成功') }
          setDictOpen(false); reload()
        }}>
          <Form.Item name="dictType" label="类型编码" rules={[{ required: true }]}><Input disabled={!!editing} placeholder="stop_reason" /></Form.Item>
          <Form.Item name="dictName" label="类型名称" rules={[{ required: true }]}><Input placeholder="停机原因" /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={editingItem ? `编辑字典项 - ${editingItem.itemLabel}` : '新增字典项'} open={itemOpen} onCancel={() => setItemOpen(false)} onOk={() => itemForm.submit()} destroyOnClose>
        <Form form={itemForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (!selected) return
          if (editingItem) {
            await updateDictItem(selected.id, editingItem.id, v); message.success('修改成功')
          } else {
            await saveDictItem(selected.id, v); message.success('新增成功')
          }
          setItemOpen(false)
          getDictItems(selected.id).then(setItems)
        }}>
          <Form.Item name="itemLabel" label="字典项" rules={[{ required: true }]}><Input placeholder="断纱" /></Form.Item>
          <Form.Item name="itemValue" label="值" rules={[{ required: true }]}><Input placeholder="1" /></Form.Item>
          <Form.Item name="sortNo" label="排序"><InputNumber style={{ width: '100%' }} min={0} /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
