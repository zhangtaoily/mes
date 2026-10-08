import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { useWorkshops } from '@/hooks/useOptions'
import { BoolTag } from '@/components/StatusTag'
import {
  pageWorkshops, saveWorkshop, updateWorkshop, deleteWorkshop,
  pageTeams, saveTeam, updateTeam, deleteTeam,
} from '@/api/base'
import type { BaseWorkshopFull, BaseTeam } from '@/api/base'

const WS_TYPE = ['纺纱', '复精梳', '倒筒', '包装']
const wsTypeText = (v?: number) => (v && WS_TYPE[v - 1]) || '-'

/** 车间与班组维护 */
export default function Workshop() {
  const { message } = App.useApp()
  const { nameMap: wsName, options: wsOptions } = useWorkshops()

  const ws = usePagedList<BaseWorkshopFull>('/api/base/workshops')
  const team = usePagedList<BaseTeam>('/api/base/teams', {}, { pageSize: 50 })
  const [wsForm] = Form.useForm()
  const [teamForm] = Form.useForm()
  const [wsOpen, setWsOpen] = useState(false)
  const [teamOpen, setTeamOpen] = useState(false)
  const [editingWs, setEditingWs] = useState<BaseWorkshopFull | null>(null)
  const [editingTeam, setEditingTeam] = useState<BaseTeam | null>(null)

  const openWs = (r?: BaseWorkshopFull) => {
    setEditingWs(r ?? null)
    setWsOpen(true)
    wsForm.resetFields()
    if (r) wsForm.setFieldsValue(r)
  }
  const openTeam = (r?: BaseTeam) => {
    setEditingTeam(r ?? null)
    setTeamOpen(true)
    teamForm.resetFields()
    if (r) teamForm.setFieldsValue(r)
  }

  const wsColumns: ColumnsType<BaseWorkshopFull> = [
    { title: '车间编码', dataIndex: 'workshopCode', width: 110 },
    { title: '车间名称', dataIndex: 'workshopName', width: 200 },
    { title: '车间类型', dataIndex: 'workshopType', width: 100, render: wsTypeText },
    { title: 'K3车间ID', dataIndex: 'k3WorkshopId', width: 120 },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openWs(r)}>编辑</a>
          <Popconfirm title="确认删除该车间？" onConfirm={async () => { await deleteWorkshop(r.id); message.success('已删除'); ws.reload() }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]
  const teamColumns: ColumnsType<BaseTeam> = [
    { title: '班组名称', dataIndex: 'teamName', width: 160 },
    { title: '所属车间', dataIndex: 'workshopId', width: 200, render: (v) => wsName[v] ?? v },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '启用']} /> },
    {
      title: '操作', width: 130,
      render: (_, r) => (
        <>
          <a onClick={() => openTeam(r)}>编辑</a>
          <Popconfirm title="确认删除该班组？" onConfirm={async () => { await deleteTeam(r.id); message.success('已删除'); team.reload() }}>
            <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
          </Popconfirm>
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">车间与班组</div>

      <Card size="small" style={{ marginBottom: 12 }}>
        <div style={{ marginBottom: 10, display: 'flex', gap: 8 }}>
          <Button type="primary" onClick={() => openWs()}>新增车间</Button>
        </div>
        <Form layout="inline" onFinish={(v) => ws.setSearch(cleanParams(v))}>
          <Form.Item name="workshopName" label="车间名称"><Input allowClear placeholder="车间名" style={{ width: 140 }} /></Form.Item>
          <Form.Item name="workshopType" label="车间类型">
            <Select allowClear placeholder="全部" style={{ width: 110 }} options={WS_TYPE.map((n, i) => ({ label: n, value: i + 1 }))} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => ws.setSearch({})}>重置</Button></Form.Item>
        </Form>
        <Table rowKey="id" size="small" style={{ marginTop: 10 }} columns={wsColumns} dataSource={ws.rows} loading={ws.loading} pagination={ws.pagination} />
      </Card>

      <Card size="small">
        <div style={{ marginBottom: 10, display: 'flex', gap: 8 }}>
          <Button type="primary" onClick={() => openTeam()}>新增班组</Button>
        </div>
        <Form layout="inline" onFinish={(v) => team.setSearch(cleanParams(v))}>
          <Form.Item name="workshopId" label="所属车间">
            <Select allowClear placeholder="全部" style={{ width: 180 }} showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="teamName" label="班组名称"><Input allowClear placeholder="甲班" style={{ width: 120 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => team.setSearch({})}>重置</Button></Form.Item>
        </Form>
        <Table rowKey="id" size="small" style={{ marginTop: 10 }} columns={teamColumns} dataSource={team.rows} loading={team.loading} pagination={team.pagination} />
      </Card>

      <Modal title={editingWs ? `编辑车间 - ${editingWs.workshopName}` : '新增车间'} open={wsOpen} onCancel={() => setWsOpen(false)} onOk={() => wsForm.submit()} destroyOnClose>
        <Form form={wsForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editingWs) { await updateWorkshop(editingWs.id, v); message.success('修改成功') }
          else { await saveWorkshop(v); message.success('新增成功') }
          setWsOpen(false); ws.reload()
        }}>
          <Form.Item name="workshopCode" label="车间编码" rules={[{ required: true }]}><Input disabled={!!editingWs} placeholder="FS004" /></Form.Item>
          <Form.Item name="workshopName" label="车间名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="workshopType" label="车间类型" rules={[{ required: true }]}>
            <Select options={WS_TYPE.map((n, i) => ({ label: n, value: i + 1 }))} />
          </Form.Item>
          <Form.Item name="k3WorkshopId" label="K3车间ID"><Input /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={editingTeam ? `编辑班组 - ${editingTeam.teamName}` : '新增班组'} open={teamOpen} onCancel={() => setTeamOpen(false)} onOk={() => teamForm.submit()} destroyOnClose>
        <Form form={teamForm} layout="vertical" style={{ marginTop: 12 }} onFinish={async (v) => {
          if (editingTeam) { await updateTeam(editingTeam.id, v); message.success('修改成功') }
          else { await saveTeam(v); message.success('新增成功') }
          setTeamOpen(false); team.reload()
        }}>
          <Form.Item name="workshopId" label="所属车间" rules={[{ required: true }]}>
            <Select showSearch optionFilterProp="label" options={wsOptions} />
          </Form.Item>
          <Form.Item name="teamName" label="班组名称" rules={[{ required: true }]}><Input placeholder="甲班" /></Form.Item>
          <Form.Item name="status" label="状态" initialValue={1}>
            <Select options={[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
