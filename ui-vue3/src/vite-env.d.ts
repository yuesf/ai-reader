/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly MODE: string
  readonly VITE_ENV_CONFIG: string
  readonly VITE_CURRENT_ENV: string
  readonly VITE_IS_DEV: string
  readonly VITE_IS_DEBUG: string
  readonly VITE_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}