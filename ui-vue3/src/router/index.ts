import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/reports'
  },
  {
    path: '/login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/reports',
    component: () => import('../views/ReportList.vue')
  },
  {
    path: '/reports/create',
    component: () => import('../views/ReportCreate.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/upload',
    component: () => import('../views/FileUpload.vue'),
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token');
  if (to.path === '/login') {
    return next();
  }
  if (to.meta.requiresAuth && !token) {
    return next('/login');
  }
  next();
});

export default router;


