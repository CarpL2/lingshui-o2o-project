<template>
  <view class="container">
    <view class="header-banner">
      <text class="title">🔥 限时秒杀专场</text>
    </view>
    
    <view style="padding: 10px; text-align: right;">
      <button size="mini" type="default" @click="showHistory">我的秒杀记录</button>
    </view>

    <view class="seckill-item" v-for="(item, index) in seckillList" :key="item.id">
      <view class="card">
        <image :src="item.image" mode="aspectFill" class="dish-img"></image>
        
        <view class="info-section">
          <view class="dish-name">{{ item.name }}</view>
          
          <view class="time-box">
			   <text class="time-icon">⏰</text>
			   <text class="time-txt">
				 {{ item.startTime ? item.startTime.substring(5, 16) : '' }} ~ {{ item.endTime ? item.endTime.substring(5, 16) : '' }}
			   </text>
		  </view>

          <view class="price-box">
             <text class="symbol">¥</text>
             <text class="now-price">{{ item.seckillPrice }}</text>
             <text class="old-price">¥{{ item.price }}</text>
          </view>

          <view class="bottom-row">
            <text class="stock-tag">剩 {{ item.stockCount }} 份</text>
            
            <button 
              v-if="item.stockCount <= 0"
              class="grab-btn disabled" 
              disabled
            >
              已抢光
            </button>

            <button 
              v-else-if="isNotStarted(item.startTime)"
              class="grab-btn wait-btn" 
            >
              {{ item.startTime ? item.startTime.substring(11, 16) : '' }} 开抢
            </button>

            <button 
              v-else
              class="grab-btn" 
              @click="handleGrab(item)"
            >
              马上抢
            </button>
          </view>
        </view>
      </view>
    </view>
    
    <view v-if="showHistoryModal" class="history-mask">
         <view class="history-box">
            <view class="box-title">我的战利品 ({{historyList.length}})</view>
            <scroll-view scroll-y class="box-list">
              <view v-for="(item, index) in historyList" :key="index" class="history-item">
                <view class="item-top">
                  <text class="dish-name">{{index + 1}}. {{item.dishName}}</text>
                  <text :class="item.status===1 ? 'status-ok' : 'status-no'">
                    {{item.status===1 ? '已核销' : '未核销'}}
                  </text>
                </view>
                <view class="item-bottom">
                  <text>核销码：<text class="code-txt">{{item.voucher_code}}</text></text>
                  <text class="time-txt">{{item.order_time.substring(5)}}</text>
                </view>
              </view>
            </scroll-view>
            <button type="primary" @click="showHistoryModal = false">关闭</button>
         </view>
    </view>
    
    <view class="footer-tip" v-if="seckillList.length > 0">已经到底啦 ~</view>
  </view>
</template>
<script>
import { getSeckillList, grabSeckill, getSeckillResult, getSeckillHistory } from '../api/api.js'

export default {
  data() {
    return {
      seckillId: null, // 接收传过来的 ID
      seckillList: [], // 存后端返回的详情
	  showHistoryModal: false, // 控制弹窗显示
	  historyList: [],
      dishName: '加载中...',
      price: '0.00',
      oldPrice: '0.00',
      voucherCode: ''
    }
  },
  // 页面加载时，接收上个页面传过来的参数
  onShow() { // 改用 onShow，每次进来都刷新
      this.loadSeckillList()
  },
  methods: {
	 async showHistory() {
	       uni.showLoading({ title: '加载中...' })
	       try {
	         const res = await getSeckillHistory()
	         uni.hideLoading()
	         
	         if (res.code === 1 && res.data && res.data.length > 0) {
	           // 🚀 核心变化：只存数据，打开开关，剩下的交给 Template
	           this.historyList = res.data;
	           this.showHistoryModal = true; 
	         } else {
	           uni.showToast({ title: '暂无记录', icon: 'none' })
	         }
	       } catch (e) {
	         uni.hideLoading()
	         uni.showToast({ title: '查询失败', icon: 'none' })
	       }
	     
	             
	  },
      // 1. 加载秒杀列表
      async loadSeckillList() {
        const res = await getSeckillList()
        if (res.code === 1) {
          this.seckillList = res.data
        }
      },
  
      // 2. 点击抢购按钮 (入口)
      async handleGrab(item) {
        // 基础校验
        if (item.stockCount <= 0) {
          return uni.showToast({ title: '已抢光', icon: 'none' })
        }
  
        // 开启 Loading (注意：这里不要在 finally 里关，因为轮询时需要它一直转)
        uni.showLoading({ title: '正在抢购...', mask: true })
  
        try {
          // --- 发起请求 (触发 Redis + MQ) ---
          const res = await grabSeckill({ seckillId: item.id })
  
          if (res.code === 1) {
            // 🚀 成功加入队列！
            // 注意：不要在这里关 Loading，直接进入轮询
            this.pollResult(item.id)
          } else {
            // 业务拒绝 (如：重复抢购、库存不足)
            uni.hideLoading()
            uni.showToast({ title: res.msg || '抢购失败', icon: 'none' })
          }
  
        } catch (error) {
          // --- 异常捕获 ---
          uni.hideLoading() // 出错一定要关 Loading
          console.error('抢购请求异常:', error);
  
          // 提取错误信息
          let showMsg = '手慢了，没抢到！';
          if (typeof error === 'string') showMsg = error;
          else if (error.msg) showMsg = error.msg;
          else if (error.data && error.data.msg) showMsg = error.data.msg;
          else if (error.data && error.data.message) showMsg = error.data.message;
  
          uni.showToast({ title: showMsg, icon: 'none', duration: 2500 });
        }
      },
	  
	  isNotStarted(startTime) {
	        // 安全判断：如果时间为空，默认不是“未开始”
	        if (!startTime) return false;
	        
	        // 1. 兼容性处理：把 "2025-12-27" 这种格式转为 "2025/12/27"
	        // 否则 iOS 手机会报错 Invalid Date
	        let timeStr = startTime.replace(/-/g, '/');
	        
	        // 2. 转换成日期对象进行比较
	        let startDate = new Date(timeStr);
	        let now = new Date();
	        
	        // 如果开始时间 > 当前时间，说明还没开始
	        return startDate > now;
	      },
  
      // 3. 🚀 轮询查询结果 (核心逻辑)
      async pollResult(seckillId) {
        // 定义最大重试次数 (比如 20 次，每次 1 秒，共等待 20 秒)
        let count = 0;
        const maxCount = 20;
  
        // 启动定时器
        const timer = setInterval(async () => {
          count++;
          try {
            // 调用后端查询接口
            const res = await getSeckillResult(seckillId)
  
            // --- 情况 A: 查到了！(抢购成功) ---
            if (res.code === 1 && res.data) {
              clearInterval(timer) // 1. 停止轮询
              uni.hideLoading()    // 2. 关闭转圈
  
              // 3. 弹窗显示核销码
              uni.showModal({
                title: '抢购成功',
                content: '您的核销码：' + res.data,
                showCancel: false,
                success: () => {
                  this.loadSeckillList() // 刷新列表库存
                }
              })
            }
  
            // --- 情况 B: 超时了还没查到 ---
            if (count >= maxCount) {
              clearInterval(timer)
              uni.hideLoading()
              uni.showToast({ title: '抢购人数过多，请稍后在订单查看', icon: 'none' })
            }
  
            // --- 情况 C: 还没查到 (res.data 为 null)，继续循环，什么都不做 ---
  
          } catch (err) {
            // 如果查询接口报错，停止轮询
            clearInterval(timer)
            uni.hideLoading()
            console.error('轮询出错', err)
          }
        }, 1000) // 每隔 1 秒查一次
      }
    },
}
</script>

<style lang="scss">
.container {
  min-height: 100vh;
  background-color: #f8f8f8;
  padding-bottom: 40rpx;
}

/* 顶部 Banner */
.header-banner {
  background: linear-gradient(135deg, #ff4b33, #ff8f73);
  padding: 40rpx 30rpx;
  color: #fff;
  margin-bottom: 20rpx;
}
.title { font-size: 40rpx; font-weight: bold; display: block; }
.sub-title { font-size: 24rpx; opacity: 0.9; margin-top: 10rpx; display: block; }

/* 列表项 */
.seckill-item {
  padding: 0 20rpx;
  margin-bottom: 20rpx;
}

/* 卡片本体 */
.card {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.05);
  display: flex; /* 改成左右布局更像外卖列表 */
  padding: 20rpx;
}

.dish-img {
  width: 220rpx;
  height: 220rpx;
  border-radius: 12rpx;
  flex-shrink: 0; /* 防止图片被挤压 */
  margin-right: 20rpx;
}

.info-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.dish-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.price-box {
  margin-top: 10rpx;
}
.symbol { font-size: 24rpx; color: #ff4b33; }
.now-price { font-size: 44rpx; color: #ff4b33; font-weight: bold; margin-right: 10rpx;}
.old-price { font-size: 24rpx; color: #999; text-decoration: line-through; }

.bottom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stock-tag {
  font-size: 22rpx;
  color: #ff4b33;
  background: #fff0ed;
  padding: 4rpx 10rpx;
  border-radius: 8rpx;
}

.grab-btn {
  margin: 0;
  background: #ff4b33;
  color: #fff;
  font-size: 26rpx;
  padding: 0 30rpx;
  height: 60rpx;
  line-height: 60rpx;
  border-radius: 30rpx;
}

.grab-btn.disabled {
  background: #ccc;
}

.footer-tip {
  text-align: center;
  color: #999;
  font-size: 24rpx;
  margin-top: 20rpx;
}
/* 🟢 复制这里：弹窗样式 (修正滚动版) */
.history-mask {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.6); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.history-box {
  width: 80%; 
  height: 60vh; /* 🚀 核心改动1：直接定死高度 (比如屏幕的60%)，不再用 max-height */
  background: #fff; border-radius: 20rpx;
  padding: 30rpx; 
  display: flex; flex-direction: column; /* 让内部元素竖着排 */
}
.box-title { 
  font-size: 36rpx; font-weight: bold; text-align: center; margin-bottom: 20rpx; 
  flex-shrink: 0; /* 防止标题被挤压 */
}
.box-list { 
  flex: 1; /* 🚀 核心改动2：占满剩余空间 */
  height: 0; /* 🚀 核心改动3：配合 flex:1 必须设置 height:0，否则 scroll-view 不会滚动 */
  margin-bottom: 20rpx; 
}
.history-item { border-bottom: 1px dashed #eee; padding: 20rpx 0; }
.item-top { display: flex; justify-content: space-between; margin-bottom: 10rpx; font-size: 30rpx; font-weight: bold; }
.item-bottom { display: flex; justify-content: space-between; font-size: 26rpx; color: #666; }
.status-ok { color: #00aa00; font-size: 24rpx; }
.status-no { color: #ff0000; font-size: 24rpx; }
.code-txt { color: #007aff; font-weight: bold; margin-left: 5rpx; }
.time-txt { font-size: 22rpx; color: #999; }

/* 补充一点样式让它好看 */
.time-box {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  background-color: #f8f8f8;
  padding: 2px 5px;
  border-radius: 4px;
  display: inline-block;
}
.time-icon {
  margin-right: 4px;
}

/* 待开始按钮样式 */
.wait-btn {
  background-color: #ff9900 !important; /* 橙色表示等待 */
  opacity: 0.8;
}

/* 禁用的样式 (已抢光) */
.disabled {
  background-color: #ccc !important;
  color: #fff;
}
</style>