import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import { setupErrorHandler } from './utils/errorHandler'
import './assets/main.css'

const app = createApp(App)

// 设置全局错误处理
setupErrorHandler(app)

app.use(createPinia())
app.use(router)

app.mount('#app')
