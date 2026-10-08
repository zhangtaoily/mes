import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { pageCheckRecords, saveCheckRecord, deleteCheckRecord } from '@/api/equip'
import type { EqCheckRecord } from '@/api/equip'

type FormValues = Partial<Omit<EqCheckRecord, 'checkDate'>> & { checkDate?: Dayjs }

/** 点检记录：机台日常点检台账 */
export default function CheckRecord() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<EqCheckRecord>('/api/equip/check-records')
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()
  const [editing, setEditing] = useState<EqCheckRecord | null>(null)
  const [open, setOpen] = useState(false)

  const openForm = (row?: EqCheckRecord) => {
    setEditing(row ?? null)
    editForm.resetFields()
    editForm.setFieldsValue(
      row
        ? { ...row, checkDate: row.checkDate ? dayjs(row.checkDate) : undefined }
        : { checkDate: dayjs() },
    )
    setOpen(true)
  }

  const submit = async (v: FormValues) => {
    await saveCheckRecord({
      ...v,
      id: editing?.id,
      checkDate: v.checkDate ? v.checkDate.format('YYYY-MM-DD') : undefined,
    })
    message.success('已保存')
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<EqCheckRecord> = [
    { title: '记录编码', dataIndex: 'recordCode', width: 150, fixed: 'left' },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '点检人', dataIndex: 'checkUser', width: 100, render: (v) => v ?? '-' },
    { title: '点检日期', dataIndex: 'checkDate', width: 105, render: (v) => v ?? '-' },
    { title: '描述', dataIndex: 'describe', width: 260, ellipsis: true, render: (v) => v ?? '-' },
    { title: '备注', dataIndex: 'remark', width: 180, ellipsis: true, render: (v) => v ?? '-' },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', width: 110, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="删除该点检记录？" onConfirm={async () => { await deleteCheckRecord(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">点检记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="recordCode" label="记录编码"><Input allowClear placeholder="编码" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" showSearch optionFilterProp="label" style={{ width: 140 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="checkUser" label="点检人"><Input allowClear placeholder="姓名" style={{ width: 100 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
          <Form.Item><Button type="primary" onClick={() => openForm()}>新增记录</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1200 }} />
      </Card>

      <Modal title={editing ? `编辑点检记录 - ${editing.recordCode}` : '新增点检记录'} open={open} onCancel={() => setOpen(false)} onOk={() => editForm.submit()} destroyOnClose>
        <Form form={editForm} layout="vertical" onFinish={submit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="recordCode" label="记录编码" rules={[{ required: true, message: '请输入记录编码' }]}>
              <Input placeholder="如 DJ-20261008-001" />
            </Form.Item>
            <Form.Item name="workshopId" label="车间">
              <Select allowClear showSearch optionFilterProp="label" placeholder="车间" options={wsOptions} />
            </Form.Item>
            <Form.Item name="checkUser" label="点检人"><Input /></Form.Item>
            <Form.Item name="checkDate" label="点检日期"><DatePicker style={{ width: '100%' }} /></Form.Item>
          </div>
          <Form.Item name="describe" label="点检描述"><Input.TextArea rows={3} placeholder="点检项目与结果描述" /></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea rows={2} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
