import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const service = axios.create({
    baseURL: "http://localhost:8080",
    timeout: 30000
})

// 判断是否登录 - 改进token检查逻辑
function hasToken() {
    const token = sessionStorage.getItem('token')
    return token !== null && token !== ''
}

//判断是否登录 - 设置用户ID
function hasUserId() {
    const userId = sessionStorage.getItem('userId')
    return userId !== null && userId !== ''
}

// 请求拦截器 - 在发送请求前添加token
service.interceptors.request.use(
    config => {
        if (hasToken()) {
            config.headers['token'] = sessionStorage.getItem('token')
        }
        if (hasUserId()) {
            config.headers['userId'] = sessionStorage.getItem('userId')
        }
        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器 - 改进错误处理和状态码检查
service.interceptors.response.use(
    response => {
        // HTTP状态码为200，表示请求成功
        if (response.status === 200) {
            // 检查业务状态码
            const code = response.data.code
            if (code && code !== '200') {
                // 业务逻辑错误
                ElMessage.error(response.data.msg || '操作失败')
            }
            return response
        } else {
            // 其他HTTP状态码
            ElMessage.error('请求失败')
            return Promise.reject('请求失败')
        }
    },
    error => {
        // 处理HTTP错误
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    // 未授权或token失效
                    ElMessage.error('登录已过期，请重新登录')
                    sessionStorage.removeItem('token')
                    sessionStorage.removeItem('username')

                    // 跳转到登录页
                    router.push('/login')
                    break

                case 403:
                    ElMessage.error('没有权限执行此操作')
                    break

                case 404:
                    ElMessage.error('请求的资源不存在')
                    break

                case 500:
                    ElMessage.error('服务器错误，请稍后再试')
                    break

                default:
                    ElMessage.error(error.response.data?.msg || `请求失败(${error.response.status})`)
            }
        } else if (error.request) {
            // 请求已发出但没有收到响应
            ElMessage.error('网络错误，无法连接到服务器')
        } else {
            // 请求配置出错
            ElMessage.error('请求配置错误')
        }

        console.error('响应错误:', error)
        return Promise.reject(error)
    }
)

// 导出axios实例
export {
    service as axios
}