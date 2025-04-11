import { Router } from 'vue-router'

/**
 * 使用 View Transition API 进行页面导航
 * @param router Vue路由实例
 * @param to 目标路由
 * @param name 过渡名称（可选）
 */
export function navigateWithTransition(router: Router, to: string, name?: string) {
  // 检查浏览器是否支持 View Transition API
  if (!document.startViewTransition) {
    // 降级处理：直接导航
    router.push(to)
    return
  }

  // 使用 View Transition API
  document.startViewTransition(() => {
    // 如果提供了过渡名称，设置过渡名称
    if (name) {
      document.documentElement.classList.add(`vt-${name}`)
    }
    // 页面导航
    router.push(to).then(() => {
      // 导航完成后，移除过渡名称类
      if (name) {
        setTimeout(() => {
          document.documentElement.classList.remove(`vt-${name}`)
        }, 300)
      }
    })
  })
}