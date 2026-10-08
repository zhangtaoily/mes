import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { pageAttendance, saveAttendance, submitAttendance, withdrawAttendance, deleteAttendance } from '@/api/site'
import type { StAttendance } from '@/api/site'

/** 车间出勤（月度编制：编制中→已提交，可撤回） */
export default function Attendance() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<StAttendance>('/api/site/attendance')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const openForm = (r?: StAttendance) => {
    setOpen(true)
    form.resetFields()
    if (r) form.setFieldsValue({ ...r })
    else form.setFieldsValue({ periodMonth: dayjs().format('YYYY-MM') })
  }

  const columns: ColumnsType<StAttendance> = [
    { title: '期间', dataIndex: 'periodMonth', width: 90 },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '开工天数', dataIndex: 'workDays', width: 90, align: 'right' },
    { title: '管理人员', dataIndex: 'managerCount', width: 90, align: 'right' },
    { title: '常日班辅助', dataIndex: 'dayAuxCount', width: 95, align: 'right' },
    { title: '一线员工', dataIndex: 'frontlineCount', width: 90, align: 'right' },
    { title: '质检组出勤', dataIndex: 'qcCount', width: 95, align: 'right' },
    { title: '总出勤', dataIndex: 'totalCount', width: 80, align: 'right' },
    { title: '折算出勤', dataIndex: 'convertCount', width: 90, align: 'right' },
    {
      title: '状态', dataIndex: 'docStatus', width: 80,
      render: (v) => v === '已提交' ? <span style={{ color: '#1677ff' }}>{v}</span> : v ?? '-',
    },
    {
      title: '操作', width: 200,
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          {r.docStatus === '编制中' && (
            <a style={{ margin: '0 8px' }} onClick={async () => { await submitAttendance(r.id); message.success('已提交'); reload() }}>提交</a>
          )}
          {r.docStatus === '已提交' && (
            <a style={{ margin: '0 8px' }} onClick={async () => { await withdrawAttendance(r.id); message.success('已撤回'); reload() }}>撤回</a>
          )}
          <Popconfirm title="确认删除该出勤单？" onConfirm={async () => { await deleteAttendance(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间出勤</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="periodMonth" label="期间"><Input allowClear placeholder="YYYY-MM" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={['编制中', '已提交', '审核通过', '已撤回'].map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => openForm()}>新增出勤</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>期间+车间唯一，重复新增将被拦截</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title={form.getFieldValue('id') ? '编辑出勤' : '新增出勤'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveAttendance(v)
          message.success('保存成功')
          setOpen(false); reload()
        }}>
          <Form.Item name="id" hidden><InputNumber /></Form.Item>
          <Form.Item name="periodMonth" label="期间(月)" rules={[{ required: true }]}><Input placeholder="2026-10" /></Form.Item>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="workDays" label="开工天数"><InputNumber style={{ width: '100%' }} min={0} precision={1} /></Form.Item>
            <Form.Item name="managerCount" label="管理人员"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
            <Form.Item name="dayAuxCount" label="常日班辅助"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
            <Form.Item name="frontlineCount" label="一线员工"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
            <Form.Item name="qcCount" label="质检组出勤"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
            <Form.Item name="totalCount" label="总出勤"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
            <Form.Item name="convertCount" label="折算出勤"><InputNumber style={{ width: '100%' }} min={0} precision={2} /></Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  )
}
