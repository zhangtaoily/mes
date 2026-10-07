import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { submitCrafts, withdrawCrafts, voidCraft, generateSchedule } from '@/api/plan'
import type { PlanCraft } from '@/api/plan'
import StatusTag from '@/components/StatusTag'

const DOC_STATUS = ['编制中', '已提交', '生效', '了批', '已关闭', '已作废'].map((s) => ({ label: s, value: s }))

/** 计划工艺单：提交/撤回/作废/生成排程 */
export default function PlanCraft() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const [sel, setSel] = useState<PlanCraft[]>([])
  const { rows, loading, pagination, setSearch, reload } = usePagedList<PlanCraft>('/api/plan/crafts')
  const [form] = Form.useForm()

  const columns: ColumnsType<PlanCraft> = [
    { title: '工艺单号', dataIndex: 'craftNo', width: 140, fixed: 'left' },
    { title: '状态', dataIndex: 'docStatus', width: 85, render: (v) => <StatusTag status={v} /> },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '追溯批号', dataIndex: 'traceBatchNo', width: 115 },
    { title: '原料', dataIndex: 'rawMaterial', width: 150, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 85 },
    { title: '实纺支数', dataIndex: 'actualCount', width: 80 },
    { title: '合并投料量', dataIndex: 'mergeFeedQty', width: 95, align: 'right' },
    { title: '投料量', dataIndex: 'feedQty', width: 85, align: 'right' },
    { title: '计划产量', dataIndex: 'planQty', width: 90, align: 'right' },
    { title: 'K3订单号', dataIndex: 'k3OrderNo', width: 130 },
    { title: '交期', dataIndex: 'deliveryDate', width: 95 },
    {
      title: '操作', width: 150, fixed: 'right',
      render: (_, r) => (
        <>
          {r.docStatus === '已提交' || r.docStatus === '生效' ? (
            <Popconfirm
              title="按工艺单工序路线生成排程？"
              onConfirm={async () => {
                const result = await generateSchedule(r.id)
                const count = (result?.count as number) ?? (result?.total as number) ?? 0
                message.success(`排程已生成${count ? `（${count} 条）` : ''}`)
                reload()
              }}
            >
              <a>生成排程</a>
            </Popconfirm>
          ) : null}
          {r.docStatus !== '已作废' && r.docStatus !== '了批' ? (
            <Popconfirm title="作废后工艺单不再参与业务。确认作废？" onConfirm={async () => { await voidCraft(r.id); message.success('已作废'); reload() }}>
              <a style={{ color: '#f56c6c', marginLeft: 8 }}>作废</a>
            </Popconfirm>
          ) : null}
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">计划工艺单</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="actualCount" label="实纺支数"><Input allowClear placeholder="1/40" style={{ width: 100 }} /></Form.Item>
          <Form.Item name="rawMaterial" label="原料"><Input allowClear placeholder="原料" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={DOC_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
          <Button
            type="primary"
            disabled={!sel.length}
            onClick={() => Modal.confirm({
              title: '提交',
              content: `已选 ${sel.length} 条。提交后自动生成生产任务（编制中→已提交）。确认提交？`,
              onOk: async () => { await submitCrafts(sel.map((r) => r.id)); message.success('提交成功'); setSel([]); reload() },
            })}
          >
            提交
          </Button>
          <Button
            disabled={!sel.length}
            onClick={() => Modal.confirm({
              title: '撤回',
              content: `已选 ${sel.length} 条。撤回后回到编制中。确认？`,
              onOk: async () => { await withdrawCrafts(sel.map((r) => r.id)); message.success('撤回成功'); setSel([]); reload() },
            })}
          >
            撤回
          </Button>
          <span className="muted" style={{ alignSelf: 'center' }}>勾选 {sel.length} 条 · 引入K3订单与工序路线编辑由后端 API 支持，编辑页后续迭代</span>
        </div>
        <Table
          rowKey="id"
          size="small"
          columns={columns}
          dataSource={rows}
          loading={loading}
          pagination={pagination}
          scroll={{ x: 1650 }}
          rowSelection={{ selectedRowKeys: sel.map((r) => r.id), onChange: (_, rows2) => setSel(rows2) }}
        />
      </Card>
    </div>
  )
}
