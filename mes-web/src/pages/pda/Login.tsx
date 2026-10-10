import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Form, Input, App } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useAuth } from '@/store/auth'
import { tokenStore } from '@/api/request'

/** PDA 登录页（工号+密码 → /api/auth/login，与 PC 同 token） */
export default function PdaLogin() {
  const { login } = useAuth()
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  // 用 localStorage 实时值判断（401 拦截器清 store 后 React 状态可能仍是旧 token）
  if (tokenStore.get()) {
    navigate('/pda/home', { replace: true })
    return null
  }

  const onFinish = async (values: { loginName: string; password: string }) => {
    setLoading(true)
    try {
      await login(values.loginName, values.password)
      message.success('登录成功')
      navigate('/pda/home', { replace: true })
    } catch {
      // 拦截器已提示错误
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        height: '100vh', maxWidth: 420, margin: '0 auto',
        display: 'flex', flexDirection: 'column', justifyContent: 'center',
        padding: '0 28px', background: 'linear-gradient(160deg, #1f2d3d 0%, #263445 70%, #2f4056 100%)',
      }}
    >
      <div style={{ textAlign: 'center', marginBottom: 36 }}>
        <div style={{ fontSize: 24, fontWeight: 700, color: '#fff', letterSpacing: 2 }}>数智 · 生产执行</div>
        <div style={{ marginTop: 8, fontSize: 13, color: 'rgba(255,255,255,.55)', letterSpacing: 3 }}>
          手持终端 PDA
        </div>
      </div>
      <Form onFinish={onFinish} size="large" initialValues={{ loginName: '' }}>
        <Form.Item name="loginName" rules={[{ required: true, message: '请输入工号/登录名' }]}>
          <Input prefix={<UserOutlined style={{ color: '#c0c4cc' }} />} placeholder="工号/登录名" autoComplete="username" />
        </Form.Item>
        <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
          <Input.Password prefix={<LockOutlined style={{ color: '#c0c4cc' }} />} placeholder="密码" autoComplete="current-password" />
        </Form.Item>
        <Form.Item style={{ marginBottom: 8 }}>
          <Button type="primary" htmlType="submit" block loading={loading} style={{ height: 46, fontSize: 16 }}>
            登 录
          </Button>
        </Form.Item>
      </Form>
      <div style={{ textAlign: 'center', marginTop: 18, fontSize: 12, color: 'rgba(255,255,255,.45)' }}>
        扫码枪即插即用 · 光标定位输入框后扫码
      </div>
    </div>
  )
}
