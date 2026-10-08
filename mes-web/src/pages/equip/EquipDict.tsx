import { useCallback, useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table, Tabs } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { listEquipDicts, saveEquipDict, updateEquipDict, deleteEquipDict } from '@/api/equip'
import type { EquipDictItem, EquipDictType } from '@/api/equip'
import { BoolTag } from '@/components/StatusTag'

/** 每类字典的业务字段定义（status 启停统一处理） */
interface FieldDef {
  name: string
  label: string
  required?: boolean
  width?: number
}
interface DictConfig {
  type: EquipDictType
  tab: string
  fields: FieldDef[]
}

const DICT_CONFIGS: DictConfig[] = [
  {
    type: 'fault-types', tab: '故障分类',
    fields: [
      { name: 'faultCode', label: '故障编码', required: true, width: 130 },
      { name: 'faultName', label: '故障名称', required: true, width: 170 },
      { name: 'faultDesc', label: '故障描述', width: 320 },
    ],
  },
  {
    type: 'maintain-items', tab: '保养项目',
    fields: [
      { name: 'itemCode', label: '项目编码', required: true, width: 130 },
      { name: 'itemName', label: '项目名称', required: true, width: 170 },
      { name: 'maintainType', label: '保养类型', width: 120 },
      { name: 'itemDesc', label: '项目描述', width: 280 },
    ],
  },
  {
    type: 'check-items', tab: '点检项目',
    fields: [
      { name: 'itemCode', label: '项目编码', required: true, width: 130 },
      { name: 'itemName', label: '项目名称', required: true, width: 170 },
      { name: 'checkType', label: '点检类型', width: 120 },
      { name: 'itemDesc', label: '项目描述', width: 280 },
    ],
  },
  {
    type: 'maintain-methods', tab: '维保方式',
    fields: [
      { name: 'methodCode', label: '方式编码', required: true, width: 130 },
      { name: 'methodName', label: '方式名称', required: true, width: 170 },
      { name: 'methodDesc', label: '方式描述', width: 320 },
    ],
  },
  {
    type: 'part-models', tab: '配件型号',
    fields: [
      { name: 'partNo', label: '配件编码', required: true, width: 130 },
      { name: 'partName', label: '配件名称', required: true, width: 170 },
      { name: 'partDesc', label: '配件描述', width: 320 },
    ],
  },
]

const STATUS_OPTIONS = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]

/** 单个字典页签：全量列表 + CRUD */
function DictTab({ config }: { config: DictConfig }) {
  const { message } = App.useApp()
  const [list, setList] = useState<EquipDictItem[]>([])
  const [loading, setLoading] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [editForm] = Form.useForm()
  const [editing, setEditing] = useState<EquipDictItem | null>(null)
  const [open, setOpen] = useState(false)

  const load = useCallback(() => {
    setLoading(true)
    listEquipDicts(config.type, { keyword: keyword || undefined })
      .then((l) => setList(l ?? []))
      .catch(() => setList([]))
      .finally(() => setLoading(false))
  }, [config.type, keyword])

  useEffect(() => { load() }, [load])

  const openForm = (row?: EquipDictItem) => {
    setEditing(row ?? null)
    editForm.resetFields()
    editForm.setFieldsValue(row ? { ...row, status: (row.status as number) ?? 1 } : { status: 1 })
    setOpen(true)
  }

  const submit = async (v: Record<string, unknown>) => {
    if (editing) {
      await updateEquipDict(config.type, editing.id, v)
    } else {
      await saveEquipDict(config.type, v)
    }
    message.success('已保存')
    setOpen(false)
    load()
  }

  const columns: ColumnsType<EquipDictItem> = [
    ...config.fields.map((f) => ({
      title: f.label,
      dataIndex: f.name,
      width: f.width,
      ellipsis: true,
      render: (v: unknown) => (v === null || v === undefined || v === '' ? '-' : String(v)),
    })),
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v as number} labels={['停用', '启用']} /> },
    {
      title: '操作', width: 110, fixed: 'right' as const,
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除该字典项？" onConfirm={async () => { await deleteEquipDict(config.type, r.id); message.success('已删除'); load() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <Card size="small">
      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <Input.Search allowClear placeholder={`${config.tab}名称关键词`} style={{ width: 220 }} onSearch={(v) => setKeyword(v)} />
        <Button type="primary" onClick={() => openForm()}>新增</Button>
      </div>
      <Table rowKey="id" size="small" columns={columns} dataSource={list} loading={loading} pagination={false} scroll={{ x: 900 }} />

      <Modal title={editing ? `编辑${config.tab}` : `新增${config.tab}`} open={open} onCancel={() => setOpen(false)} onOk={() => editForm.submit()} destroyOnClose>
        <Form form={editForm} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            {config.fields.map((f) => (
              <Form.Item key={f.name} name={f.name} label={f.label} rules={f.required ? [{ required: true, message: `请输入${f.label}` }] : undefined}>
                <Input />
              </Form.Item>
            ))}
            <Form.Item name="status" label="状态">
              <Select options={STATUS_OPTIONS} />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </Card>
  )
}

/** 设备基础字典：故障分类/保养项目/点检项目/维保方式/配件型号 */
export default function EquipDict() {
  return (
    <div className="page">
      <div className="sec-title">设备基础字典</div>
      <Tabs
        defaultActiveKey="fault-types"
        items={DICT_CONFIGS.map((c) => ({
          key: c.type,
          label: c.tab,
          children: <DictTab config={c} />,
        }))}
      />
    </div>
  )
}
