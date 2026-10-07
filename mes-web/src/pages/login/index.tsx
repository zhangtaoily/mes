import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Form, Input, App } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { useAuth } from '@/store/auth'

/** 登录页：对接 POST /api/auth/login */
export default function Login() {
  const { login } = useAuth()
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)

  const onFinish = async (values: { loginName: string; password: string }) => {
    setLoading(true)
    try {
      await login(values.loginName, values.password)
      message.success('登录成功')
      navigate('/dashboard', { replace: true })
    } catch {
      // 拦截器已提示错误
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        height: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #1f2d3d 0%, #263445 60%, #2f4056 100%)',
      }}
    >
      <div
        style={{
          width: 380, background: '#fff', borderRadius: 8, padding: '38px 36px 30px',
          boxShadow: '0 8px 30px rgba(0,0,0,.25)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 26 }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: '#1f2d3d', letterSpacing: 2 }}>
            数智 · 生产执行系统
          </div>
          <div className="muted" style={{ marginTop: 6, letterSpacing: 4 }}>MANUFACTURING EXECUTION SYSTEM</div>
        </div>
        <Form onFinish={onFinish} size="large" initialValues={{ loginName: 'admin' }}>
          <Form.Item name="loginName" rules={[{ required: true, message: '请输入登录名' }]}>
            <Input prefix={<UserOutlined style={{ color: '#c0c4cc' }} />} placeholder="登录名" />
          </Form.Item>
          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined style={{ color: '#c0c4cc' }} />} placeholder="密码" />
          </Form.Item>
          <Form.Item style={{ marginBottom: 8 }}>
            <Button type="primary" htmlType="submit" block loading={loading}>
              登 录
            </Button>
          </Form.Item>
        </Form>
        <div className="muted" style={{ textAlign: 'center', fontSize: 12, marginTop: 14 }}>
          初始账号 admin / admin@2026（超管）· zhangt（工艺员+调度员）
        </div>
      </div>
    </div>
  )
}
