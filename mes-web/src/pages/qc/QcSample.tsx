import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops, useProcesses } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { registerSamples } from '@/api/qc'
import type { QcSample } from '@/api/qc'
import { BoolTag } from '@/components/StatusTag'

/** 抽样登记：勾选未登记批次 → 生成检验报告矩阵 */
export default function QcSample() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { nameMap: procName } = useProcesses()
  const [sel, setSel] = useState<QcSample[]>([])
  const { rows, loading, pagination, setSearch, reload } = usePagedList<QcSample>('/api/qc/samples')
  const [form] = Form.useForm()

  const columns: ColumnsType<QcSample> = [
    { title: '生产批号', dataIndex: 'batchNo', width: 130, fixed: 'left' },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '工序', dataIndex: 'processId', width: 90, render: (v) => procName[v] ?? v ?? '-' },
    { title: '批次量', dataIndex: 'feedQty', width: 90, align: 'right' },
    { title: '汇报时间', dataIndex: 'reportTime', width: 150, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '是否登记', dataIndex: 'isRegistered', width: 90, render: (v) => <BoolTag value={v} /> },
    { title: '登记时间', dataIndex: 'registerTime', width: 150, render: (v) => (v ? dayjs(v).format('MM-DD HH:mm') : '-') },
    { title: '是否下批', dataIndex: 'isBatchOff', width: 90, render: (v) => <BoolTag value={v} /> },
    { title: '任务了批', dataIndex: 'taskFinished', width: 90, render: (v) => <BoolTag value={v} /> },
  ]

  const selUnregistered = sel.filter((r) => !r.isRegistered)

  return (
    <div className="page">
      <div className="sec-title">抽样登记</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="isRegistered" label="登记状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={[{ label: '未登记', value: 0 }, { label: '已登记', value: 1 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ display: 'flex', gap: 8, marginBottom: 10, alignItems: 'center' }}>
          <Button
            type="primary"
            disabled={!selUnregistered.length}
            onClick={() => Modal.confirm({
              title: '抽样登记',
              content: `已选 ${selUnregistered.length} 个未登记批次，将按质检方案生成检验报告（试样×指标矩阵）。确认登记？`,
              onOk: async () => {
                await registerSamples(selUnregistered.map((r) => r.id))
                message.success('登记成功，检验报告已生成')
                setSel([])
                reload()
              },
            })}
          >
            批量登记
          </Button>
          <span className="muted">勾选 {sel.length} 条（未登记 {selUnregistered.length} 条可登记）</span>
        </div>
        <Table
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={rows}
          loading={loading}
          pagination={pagination}
          scroll={{ x: 1250 }}
          rowSelection={{ selectedRowKeys: sel.map((r) => r.id), onChange: (_, rows2) => setSel(rows2) }}
        />
      </Card>
    </div>
  )
}
