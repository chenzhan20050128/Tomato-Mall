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
  username: string,
  password: string,
  name: string,
  role: string,
  telephone?: string,
  email?: string,
  location?: string
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