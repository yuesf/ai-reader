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
  history: createWebHistory(),
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


