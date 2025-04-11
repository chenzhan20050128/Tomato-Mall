import { axios } from '../utils/request'

// 修改购物车项目接口，与Product保持一致
export interface CartItem {
  cartItemId: string;
  productId: string;
  title: string;            // 使用title，而非name
  price: number;
  description: string;
  cover: string;            // 使用cover，而非image
  detail?: string;
  quantity: number;
}

// 购物车列表返回类型
export interface CartResponse {
  items: CartItem[];
  total: number;
  totalAmount: number;
}

// 添加商品到购物车
export function addToCart(productId: string | number, quantity: number) {
  return axios({
    url: '/api/cart',
    method: 'post',
    data: {
      productId,
      quantity
    }
  })
}

// 从购物车删除商品
export function removeFromCart(cartItemId: string | number) {
  return axios({
    url: `/api/cart/${cartItemId}`,
    method: 'delete'
  })
}

// 修改购物车商品数量
export function updateCartItemQuantity(cartItemId: string | number, quantity: number) {
  return axios({
    url: `/api/cart/${cartItemId}/quantity`,
    method: 'patch',
    data: {
      quantity
    }
  })
}

// 获取购物车商品列表
export function getCartItems() {
  return axios({
    url: '/api/cart',
    method: 'get'
  })
}

// 购物车结算 (提交订单)
export function checkoutCart(params: {
  cartItemIds: string[];
  shipping_address: {
    name: string;
    phone: string;
    zipCode: string;
    address: string;
  };
  payment_method: string;
}) {
  return axios({
    url: '/api/cart/checkout',
    method: 'post',
    data: params
  })
}