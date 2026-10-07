import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { taskBatchOp, deleteTask } from '@/api/plan'
import type { ProdTask } from '@/api/plan'
import StatusTag from '@/components/StatusTag'
import { useAuth } from '@/store/auth'

const DOC_STATUS = ['编制中', '已提交', '生效', '已暂停', '了批', '已关闭', '已作废'].map((s) => ({ label: s, value: s }))

/** 生产任务列表：状态流转（提交/撤回/了批/关闭/作废）+ 删除 */
export default function ProdTask() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const [sel, setSel] = useState<ProdTask[]>([])
  const { rows, loading, pagination, setSearch, reload } = usePagedList<ProdTask>('/api/plan/tasks')
  const [form] = Form.useForm()

  /** 批量操作确认弹窗文案 */
  const OPS: Record<'submit' | 'withdraw' | 'finish' | 'close' | 'void', { label: string; danger?: boolean; confirm: string }> = {
    submit: { label: '提交', confirm: '提交后任务生效（编制中→生效）。确认提交？' },
    withdraw: { label: '撤回', confirm: '撤回后任务回到编制中（仅无排程的任务可撤回）。确认？' },
    finish: { label: '了批', confirm: '了批后该批次封账，不可再进行产量汇报与投料。确认了批？' },
    close: { label: '关闭', confirm: '关闭后任务为终态，不可恢复。确认关闭？' },
    void: { label: '作废', danger: true, confirm: '作废后任务不再参与业务。确认作废？' },
  }

  const doOp = async (op: keyof typeof OPS) => {
    const cfg = OPS[op]
    Modal.confirm({
      title: cfg.label,
      content: `已选 ${sel.length} 条。${cfg.confirm}`,
      okButtonProps: { danger: cfg.danger },
      onOk: async () => {
        await taskBatchOp(op, sel.map((r) => r.id))
        message.success(`${cfg.label}成功`)
        setSel([])
        reload()
      },
    })
  }

  const columns: ColumnsType<ProdTask> = [
    { title: '任务编号', dataIndex: 'taskNo', width: 140, fixed: 'left' },
    { title: '编制状态', dataIndex: 'docStatus', width: 90, render: (v) => <StatusTag status={v} /> },
    { title: '执行状态', dataIndex: 'execStatus', width: 90, render: (v) => <StatusTag status={v} /> },
    { title: '生产批号', dataIndex: 'batchNo', width: 120 },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '原料明细', dataIndex: 'rawMaterial', width: 160, ellipsis: true },
    { title: '产品编号', dataIndex: 'productNo', width: 120 },
    { title: '产品规格', dataIndex: 'productSpec', width: 100 },
    { title: '实纺支数', dataIndex: 'actualCount', width: 80 },
    { title: '色号', dataIndex: 'colorNo', width: 90 },
    { title: '计划投料量', dataIndex: 'planFeedQty', width: 95, align: 'right' },
    { title: '计划产量', dataIndex: 'planQty', width: 90, align: 'right' },
    { title: '单位', dataIndex: 'unit', width: 55 },
    { title: 'K3订单号', dataIndex: 'k3OrderNo', width: 130 },
    { title: '交期', dataIndex: 'deliveryDate', width: 95 },
    {
      title: '操作', width: 120, fixed: 'right',
      render: (_, r) => hasPerm('prod:task:del') && r.docStatus === '编制中' ? (
        <Popconfirm title="删除后列表不再显示（逻辑删除）。确认？" onConfirm={async () => { await deleteTask(r.id); message.success('删除成功'); reload() }}>
          <a style={{ color: '#f56c6c' }}>删除</a>
        </Popconfirm>
      ) : <span className="muted">-</span>,
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">生产任务</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="生产车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="actualCount" label="实纺支数"><Input allowClear placeholder="1/40" style={{ width: 100 }} /></Form.Item>
          <Form.Item name="rawMaterial" label="原料明细"><Input allowClear placeholder="原料" style={{ width: 150 }} /></Form.Item>
          <Form.Item name="docStatus" label="编制状态">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={DOC_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ display: 'flex', gap: 8, marginBottom: 10, flexWrap: 'wrap' }}>
          {(Object.keys(OPS) as (keyof typeof OPS)[]).map((op) => (
            <Button key={op} disabled={!sel.length} danger={OPS[op].danger} onClick={() => doOp(op)}>
              {OPS[op].label}
            </Button>
          ))}
          <span className="muted" style={{ alignSelf: 'center' }}>勾选 {sel.length} 条</span>
        </div>
        <Table
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={rows}
          loading={loading}
          pagination={pagination}
          scroll={{ x: 1750 }}
          rowSelection={{ selectedRowKeys: sel.map((r) => r.id), onChange: (_, rows2) => setSel(rows2) }}
        />
      </Card>
    </div>
  )
}
