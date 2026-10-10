import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Avatar, Badge, Button, List, Spin } from 'antd'
import { CloudUploadOutlined, DeleteOutlined, DesktopOutlined, LogoutOutlined } from '@ant-design/icons'
import { useAuth } from '@/store/auth'
import { offlineClear, offlineList, offlineRemove, offlineRetryAll } from '@/utils/offline'
import type { OfflineItem } from '@/utils/offline'

/** PDA 我的：用户信息 + 离线待提交 + 返回PC端 + 退出登录 */
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

      <OfflinePanel />

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
        数智·生产执行系统 手持终端
      </div>
    </div>
  )
}

/** 离线待提交：网络异常时暂存的提交，恢复联网后重放（后端重新取号） */
function OfflinePanel() {
  const { message } = App.useApp()
  const [items, setItems] = useState<OfflineItem[]>([])
  const [retrying, setRetrying] = useState(false)

  const refresh = () => setItems(offlineList())

  useEffect(() => { refresh() }, [])

  if (items.length === 0) {
    return null
  }

  const retry = async () => {
    setRetrying(true)
    try {
      const { ok, fail } = await offlineRetryAll()
      if (ok > 0) message.success(`已重试提交 ${ok} 条`)
      if (fail > 0) message.warning(`${fail} 条仍失败，已保留`)
      refresh()
    } catch {
      message.error('登录已过期，请重新登录后重试')
    } finally {
      setRetrying(false)
    }
  }

  return (
    <div style={{ background: '#fff', borderRadius: 12, padding: '12px 14px', marginBottom: 12, boxShadow: '0 1px 4px rgba(0,0,0,.06)' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <Badge count={items.length} offset={[10, 0]} size="small">
          <span style={{ fontSize: 15, fontWeight: 600 }}>
            <CloudUploadOutlined style={{ color: '#fa8c16', marginRight: 6 }} />离线待提交
          </span>
        </Badge>
        <div style={{ display: 'flex', gap: 8 }}>
          <Button size="small" loading={retrying} onClick={retry}>全部重试</Button>
          <Button size="small" danger onClick={() => { offlineClear(); refresh() }}>清空</Button>
        </div>
      </div>
      {retrying && <div style={{ textAlign: 'center', padding: 16 }}><Spin /></div>}
      {!retrying && (
        <List
          size="small"
          dataSource={items}
          renderItem={(it) => (
            <List.Item
              style={{ padding: '8px 0' }}
              actions={[
                <a key="del" style={{ color: '#f56c6c' }} onClick={() => { offlineRemove(it.id); refresh() }}>
                  <DeleteOutlined />
                </a>,
              ]}
            >
              <List.Item.Meta
                title={<span style={{ fontSize: 13 }}>{it.label}</span>}
                description={<span style={{ fontSize: 11 }}>{it.createTime}</span>}
              />
            </List.Item>
          )}
        />
      )}
    </div>
  )
}
