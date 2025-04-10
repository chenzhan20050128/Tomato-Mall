import { axios } from '../utils/request'

// 修改产品接口定义以匹配API返回格式
export interface Product {
  id: string;                // API返回的是字符串ID
  title: string;             // API返回的是title
  price: number;
  rate: number;              // 评分
  description: string;
  cover: string;             // API返回的是cover
  detail: string;            // 详细描述
  specifications: Array<{    // 规格信息数组
    id: string | number;
    item: string;            // 如"作者"、"ISBN"等
    value: string;
    productId: string;
  }>;
  
  // 前端使用字段
  stock?: number;            // 库存量
  isAvailable?: boolean;     // 是否可用
}

// API响应类型定义
export interface ApiResponse<T> {
  code: number;              // API返回的是数字类型的状态码
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
export function getProductDetail(id: string) {
  return axios<ApiResponse<Product>>({
    url: `/api/products/${id}`,
    method: 'get'
  })
}

// 获取商品库存
export function getProductStock(productId: string) {
  return axios<ApiResponse<{amount: number}>>({
    url: `/api/products/stockpile/${productId}`,
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