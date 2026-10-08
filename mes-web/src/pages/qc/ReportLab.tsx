import { App, Button, Card, Form, Input, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { useWorkshops } from '@/hooks/useOptions'
import { cleanParams } from '@/utils/cleanParams'
import { calcQcReport, submitQcReport, voidQcReport, unvoidQcReport, publicQcReport } from '@/api/qc'
import type { QcReport } from '@/api/qc'
import StatusTag, { BoolTag } from '@/components/StatusTag'

const DOC_STATUS = ['编制中', '已提交', '已公示', '已作废'].map((s) => ({ label: s, value: s }))
/** 实验室检验类型（原料快检归车间现场） */
const LAB_TYPES = ['工序', '成品'].map((s) => ({ label: s, value: s }))

/** 检验报告（中心实验室）：工序检/成品检 集中检验工作台 */
export default function ReportLab() {
  const { message } = App.useApp()
  const { options: wsOptions, nameMap: wsName } = useWorkshops()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<QcReport>('/api/qc/reports')
  const [form] = Form.useForm()

  const act = async (fn: () => Promise<void>, ok: string) => {
    await fn()
    message.success(ok)
    reload()
  }

  const columns: ColumnsType<QcReport> = [
    { title: '报告编号', dataIndex: 'qcNo', width: 140, fixed: 'left' },
    { title: '状态', dataIndex: 'docStatus', width: 85, render: (v) => <StatusTag status={v} /> },
    { title: '车间', dataIndex: 'workshopId', width: 130, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '质检类型', dataIndex: 'qcType', width: 85 },
    { title: '生产批号', dataIndex: 'batchNo', width: 115 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 90 },
    { title: '检验员', dataIndex: 'inspector', width: 90 },
    { title: '检验日期', dataIndex: 'inspectDate', width: 100 },
    { title: '抽样数', dataIndex: 'sampleQty', width: 75, align: 'right' },
    { title: '留样数', dataIndex: 'retainQty', width: 75, align: 'right' },
    { title: '原料', dataIndex: 'rawMaterial', width: 140, ellipsis: true },
    { title: '支数', dataIndex: 'spinCount', width: 80 },
    { title: '公示', dataIndex: 'isPublic', width: 70, render: (v) => <BoolTag value={v} /> },
    {
      title: '操作', width: 230, fixed: 'right',
      render: (_, r) => (
        <>
          {r.docStatus === '编制中' ? (
            <>
              <Popconfirm title="按公式计算全部指标并判定越限？" onConfirm={() => act(() => calcQcReport(r.id), '计算完成')}>
                <a>计算</a>
              </Popconfirm>
              <Popconfirm title="提交后进入已提交状态。" onConfirm={() => act(() => submitQcReport(r.id), '已提交')}>
                <a style={{ marginLeft: 8 }}>提交</a>
              </Popconfirm>
            </>
          ) : null}
          {r.docStatus === '已提交' ? (
            <Popconfirm title="公示后报告对外可见。" onConfirm={() => act(() => publicQcReport(r.id), '已公示')}>
              <a style={{ marginLeft: 8 }}>公示</a>
            </Popconfirm>
          ) : null}
          {r.docStatus !== '已作废' ? (
            <Popconfirm title="作废后不计入统计口径。" onConfirm={() => act(() => voidQcReport(r.id), '已作废')}>
              <a style={{ color: '#f56c6c', marginLeft: 8 }}>作废</a>
            </Popconfirm>
          ) : (
            <Popconfirm title="取消作废后恢复为已提交状态。" onConfirm={() => act(() => unvoidQcReport(r.id), '已恢复')}>
              <a style={{ color: '#67c23a', marginLeft: 8 }}>恢复</a>
            </Popconfirm>
          )}
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">检验报告（中心实验室）</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="qcNo" label="报告编号"><Input allowClear placeholder="QC..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="batchNo" label="生产批号"><Input allowClear placeholder="批号" style={{ width: 115 }} /></Form.Item>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 145 }} options={wsOptions} />
          </Form.Item>
          <Form.Item name="qcType" label="质检类型">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={LAB_TYPES} />
          </Form.Item>
          <Form.Item name="docStatus" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 100 }} options={DOC_STATUS} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => { form.resetFields(); setSearch({}) }}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1650 }} />
      </Card>
    </div>
  )
}
