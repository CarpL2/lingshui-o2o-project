import request from '@/utils/request'

// 核销券码接口
export const verifyVoucher = (code: string) => {
  return request({
    url: '/seckill/verify', // 后端接口地址
    method: 'post',
    data: { voucherCode: code }
  })
}