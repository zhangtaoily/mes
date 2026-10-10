import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { getMenus, getUserInfo, loginApi } from '@/api/auth'
import type { MenuNode, UserInfo } from '@/api/auth'
import { tokenStore } from '@/api/request'

interface AuthContextValue {
  token: string | null
  user: UserInfo | null
  menus: MenuNode[]
  ready: boolean // 初始化完成（token 存在时已拉取用户与菜单）
  login: (loginName: string, password: string) => Promise<void>
  logout: () => void
  /** 按钮级权限判断：超管 perms=['*'] 直接放行 */
  hasPerm: (perm?: string | null) => boolean
}

const AuthContext = createContext<AuthContextValue>(null as unknown as AuthContextValue)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => tokenStore.get())
  const [user, setUser] = useState<UserInfo | null>(null)
  const [menus, setMenus] = useState<MenuNode[]>([])
  const [ready, setReady] = useState(true)

  // 存在 token 时加载用户信息 + 菜单
  useEffect(() => {
    if (!token) {
      setUser(null)
      setMenus([])
      return
    }
    let alive = true
    setReady(false)
    Promise.all([getUserInfo(), getMenus()])
      .then(([u, m]) => {
        if (!alive) return
        setUser(u)
        setMenus(m)
      })
      .catch(() => {
        // 401 已由拦截器处理（清 token 跳登录）
      })
      .finally(() => alive && setReady(true))
    return () => {
      alive = false
    }
  }, [token])

  const login = useCallback(async (loginName: string, password: string) => {
    const r = await loginApi({ loginName, password })
    tokenStore.set(r.token)
    setToken(r.token)
  }, [])

  const logout = useCallback(() => {
    tokenStore.clear()
    setToken(null)
    // PDA 上下文回 PDA 登录页
    location.hash = location.hash.startsWith('#/pda') ? '#/pda/login' : '#/login'
  }, [])

  const hasPerm = useCallback(
    (perm?: string | null) => {
      if (!perm) return true
      if (!user) return false
      if (user.perms?.includes('*')) return true
      return user.perms?.includes(perm) ?? false
    },
    [user],
  )

  const value = useMemo(
    () => ({ token, user, menus, ready, login, logout, hasPerm }),
    [token, user, menus, ready, login, logout, hasPerm],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}
