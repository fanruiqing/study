<template>
  <div class="prompts-view">
    <div class="prompts-header">
      <h2>提示词模板</h2>
      <el-button type="primary" :icon="Plus" @click="showAddTemplate">
        新建模板
      </el-button>
    </div>
    
    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索提示词..."
        :prefix-icon="Search"
        clearable
        @input="handleSearch"
      />
    </div>
    
    <div class="templates-container">
      <template v-if="Object.keys(groupedTemplates).length > 0">
        <div v-for="(templates, category) in groupedTemplates" :key="category" class="category-section">
          <h3 class="category-title">{{ category }}</h3>
          <div class="template-grid">
            <el-card
              v-for="template in templates"
              :key="template.id"
              class="template-card"
              @click="showTemplateDetail(template)"
            >
              <div class="template-title">{{ template.title }}</div>
              <div class="template-preview">{{ template.content.slice(0, 100) }}...</div>
              <div class="template-actions" @click.stop>
                <el-button text size="small" @click="editTemplate(template)">编辑</el-button>
                <el-button text size="small" type="danger" @click="deleteTemplate(template.id)">删除</el-button>
              </div>
            </el-card>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无提示词模板" />
    </div>
    
    <!-- Add/Edit Template Dialog -->
    <el-dialog
      v-model="templateDialogVisible"
      :title="editingTemplate ? '编辑模板' : '新建模板'"
      width="600px"
    >
      <el-form :model="templateForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="templateForm.title" placeholder="模板标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="templateForm.category" filterable allow-create placeholder="选择或输入分类">
            <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="templateForm.content"
            type="textarea"
            :rows="10"
            placeholder="提示词内容..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- Template Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedTemplate?.title"
      width="600px"
    >
      <div class="template-detail">
        <pre>{{ selectedTemplate?.content }}</pre>
      </div>
      <template #footer>
        <el-button @click="copyTemplate">复制内容</el-button>
        <el-button type="primary" @click="applyTemplate">应用到对话</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { promptApi } from '@/api'
import type { PromptTemplate } from '@/types'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const templates = ref<PromptTemplate[]>([])
const categories = ref<string[]>([])
const searchQuery = ref('')
const templateDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editingTemplate = ref<PromptTemplate | null>(null)
const selectedTemplate = ref<PromptTemplate | null>(null)

const templateForm = reactive({
  title: '',
  content: '',
  category: ''
})

const groupedTemplates = computed(() => {
  const filtered = searchQuery.value
    ? templates.value.filter(t => 
        t.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
        t.content.toLowerCase().includes(searchQuery.value.toLowerCase())
      )
    : templates.value
  
  return filtered.reduce((acc, template) => {
    const category = template.category || '未分类'
    if (!acc[category]) {
      acc[category] = []
    }
    acc[category].push(template)
    return acc
  }, {} as Record<string, PromptTemplate[]>)
})

const fetchTemplates = async () => {
  templates.value = await promptApi.getAll()
  categories.value = await promptApi.getCategories()
}

const handleSearch = async () => {
  if (searchQuery.value) {
    templates.value = await promptApi.search(searchQuery.value)
  } else {
    await fetchTemplates()
  }
}

const showAddTemplate = () => {
  editingTemplate.value = null
  templateForm.title = ''
  templateForm.content = ''
  templateForm.category = ''
  templateDialogVisible.value = true
}

const editTemplate = (template: PromptTemplate) => {
  editingTemplate.value = template
  templateForm.title = template.title
  templateForm.content = template.content
  templateForm.category = template.category || ''
  templateDialogVisible.value = true
}

const saveTemplate = async () => {
  if (!templateForm.title || !templateForm.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  
  try {
    if (editingTemplate.value) {
      await promptApi.update(editingTemplate.value.id, templateForm)
      ElMessage.success('更新成功')
    } else {
      await promptApi.create(templateForm)
      ElMessage.success('创建成功')
    }
    templateDialogVisible.value = false
    await fetchTemplates()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const deleteTemplate = async (id: string) => {
  await ElMessageBox.confirm('确定要删除这个模板吗？', '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await promptApi.delete(id)
  ElMessage.success('删除成功')
  await fetchTemplates()
}

const showTemplateDetail = (template: PromptTemplate) => {
  selectedTemplate.value = template
  detailDialogVisible.value = true
}

const copyTemplate = () => {
  if (selectedTemplate.value) {
    navigator.clipboard.writeText(selectedTemplate.value.content)
    ElMessage.success('已复制到剪贴板')
  }
}

const applyTemplate = () => {
  // Store template in session storage and navigate to chat
  if (selectedTemplate.value) {
    sessionStorage.setItem('applyTemplate', selectedTemplate.value.content)
    router.push('/')
    detailDialogVisible.value = false
  }
}

onMounted(fetchTemplates)
</script>

<style lang="scss" scoped>
.prompts-view {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.prompts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  
  h2 {
    font-size: 24px;
    font-weight: 600;
  }
}

.search-bar {
  margin-bottom: 24px;
  max-width: 400px;
}

.category-section {
  margin-bottom: 32px;
}

.category-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #666;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.template-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  
  .template-title {
    font-weight: 500;
    margin-bottom: 8px;
  }
  
  .template-preview {
    font-size: 13px;
    color: #666;
    line-height: 1.5;
    height: 60px;
    overflow: hidden;
  }
  
  .template-actions {
    margin-top: 12px;
    display: flex;
    gap: 8px;
  }
}

.template-detail {
  pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    background: #f5f5f5;
    padding: 16px;
    border-radius: 8px;
    font-size: 14px;
    line-height: 1.6;
    max-height: 400px;
    overflow-y: auto;
  }
}
</style>
