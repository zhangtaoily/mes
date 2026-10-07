import { App, Button, Card, Form, Input, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { printCard, withdrawCard } from '@/api/plan'
import type { MachineCardRow } from '@/api/plan'
import StatusTag from '@/components/StatusTag'
import { useWorkshops, useProcesses } from '@/hooks/useOptions'

const CARD_STATUS = ['已排在做', '了机'].map((s) => ({ label: s, value: s }))

/** 上机制造卡：打印留痕与撤回（撤回释放排程占用） */
export default function MachineCard() {
  const { message } = App.useApp()
  const { nameMap: wsName } = useWorkshops()
  const { nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<MachineCardRow>('/api/plan/cards')
  const [form] = Form.useForm()

  const columns: ColumnsType<MachineCardRow> = [
    { title: '制造卡号', dataIndex: ['card', 'cardNo'], width: 140, fixed: 'left' },
    { title: '状态', dataIndex: ['card', 'status'], width: 85, render: (v) => <StatusTag status={v} /> },
    { title: '排程单号', dataIndex: 'scheduleNo', width: 150 },
    { title: '车间', dataIndex: ['card', 'workshopId'], width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '工序', dataIndex: ['card', 'processId'], width: 90, render: (v) => procName[v] ?? v ?? '-' },
    { title: '机台', dataIndex: 'machineNo', width: 90 },
    { title: '机型', dataIndex: ['card', 'machineModel'], width: 110 },
    { title: '生产批号', dataIndex: ['card', 'batchNo'], width: 115 },
    { title: '支数/规格', dataIndex: ['card', 'countSpec'], width: 100 },
    { title: '车速', dataIndex: ['card', 'speed'], width: 80 },
    { title: '定长', dataIndex: ['card', 'fixedLength'], width: 80 },
    { title: '张力', dataIndex: ['card', 'tension'], width: 80 },
    { title: '配额', dataIndex: ['card', 'quota'], width: 80 },
    { title: '打印时间', dataIndex: ['card', 'printTime'], width: 140 },
    {
      title: '操作', width: 120, fixed: 'right',
      render: (_, r) => (
        <>
          {r.card.status === '已排在做' ? (
            <Popconfirm title="确认补打？（打印动作留痕 flow_card_print_log）" onConfirm={async () => { await printCard(r.card.id); message.success('已补打'); reload() }}>
              <a>补打</a>
            </Popconfirm>
          ) : null}
          {r.card.status === '已排在做' ? (
            <Popconfirm title="撤回后释放排程占用，可重新排程。确认撤回？" onConfirm={async () => { await withdrawCard(r.card.id); message.success('已撤回'); reload() }}>
              <a style={{ color: '#e6a23c', marginLeft: 8 }}>撤回</a>
            </Popconfirm>
          ) : null}
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">上机制造卡</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="cardNo" label="制造卡号"><Input allowClear placeholder="卡号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="status" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={CARD_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey={(r) => r.card.id} size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1650 }} />
      </Card>
    </div>
  )
}
