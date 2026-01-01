import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'chat',
      component: () => import('@/views/ChatView.vue')
    },
    {
      path: '/prompts',
      name: 'prompts',
      component: () => import('@/views/PromptsView.vue')
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/SettingsView.vue')
    },
    {
      path: '/knowledge-base',
      name: 'knowledge-base',
      component: () => import('@/views/KnowledgeBaseView.vue')
    },
    {
      path: '/knowledge-base/:id',
      name: 'document-manage',
      component: () => import('@/views/DocumentManageView.vue')
    },
    {
      path: '/workflow-builder',
      name: 'workflow-builder',
      component: () => import('@/views/WorkflowBuilderView.vue')
    }
  ]
})

export default router
