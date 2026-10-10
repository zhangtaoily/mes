import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/store/auth'
import dayjs from 'dayjs'
import {
  ExperimentOutlined, ImportOutlined, PauseCircleOutlined,
  PlayCircleOutlined, RiseOutlined, ScanOutlined,
} from '@ant-design/icons'

const MODULES = [
  { path: '/pda/scan', label: '扫码作业', desc: '扫制造卡/排程单', icon: <ScanOutlined /> },
  { path: '/pda/batch-on', label: '上批', desc: '扫排程上机', icon: <PlayCircleOutlined /> },
  { path: '/pda/output', label: '产量汇报', desc: '逐班报产', icon: <RiseOutlined /> },
  { path: '/pda/feed', label: '投料', desc: '选任务投料', icon: <ImportOutlined /> },
  { path: '/pda/stop', label: '启停汇报', desc: '停机记录', icon: <PauseCircleOutlined /> },
  { path: '/pda/front-report', label: '前纺情况', desc: '温湿度/绕毛', icon: <ExperimentOutlined /> },
]

/** PDA 工作台：模块大图标网格 */
export default function PdaHome() {
  const navigate = useNavigate()
  const { user } = useAuth()

  return (
    <div style={{ padding: 14 }}>
      <div
        style={{
          background: 'linear-gradient(135deg, #409eff 0%, #2f7dd1 100%)',
          borderRadius: 12, color: '#fff', padding: '18px 16px', marginBottom: 14,
        }}
      >
        <div style={{ fontSize: 18, fontWeight: 700 }}>
          {user ? `${user.userName}，你好` : '你好'}
        </div>
        <div style={{ marginTop: 6, fontSize: 12, opacity: 0.85 }}>
          {dayjs().format('YYYY-MM-DD dddd')}
        </div>
      </div>

      <div
        onClick={() => navigate('/pda/scan')}
        style={{
          background: '#fff', borderRadius: 12, padding: '22px 16px', marginBottom: 14,
          display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer',
          boxShadow: '0 1px 4px rgba(0,0,0,.06)',
        }}
      >
        <div
          style={{
            width: 52, height: 52, borderRadius: 12, background: '#409eff',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 26, color: '#fff',
          }}
        >
          <ScanOutlined />
        </div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 17, fontWeight: 600 }}>扫码作业</div>
          <div className="muted" style={{ fontSize: 12, marginTop: 3 }}>扫制造卡二维码进入上批/下批/报产</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {MODULES.filter((m) => m.path !== '/pda/scan').map((m) => (
          <div
            key={m.path}
            onClick={() => navigate(m.path)}
            style={{
              background: '#fff', borderRadius: 12, padding: '16px 14px', cursor: 'pointer',
              boxShadow: '0 1px 4px rgba(0,0,0,.06)',
            }}
          >
            <div style={{ fontSize: 24, color: '#409eff' }}>{m.icon}</div>
            <div style={{ fontSize: 15, fontWeight: 600, marginTop: 8 }}>{m.label}</div>
            <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>{m.desc}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
