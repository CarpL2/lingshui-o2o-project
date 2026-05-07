<template>
  <div>
    <div class="logo">
      <div v-if="!isCollapse" class="sidebar-logo">
        <h1 class="sidebar-title">校园服务后台</h1>
      </div>
      <div v-else class="sidebar-logo-mini">
        <h1 class="sidebar-title" style="font-size: 14px;">闪购</h1>
      </div>
    </div>
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-openeds="defOpen"
        :default-active="defAct"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :active-text-color="variables.menuActiveText"
        :unique-opened="false"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          v-for="route in routes"
          :key="route.path"
          :item="route"
          :base-path="route.path"
          :is-collapse="isCollapse"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator'
import { AppModule } from '@/store/modules/app'
import { UserModule } from '@/store/modules/user'
import SidebarItem from './SidebarItem.vue'
import variables from '@/styles/_variables.scss'
import { getSidebarStatus, setSidebarStatus } from '@/utils/cookies'
import Cookies from 'js-cookie'

@Component({
  name: 'SideBar',
  components: {
    SidebarItem
  }
})
export default class extends Vue {
  private restKey: number = 0

  get name() {
    return (UserModule.userInfo as any).name
      ? (UserModule.userInfo as any).name
      : JSON.parse(Cookies.get('user_info') as any).name
  }

  get defOpen() {
    let path = ['/']
    this.routes.forEach((n: any, i: number) => {
      if (n.meta.roles && n.meta.roles[0] === this.roles[0]) {
        path.splice(0, 1, n.path)
      }
    })
    return path
  }

  get defAct() {
    let path = this.$route.path
    return path
  }

  get sidebar() {
    return AppModule.sidebar
  }

  get roles() {
    return UserModule.roles
  }

  get routes() {
    let routes = JSON.parse(
      JSON.stringify([...(this.$router as any).options.routes])
    )
    let menuList = []
    let menu = routes.find((item: any) => item.path === '/')
    if (menu) {
      menuList = menu.children
    }
    return menuList
  }

  get variables() {
    return variables
  }

  get isCollapse() {
    return !this.sidebar.opened
  }

  private async logout() {
    this.$store.dispatch('LogOut').then(() => {
      this.$router.replace({ path: '/login' })
    })
  }
}
</script>

<style lang="scss" scoped>
// ✨ 关键：引入变量文件，以便使用 $mine 红色变量
@import '@/styles/variables.scss';

.logo {
  text-align: center;
  // ✨ 修改：使用变量背景色，替换原来的 #ffc100
  background-color: $mine;
  
  height: 60px;
  line-height: 60px; // 让文字垂直居中
  overflow: hidden;

  // ✨ 新增：文字标题样式
  .sidebar-title {
    display: inline-block;
    margin: 0;
    color: #fff; // 白色字体
    font-weight: 600;
    font-size: 20px; // 字体大小
    font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
    vertical-align: middle;
  }
}

.sidebar-logo-mini {
  // 折叠时的样式调整
  width: 100%;
}

.el-scrollbar {
  height: 100%;
  background-color: rgb(52, 55, 68);
}

.el-menu {
  border: none;
  height: calc(95vh - 23px);
  width: 100% !important;
  padding: 47px 15px 0;
}
</style>