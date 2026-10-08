import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pagePackExecutes, getPackExecuteDetail, deletePackBox, finishPackExecute } from '@/api/pack'
import type { PkExecute, PkBox } from '@/api/pack'

/** 装箱记录（执行单 + 逐箱明细，恒等式审计） */
export default function BoxRecord() {
  const { message } = App.useApp()
  const { rows, loading, pagination, setSearch, reload } = usePagedList<PkExecute>('/api/pack/executes')
  const [detail, setDetail] = useState<{ execute: PkExecute; boxes: PkBox[] } | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const openDetail = async (r: PkExecute) => {
    setDetailLoading(true)
    setDetail({ execute: r, boxes: [] })
    try {
      setDetail(await getPackExecuteDetail(r.id))
    } catch {
      setDetail(null)
    } finally {
      setDetailLoading(false)
    }
  }

  const columns: ColumnsType<PkExecute> = [
    { title: '执行单号', dataIndex: 'executeNo', width: 150, fixed: 'left' },
    { title: '包装批号', dataIndex: 'packBatchNo', width: 130 },
    { title: '入库批号', dataIndex: 'warehouseBatchNo', width: 120 },
    { title: '缸号', dataIndex: 'dyeVatNo', width: 90 },
    { title: '产品名称', dataIndex: 'productName', width: 130, ellipsis: true },
    { title: '原料', dataIndex: 'rawMaterial', width: 110, ellipsis: true },
    { title: '色号', dataIndex: 'colorNo', width: 80 },
    { title: '规格', dataIndex: 'productSpec', width: 100 },
    { title: '箱数', dataIndex: 'boxCount', width: 70, align: 'right' },
    { title: '总筒数', dataIndex: 'sumTube', width: 80, align: 'right' },
    { title: '总公重', dataIndex: 'sumPublicWeight', width: 90, align: 'right' },
    { title: '总毛重', dataIndex: 'sumGrossWeight', width: 90, align: 'right' },
    { title: '总净重', dataIndex: 'sumNetWeight', width: 90, align: 'right' },
    {
      title: '状态', dataIndex: 'isPacked', width: 80,
      render: (v) => (v === 1 ? <span style={{ color: '#52c41a' }}>已了包</span> : '包装中'),
    },
    {
      title: '操作', width: 140, fixed: 'right',
      render: (_, r) => (
        <>
          <a onClick={() => openDetail(r)}>明细</a>
          {r.isPacked !== 1 && (
            <Popconfirm title="确认了包？了包后不可再加箱" onConfirm={async () => { await finishPackExecute(r.id); message.success('已了包'); reload() }}>
              <a style={{ marginLeft: 8, color: '#52c41a' }}>了包</a>
            </Popconfirm>
          )}
        </>
      ),
    },
  ]

  const boxColumns: ColumnsType<PkBox> = [
    { title: '箱号', dataIndex: 'boxCode', width: 130 },
    { title: '序号', dataIndex: 'seqNo', width: 60, align: 'right' },
    { title: '状态', dataIndex: 'boxStatus', width: 80 },
    { title: '筒数', dataIndex: 'grainCount', width: 70, align: 'right' },
    { title: '公重(kg)', dataIndex: 'publicWeight', width: 90, align: 'right' },
    { title: '净重(kg)', dataIndex: 'netWeight', width: 90, align: 'right' },
    { title: '毛重(kg)', dataIndex: 'grossWeight', width: 90, align: 'right' },
    { title: '皮重(kg)', dataIndex: 'tareWeight', width: 90, align: 'right' },
    { title: '包装规格', dataIndex: 'packSpec', width: 100 },
    { title: '扎线颜色', dataIndex: 'tieLineColor', width: 85 },
    { title: '托盘码', dataIndex: 'palletCode', width: 110 },
    { title: '包装工', dataIndex: 'packUser', width: 80 },
    { title: '装箱时间', dataIndex: 'boxTime', width: 150 },
    {
      title: '操作', width: 80,
      render: (_, b) => (
        <Popconfirm title="删箱将审计留痕并回退汇总，确认？" onConfirm={async () => {
          await deletePackBox(b.id)
          message.success('已删箱')
          if (detail) setDetail(await getPackExecuteDetail(detail.execute.id))
          reload()
        }}>
          <a style={{ color: '#f56c6c' }}>删箱</a>
        </Popconfirm>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">装箱记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="executeNo" label="执行单号"><Input allowClear placeholder="BX..." style={{ width: 140 }} /></Form.Item>
          <Form.Item name="packBatchNo" label="包装批号"><Input allowClear placeholder="批号" style={{ width: 130 }} /></Form.Item>
          <Form.Item name="warehouseBatchNo" label="入库批号"><Input allowClear placeholder="批号" style={{ width: 120 }} /></Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">搜索</Button>
          </Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} scroll={{ x: 1600 }} />
      </Card>

      <Modal
        title={`箱子明细 - ${detail?.execute.executeNo ?? ''}`}
        open={!!detail}
        onCancel={() => setDetail(null)}
        footer={null}
        width={1250}
        destroyOnClose
      >
        <div className="muted" style={{ marginBottom: 8 }}>
          {detail?.execute.productName} | {detail?.execute.rawMaterial} | {detail?.execute.colorNo} | {detail?.execute.productSpec}
          （箱数 {detail?.execute.boxCount ?? 0}，总筒 {detail?.execute.sumTube ?? 0}，总净重 {detail?.execute.sumNetWeight ?? 0}kg）
        </div>
        <Table rowKey="id" size="small" columns={boxColumns} dataSource={detail?.boxes ?? []} loading={detailLoading} pagination={false} scroll={{ x: 1400 }} />
      </Modal>
    </div>
  )
}
