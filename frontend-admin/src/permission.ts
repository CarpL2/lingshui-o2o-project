import router from './router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { Message } from 'element-ui'
import { Route } from 'vue-router'
import { UserModule } from '@/store/modules/user'
import Cookies from 'js-cookie'

NProgress.configure({ 'showSpinner': false })

router.beforeEach(async (to: Route, _: Route, next: any) => {
  NProgress.start()
  if (Cookies.get('token')) {
    next()
  } else {
    if (!to.meta.notNeedAuth) {
      next('/login')
    } else {
      next()
    }
  }
})

router.afterEach((to: Route) => {
  NProgress.done()
  //document.title = to.meta.title
  if (to.meta.title) {
    // 如果当前页面有标题（比如“员工管理”），就显示“员工管理 - 凌水校园生活服务平台”
    document.title = to.meta.title + ' - 凌水校园生活服务平台'
  } else {
    // 如果没有，就直接显示系统名
    document.title = '凌水校园生活服务平台'
  }
})
