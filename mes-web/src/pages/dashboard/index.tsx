import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Col, Row, Spin } from 'antd'
import {
  AppstoreOutlined, AimOutlined, BarChartOutlined, CheckCircleOutlined, GiftOutlined,
  ProfileOutlined, SettingOutlined, TableOutlined,
} from '@ant-design/icons'
import dayjs from 'dayjs'
import { useAuth } from '@/store/auth'
import { pageTasks } from '@/api/plan'
import { pageBatchOn, pageOutputs } from '@/api/site'
import { getQcStatistics } from '@/api/qc'

interface Stat {
  title: string
  value: number | string
  color: string
  sub: string
}

/** 首页仪表盘：欢迎条 + 实时统计卡 + 快捷入口 + 待办 */
export default function Dashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [stats, setStats] = useState<Stat[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      pageTasks({ pageNum: 1, pageSize: 1 }),
      pageBatchOn({ pageNum: 1, pageSize: 1 }),
      pageOutputs({ pageNum: 1, pageSize: 1 }),
      getQcStatistics(),
    ])
      .then(([tasks, batchOn, outputs, qc]) => {
        setStats([
          { title: '生产任务总数', value: tasks.total, color: '#409eff', sub: 'plan 域 · 全部状态' },
          { title: '上批汇报记录', value: batchOn.total, color: '#67c23a', sub: 'produce 域 · 含历史批次' },
          { title: '产量汇报笔数', value: outputs.total, color: '#e6a23c', sub: 'produce 域 · 全部工序' },
          { title: '检验报告/超限', value: `${qc.total ?? 0} / ${qc.overLimitCount ?? 0}`, color: '#f56c6c', sub: 'qc 域 · 未作废口径' },
        ])
      })
      .catch(() => setStats([]))
      .finally(() => setLoading(false))
  }, [])

  const quicks: { icon: React.ReactNode; label: string; path: string; color: string }[] = [
    { icon: <SettingOutlined />, label: '计划工艺单', path: '/craft/plan', color: '#409eff' },
    { icon: <AppstoreOutlined />, label: 'K3订单查看', path: '/prod/k3-order', color: '#67c23a' },
    { icon: <ProfileOutlined />, label: '生产任务', path: '/prod/task', color: '#f56c6c' },
    { icon: <BarChartOutlined />, label: '生产排程', path: '/prod/schedule', color: '#e6a23c' },
    { icon: <AimOutlined />, label: '投料记录', path: '/site/feed-record', color: '#16a085' },
    { icon: <CheckCircleOutlined />, label: '检验报告', path: '/qc/report', color: '#9b59b6' },
    { icon: <GiftOutlined />, label: '包装执行', path: '/pack/execute', color: '#d35400' },
    { icon: <TableOutlined />, label: '报表中心', path: '/report/center', color: '#2980b9' },
  ]

  const todos: { text: string; path: string }[] = [
    { text: '计划工艺单：引入 K3 订单并编制', path: '/craft/plan' },
    { text: '生产排程：查看排程与制造卡打印', path: '/craft/machine-card' },
    { text: '现场：投料 → 上批 → 产量汇报闭环', path: '/site/batch-on' },
    { text: '质检：抽样登记 → 检验报告 → 公示', path: '/qc/sample' },
    { text: '包装：任务 → 逐箱称重 → 组托入库', path: '/pack/task' },
  ]

  return (
    <div className="page">
      {/* 欢迎条 */}
      <div
        style={{
          display: 'flex', gap: 24, background: '#fff', borderRadius: 5, padding: '12px 18px',
          color: '#606266', marginBottom: 12, boxShadow: '0 1px 3px rgba(0,0,0,.06)', flexWrap: 'wrap',
        }}
      >
        <span>欢迎您：<b style={{ color: '#303133' }}>{user?.userName}</b>（{user?.empNo}）</span>
        <span>角色：{user?.roles?.join('、') || '-'}</span>
        <span>登录时间：{dayjs().format('YYYY-MM-DD HH:mm:ss')}</span>
      </div>

      {/* 统计卡 */}
      <Row gutter={12} style={{ marginBottom: 12 }}>
        {loading ? (
          <div style={{ padding: 40, textAlign: 'center', width: '100%' }}><Spin /></div>
        ) : (
          stats.map((s) => (
            <Col span={6} key={s.title}>
              <div style={{ background: '#fff', borderRadius: 5, padding: 18, boxShadow: '0 1px 3px rgba(0,0,0,.06)' }}>
                <div style={{ color: '#606266', fontSize: 13, fontWeight: 600 }}>{s.title}</div>
                <div style={{ fontSize: 26, fontWeight: 700, margin: '6px 0 2px', color: s.color }}>{s.value}</div>
                <div style={{ color: '#909399', fontSize: 12 }}>{s.sub}</div>
              </div>
            </Col>
          ))
        )}
      </Row>

      {/* 快捷入口 */}
      <Card size="small" style={{ marginBottom: 12 }} styles={{ body: { padding: 14 } }}>
        <div className="sec-title" style={{ marginBottom: 10 }}>快捷入口</div>
        <Row gutter={[10, 10]}>
          {quicks.map((q) => (
            <Col span={3} key={q.path}>
              <div
                onClick={() => navigate(q.path)}
                style={{
                  background: '#fff', borderRadius: 5, textAlign: 'center', padding: '14px 4px',
                  color: '#606266', boxShadow: '0 1px 3px rgba(0,0,0,.06)', cursor: 'pointer',
                }}
              >
                <span style={{ fontSize: 22, color: q.color, display: 'block', marginBottom: 6 }}>{q.icon}</span>
                <span style={{ fontSize: 12 }}>{q.label}</span>
              </div>
            </Col>
          ))}
        </Row>
      </Card>

      {/* 业务闭环导航 */}
      <Card size="small" className="mb0" styles={{ body: { padding: 14 } }}>
        <div className="sec-title" style={{ marginBottom: 6 }}>业务主线（点击进入）</div>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {todos.map((t) => (
            <li
              key={t.path}
              style={{
                display: 'flex', justifyContent: 'space-between', padding: '9px 4px',
                borderBottom: '1px dashed #ebeef5', fontSize: 13,
              }}
            >
              <span>{t.text}</span>
              <a onClick={() => navigate(t.path)} style={{ cursor: 'pointer' }}>去处理</a>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  )
}
