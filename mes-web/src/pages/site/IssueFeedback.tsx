import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Input, InputNumber, Modal, Popconfirm, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageIssues, saveIssue, deleteIssue } from '@/api/site'
import type { StIssue } from '@/api/site'

const RECTIFY_STATUS = ['未整改', '整改中', '已整改', '已闭环']

/** 现场问题反馈（检查→整改跟踪） */
export default function IssueFeedback() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<StIssue>('/api/site/issue')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const statusColor = (v?: string) =>
    v === '已闭环' ? 'green' : v === '已整改' ? 'blue' : v === '整改中' ? 'orange' : 'red'

  const openForm = (r?: StIssue) => {
    setOpen(true)
    form.resetFields()
    if (r) form.setFieldsValue({ ...r, checkDate: r.checkDate ? dayjs(r.checkDate) : undefined })
    else form.setFieldsValue({ checkDate: dayjs(), rectifyStatus: '未整改' })
  }

  const columns: ColumnsType<StIssue> = [
    { title: '检查日期', dataIndex: 'checkDate', width: 110 },
    { title: '检查人', dataIndex: 'checkUser', width: 90 },
    { title: '问题所在', dataIndex: 'issueLocation', width: 160, ellipsis: true },
    { title: '问题描述', dataIndex: 'issueDesc', ellipsis: true },
    {
      title: '整改状态', dataIndex: 'rectifyStatus', width: 100,
      render: (v) => v ? <Tag color={statusColor(v)}>{v}</Tag> : '-',
    },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openForm(r)}>编辑</a>
          <Popconfirm title="确认删除该问题记录？" onConfirm={async () => { await deleteIssue(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">现场问题反馈</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="checkUser" label="检查人"><Input allowClear placeholder="检查人" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="issueLocation" label="问题所在"><Input allowClear placeholder="位置" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="rectifyStatus" label="整改状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={RECTIFY_STATUS.map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => openForm()}>新增问题记录</Button>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title={form.getFieldValue('id') ? '编辑问题记录' : '新增问题记录'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveIssue({ ...v, checkDate: v.checkDate?.format('YYYY-MM-DD') })
          message.success('保存成功')
          setOpen(false); reload()
        }}>
          <Form.Item name="id" hidden><InputNumber /></Form.Item>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: 12 }}>
            <Form.Item name="checkUser" label="检查人" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item name="checkDate" label="检查日期" rules={[{ required: true }]}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </div>
          <Form.Item name="issueLocation" label="问题所在" rules={[{ required: true }]}><Input placeholder="车间/工序/机台位置" /></Form.Item>
          <Form.Item name="issueDesc" label="问题描述" rules={[{ required: true }]}><Input.TextArea rows={3} /></Form.Item>
          <Form.Item name="rectifyStatus" label="整改状态" rules={[{ required: true }]}>
            <Select options={RECTIFY_STATUS.map((s) => ({ label: s, value: s }))} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
