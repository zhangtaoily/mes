import { useNavigate } from 'react-router-dom'
import { Avatar, Button } from 'antd'
import { DesktopOutlined, LogoutOutlined } from '@ant-design/icons'
import { useAuth } from '@/store/auth'

/** PDA 我的：用户信息 + 返回PC端 + 退出登录 */
export default function PdaMine() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()

  return (
    <div style={{ padding: 14 }}>
      <div
        style={{
          background: '#fff', borderRadius: 12, padding: '18px 16px', marginBottom: 12,
          display: 'flex', alignItems: 'center', gap: 14, boxShadow: '0 1px 4px rgba(0,0,0,.06)',
        }}
      >
        <Avatar size={54} style={{ background: '#409eff', fontSize: 22 }}>
          {(user?.userName ?? '用').slice(0, 1)}
        </Avatar>
        <div>
          <div style={{ fontSize: 17, fontWeight: 600 }}>{user?.userName ?? '-'}</div>
          <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>工号 {user?.empNo ?? '-'}</div>
        </div>
      </div>

      <div style={{ background: '#fff', borderRadius: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)', overflow: 'hidden' }}>
        <div
          onClick={() => navigate('/dashboard')}
          style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '16px 16px', cursor: 'pointer', borderBottom: '1px solid #f5f5f5' }}
        >
          <DesktopOutlined style={{ fontSize: 18, color: '#409eff' }} />
          <span style={{ fontSize: 15 }}>返回 PC 端</span>
        </div>
        <div
          onClick={logout}
          style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '16px 16px', cursor: 'pointer' }}
        >
          <LogoutOutlined style={{ fontSize: 18, color: '#f56c6c' }} />
          <span style={{ fontSize: 15, color: '#f56c6c' }}>退出登录</span>
        </div>
      </div>

      <Button block size="large" style={{ marginTop: 16, height: 46 }} onClick={() => navigate('/pda/home')}>
        返回工作台
      </Button>

      <div className="muted" style={{ textAlign: 'center', fontSize: 11, marginTop: 18 }}>
        数智·生产执行系统 手持终端 M1
      </div>
    </div>
  )
}
