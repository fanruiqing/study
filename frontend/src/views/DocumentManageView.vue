<template>
  <div class="document-manage-view">
    <!-- 顶部导航栏 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <button @click="$router.back()" class="btn-back">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
          </button>
          <div class="icon-wrapper">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
          </div>
          <div>
            <h1>{{ knowledgeBase?.name }}</h1>
            <p class="subtitle">文档管理 · {{ documents.length }} 个文档</p>
          </div>
        </div>
        <button @click="showUploadDialog = true" class="btn-upload">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="17 8 12 3 7 8"></polyline>
            <line x1="12" y1="3" x2="12" y2="15"></line>
          </svg>
          上传文档
        </button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="content-wrapper">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="documents.length === 0" class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
        </div>
        <h3>还没有文档</h3>
        <p>上传文档开始构建您的知识库</p>
        <button @click="showUploadDialog = true" class="btn-upload-empty">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="17 8 12 3 7 8"></polyline>
            <line x1="12" y1="3" x2="12" y2="15"></line>
          </svg>
          上传文档
        </button>
      </div>

      <!-- 文档列表 -->
      <div v-else class="documents-container">
        <div class="documents-grid">
          <div
            v-for="doc in documents"
            :key="doc.id"
            class="doc-card"
          >
            <div class="doc-card-header">
              <div class="doc-icon" :class="getFileTypeClass(doc.fileType)">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                </svg>
              </div>
              <div class="doc-actions">
                <button @click="viewChunks(doc)" class="action-btn" title="查看块">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                    <circle cx="12" cy="12" r="3"></circle>
                  </svg>
                </button>
                <button @click="deleteDocument(doc.id)" class="action-btn danger" title="删除">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                  </svg>
                </button>
              </div>
            </div>
            
            <h3 class="doc-title" :title="doc.fileName">{{ doc.fileName }}</h3>
            
            <div class="doc-meta">
              <div class="meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                  <line x1="9" y1="3" x2="9" y2="21"></line>
                </svg>
                <span>{{ doc.fileType.toUpperCase() }}</span>
              </div>
              <div class="meta-item">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                </svg>
                <span>{{ formatFileSize(doc.fileSize) }}</span>
              </div>
            </div>

            <div class="doc-status">
              <span :class="['status-badge', doc.status.toLowerCase()]">
                <span class="status-dot"></span>
                {{ getStatusText(doc.status) }}
              </span>
              <span class="chunk-count">{{ doc.chunkCount }} 块</span>
            </div>

            <div v-if="doc.category || (doc.tags && doc.tags.length)" class="doc-tags">
              <span v-if="doc.category" class="tag category">{{ doc.category }}</span>
              <span v-for="tag in doc.tags?.slice(0, 2)" :key="tag" class="tag">{{ tag }}</span>
              <span v-if="doc.tags && doc.tags.length > 2" class="tag more">+{{ doc.tags.length - 2 }}</span>
            </div>

            <div class="doc-footer">
              <div class="doc-date">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
                <span>{{ formatDate(doc.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传对话框 -->
    <transition name="modal">
      <div v-if="showUploadDialog" class="modal-overlay" @click.self="uploading ? null : closeUploadDialog()">
        <div class="modal-container" :class="{ 'uploading-mode': uploading }">
          <div class="modal-header">
            <h2>{{ uploading ? '上传进度' : '上传文档' }}</h2>
            <button v-if="!uploading" @click="closeUploadDialog" class="close-btn">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          
          <!-- 上传进度显示 -->
          <div v-if="uploading" class="upload-progress-container">
            <div class="progress-file-info">
              <div class="file-icon-large">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                  <polyline points="14 2 14 8 20 8"></polyline>
                </svg>
              </div>
              <div>
                <p class="progress-file-name">{{ uploadForm.file?.name }}</p>
                <p class="progress-file-size">{{ formatFileSize(uploadForm.file?.size || 0) }}</p>
              </div>
            </div>

            <!-- 进度步骤 -->
            <div class="progress-steps">
              <!-- 步骤1: 上传文件 -->
              <div class="progress-step" :class="getStepClass('upload')">
                <div class="step-icon">
                  <svg v-if="uploadProgress.upload === 'completed'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  <div v-else-if="uploadProgress.upload === 'processing'" class="step-spinner"></div>
                  <span v-else class="step-number">1</span>
                </div>
                <div class="step-content">
                  <h4>上传文件</h4>
                  <p>{{ uploadProgress.uploadMessage || '准备上传...' }}</p>
                  <div v-if="uploadProgress.upload === 'processing' && uploadProgress.uploadPercent > 0" class="progress-bar">
                    <div class="progress-fill" :style="{ width: uploadProgress.uploadPercent + '%' }"></div>
                  </div>
                </div>
              </div>

              <!-- 步骤2: 提取内容 -->
              <div class="progress-step" :class="getStepClass('extract')">
                <div class="step-icon">
                  <svg v-if="uploadProgress.extract === 'completed'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  <div v-else-if="uploadProgress.extract === 'processing'" class="step-spinner"></div>
                  <span v-else class="step-number">2</span>
                </div>
                <div class="step-content">
                  <h4>提取内容</h4>
                  <p>{{ uploadProgress.extractMessage || '等待提取文档内容...' }}</p>
                </div>
              </div>

              <!-- 步骤3: 文本分块 -->
              <div class="progress-step" :class="getStepClass('chunk')">
                <div class="step-icon">
                  <svg v-if="uploadProgress.chunk === 'completed'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  <div v-else-if="uploadProgress.chunk === 'processing'" class="step-spinner"></div>
                  <span v-else class="step-number">3</span>
                </div>
                <div class="step-content">
                  <h4>文本分块</h4>
                  <p>{{ uploadProgress.chunkMessage || '等待分块处理...' }}</p>
                  <div v-if="uploadProgress.chunkCount > 0" class="chunk-info">
                    已生成 <strong>{{ uploadProgress.chunkCount }}</strong> 个文本块
                  </div>
                </div>
              </div>

              <!-- 步骤4: 向量化 -->
              <div class="progress-step" :class="getStepClass('vectorize')">
                <div class="step-icon">
                  <svg v-if="uploadProgress.vectorize === 'completed'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  <div v-else-if="uploadProgress.vectorize === 'processing'" class="step-spinner"></div>
                  <span v-else class="step-number">4</span>
                </div>
                <div class="step-content">
                  <h4>向量化处理</h4>
                  <p>{{ uploadProgress.vectorizeMessage || '等待向量化...' }}</p>
                  <div v-if="uploadProgress.vectorize === 'processing' && uploadProgress.vectorizePercent > 0" class="progress-bar">
                    <div class="progress-fill" :style="{ width: uploadProgress.vectorizePercent + '%' }"></div>
                    <span class="progress-text">{{ uploadProgress.vectorizePercent }}%</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 完成或错误状态 -->
            <div v-if="uploadProgress.status === 'completed'" class="upload-result success">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
              </svg>
              <h3>上传成功!</h3>
              <p>文档已成功处理并添加到知识库</p>
              <button @click="closeUploadDialog" class="btn-done">完成</button>
            </div>

            <div v-else-if="uploadProgress.status === 'error'" class="upload-result error">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="15" y1="9" x2="9" y2="15"></line>
                <line x1="9" y1="9" x2="15" y2="15"></line>
              </svg>
              <h3>处理异常</h3>
              <p>{{ uploadProgress.errorMessage || '处理文档时发生错误' }}</p>
              <div class="result-actions">
                <button @click="closeUploadDialog" class="btn-secondary">关闭</button>
                <button @click="resetUpload" class="btn-retry">重新上传</button>
              </div>
            </div>

            <!-- 后台处理提示 -->
            <div v-else class="background-processing-hint">
              <p class="hint-text">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                文档处理需要一些时间,您可以关闭此窗口,处理将在后台继续进行
              </p>
              <button @click="closeUploadDialog" class="btn-background">后台处理</button>
            </div>
          </div>

          <!-- 上传表单 -->
          <form v-else @submit.prevent="uploadDocument" class="modal-body">
            <div class="upload-area" :class="{ 'drag-over': isDragging }" @dragover.prevent="isDragging = true" @dragleave.prevent="isDragging = false" @drop.prevent="handleDrop">
              <input
                type="file"
                ref="fileInput"
                @change="handleFileSelect"
                accept=".pdf,.docx,.txt,.md"
                required
                style="display: none"
              />
              <div v-if="!uploadForm.file" class="upload-placeholder" @click="() => fileInput?.click()">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="17 8 12 3 7 8"></polyline>
                  <line x1="12" y1="3" x2="12" y2="15"></line>
                </svg>
                <p class="upload-text">点击或拖拽文件到此处</p>
                <p class="upload-hint">支持 PDF、Word、TXT、Markdown 格式，最大 10MB</p>
              </div>
              <div v-else class="file-selected">
                <div class="file-icon">
                  <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                  </svg>
                </div>
                <div class="file-info">
                  <p class="file-name">{{ uploadForm.file.name }}</p>
                  <p class="file-size">{{ formatFileSize(uploadForm.file.size) }}</p>
                </div>
                <button type="button" @click="clearFile" class="remove-file-btn">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </button>
              </div>
            </div>

            <div class="form-section">
              <div class="form-group">
                <label class="form-label">分类</label>
                <input v-model="uploadForm.category" placeholder="例如：技术文档、产品手册" class="form-input" />
              </div>
              
              <div class="form-group">
                <label class="form-label">标签</label>
                <input v-model="uploadForm.tags" placeholder="用逗号分隔，例如：Java, Spring, 后端" class="form-input" />
                <p class="form-hint">添加标签便于后续检索和管理</p>
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" @click="closeUploadDialog" class="btn-cancel" :disabled="uploading">
                取消
              </button>
              <button type="submit" class="btn-submit" :disabled="!uploadForm.file || uploading">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                  <polyline points="17 8 12 3 7 8"></polyline>
                  <line x1="12" y1="3" x2="12" y2="15"></line>
                </svg>
                开始上传
              </button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <!-- 查看块对话框 -->
    <transition name="modal">
      <div v-if="showChunksDialog" class="modal-overlay" @click.self="showChunksDialog = false">
        <div class="modal-container modal-large">
          <div class="modal-header">
            <div>
              <h2>文档块</h2>
              <p class="modal-subtitle">{{ selectedDoc?.fileName }}</p>
            </div>
            <button @click="showChunksDialog = false" class="close-btn">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          
          <div class="modal-body">
            <div v-if="loadingChunks" class="loading-state">
              <div class="spinner"></div>
              <p>加载中...</p>
            </div>
            <div v-else class="chunks-list">
              <div class="chunks-stats">
                <div class="stat-card">
                  <div class="stat-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                    </svg>
                  </div>
                  <div class="stat-info">
                    <p class="stat-label">总块数</p>
                    <p class="stat-value">{{ chunks.length }}</p>
                  </div>
                </div>
                <div class="stat-card">
                  <div class="stat-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                    </svg>
                  </div>
                  <div class="stat-info">
                    <p class="stat-label">平均长度</p>
                    <p class="stat-value">{{ getAverageLength() }} 字符</p>
                  </div>
                </div>
                <div class="stat-card">
                  <div class="stat-icon">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"></circle>
                      <polyline points="12 6 12 12 16 14"></polyline>
                    </svg>
                  </div>
                  <div class="stat-info">
                    <p class="stat-label">向量化状态</p>
                    <p class="stat-value">{{ selectedDoc?.status === 'COMPLETED' ? '已完成' : '处理中' }}</p>
                  </div>
                </div>
              </div>

              <div class="chunks-search">
                <input 
                  v-model="chunkSearchQuery" 
                  placeholder="搜索文本块内容..." 
                  class="search-input"
                  @input="filterChunks"
                />
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"></circle>
                  <path d="m21 21-4.35-4.35"></path>
                </svg>
              </div>

              <div v-for="chunk in filteredChunks" :key="chunk.id" class="chunk-card">
                <div class="chunk-header">
                  <span class="chunk-badge">块 #{{ chunk.chunkIndex + 1 }}</span>
                  <div class="chunk-meta">
                    <span class="chunk-length">{{ chunk.content.length }} 字符</span>
                    <span class="chunk-id">{{ chunk.id }}</span>
                  </div>
                </div>
                <div class="chunk-content">{{ chunk.content }}</div>
                <div v-if="chunk.vectorId" class="chunk-footer">
                  <div class="vector-info">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
                    </svg>
                    <span>向量 ID: {{ chunk.vectorId }}</span>
                  </div>
                </div>
              </div>
              
              <div v-if="filteredChunks.length === 0 && chunkSearchQuery" class="no-results">
                <p>未找到匹配的文本块</p>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button @click="showChunksDialog = false" class="btn-cancel">关闭</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const knowledgeBaseId = route.params.id as string

interface Document {
  id: string
  fileName: string
  fileType: string
  fileSize: number
  status: string
  chunkCount: number
  category?: string
  tags?: string[]
  createdAt: number
}

interface Chunk {
  id: string
  chunkIndex: number
  content: string
}

const knowledgeBase = ref<any>(null)
const documents = ref<Document[]>([])
const loading = ref(false)
const showUploadDialog = ref(false)
const uploading = ref(false)
const isDragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadForm = ref({
  file: null as File | null,
  category: '',
  tags: ''
})

const uploadProgress = ref({
  status: 'idle', // idle, uploading, completed, error
  upload: 'pending', // pending, processing, completed
  uploadPercent: 0,
  uploadMessage: '',
  extract: 'pending',
  extractMessage: '',
  chunk: 'pending',
  chunkMessage: '',
  chunkCount: 0,
  vectorize: 'pending',
  vectorizeMessage: '',
  vectorizePercent: 0,
  errorMessage: ''
})

const showChunksDialog = ref(false)
const selectedDoc = ref<Document | null>(null)
const chunks = ref<Chunk[]>([])
const filteredChunks = ref<Chunk[]>([])
const chunkSearchQuery = ref('')
const loadingChunks = ref(false)
let pollInterval: number | null = null

const loadKnowledgeBase = async () => {
  try {
    const response = await api.get(`/knowledge-bases/${knowledgeBaseId}`)
    knowledgeBase.value = response.data
  } catch (error) {
    console.error('加载知识库失败:', error)
  }
}

const loadDocuments = async () => {
  loading.value = true
  try {
    const response = await api.get(`/documents?knowledgeBaseId=${knowledgeBaseId}`)
    documents.value = response.data
  } catch (error) {
    console.error('加载文档失败:', error)
    alert('加载文档失败')
  } finally {
    loading.value = false
  }
}

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    uploadForm.value.file = target.files[0]
  }
}

const handleDrop = (event: DragEvent) => {
  isDragging.value = false
  if (event.dataTransfer?.files && event.dataTransfer.files[0]) {
    uploadForm.value.file = event.dataTransfer.files[0]
  }
}

const clearFile = () => {
  uploadForm.value.file = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const closeUploadDialog = () => {
  // 允许在错误状态、完成状态或非上传状态下关闭
  if (!uploading.value || uploadProgress.value.status === 'error' || uploadProgress.value.status === 'completed') {
    showUploadDialog.value = false
    uploadForm.value = { file: null, category: '', tags: '' }
    clearFile()
    resetUpload()
    if (pollInterval) {
      clearInterval(pollInterval)
      pollInterval = null
    }
  }
}

const uploadDocument = async () => {
  if (!uploadForm.value.file || uploading.value) {
    return // 防止重复提交
  }
  
  // 检查是否已存在同名文件
  const existingDoc = documents.value.find(doc => doc.fileName === uploadForm.value.file?.name)
  if (existingDoc) {
    const confirmed = confirm(`已存在同名文件"${uploadForm.value.file.name}"，是否继续上传？`)
    if (!confirmed) {
      return
    }
  }
  
  uploading.value = true
  uploadProgress.value = {
    status: 'uploading',
    upload: 'processing',
    uploadPercent: 0,
    uploadMessage: '正在上传文件...',
    extract: 'pending',
    extractMessage: '',
    chunk: 'pending',
    chunkMessage: '',
    chunkCount: 0,
    vectorize: 'pending',
    vectorizeMessage: '',
    vectorizePercent: 0,
    errorMessage: ''
  }
  
  try {
    const formData = new FormData()
    formData.append('knowledgeBaseId', knowledgeBaseId)
    formData.append('file', uploadForm.value.file)
    if (uploadForm.value.category) {
      formData.append('category', uploadForm.value.category)
    }
    if (uploadForm.value.tags) {
      formData.append('tags', uploadForm.value.tags)
    }
    
    // 模拟上传进度
    const uploadProgressInterval = setInterval(() => {
      if (uploadProgress.value.uploadPercent < 90) {
        uploadProgress.value.uploadPercent += 10
      }
    }, 200)
    
    const response = await api.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          uploadProgress.value.uploadPercent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      }
    })
    
    clearInterval(uploadProgressInterval)
    uploadProgress.value.uploadPercent = 100
    uploadProgress.value.upload = 'completed'
    uploadProgress.value.uploadMessage = '文件上传成功'
    
    // 获取文档ID并开始轮询状态
    const documentId = response.data.id
    if (documentId) {
      await pollDocumentProgress(documentId)
    } else {
      throw new Error('未能获取文档ID')
    }
    
  } catch (error: any) {
    console.error('上传文档失败:', error)
    uploadProgress.value.status = 'error'
    uploadProgress.value.errorMessage = error.response?.data?.message || error.message || '上传失败'
    uploading.value = false // 失败时重置状态,允许重试
  }
}

const pollDocumentProgress = async (documentId: string) => {
  uploadProgress.value.extract = 'processing'
  uploadProgress.value.extractMessage = '正在提取文档内容...'
  
  let attempts = 0
  const maxAttempts = 40 // 最多轮询40次 (2分钟)
  const startTime = Date.now()
  
  pollInterval = window.setInterval(async () => {
    attempts++
    const elapsedTime = Math.floor((Date.now() - startTime) / 1000) // 秒
    
    try {
      const response = await api.get(`/documents/${documentId}`)
      const doc = response.data
      
      // 更新进度状态
      if (doc.status === 'PENDING') {
        uploadProgress.value.extract = 'processing'
        uploadProgress.value.extractMessage = `等待处理... (${elapsedTime}s)`
      } else if (doc.status === 'PROCESSING') {
        uploadProgress.value.extract = 'completed'
        uploadProgress.value.extractMessage = '内容提取完成'
        uploadProgress.value.chunk = 'processing'
        
        // 如果有块数量,显示向量化进度
        if (doc.chunkCount > 0) {
          uploadProgress.value.chunkCount = doc.chunkCount
          uploadProgress.value.chunk = 'completed'
          uploadProgress.value.chunkMessage = `已生成 ${doc.chunkCount} 个文本块`
          uploadProgress.value.vectorize = 'processing'
          uploadProgress.value.vectorizeMessage = `正在生成向量嵌入... (${elapsedTime}s)`
          
          // 根据时间估算进度
          const estimatedProgress = Math.min(95, Math.floor((elapsedTime / 120) * 100))
          uploadProgress.value.vectorizePercent = estimatedProgress
        } else {
          uploadProgress.value.chunkMessage = `正在分块处理... (${elapsedTime}s)`
        }
      } else if (doc.status === 'COMPLETED') {
        // 处理完成
        uploadProgress.value.extract = 'completed'
        uploadProgress.value.chunk = 'completed'
        uploadProgress.value.chunkCount = doc.chunkCount
        uploadProgress.value.vectorize = 'completed'
        uploadProgress.value.vectorizePercent = 100
        uploadProgress.value.vectorizeMessage = '向量化完成'
        uploadProgress.value.status = 'completed'
        
        if (pollInterval) {
          clearInterval(pollInterval)
          pollInterval = null
        }
        
        await loadDocuments()
      } else if (doc.status === 'FAILED') {
        // 处理失败
        uploadProgress.value.status = 'error'
        uploadProgress.value.errorMessage = doc.errorMessage || '文档处理失败'
        
        if (pollInterval) {
          clearInterval(pollInterval)
          pollInterval = null
        }
      }
      
      // 超时处理 - 2分钟后自动标记为超时
      if (attempts >= maxAttempts) {
        uploadProgress.value.status = 'error'
        uploadProgress.value.errorMessage = `处理超时 (${elapsedTime}s)。文档可能较大或系统繁忙,请稍后在文档列表中查看处理结果。`
        
        if (pollInterval) {
          clearInterval(pollInterval)
          pollInterval = null
        }
        
        // 即使超时也刷新列表,让用户看到文档
        await loadDocuments()
      }
    } catch (error) {
      console.error('轮询文档状态失败:', error)
      
      // 如果是网络错误且已经尝试多次,停止轮询
      if (attempts >= 10) {
        uploadProgress.value.status = 'error'
        uploadProgress.value.errorMessage = '无法获取处理状态,请刷新页面查看'
        
        if (pollInterval) {
          clearInterval(pollInterval)
          pollInterval = null
        }
      }
    }
  }, 3000) // 每3秒轮询一次
}

const getStepClass = (step: string) => {
  const status = uploadProgress.value[step as keyof typeof uploadProgress.value]
  return {
    'step-pending': status === 'pending',
    'step-processing': status === 'processing',
    'step-completed': status === 'completed'
  }
}

const resetUpload = () => {
  uploadProgress.value = {
    status: 'idle',
    upload: 'pending',
    uploadPercent: 0,
    uploadMessage: '',
    extract: 'pending',
    extractMessage: '',
    chunk: 'pending',
    chunkMessage: '',
    chunkCount: 0,
    vectorize: 'pending',
    vectorizeMessage: '',
    vectorizePercent: 0,
    errorMessage: ''
  }
  uploading.value = false
}

const pollDocumentStatus = () => {
  const interval = setInterval(async () => {
    await loadDocuments()
    const processing = documents.value.some(d => d.status === 'PENDING' || d.status === 'PROCESSING')
    if (!processing) {
      clearInterval(interval)
    }
  }, 3000)
}

const deleteDocument = async (id: string) => {
  if (!confirm('确定要删除这个文档吗？此操作不可恢复。')) return
  
  try {
    await api.delete(`/documents/${id}`)
    await loadDocuments()
  } catch (error) {
    console.error('删除文档失败:', error)
    alert('删除文档失败')
  }
}

const viewChunks = async (doc: Document) => {
  selectedDoc.value = doc
  showChunksDialog.value = true
  loadingChunks.value = true
  chunkSearchQuery.value = ''
  
  try {
    const response = await api.get(`/documents/${doc.id}/chunks`)
    chunks.value = response.data
    filteredChunks.value = response.data
  } catch (error) {
    console.error('加载文档块失败:', error)
    alert('加载文档块失败')
  } finally {
    loadingChunks.value = false
  }
}

const filterChunks = () => {
  if (!chunkSearchQuery.value) {
    filteredChunks.value = chunks.value
  } else {
    const query = chunkSearchQuery.value.toLowerCase()
    filteredChunks.value = chunks.value.filter(chunk => 
      chunk.content.toLowerCase().includes(query)
    )
  }
}

const getAverageLength = () => {
  if (chunks.value.length === 0) return 0
  const total = chunks.value.reduce((sum, chunk) => sum + chunk.content.length, 0)
  return Math.round(total / chunks.value.length)
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  if (days < 30) return `${Math.floor(days / 7)} 周前`
  if (days < 365) return `${Math.floor(days / 30)} 月前`
  return `${Math.floor(days / 365)} 年前`
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    PENDING: '等待处理',
    PROCESSING: '处理中',
    COMPLETED: '已完成',
    FAILED: '失败'
  }
  return map[status] || status
}

const getFileTypeClass = (fileType: string) => {
  const type = fileType.toLowerCase()
  if (type === 'pdf') return 'pdf'
  if (type === 'docx' || type === 'doc') return 'word'
  if (type === 'txt') return 'text'
  if (type === 'md') return 'markdown'
  return 'default'
}

onMounted(() => {
  loadKnowledgeBase()
  loadDocuments()
  pollDocumentStatus()
})
</script>

<style scoped>
.document-manage-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 100%);
}

/* 页面头部 */
.page-header {
  background: white;
  border-bottom: 1px solid #e8eaed;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 1.5rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn-back {
  width: 40px;
  height: 40px;
  background: #f3f4f6;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s;
}

.btn-back:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.icon-wrapper {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.title-section h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: -0.02em;
}

.subtitle {
  margin: 0.25rem 0 0 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.btn-upload {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-upload:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 内容区域 */
.content-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 6rem 2rem;
  color: #6b7280;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e5e7eb;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 6rem 2rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 1.5rem;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.empty-state h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a1a;
}

.empty-state p {
  margin: 0 0 2rem 0;
  color: #6b7280;
  font-size: 0.9375rem;
}

.btn-upload-empty {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.875rem 2rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-upload-empty:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 文档网格 */
.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

/* 文档卡片 */
.doc-card {
  background: white;
  border-radius: 16px;
  padding: 1.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e8eaed;
  position: relative;
  overflow: hidden;
}

.doc-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transform: scaleX(0);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.doc-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: #667eea;
}

.doc-card:hover::before {
  transform: scaleX(1);
}

.doc-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.doc-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #667eea;
}

.doc-icon.pdf {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
}

.doc-icon.word {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #2563eb;
}

.doc-icon.text {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #4f46e5;
}

.doc-icon.markdown {
  background: linear-gradient(135deg, #f3e8ff 0%, #e9d5ff 100%);
  color: #9333ea;
}

.doc-actions {
  display: flex;
  gap: 0.5rem;
  opacity: 0;
  transition: opacity 0.2s;
}

.doc-card:hover .doc-actions {
  opacity: 1;
}

.action-btn {
  width: 32px;
  height: 32px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.action-btn.danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

.doc-title {
  margin: 0 0 1rem 0;
  font-size: 1.0625rem;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.8rem;
}

.doc-meta {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #f3f4f6;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: #6b7280;
}

.meta-item svg {
  color: #9ca3af;
}

.doc-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 500;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.pending .status-dot {
  background: #f59e0b;
}

.status-badge.processing {
  background: #dbeafe;
  color: #1e40af;
}

.status-badge.processing .status-dot {
  background: #3b82f6;
}

.status-badge.completed {
  background: #d1fae5;
  color: #065f46;
}

.status-badge.completed .status-dot {
  background: #10b981;
}

.status-badge.failed {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge.failed .status-dot {
  background: #ef4444;
}

.chunk-count {
  font-size: 0.8125rem;
  color: #6b7280;
  font-weight: 500;
}

.doc-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.tag {
  background: #f3f4f6;
  color: #374151;
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 500;
}

.tag.category {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #4f46e5;
}

.tag.more {
  background: #e5e7eb;
  color: #6b7280;
}

.doc-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.doc-date {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: #9ca3af;
}

.doc-date svg {
  color: #9ca3af;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-container {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  transition: max-width 0.3s;
}

.modal-container.uploading-mode {
  max-width: 700px;
}

.modal-large {
  max-width: 900px;
}

.modal-header {
  padding: 1.5rem 2rem;
  border-bottom: 1px solid #e8eaed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a1a;
}

.modal-subtitle {
  margin: 0.25rem 0 0 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: #f3f4f6;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #e5e7eb;
  color: #1a1a1a;
}

.modal-body {
  padding: 2rem;
  overflow-y: auto;
}

/* 上传区域 */
.upload-area {
  border: 2px dashed #e5e7eb;
  border-radius: 12px;
  padding: 2rem;
  margin-bottom: 1.5rem;
  transition: all 0.2s;
}

.upload-area.drag-over {
  border-color: #667eea;
  background: #f5f7ff;
}

.upload-placeholder {
  text-align: center;
  cursor: pointer;
}

.upload-placeholder svg {
  color: #9ca3af;
  margin-bottom: 1rem;
}

.upload-text {
  margin: 0 0 0.5rem 0;
  font-size: 1rem;
  font-weight: 500;
  color: #374151;
}

.upload-hint {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.file-selected {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: #f9fafb;
  border-radius: 10px;
}

.file-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  margin: 0 0 0.25rem 0;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  margin: 0;
  font-size: 0.8125rem;
  color: #6b7280;
}

.remove-file-btn {
  width: 32px;
  height: 32px;
  background: #fee2e2;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #dc2626;
  transition: all 0.2s;
  flex-shrink: 0;
}

.remove-file-btn:hover {
  background: #fecaca;
}

.form-section {
  margin-bottom: 1.5rem;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}

.form-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  font-size: 0.9375rem;
  color: #1a1a1a;
  transition: all 0.2s;
  font-family: inherit;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-hint {
  margin: 0.5rem 0 0 0;
  font-size: 0.8125rem;
  color: #6b7280;
}

.modal-footer {
  padding: 1.5rem 2rem;
  border-top: 1px solid #e8eaed;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.btn-cancel {
  background: #f3f4f6;
  color: #374151;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-cancel:hover:not(:disabled) {
  background: #e5e7eb;
}

.btn-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.btn-submit:disabled:hover {
  transform: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

/* 块列表 */
.chunks-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.chunks-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.stat-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-label {
  margin: 0 0 0.25rem 0;
  font-size: 0.75rem;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a1a;
}

.chunks-search {
  position: relative;
  margin-bottom: 1rem;
}

.search-input {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 1rem;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  font-size: 0.9375rem;
  color: #1a1a1a;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.chunks-search svg {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: #9ca3af;
  pointer-events: none;
}

.no-results {
  text-align: center;
  padding: 3rem;
  color: #6b7280;
}

.no-results p {
  margin: 0;
  font-size: 0.9375rem;
}

.chunk-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1.25rem;
  transition: all 0.2s;
}

.chunk-card:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.chunk-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.chunk-length {
  font-size: 0.75rem;
  color: #6b7280;
  padding: 0.25rem 0.5rem;
  background: white;
  border-radius: 4px;
}

.chunk-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
}

.chunk-id {
  font-size: 0.75rem;
  color: #9ca3af;
  font-family: monospace;
}

.chunk-content {
  line-height: 1.7;
  white-space: pre-wrap;
  color: #374151;
  font-size: 0.9375rem;
  margin-bottom: 0.75rem;
}

.chunk-footer {
  padding-top: 0.75rem;
  border-top: 1px solid #e5e7eb;
}

.vector-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: #667eea;
  font-weight: 500;
}

.vector-info svg {
  color: #667eea;
}

/* 动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.95) translateY(20px);
}

/* 响应式 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  
  .title-section {
    flex-wrap: wrap;
  }
  
  .btn-upload {
    width: 100%;
    justify-content: center;
  }
  
  .documents-grid {
    grid-template-columns: 1fr;
  }
}

/* 上传进度样式 */
.upload-progress-container {
  padding: 2rem;
}

.progress-file-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-radius: 12px;
  margin-bottom: 2rem;
}

.file-icon-large {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.progress-file-name {
  margin: 0 0 0.25rem 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1a1a1a;
}

.progress-file-size {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.progress-step {
  display: flex;
  gap: 1rem;
  position: relative;
}

.progress-step::before {
  content: '';
  position: absolute;
  left: 20px;
  top: 48px;
  bottom: -24px;
  width: 2px;
  background: #e5e7eb;
}

.progress-step:last-child::before {
  display: none;
}

.progress-step.step-completed::before {
  background: linear-gradient(180deg, #667eea 0%, #e5e7eb 100%);
}

.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f3f4f6;
  color: #9ca3af;
  font-weight: 600;
  transition: all 0.3s;
}

.progress-step.step-processing .step-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.progress-step.step-completed .step-icon {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.step-number {
  font-size: 1rem;
}

.step-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.step-content {
  flex: 1;
  padding-top: 0.25rem;
}

.step-content h4 {
  margin: 0 0 0.375rem 0;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1a1a1a;
}

.step-content p {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.progress-step.step-processing .step-content p {
  color: #667eea;
  font-weight: 500;
}

.progress-bar {
  margin-top: 0.75rem;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.75rem;
  font-weight: 600;
  color: #1a1a1a;
}

.chunk-info {
  margin-top: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: #f0fdf4;
  border-radius: 6px;
  font-size: 0.8125rem;
  color: #065f46;
}

.chunk-info strong {
  color: #047857;
  font-weight: 600;
}

.upload-result {
  text-align: center;
  padding: 2rem;
  margin-top: 2rem;
  border-radius: 12px;
}

.upload-result.success {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
}

.upload-result.error {
  background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
}

.upload-result svg {
  margin-bottom: 1rem;
}

.upload-result.success svg {
  color: #10b981;
}

.upload-result.error svg {
  color: #ef4444;
}

.upload-result h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
}

.upload-result.success h3 {
  color: #065f46;
}

.upload-result.error h3 {
  color: #991b1b;
}

.upload-result p {
  margin: 0 0 1.5rem 0;
  color: #6b7280;
  line-height: 1.6;
}

.result-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.btn-done,
.btn-retry {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 0.75rem 2rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-done:hover,
.btn-retry:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.background-processing-hint {
  margin-top: 2rem;
  padding: 1.5rem;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-radius: 12px;
  text-align: center;
}

.hint-text {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin: 0 0 1rem 0;
  font-size: 0.875rem;
  color: #92400e;
  line-height: 1.6;
}

.hint-text svg {
  flex-shrink: 0;
  color: #f59e0b;
}

.btn-background {
  background: white;
  color: #92400e;
  border: 1.5px solid #fbbf24;
  padding: 0.75rem 2rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-background:hover {
  background: #fef3c7;
  border-color: #f59e0b;
}
</style>
