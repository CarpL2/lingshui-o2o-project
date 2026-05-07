<template>
  <div class="login">
    <div class="login-box">
      <img src="@/assets/login/login-1.png" alt="" />
      <div class="login-form">
        <el-form ref="loginForm" :model="loginForm" :rules="loginRules">
          <div class="login-form-title">
            <span class="title-label">校园服务平台管理系统</span>
          </div>
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              auto-complete="off"
              placeholder="账号"
              prefix-icon="iconfont icon-user"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="iconfont icon-lock"
              @keyup.enter.native="handleLogin"
            />
          </el-form-item>
          <el-form-item style="width: 100%">
            <el-button
              :loading="loading"
              class="login-btn"
              size="medium"
              type="primary"
              style="width: 100%"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">登录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import { Route } from 'vue-router'
import { Form as ElForm, Input } from 'element-ui'
import { UserModule } from '@/store/modules/user'
import { isValidUsername } from '@/utils/validate'

@Component({
  name: 'Login',
})
export default class extends Vue {
  private validateUsername = (rule: any, value: string, callback: Function) => {
    if (!value) {
      callback(new Error('请输入用户名'))
    } else {
      callback()
    }
  }
  private validatePassword = (rule: any, value: string, callback: Function) => {
    if (value.length < 6) {
      callback(new Error('密码必须在6位以上'))
    } else {
      callback()
    }
  }
  private loginForm = {
    username: 'admin',
    password: '123456',
  } as {
    username: String
    password: String
  }

  loginRules = {
    username: [{ validator: this.validateUsername, trigger: 'blur' }],
    password: [{ validator: this.validatePassword, trigger: 'blur' }],
  }
  private loading = false
  private redirect?: string

  @Watch('$route', { immediate: true })
  private onRouteChange(route: Route) {}

  // 登录
  private handleLogin() {
    ;(this.$refs.loginForm as ElForm).validate(async (valid: boolean) => {
      if (valid) {
        this.loading = true
        await UserModule.Login(this.loginForm as any)
          .then((res: any) => {
            if (String(res.code) === '1') {
              this.$router.push('/')
            } else {
              // this.$message.error(res.msg)
              this.loading = false
            }
          })
          .catch(() => {
            // this.$message.error('用户名或密码错误！')
            this.loading = false
          })
      } else {
        return false
      }
    })
  }
}
</script>

<style lang="scss">
// 引入全局变量
@import '@/styles/variables.scss';

.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  // ✨ 关键修改：使用变量背景色，不再是死板的灰色
  background-color: $loginBg; 
}

.title-label {
    font-weight: 600;
    font-size: 24px;
    color: $mine; // 会自动变成红色
    letter-spacing: 2px;
  }

.login-box {
  width: 1000px;
  height: 474.38px;
  border-radius: 8px;
  display: flex;
  // 给整个盒子加个投影，更有质感
  box-shadow: 0 4px 20px rgba(0,0,0,0.08); 
  
  img {
    width: 60%;
    height: auto;
    // 如果左边的图还是原来那个外卖员，可以先暂时隐藏，或者换个图
    // display: none; 
    border-radius: 8px 0 0 8px;
  }
}

.login-form {
  background: #ffffff;
  width: 40%;
  border-radius: 0px 8px 8px 0px;
  display: flex;
  justify-content: center;
  align-items: center;
  
  .el-form {
    width: 214px;
    height: 307px;
  }
  .el-form-item {
    margin-bottom: 30px;
  }
  
  // 输入框下划线颜色微调
  .el-form-item.is-error .el-input__inner {
    border: 0 !important;
    border-bottom: 1px solid $pink !important;
    background: #fff !important;
  }
  
  .el-input__inner {
    border: 0;
    border-bottom: 1px solid #e9e9e8;
    border-radius: 0;
    font-size: 14px; // 稍微调大一点字体
    font-weight: 400;
    color: #333333;
    height: 32px;
    line-height: 32px;
    
    &:focus {
       // ✨ 关键修改：输入框聚焦时变成主题色
       border-bottom: 1px solid $mine; 
    }
  }
  
  // 图标颜色
  .iconfont {
    color: $gray-2;
  }
}

.login-btn {
  border-radius: 4px; // 稍微圆角一点，不要那么圆
  padding: 11px 20px !important;
  margin-top: 10px;
  font-weight: 500;
  font-size: 14px;
  border: 0;
  color: #ffffff; // 字体改成白色，更清晰
  // ✨ 关键修改：按钮背景色使用主题变量
  background-color: $mine !important; 
  transition: all 0.3s; // 加个过渡动画
  
  &:hover,
  &:focus {
    // 鼠标悬停时稍微变浅一点，增加交互感
    opacity: 0.9;
    background-color: $mine !important;
    color: #ffffff;
  }
}

.login-form-title {
  height: 36px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 40px;
  
  // ✨ 临时修改：如果你没有新 Logo，就用纯文字代替
  .title-label {
    font-weight: 600;
    font-size: 24px;
    color: $mine; // 标题也用主题色
    letter-spacing: 2px;
    // 比如：font-family: 'Helvetica Neue', Helvetica, 'PingFang SC';
  }
}
</style>