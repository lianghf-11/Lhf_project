import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import LoveApp from '../views/LoveApp.vue'
import ManusApp from '../views/ManusApp.vue'

const routes = [
  { path: '/', component: Home },
  { path: '/love-app', component: LoveApp },
  { path: '/manus', component: ManusApp }
]

export default createRouter({ history: createWebHistory(), routes })
