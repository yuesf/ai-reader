import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  // base: '/admin/', // 指定基础路径为 /admin/
  server: {
    port: 5173,
    proxy: {
      '/v1': {
        // target: 'http://127.0.0.1:8080',
        target: 'https://yuesf.cn/reader',
        changeOrigin: true,
        timeout: 300000, // 5分钟超时
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('代理错误:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('代理请求:', req.method, req.url);
            // 对于文件上传请求，设置更大的超时时间
            if (req.url && req.url.includes('/upload/')) {
              proxyReq.setTimeout(300000); // 5分钟
            }
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('代理响应:', proxyRes.statusCode, req.url);
          });
        }
      }
    }
  }
});


