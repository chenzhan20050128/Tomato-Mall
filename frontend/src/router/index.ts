import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

// 导入布局组件
import Layout from '../components/layout/Layout.vue'

// 导入页面组件
const Login = () => import('../views/user/Login.vue')
const Register = () => import('../views/user/Register.vue')
const Profile = () => import('../views/user/Profile.vue')
const Home = () => import('../views/home/Home.vue')
const Mall = () => import('../views/mall/Mall.vue')
const Detail = () => import('../views/product/ProductDetail.vue')
const Cart = () => import('../views/cart/Cart.vue')
const Order = () => import('../views/order/Order.vue')
const Pay = () => import('../views/payment/PaymentResult.vue')
const Recommend = () => import('../views/recommend/Recommend.vue')
const Default = () => import('../views/Default.vue')
const Ads = () => import('../views/ads/Ads.vue')
const ProductManagement = () => import('../views/productManagement/ProductManagement.vue')



// 路由配置
const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        redirect: '/home'
      },
      {
        path: 'home',
        name: 'Home',
        component: Home,
        meta: {
          title: '首页',
          requiresAuth: false
        }
      },
      {
        path: 'login',
        name: 'Login',
        component: Login,
        meta: {
          title: '登录',
          requiresAuth: false
        }
      },
      {
        path: 'register',
        name: 'Register',
        component: Register,
        meta: {
          title: '注册',
          requiresAuth: false
        }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: Profile,
        meta: {
          title: '个人资料',
          requiresAuth: true
        }
      },
      {
        path: 'mall',
        name: 'Mall',
        component: Mall,
        meta: {
          title: '图书商城',
          requiresAuth: true
        }
      },
      {
        path: '/product/:id',
        name: 'ProductDetail',
        component: Detail,
        meta: {
          title: '商品详情',
        }
      }, {
        path: '/cart',
        name: 'Cart',
        component: Cart,
        meta: {
          title: '购物车',
          requiresAuth: true
        }
      },
      {
        path: '/order',
        name: 'Order',
        component: Order,
        meta: {
          title: '我的订单',
          requiresAuth: true
        }
      },
      {
        path: '/payment-result',
        name: 'PaymentResult',
        component: Pay,
        meta: { requiresAuth: true }
      },
      {
        path: '/recommend',
        name: 'Recommend',
        component: Recommend,
        meta: {
          title: '图书推荐',
          requiresAuth: true
        }
      },
      {
        path: '/ads',
        name: 'Ads',
        component: Ads,
        meta: {
          title: '广告管理',
          requiresAuth: true
        }
      },
      {
        path: '/productManagement',
        name: 'ProductManagement',
        component: ProductManagement,
        meta: {
          title: '商品管理',
          requiresAuth: true,
          isAdmin: true // 仅管理员可见
        }
      }
    ]
  },
  // 捕获所有未匹配路由
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: Default,
    meta: {
      title: '页面未找到',
      requiresAuth: false
    }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 番茄读书商城` : '番茄读书商城'

  // 需要登录权限的路由验证
  if (to.meta.requiresAuth) {
    const token = sessionStorage.getItem('token')
    if (token) {
      next()
    } else {
      next({ path: '/login' })
    }
  } else {
    next()
  }
})

export default router