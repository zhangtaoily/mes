import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { getEquipments } from '@/api/plan'
import type { BaseEquipment } from '@/api/plan'
import { pageMaintainRecords, saveMaintainRecord, deleteMaintainRecord } from '@/api/equip'
import type { EqMaintainRecord } from '@/api/equip'
import { BoolTag } from '@/components/StatusTag'

type FormValues = Partial<Omit<EqMaintainRecord, 'expectDate' | 'finishDate'>> & { expectDate?: Dayjs; finishDate?: Dayjs }

/** 保养记录：机台保养执行台账（预计→完成） */
export default function MaintainRecord() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<EqMaintainRecord>('/api/equip/maintain-records')
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()
  const [editing, setEditing] = useState<EqMaintainRecord | null>(null)
  const [open, setOpen] = useState(false)
  const [equipments, setEquipments] = useState<BaseEquipment[]>([])

  const loadEquipments = (ws?: number) => {
    if (ws) {
      getEquipments(ws).then(setEquipments).catch(() => setEquipments([]))
    } else {
      setEquipments([])
    }
  }

  const openForm = async (row?: EqMaintainRecord) => {
    setEditing(row ?? null)
    editForm.resetFields()
    if (row?.workshopId) loadEquipments(row.workshopId)
    editForm.setFieldsValue(
      row
        ? { ...row, expectDate: row.expectDate ? dayjs(row.expectDate) : undefined, finishDate: row.finishDate ? dayjs(row.finishDate) : undefined }
        : { isFinish: 0 },
    )
    setOpen(true)
  }

  const submit = async (v: FormValues) => {
    await saveMaintainRecord({
      ...v,
      id: editing?.id,
      expectDate: v.expectDate ? v.expectDate.format('YYYY-MM-DD') : undefined,
      finishDate: v.finishDate ? v.finishDate.format('YYYY-MM-DD') : undefined,
    })
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const eqName = (id?: number) => equipments.find((e) => e.id === id)?.machineNo ?? id ?? '-'
  const columns: ColumnsType<EqMaintainRecord> = [
    { title: '记录编码', dataIndex: 'recordCode', width: 140, fixed: 'left', render: (v) => v ?? '-' },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '机台', dataIndex: 'equipmentId', width: 100, render: (v) => `#${v ?? '-'}` },
    { title: '保养类型', dataIndex: 'maintainType', width: 95, render: (v) => v ?? '-' },
    { title: '保养项目', dataIndex: 'maintainItem', width: 160, ellipsis: true, render: (v) => v ?? '-' },
    { title: '预计保养日期', dataIndex: 'expectDate', width: 105, render: (v) => v ?? '-' },
    { title: '保养人', dataIndex: 'maintainUser', width: 85, render: (v) => v ?? '-' },
    { title: '完成日期', dataIndex: 'finishDate', width: 100, render: (v) => v ?? '-' },
    { title: '完成', dataIndex: 'isFinish', width: 70, render: (v) => <BoolTag value={v} /> },
    { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true, render: (v) => v ?? '-' },
    {
      title: '操作', width: 110, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除该保养记录？" onConfirm={async () => { await deleteMaintainRecord(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">保养记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 140 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="isFinish" label="完成状态">
            <Select allowClear placeholder="全部" style={{ width: 95 }} options={[{ label: '未完成', value: 0 }, { label: '已完成', value: 1 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增记录</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1300 }} />
      </Card>

      <Modal title={editing ? '编辑保养记录' : '新增保养记录'} open={open} onCancel={() => setOpen(false)} onOk={() => editForm.submit()} width={640} destroyOnClose>
        <Form form={editForm} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="recordCode" label="记录编码"><Input placeholder="留空自动生成" /></Form.Item>
            <Form.Item name="maintainType" label="保养类型"><Input placeholder="如 保养/润滑" /></Form.Item>
            <Form.Item name="workshopId" label="车间">
              <Select allowClear showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} onChange={loadEquipments} />
            </Form.Item>
            <Form.Item name="equipmentId" label="机台" rules={[{ required: true, message: '请选择机台' }]}>
              <Select showSearch optionFilterProp="label" placeholder="先选车间" options={equipments.map((e) => ({ label: e.machineNo, value: e.id }))} />
            </Form.Item>
            <Form.Item name="maintainItem" label="保养项目"><Input /></Form.Item>
            <Form.Item name="maintainUser" label="保养人"><Input /></Form.Item>
            <Form.Item name="expectDate" label="预计保养日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="finishDate" label="完成日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
            <Form.Item name="isFinish" label="是否完成">
              <Select options={[{ label: '未完成', value: 0 }, { label: '已完成', value: 1 }]} />
            </Form.Item>
          </div>
          <Form.Item name="detail" label="保养详情"><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
