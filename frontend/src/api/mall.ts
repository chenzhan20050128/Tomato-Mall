import { axios } from '../utils/request'

// 修改产品接口定义以匹配API返回格式
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  image: string;
  stock: number;
  isAvailable: boolean;
  // 添加可选字段，以防API返回更多信息
  sales?: number;
  categories?: string[];
  createdTime?: string;
}

// 修改API响应类型定义
export interface ApiResponse<T> {
  code: string;
  msg: string | null;
  data: T;
}

// 获取商品列表
export function getProductList() {
  return axios<ApiResponse<Product[]>>({
    url: '/api/products',
    method: 'get'
  })
}

// 获取商品详情
export function getProductDetail(id: number) {
  return axios<ApiResponse<Product>>({
    url: `/api/products/${id}`,
    method: 'get'
  })
}

// 按分类获取商品
export function getProductsByCategory(category: string) {
  return axios<ApiResponse<Product[]>>({
    url: '/api/products',
    method: 'get',
    params: {
      category: category
    }
  })
}

// 搜索商品
export function searchProducts(keyword: string) {
  return axios<ApiResponse<Product[]>>({
    url: '/api/products',
    method: 'get',
    params: {
      keyword: keyword
    }
  })
}