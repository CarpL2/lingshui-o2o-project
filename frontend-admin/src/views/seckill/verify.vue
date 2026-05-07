<template>
  <div class="verify-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span style="font-weight: bold; font-size: 18px">🔥 秒杀券核销台</span>
      </div>
      
      <div class="content">
        <div class="input-area">
          <el-input
            v-model="voucherCode"
            placeholder="请输入 6 位数字核销码"
            maxlength="6"
            show-word-limit
            clearable
            style="width: 300px; margin-right: 20px"
            @keyup.enter.native="handleVerify"
          >
            <i slot="prefix" class="el-icon-s-ticket el-input__icon"></i>
          </el-input>
          
          <el-button type="primary" @click="handleVerify" :loading="loading">
            立即核销
          </el-button>
        </div>

        <div v-if="resultInfo" class="result-ticket">
          <div class="success-icon"><i class="el-icon-success"></i> 核销成功</div>
          <div class="ticket-row"><span>菜品名称：</span> <strong>{{ resultInfo.dishName }}</strong></div>
          <div class="ticket-row"><span>秒杀价格：</span> <span class="price">￥{{ resultInfo.price }}</span></div>
          <div class="ticket-row"><span>核销时间：</span> {{ resultInfo.time }}</div>
          <div class="ticket-row"><span>用户ID：</span> {{ resultInfo.userId }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator'
import { verifyVoucher } from '@/api/seckill'

@Component({
  name: 'SeckillVerify'
})
export default class extends Vue {
  private voucherCode = ''
  private loading = false
  private resultInfo: any = null // 用来存核销成功后的信息

  private async handleVerify() {
    // 1. 前端校验
    if (!this.voucherCode || this.voucherCode.length !== 6) {
      this.$message.warning('请输入正确的 6 位核销码')
      return
    }

    this.loading = true
    try {
      // 发送请求给后端
      const res: any = await verifyVoucher(this.voucherCode)
      
      if (res.data.code === 1) {
        this.$message.success('核销成功！')
        // 模拟展示回显数据 (如果后端没传这么多，你可以只展示成功提示)
        this.resultInfo = {
          dishName: res.data.data.dishName || '特价红烧肉', // 演示用默认值
          price: res.data.data.price || '0.01',
          userId: res.data.data.userId || '***',
          time: new Date().toLocaleString()
        }
        this.voucherCode = '' // 清空输入框，方便下一次核销
      } else {
        this.$message.error(res.data.msg || '核销失败，券码无效或已使用')
        this.resultInfo = null
      }
    } catch (err) {
      this.$message.error('系统繁忙，请稍后再试')
    } finally {
      this.loading = false
    }
  }
}
</script>

<style lang="scss" scoped>
.verify-container {
  padding: 30px;
  display: flex;
  justify-content: center;
  
  .box-card {
    width: 600px;
    height: 400px;
  }

  .content {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 50px;
  }

  .result-ticket {
    margin-top: 40px;
    padding: 20px;
    background: #f0f9eb;
    border: 1px dashed #67c23a;
    border-radius: 4px;
    width: 100%;
    
    .success-icon {
      color: #67c23a;
      font-size: 20px;
      font-weight: bold;
      text-align: center;
      margin-bottom: 15px;
    }

    .ticket-row {
      margin-bottom: 10px;
      color: #606266;
      display: flex;
      justify-content: space-between;
      
      .price {
        color: #f56c6c;
        font-weight: bold;
      }
    }
  }
}
</style>