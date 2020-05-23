// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App.vue'
import router from './router'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'
import globalVue from '@/global/global.vue'


Vue.prototype.global = globalVue
Vue.prototype.axios = axios;
Vue.config.productionTip = false

Vue.use(ElementUI)

/* eslint-disable no-new */
new Vue({
  el: '#app',
  components: { App },
  template: '<App/>',
  router,
  render: h => h(App)
})
