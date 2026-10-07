import { useMemo } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Avatar, Breadcrumb, Dropdown, Layout, Menu } from 'antd'
import type { MenuProps } from 'antd'
import {
  AppstoreOutlined, AimOutlined, BarChartOutlined, BoxPlotOutlined, CheckCircleOutlined,
  DatabaseOutlined, DownOutlined, HomeOutlined, LogoutOutlined, ProfileOutlined,
  SafetyOutlined, SettingOutlined, TableOutlined, ToolOutlined,
} from '@ant-design/icons'
import { useAuth } from '@/store/auth'
import type { MenuNode } from '@/api/auth'
import dayjs from 'dayjs'

const { Sider, Header, Content } = Layout

/** 顶层目录图标（按 path 前缀匹配） */
const ICONS: Record<string, React.ReactNode> = {
  '/dashboard': <HomeOutlined />,
  '/prod': <AppstoreOutlined />,
  '/craft': <SettingOutlined />,
  '/qc': <CheckCircleOutlined />,
  '/site': <AimOutlined />,
  '/equip': <ToolOutlined />,
  '/pack': <BoxPlotOutlined />,
  '/stat': <BarChartOutlined />,
  '/report': <TableOutlined />,
  '/base': <DatabaseOutlined />,
  '/sys': <SafetyOutlined />,
}

type MenuItem = Required<MenuProps>['items'][number]

/** 后端菜单树 → antd Menu items（只保留目录与菜单，即 menuType 1/2） */
function toMenuItems(nodes: MenuNode[]): MenuItem[] {
  return nodes
    .filter((n) => n.menuType === 1 || n.menuType === 2)
    .map((n) => {
      const children = n.children?.length ? toMenuItems(n.children) : undefined
      const icon = n.menuType === 1 ? (ICONS[n.path ?? ''] ?? <ProfileOutlined />) : undefined
      return {
        key: n.menuType === 2 ? (n.path ?? String(n.id)) : `dir-${n.id}`,
        label: n.menuName,
        icon,
        children,
      } as MenuItem
    })
}

/** 找当前路径的菜单链（面包屑） */
function findChain(nodes: MenuNode[], path: string, chain: MenuNode[] = []): MenuNode[] | null {
  for (const n of nodes) {
    const next = [...chain, n]
    if (n.menuType === 2 && n.path === path) return next
    if (n.children?.length) {
      const r = findChain(n.children, path, next)
      if (r) return r
    }
  }
  return null
}

export default function MainLayout() {
  const { menus, user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const items = useMemo(() => toMenuItems(menus), [menus])

  const chain = useMemo(() => findChain(menus, location.pathname), [menus, location.pathname])
  const crumbs = (chain ?? []).map((n) => ({ title: n.menuName }))

  // 展开的目录：当前路径所在顶层目录
  const openKeys = chain?.length ? [`dir-${chain[0].id}`] : []

  const userMenu: MenuProps['items'] = [
    { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
  ]

  return (
    <Layout style={{ height: '100vh', overflow: 'hidden' }}>
      <Sider width={216} theme="dark" style={{ background: '#263445', display: 'flex', flexDirection: 'column' }}>
        <div
          style={{
            height: 56, display: 'flex', alignItems: 'center', justifyContent: 'center',
            background: '#1f2d3d', color: '#fff', fontWeight: 700, fontSize: 14,
            letterSpacing: 1, flexShrink: 0,
          }}
        >
          数智 · 生产执行系统
        </div>
        <Menu
          theme="dark"
          mode="inline"
          style={{ flex: 1, overflowY: 'auto', paddingTop: 4, background: '#263445' }}
          items={items}
          selectedKeys={[location.pathname]}
          defaultOpenKeys={openKeys}
          onClick={({ key }) => {
            if (!key.startsWith('dir-')) navigate(key)
          }}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            height: 56, background: '#fff', boxShadow: '0 1px 4px rgba(0,0,0,.08)',
            display: 'flex', alignItems: 'center', padding: '0 20px', gap: 16, zIndex: 5,
          }}
        >
          <Breadcrumb items={crumbs.length ? crumbs : [{ title: '首页' }]} />
          <div style={{ flex: 1 }} />
          <span className="muted">{dayjs().format('YYYY-MM-DD ddd')}</span>
          <Dropdown
            menu={{ items: userMenu, onClick: ({ key }) => key === 'logout' && logout() }}
            placement="bottomRight"
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', color: '#606266' }}>
              <Avatar size={30} style={{ background: '#409eff', fontSize: 13 }}>
                {(user?.userName ?? '用').slice(0, 1)}
              </Avatar>
              <span>{user ? `${user.userName}(${user.empNo})` : '-'}</span>
              <DownOutlined style={{ fontSize: 10, color: '#909399' }} />
            </span>
          </Dropdown>
        </Header>
        <Content style={{ background: '#f0f2f5', overflowY: 'auto' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
