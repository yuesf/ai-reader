/**
 * 小程序配置文件
 * 统一管理各种配置参数
 */

// 环境配置
const ENV = {
  
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

// 当前环境（根据实际部署情况修改）
const CURRENT_ENV = 'DEV';

// 获取当前环境配置
const getCurrentConfig = () => {
  return ENV[CURRENT_ENV];
};

// 导出配置
module.exports = {
  // 当前环境配置
  config: getCurrentConfig(),
  
  // 环境枚举
  ENV,
  
  // 当前环境
  CURRENT_ENV,
  
  // 获取完整API地址
  getApiUrl: (path) => {
    const config = getCurrentConfig();
    return `${config.BASE_URL}/${config.API_VERSION}${path}`;
  },
  
  // 获取基础URL
  getBaseUrl: () => {
    return getCurrentConfig().BASE_URL;
  },
  
  // 是否调试模式
  isDebug: () => {
    return getCurrentConfig().DEBUG;
  }
};
