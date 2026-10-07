import { useState } from 'react'
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Table } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { usePagedList } from '@/hooks/usePagedList'
import { cleanParams } from '@/utils/cleanParams'
import { saveUser, updateUser, deleteUser, resetPassword, changeUserStatus, getAllRoles } from '@/api/sys'
import type { SysUser, SysRole } from '@/api/sys'
import { useAuth } from '@/store/auth'
import { BoolTag } from '@/components/StatusTag'
import { useEffect } from 'react'

interface FormVals {
  loginName: string
  userName: string
  empNo?: string
  phone?: string
  password?: string
}

/** 用户管理：CRUD + 重置密码 + 状态切换 */
export default function SysUser() {
  const { message } = App.useApp()
  const { hasPerm } = useAuth()
  const [roles, setRoles] = useState<SysRole[]>([])
  const [editing, setEditing] = useState<SysUser | null>(null)
  const [open, setOpen] = useState(false)
  const { rows, loading, pagination, setSearch, reload } = usePagedList<SysUser>('/api/sys/users')
  const [form] = Form.useForm()
  const [pwdForm] = Form.useForm()
  const [pwdUser, setPwdUser] = useState<SysUser | null>(null)

  useEffect(() => {
    getAllRoles().then(setRoles).catch(() => setRoles([]))
  }, [])

  const openForm = (u?: SysUser) => {
    setEditing(u ?? null)
    setOpen(true)
    form.resetFields()
    if (u) form.setFieldsValue({ loginName: u.loginName, userName: u.userName, empNo: u.empNo, phone: u.phone })
  }

  const submit = async (v: FormVals) => {
    if (editing) {
      await updateUser(editing.id, v)
      message.success('修改成功')
    } else {
      await saveUser(v)
      message.success('新增成功')
    }
    setOpen(false)
    reload()
  }

  const columns: ColumnsType<SysUser> = [
    { title: '用户ID', dataIndex: 'id', width: 70 },
    { title: '登录名', dataIndex: 'loginName', width: 120 },
    { title: '姓名', dataIndex: 'userName', width: 110 },
    { title: '工号', dataIndex: 'empNo', width: 90 },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '状态', dataIndex: 'status', width: 80, render: (v) => <BoolTag value={v} labels={['停用', '正常']} /> },
    {
      title: '操作', width: 240,
      render: (_, r) => (
        <>
          {hasPerm('sys:user:edit') ? <a onClick={() => openForm(r)}>编辑</a> : null}
          {hasPerm('sys:user:resetPwd') ? (
            <a style={{ marginLeft: 8 }} onClick={() => { setPwdUser(r); pwdForm.resetFields() }}>重置密码</a>
          ) : null}
          {hasPerm('sys:user:edit') ? (
            <Popconfirm
              title={r.status ? '确认停用该用户？' : '确认启用该用户？'}
              onConfirm={async () => { await changeUserStatus(r.id, r.status ? 0 : 1); message.success('已更新'); reload() }}
            >
              <a style={{ marginLeft: 8, color: r.status ? '#e6a23c' : '#67c23a' }}>{r.status ? '停用' : '启用'}</a>
            </Popconfirm>
          ) : null}
          {hasPerm('sys:user:del') ? (
            <Popconfirm title="确认删除该用户？" onConfirm={async () => { await deleteUser(r.id); message.success('已删除'); reload() }}>
              <a style={{ marginLeft: 8, color: '#f56c6c' }}>删除</a>
            </Popconfirm>
          ) : null}
        </>
      ),
    },
  ]

  return (
    <div className="page">
      <div className="sec-title">用户管理</div>
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={form} layout="inline" onFinish={(v) => setSearch(cleanParams(v))}>
          <Form.Item name="loginName" label="登录名"><Input allowClear placeholder="登录名" style={{ width: 120 }} /></Form.Item>
          <Form.Item name="userName" label="姓名"><Input allowClear placeholder="姓名" style={{ width: 100 }} /></Form.Item>
          <Form.Item name="status" label="状态">
            <Select allowClear placeholder="全部" style={{ width: 90 }} options={[{ label: '正常', value: 1 }, { label: '停用', value: 0 }]} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit">搜索</Button></Form.Item>
          <Form.Item><Button onClick={() => setSearch({})}>重置</Button></Form.Item>
        </Form>
      </Card>
      <Card size="small">
        <div style={{ marginBottom: 10 }}>
          {hasPerm('sys:user:add') ? <Button type="primary" onClick={() => openForm()}>新增用户</Button> : null}
        </div>
        <Table rowKey="id" size="small" columns={columns} dataSource={rows} loading={loading} pagination={pagination} />
      </Card>

      {/* 新增/编辑弹窗 */}
      <Modal
        title={editing ? `编辑用户 - ${editing.userName}` : '新增用户'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        destroyOnClose
      >
        <Form form={form} layout="vertical" onFinish={submit} style={{ marginTop: 12 }}>
          <Form.Item name="loginName" label="登录名" rules={[{ required: true }]}>
            <Input disabled={!!editing} placeholder="登录名" />
          </Form.Item>
          <Form.Item name="userName" label="姓名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="empNo" label="工号"><Input /></Form.Item>
          <Form.Item name="phone" label="手机号"><Input /></Form.Item>
          {!editing ? (
            <Form.Item name="password" label="初始密码" rules={[{ required: true, min: 6, message: '至少6位' }]}>
              <Input.Password placeholder="至少6位" />
            </Form.Item>
          ) : null}
        </Form>
      </Modal>

      {/* 重置密码弹窗 */}
      <Modal
        title={`重置密码 - ${pwdUser?.userName ?? ''}`}
        open={!!pwdUser}
        onCancel={() => setPwdUser(null)}
        onOk={() => pwdForm.submit()}
        destroyOnClose
      >
        <Form form={pwdForm} layout="vertical" onFinish={async (v) => { await resetPassword(pwdUser!.id, v.password); message.success('密码已重置'); setPwdUser(null) }} style={{ marginTop: 12 }}>
          <Form.Item name="password" label="新密码" rules={[{ required: true, min: 6, message: '至少6位' }]}>
            <Input.Password placeholder="至少6位" />
          </Form.Item>
        </Form>
        <div className="muted" style={{ fontSize: 12 }}>可用角色（分配角色请用角色管理接口）：{roles.map((r) => r.roleName).join('、') || '-'}</div>
      </Modal>
    </div>
  )
}
