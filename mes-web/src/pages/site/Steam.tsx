import { useState } from 'react'
import { App, Button, Card, Form, Input, InputNumber, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageSteams, saveSteam, printSteam, receiveSteam, deleteSteam } from '@/api/site'
import type { RepYarnSteam } from '@/api/site'

/** 蒸纱信息（打印/接收闭环：未打印 → 已打印 → 已接收） */
export default function Steam() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<RepYarnSteam>('/api/site/steam')
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const columns: ColumnsType<RepYarnSteam> = [
    { title: '打印单号', dataIndex: 'printCode', width: 150 },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (v) => v === '已接收' ? <span style={{ color: '#52c41a' }}>{v}</span> : v === '已打印' ? <span style={{ color: '#1677ff' }}>{v}</span> : v ?? '-',
    },
    { title: '车号', dataIndex: 'truckNo', width: 110 },
    { title: '生产批号', dataIndex: 'batchNo', width: 130 },
    { title: '粒数', dataIndex: 'grainCount', width: 70, align: 'right' },
    { title: '重量(kg)', dataIndex: 'weight', width: 90, align: 'right' },
    { title: '车数', dataIndex: 'truckCount', width: 70, align: 'right' },
    { title: '打印人', dataIndex: 'printUser', width: 80 },
    { title: '打印时间', dataIndex: 'printTime', width: 150 },
    { title: '接收人', dataIndex: 'receiveUser', width: 80 },
    { title: '接收时间', dataIndex: 'receiveTime', width: 150 },
    {
      title: '操作', width: 180, fixed: 'right',
      render: (_, r) => (
        <>
          {r.status !== '已接收' && (
            <>
              {r.status !== '已打印' && (
                <a onClick={async () => { await printSteam(r.id); message.success('已打印'); reload() }}>打印</a>
              )}
              <a style={{ margin: '0 8px' }} onClick={async () => { await receiveSteam(r.id); message.success('已接收'); reload() }}>接收</a>
            </>
          )}
          <Popconfirm title="确认删除该蒸纱信息？" onConfirm={async () => { await deleteSteam(r.id); message.success('已删除'); reload() }}>
            <a style={{ color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">蒸纱信息</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="batchNo" label="批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="truckNo" label="车号"><Input allowClear placeholder="车号" style={{ width: 110 }} /></Form.Item>
          <Form.Item name="status" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={['未打印', '已打印', '已接收'].map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => { setOpen(true); form.resetFields() }}>新增蒸纱信息</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>先打印后接收；未打印不可接收</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1400 }} />
      </Card>

      <Modal title="新增蒸纱信息" open={open} onCancel={() => setOpen(false)} onOk={() => form.submit()} destroyOnClose>
        <Form form={form} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await saveSteam(v)
          message.success('保存成功，打印单号后端自动取号')
          setOpen(false); reload()
        }}>
          <Form.Item name="truckNo" label="车号" rules={[{ required: true }]}><Input placeholder="如 TC001" /></Form.Item>
          <Form.Item name="batchNo" label="生产批号" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="grainCount" label="粒数" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} precision={0} />
          </Form.Item>
          <Form.Item name="weight" label="重量(kg)" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} precision={2} />
          </Form.Item>
          <Form.Item name="truckCount" label="车数"><InputNumber style={{ width: '100%' }} min={0} precision={0} /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
