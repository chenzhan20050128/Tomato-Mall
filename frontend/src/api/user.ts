import request from '../utils/request'

// 用户登录
export function userLogin(data: {
  username: string,  // 修改为username，可以是用户名或手机号
  password: string
}) {
  return request({
    url: '/api/accounts/login',  // 调整为后端API路径
    method: 'post',
    data
  })
}

// 用户注册
export function userRegister(data: {
  username: string,
  password: string,
  role: string
}) {
  return request({
    url: '/api/accounts',  // 调整为后端API路径
    method: 'post',
    data
  })
}

// 获取用户信息
export function userInfo(username: string) {
  return request({
    url: `/api/accounts/${username}`,
    method: 'get'
  })
}

// 更新用户信息
export function updateUserInfo(data: any) {
  return request({
    url: '/api/accounts',
    method: 'put',
    data
  })
}