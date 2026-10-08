import { useEffect, useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageUsers } from '@/api/sys'
import { pageLaborHours, saveLaborHour, deleteLaborHour } from '@/api/site'
import type { RepLaborHour } from '@/api/site'

/** 人员工时（月度工时录入） */
export default function WorkHours() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<RepLaborHour>('/api/site/work-hours')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()
  const [users, setUsers] = useState<{ label: string; value: number }[]>([])

  useEffect(() => {
    pageUsers({ pageNum: 1, pageSize: 200 })
      .then((p) => setUsers((p.rows ?? []).map((u) => ({ label: u.userName, value: u.id }))))
      .catch(() => setUsers([]))
  }, [])

  const columns: ColumnsType<RepLaborHour> = [
    { title: '年月', dataIndex: 'periodMonth', width: 100 },
    { title: '工时产线', dataIndex: 'prodLine', width: 150 },
    { title: '人员', dataIndex: 'userId', width: 120, render: (v) => users.find((u) => u.value === v)?.label ?? v ?? '-' },
    { title: '工时(h)', dataIndex: 'workHour', width: 100, align: 'right' },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => { setOpen(true); form.resetFields(); form.setFieldsValue(r) }}>编辑</a>
          <Popconfirm title="确认删除该工时记录？" onConfirm={async () => { await deleteLaborHour(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c', marginLeft: 8 }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">人员工时</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="periodMonth" label="年月"><Input allowClear placeholder="YYYY-MM" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="prodLine" label="产线"><Input allowClear placeholder="产线" style={{ width: 140 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => { setOpen(true); form.resetFields(); form.setFieldsValue({ periodMonth: dayjs().format('YYYY-MM') }) }}>新增工时</Button>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title={form.getFieldValue('id') ? '编辑工时' : '新增工时'} open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveLaborHour(v)
          message.success('保存成功')
          setOpen(false); reload()
        }}>
          <Form.Item name="id" hidden><InputNumber /></Form.Item>
          <Form.Item name="periodMonth" label="年月" rules={[{ required: true }]}><Input placeholder="2026-10" /></Form.Item>
          <Form.Item name="prodLine" label="工时产线" rules={[{ required: true }]}><Input placeholder="如 前纺/复精梳/倒筒/包装" /></Form.Item>
          <Form.Item name="userId" label="人员" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={users} />
          </Form.Item>
          <Form.Item name="workHour" label="工时(小时)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} max={744} precision={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
