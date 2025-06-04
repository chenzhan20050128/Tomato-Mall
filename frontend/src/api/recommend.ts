import { axios } from '../utils/request'

// 商品排名数据类型定义
export interface ProductRankingDTO {
  productId: number
  title: string
  cover: string
  price: number
  sales: number
  stock: number
  description: string
  category: string
  author: string
  averageScore: number
  ratingCount: number
}

// API响应类型定义
export interface ApiResponse<T> {
  code: string
  msg: string | null
  data: T
}

/**
 * 获取畅销书榜单
 * @param category 可选分类
 * @param days 时间范围（天）
 * @param limit 返回数量
 * @returns 畅销书榜单数据
 */
export function getBestSellers(params: {
  category?: string
  days?: number
  limit?: number
}) {
  return axios<ApiResponse<ProductRankingDTO[]>>({
    url: '/api/recommendations/bestsellers',
    method: 'get',
    params
  })
}

/**
 * 获取评分榜
 * @param category 可选分类
 * @param limit 返回数量
 * @returns 评分榜数据
 */
export function getTopRated(params: {
  category?: string
  limit?: number
}) {
  return axios<ApiResponse<ProductRankingDTO[]>>({
    url: '/api/recommendations/top-rated',
    method: 'get',
    params
  })
}

/**
 * 获取随机推荐
 * @param userId 用户ID（可选，用于基于历史的推荐）
 * @param count 推荐数量
 * @returns 随机推荐数据
 */
export function getRandomRecommendations(params: {
  userId?: number
  count?: number
}) {
  return axios<ApiResponse<ProductRankingDTO[]>>({
    url: '/api/recommendations/random',
    method: 'get',
    params
  })
}

/**
 * 获取特定用户的偏好分类
 * @param userId 用户ID
 * @returns 用户偏好分类列表
 */
export function getUserPreferredCategories(userId: number) {
  return axios<ApiResponse<string[]>>({
    url: `/api/recommendations/user/${userId}/categories`,
    method: 'get'
  })
}

/**
 * 获取相似商品推荐
 * @param productId 商品ID
 * @param limit 返回数量
 * @returns 相似商品推荐
 */
export function getSimilarProducts(productId: number, limit: number = 6) {
  return axios<ApiResponse<ProductRankingDTO[]>>({
    url: `/api/recommendations/similar/${productId}`,
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取用户可能感兴趣的商品
 * @param userId 用户ID
 * @param limit 返回数量
 * @returns 用户可能感兴趣的商品
 */
export function getUserInterests(userId: number, limit: number = 10) {
  return axios<ApiResponse<ProductRankingDTO[]>>({
    url: `/api/recommendations/user/${userId}/interests`,
    method: 'get',
    params: { limit }
  })
}