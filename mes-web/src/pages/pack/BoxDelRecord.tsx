import { Button, Card, Form, Input, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { pageBoxDeletes } from '@/api/pack'
import type { PkBoxDelete } from '@/api/pack'

/** 箱子删除记录（删箱审计，只读） */
export default function BoxDelRecord() {
  const { rows, loading, pagination, setSearch } = usePagedList<PkBoxDelete>('/api/pack/box-deletes')

  const columns: ColumnsType<PkBoxDelete> = [
    { title: '箱子编码', dataIndex: 'boxCode', width: 140, fixed: 'left' },
    { title: '托盘码', dataIndex: 'palletCode', width: 130 },
    { title: '箱序', dataIndex: 'seqNo', width: 70, align: 'right' },
    { title: '皮重(kg)', dataIndex: 'tareWeight', width: 95, align: 'right' },
    { title: '毛重(kg)', dataIndex: 'grossWeight', width: 95, align: 'right' },
    { title: '筒数', dataIndex: 'tubeCount', width: 75, align: 'right' },
    { title: '纸管重(kg)', dataIndex: 'paperTubeWeight', width: 100, align: 'right' },
    { title: '删除人', dataIndex: 'deleteUser', width: 100 },
    { title: '删除时间', dataIndex: 'deleteTime', width: 170 },
  ]

  return (
    <div className="page">
      <div className="sec-title">箱子删除记录</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="boxCode" label="箱子编码"><Input allowClear placeholder="BX..." style={{ width: 130 }} /></Form.Item>
          <Form.Item name="palletCode" label="托盘码"><Input allowClear placeholder="TP..." style={{ width: 120 }} /></Form.Item>
          <Form.Item name="deleteUser" label="删除人"><Input allowClear placeholder="删除人" style={{ width: 110 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>
    </div>
  )
}
