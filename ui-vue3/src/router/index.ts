import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    children: [
      { path: '', redirect: '/reports' },
      { path: 'reports', component: () => import('../views/ReportList.vue') },
      { path: 'reports/create', component: () => import('../views/ReportCreate.vue'), meta: { requiresAuth: true } },
      { path: 'upload', component: () => import('../views/FileUpload.vue'), meta: { requiresAuth: true } },
      { path: 'users', component: () => import('../views/UserList.vue'), meta: { requiresAuth: true } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },
      
      // 埋点监控路由
      { path: 'tracker/dashboard', component: () => import('../views/tracker/Dashboard.vue'), meta: { requiresAuth: true } },
      { path: 'tracker/user-behavior', component: () => import('../views/tracker/UserBehavior.vue'), meta: { requiresAuth: true } },
      { path: 'tracker/heatmap', component: () => import('../views/tracker/Heatmap.vue'), meta: { requiresAuth: true } },
      { path: 'tracker/statistics', component: () => import('../views/tracker/Statistics.vue'), meta: { requiresAuth: true } },
    ]
  },
  {
    path: '/logout',
    component: () => import('../views/Logout.vue')
  },
  {
    path: '/login',
    component: () => import('../views/Login.vue')
  },
  
];

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token');
  // 已登录访问登录页 -> 跳到主页面
  if (to.path === '/login') {
    return token ? next('/') : next();
  }
  // 其他页面未登录 -> 统一跳转登录页
  if (!token) {
    return next('/login');
  }
  next();
});

export default router;
