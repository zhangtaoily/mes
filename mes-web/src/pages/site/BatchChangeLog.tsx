import { useState } from 'react'
import { App, Button, Card, DatePicker, Form, Modal, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops, useProcesses } from '@/hooks/useOptions'
import { useTeams } from '@/hooks/useTeams'
import { getEquipments } from '@/api/plan'
import { pageRebatchLogs, getRebatchDetails, generateRebatch } from '@/api/site'
import type { RepRebatchLog, RepRebatchDetail } from '@/api/site'

/** 翻批日志（按车间+班组+时段汇总上批记录生成） */
export default function BatchChangeLog() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()
  const { nameMap: teamName, options: teamOptions } = useTeams()
  const { nameMap: procName } = useProcesses()
  const { rows, loading, pagination, setSearch } = usePagedList<RepRebatchLog>('/api/site/rebatch')
  const [genOpen, setGenOpen] = useState(false)
  const [genForm] = Form.useForm()
  const [detail, setDetail] = useState<{ title: string; rows: RepRebatchDetail[] } | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [eqName, setEqName] = useState<Record<number, string>>({})

  const loadDetail = async (r: RepRebatchLog) => {
    setDetailLoading(true)
    setDetail({ title: r.title ?? `翻批记录#${r.id}`, rows: [] })
    try {
      const list = await getRebatchDetails(r.id)
      const eqIds = [...new Set(list.map((d) => d.equipmentId).filter((v): v is number => v != null))]
      if (eqIds.length) {
        // 机台名称就近取一次（各车间合并查）
        const all = await Promise.all([...new Set(list.map((d) => d.workshopId).filter((v): v is number => v != null))].map((ws) => getEquipments(ws)))
        const m: Record<number, string> = {}
        all.flat().forEach((e) => (m[e.id] = e.machineNo))
        setEqName(m)
      }
      setDetail({ title: r.title ?? `翻批记录#${r.id}`, rows: list })
    } catch {
      setDetail(null)
    } finally {
      setDetailLoading(false)
    }
  }

  const columns: ColumnsType<RepRebatchLog> = [
    { title: '标题', dataIndex: 'title', width: 200, ellipsis: true },
    { title: '车间', dataIndex: 'workshopId', width: 120, render: (v) => wsName[v] ?? v ?? '-' },
    { title: '班组', dataIndex: 'teamId', width: 100, render: (v) => teamName[v] ?? v ?? '-' },
    { title: '开始时间', dataIndex: 'startTime', width: 160 },
    { title: '结束时间', dataIndex: 'endTime', width: 160 },
    { title: '创建人', dataIndex: 'createBy', width: 90 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
    {
      title: '操作', width: 90,
      render: (_, r) => <a onClick={() => loadDetail(r)}>明细</a>,
    },
  ]

  const detailColumns: ColumnsType<RepRebatchDetail> = [
    { title: '生产批号', dataIndex: 'batchNo', width: 130 },
    { title: '追溯批号', dataIndex: 'traceBatchNo', width: 130 },
    { title: '原料', dataIndex: 'rawMaterial', width: 120, ellipsis: true },
    { title: '工序', dataIndex: 'processId', width: 90, render: (v) => procName[v] ?? v ?? '-' },
    { title: '机台', dataIndex: 'equipmentId', width: 90, render: (v) => eqName[v] ?? v ?? '-' },
    { title: '上批时间', dataIndex: 'batchOnTime', width: 150 },
    { title: '上批人', dataIndex: 'batchOnUser', width: 80 },
    { title: '下批时间', dataIndex: 'batchOffTime', width: 150 },
    { title: '下批人', dataIndex: 'batchOffUser', width: 80 },
    { title: '已下批', dataIndex: 'isBatchOff', width: 70, render: (v) => (v === 1 ? '是' : '否') },
    { title: '已了批', dataIndex: 'isFinished', width: 70, render: (v) => (v === 1 ? '是' : '否') },
  ]

  return (
    <div className="page">
      <div className="sec-title">翻批日志</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="车间">
            <Select allowClear placeholder="全部" style={{ width: 150 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="teamId" label="班组">
            <Select allowClear placeholder="全部" style={{ width: 130 }} showSearch optionFilterProp="label" options={teamOptions} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          <Button type="primary" onClick={() => { setGenOpen(true); genForm.resetFields() }}>生成翻批记录</Button>
          <span className="muted" style={{ marginLeft: 12, fontSize: 12 }}>按车间+班组+时段汇总上批汇报自动生成</span>
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      <Modal title="生成翻批记录" open={genOpen} onCancel={() => setGenOpen(false)} onOk={() => genForm.submit()} destroyOnClose>
        <Form form={genForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          await generateRebatch({
            workshopId: v.workshopId,
            teamId: v.teamId,
            startTime: v.range?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
            endTime: v.range?.[1]?.format('YYYY-MM-DDTHH:mm:ss'),
          })
          message.success('翻批记录已生成')
          setGenOpen(false)
          setSearch({})
        }}>
          <Form.Item name="workshopId" label="车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="teamId" label="班组" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={teamOptions} />
          </Form.Item>
          <Form.Item name="range" label="时段" rules={[{ required: true }]}>
            <DatePicker.RangePicker showTime style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={`翻批明细 - ${detail?.title ?? ''}`} open={!!detail} onCancel={() => setDetail(null)} footer={null} width={1100} destroyOnClose>
        <Table rowKey="id" size="small" columns={detailColumns} dataSource={detail?.rows ?? []} loading={detailLoading} pagination={false} scroll={{ x: 1200 }} style={{ marginTop: 8 }} />
      </Modal>
    </div>
  )
}
