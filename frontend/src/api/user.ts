import { axios } from '../utils/request'

// 用户登录
export function userLogin(data: {
  username: string,
  password: string
}) {
  return axios({
    url: '/api/accounts/login',
    method: 'post',
    data
  })
}

// 用户注册
export function userRegister(data: {
  username: string, // 用户名，必填
  password: string, // 密码，必填
  name: string,     // 真实姓名，必填
  role: string,     // 角色，必填 (admin/user/merchant)
  avatar?: string,  // 头像 URL，选填
  telephone?: string, // 手机号，选填
  email?: string,   // 邮箱，选填
  location?: string // 位置，选填
}) {
  return axios({
    url: '/api/accounts',
    method: 'post',
    data
  })
}

// 获取用户信息
export function getUserInfo(username: string) {
  return axios({
    url: `/api/accounts/${username}`,
    method: 'get'
  })
}

// 更新用户信息
export function updateUserInfo(data: {
  username: string,
  password?: string,
  name?: string,
  avatar?: string,
  role?: string,
  telephone?: string,
  email?: string,
  location?: string
}) {
  return axios({
    url: '/api/accounts',
    method: 'put',
    data
  })
}