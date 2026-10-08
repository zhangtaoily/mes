import { useEffect, useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { getEquipments } from '@/api/plan'
import type { BaseEquipment } from '@/api/plan'
import { pageRepairRecords, saveRepairRecord, deleteRepairRecord, listEquipDicts } from '@/api/equip'
import type { EqRepairRecord, EquipDictItem } from '@/api/equip'

type FormValues = Partial<Omit<EqRepairRecord, 'faultTime' | 'solveTime'>> & { faultTime?: Dayjs; solveTime?: Dayjs }

const dictNameMap = (list: EquipDictItem[], key: string): Record<number, string> => {
  const m: Record<number, string> = {}
  list.forEach((i) => { m[i.id] = String(i[key] ?? i.id) })
  return m
}

/** 维保记录：机台故障维修台账 */
export default function RepairRecord() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<EqRepairRecord>('/api/equip/repair-records')
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()
  const [editing, setEditing] = useState<EqRepairRecord | null>(null)
  const [open, setOpen] = useState(false)
  const [equipments, setEquipments] = useState<BaseEquipment[]>([])
  const [faultMap, setFaultMap] = useState<Record<number, string>>({})
  const [methodMap, setMethodMap] = useState<Record<number, string>>({})

  useEffect(() => {
    listEquipDicts('fault-types').then((l) => setFaultMap(dictNameMap(l, 'faultName'))).catch(() => setFaultMap({}))
    listEquipDicts('maintain-methods').then((l) => setMethodMap(dictNameMap(l, 'methodName'))).catch(() => setMethodMap({}))
  }, [])

  const loadEquipments = (ws?: number) => {
    if (ws) {
      getEquipments(ws).then(setEquipments).catch(() => setEquipments([]))
    } else {
      setEquipments([])
    }
  }

  const openForm = (row?: EqRepairRecord) => {
    setEditing(row ?? null)
    editForm.resetFields()
    if (row?.workshopId) loadEquipments(row.workshopId)
    editForm.setFieldsValue(
      row
        ? {
            ...row,
            faultTime: row.faultTime ? dayjs(row.faultTime) : undefined,
            solveTime: row.solveTime ? dayjs(row.solveTime) : undefined,
          }
        : { faultTime: dayjs() },
    )
    setOpen(true)
  }

  const submit = async (v: FormValues) => {
    await saveRepairRecord({
      ...v,
      id: editing?.id,
      faultTime: v.faultTime ? v.faultTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
      solveTime: v.solveTime ? v.solveTime.format('YYYY-MM-DD HH:mm:ss') : undefined,
    })
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<EqRepairRecord> = [
    { title: '维修编码', dataIndex: 'repairCode', width: 140, fixed: 'left' },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '机台', dataIndex: 'equipmentId', width: 90, render: (v) => `#${v ?? '-'}` },
    { title: '故障时间', dataIndex: 'faultTime', width: 150, render: (v) => v ?? '-' },
    { title: '故障分类', dataIndex: 'faultTypeId', width: 110, render: (v) => (v != null ? faultMap[v] ?? v : '-') },
    { title: '维修人', dataIndex: 'repairUser', width: 90, render: (v) => v ?? '-' },
    { title: '解决时间', dataIndex: 'solveTime', width: 150, render: (v) => v ?? '-' },
    { title: '维保方式', dataIndex: 'repairMethodId', width: 110, render: (v) => (v != null ? methodMap[v] ?? v : '-') },
    { title: '维修详情', dataIndex: 'detail', width: 200, ellipsis: true, render: (v) => v ?? '-' },
    { title: '备注', dataIndex: 'remark', width: 140, ellipsis: true, render: (v) => v ?? '-' },
    {
      title: '操作', width: 110, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除该维修记录？" onConfirm={async () => { await deleteRepairRecord(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  const dictOptions = (m: Record<number, string>) => Object.entries(m).map(([id, name]) => ({ label: name, value: Number(id) }))

  return (
    <div className="page">
      <div className="sec-title">维保记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="repairCode" label="维修编码"><Input allowClear placeholder="编码" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 140 }} options={wsOptions} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增记录</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>

      <Modal title={editing ? `编辑维修记录 - ${editing.repairCode}` : '新增维修记录'} open={open} onCancel={() => setOpen(false)} onOk={() => editForm.submit()} width={640} destroyOnClose>
        <Form form={editForm} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="repairCode" label="维修编码" rules={[{ required: true, message: '请输入维修编码' }]}>
              <Input placeholder="如 WX-20261008-001" />
            </Form.Item>
            <Form.Item name="workshopId" label="车间">
              <Select allowClear showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} onChange={loadEquipments} />
            </Form.Item>
            <Form.Item name="equipmentId" label="机台" rules={[{ required: true, message: '请选择机台' }]}>
              <Select showSearch optionFilterProp="label" placeholder="先选车间" options={equipments.map((e) => ({ label: e.machineNo, value: e.id }))} />
            </Form.Item>
            <Form.Item name="faultTime" label="故障时间"><DatePicker showTime style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="faultTypeId" label="故障分类">
              <Select allowClear showSearch optionFilterProp="label" placeholder="故障分类" options={dictOptions(faultMap)} />
            </Form.Item>
            <Form.Item name="repairUser" label="维修人"><Input /></Form.Item>
            <Form.Item name="solveTime" label="解决时间"><DatePicker showTime style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="repairMethodId" label="维保方式">
              <Select allowClear showSearch optionFilterProp="label" placeholder="维保方式" options={dictOptions(methodMap)} />
            </Form.Item>
          </div>
          <Form.Item name="detail" label="维修详情"><Input.TextArea rows={3} placeholder="故障现象、处理过程" /></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
