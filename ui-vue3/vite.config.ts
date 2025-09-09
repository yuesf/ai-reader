import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// 环境配置类型定义
interface EnvConfig {
  BASE_URL: string;
  API_VERSION: string;
  DEBUG: boolean;
}

// 环境配置 - 参考小程序配置模式
const ENV: Record<string, EnvConfig> = {
  // 开发环境
  DEV: {
    BASE_URL: 'http://127.0.0.1:8080',
    API_VERSION: 'v1',
    DEBUG: true
  },
  
  // 生产环境
  PROD: {
    BASE_URL: 'https://yuesf.cn/reader',
    API_VERSION: 'v1',
    DEBUG: false
  }
};

// 获取当前环境配置 - 类似小程序的 getCurrentConfig
const getCurrentConfig = (isDev: boolean): EnvConfig => {
  return isDev ? ENV.DEV : ENV.PROD;
};

// 获取完整API地址 - 类似小程序的 getApiUrl
const getApiUrl = (config: EnvConfig, path: string): string => {
  return `${config.BASE_URL}/${config.API_VERSION}${path}`;
};

// 获取基础URL - 类似小程序的 getBaseUrl
const getBaseUrl = (config: EnvConfig): string => {
  return config.BASE_URL;
};

// 是否调试模式 - 类似小程序的 isDebug
const isDebug = (config: EnvConfig): boolean => {
  return config.DEBUG;
};

export default defineConfig(({ mode }) => {
  // 根据构建模式选择环境配置
  const isDev = mode === 'development';
  const currentConfig = getCurrentConfig(isDev);
  const envName = isDev ? 'DEV' : 'PROD';
  
  console.log(`🚀 当前环境: ${mode} (${envName})`);
  console.log(`🔗 后台服务地址: ${getBaseUrl(currentConfig)}`);
  console.log(`📡 代理模式: ${isDev ? '启用' : '禁用'}`);
  console.log(`🐛 调试模式: ${isDebug(currentConfig) ? '启用' : '禁用'}`);

  return {
    plugins: [vue()],
    // base: '/admin/', // 指定基础路径为 /admin/
    
    // 定义全局常量，供前端代码使用 - 不再依赖 global.d.ts
    define: {
      // 环境配置对象
      'import.meta.env.VITE_ENV_CONFIG': JSON.stringify(currentConfig),
      'import.meta.env.VITE_CURRENT_ENV': JSON.stringify(envName),
      'import.meta.env.VITE_IS_DEV': JSON.stringify(isDev),
      // 基础配置
      'import.meta.env.VITE_BASE_URL': JSON.stringify(getBaseUrl(currentConfig)),
      'import.meta.env.VITE_IS_DEBUG': JSON.stringify(isDebug(currentConfig))
    },
    
    server: {
      port: 5173,
      proxy: {
        '/v1': {
          target: currentConfig.BASE_URL,
          changeOrigin: true,
          timeout: 300000, // 5分钟超时
          configure: (proxy, options) => {
            proxy.on('error', (err, req, res) => {
              console.log('❌ 代理错误:', err.message);
              console.log(`🔗 目标地址: ${currentConfig.BASE_URL}`);
            });
            proxy.on('proxyReq', (proxyReq, req, res) => {
              console.log(`📤 代理请求: ${req.method} ${req.url} -> ${currentConfig.BASE_URL}${req.url}`);
              // 对于文件上传请求，设置更大的超时时间
              if (req.url && req.url.includes('/upload/')) {
                proxyReq.setTimeout(300000); // 5分钟
              }
            });
            proxy.on('proxyRes', (proxyRes, req, res) => {
              const status = proxyRes.statusCode;
              const statusIcon = status >= 200 && status < 300 ? '✅' : '❌';
              console.log(`📥 代理响应: ${statusIcon} ${status} ${req.url}`);
            });
          }
        }
      }
    },
    
    build: {
      // 生产环境构建配置
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['vue', 'axios'],
            router: ['vue-router']
          }
        }
      }
    }
  };
});

// 导出配置工具函数，供其他模块使用
export { ENV, getCurrentConfig, getApiUrl, getBaseUrl, isDebug };
export type { EnvConfig };