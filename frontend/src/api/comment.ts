import { axios } from '../utils/request'

// 评论数据类型定义
export interface Comment {
  id: number
  userId: number
  productId: number
  username: string
  avatar?: string
  rating: number
  content: string
  createdAt: string
  updatedAt?: string
}

export interface CommentStats {
  total: number
  average: number
  starCounts: number[] // [1星数量, 2星数量, 3星数量, 4星数量, 5星数量]
}

export interface CommentResponse {
  comments: Comment[]
  currentPage: number
  totalItems: number
  totalPages: number
}

export interface CommentCreateDTO {
  productId: number
  rating: number
  content: string
}

export interface CommentReplyDTO {
  id: number
  commentId: number
  userId: number
  username: string
  avatar?: string
  content: string
  createdAt: string
}

export interface CommentReplyCreateDTO {
  commentId: number
  content: string
}

// API响应类型定义
export interface ApiResponse<T> {
  code: number
  msg: string | null
  data: T
}

// 获取产品评论列表
export function getProductComments(productId: string | number, page = 0, size = 10) {
  return axios<CommentResponse>({
    url: `/api/comments/product/${productId}`,
    method: 'get',
    params: {
      page: page,
      size: size
    }
  })
}

// 获取产品平均评分
export function getProductAverageRating(productId: string | number) {
  return axios<number>({
    url: `/api/comments/product/${productId}/rating`,
    method: 'get'
  })
}

// 添加评论
export function addComment(data: CommentCreateDTO) {
  return axios<Comment>({
    url: '/api/comments',
    method: 'post',
    data: data
  })
}

// 获取评论的回复列表
export function getCommentReplies(commentId: number, page = 0, size = 10) {
  return axios<{
    replies: CommentReplyDTO[]
    currentPage: number
    totalItems: number
    totalPages: number
  }>({
    url: `/api/comments/${commentId}/replies`,
    method: 'get',
    params: {
      page: page,
      size: size
    }
  })
}

// 添加评论回复
export function addReply(data: CommentReplyCreateDTO) {
  return axios<CommentReplyDTO>({
    url: '/api/comments/replies',
    method: 'post',
    data: data
  })
}

// 删除评论
export function deleteComment(commentId: number) {
  return axios<void>({
    url: `/api/comments/${commentId}`,
    method: 'delete'
  })
}

// 删除评论回复
export function deleteReply(replyId: number) {
  return axios<void>({
    url: `/api/comments/replies/${replyId}`,
    method: 'delete'
  })
}