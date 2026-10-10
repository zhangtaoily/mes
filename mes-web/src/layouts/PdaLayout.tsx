import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { HomeOutlined, ScanOutlined, UserOutlined } from '@ant-design/icons'
import { useAuth } from '@/store/auth'

const TITLES: Record<string, string> = {
  '/pda/home': '工作台',
  '/pda/scan': '扫码作业',
  '/pda/batch-on': '上批',
  '/pda/output': '产量汇报',
  '/pda/feed': '投料',
  '/pda/stop': '启停汇报',
  '/pda/front-report': '前纺情况',
  '/pda/pack': '装箱作业',
  '/pda/pallet': '组托作业',
  '/pda/print': '打印作业',
  '/pda/qc-pack': '包装质检',
  '/pda/pick': '领退料确认',
  '/pda/truck': '周转车作业',
  '/pda/steam': '蒸纱作业',
  '/pda/check': '点检执行',
  '/pda/repair': '报修登记',
  '/pda/mine': '我的',
}

const TABS = [
  { path: '/pda/home', label: '工作台', icon: <HomeOutlined /> },
  { path: '/pda/scan', label: '扫码', icon: <ScanOutlined /> },
  { path: '/pda/mine', label: '我的', icon: <UserOutlined /> },
]

/** 手持终端壳：手机版布局（顶部标题栏 + 内容区 + 底部Tab），桌面浏览器居中手机框预览 */
export default function PdaLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const title = TITLES[location.pathname] ?? '手持终端'
  const active = TABS.some((t) => t.path === location.pathname) ? location.pathname : ''

  return (
    <div
      style={{
        maxWidth: 420, margin: '0 auto', height: '100vh',
        display: 'flex', flexDirection: 'column', background: '#f5f6fa',
        boxShadow: '0 0 24px rgba(0,0,0,.12)',
      }}
    >
      <div
        style={{
          height: 48, background: '#1f2d3d', color: '#fff', flexShrink: 0,
          display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 14px',
        }}
      >
        <span style={{ fontSize: 16, fontWeight: 600 }}>{title}</span>
        <span style={{ fontSize: 12, opacity: 0.85 }}>
          {user ? `${user.userName}${user.empNo ? `(${user.empNo})` : ''}` : ''}
        </span>
      </div>
      <div style={{ flex: 1, overflowY: 'auto' }}>
        <Outlet />
      </div>
      <div style={{ height: 58, background: '#fff', borderTop: '1px solid #e8e8e8', display: 'flex', flexShrink: 0 }}>
        {TABS.map((t) => (
          <div
            key={t.path}
            onClick={() => navigate(t.path)}
            style={{
              flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
              justifyContent: 'center', gap: 2, cursor: 'pointer',
              color: active === t.path ? '#409eff' : '#909399',
            }}
          >
            <span style={{ fontSize: 20 }}>{t.icon}</span>
            <span style={{ fontSize: 11 }}>{t.label}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
