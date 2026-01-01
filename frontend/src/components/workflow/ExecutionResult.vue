<template>
  <div class="execution-result">
    <div v-if="!result" class="no-result">
      <el-icon><InfoFilled /></el-icon>
      <span>暂无执行记录</span>
    </div>
    <div v-else class="result-content">
      <div class="result-section">
        <div class="section-title">执行状态</div>
        <el-tag :type="result.status === 'success' ? 'success' : 'danger'" size="large">
          {{ result.status === 'success' ? '成功' : '失败' }}
        </el-tag>
      </div>

      <div class="result-section">
        <div class="section-title">执行耗时</div>
        <div class="section-value">{{ result.duration }}ms</div>
      </div>

      <div class="result-section">
        <div class="section-title">输入数据</div>
        <pre class="json-display">{{ formatJSON(result.inputs) }}</pre>
      </div>

      <div class="result-section">
        <div class="section-title">输出数据</div>
        <pre class="json-display">{{ formatJSON(result.outputs) }}</pre>
      </div>

      <div v-if="result.error" class="result-section error-section">
        <div class="section-title">错误信息</div>
        <div class="error-message">{{ result.error }}</div>
      </div>

      <div class="result-section">
        <div class="section-title">执行时间</div>
        <div class="section-value">{{ formatTime(result.timestamp) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { InfoFilled } from '@element-plus/icons-vue'
import type { ExecutionResult } from '@/types/workflow'

interface Props {
  result?: ExecutionResult | null
}

defineProps<Props>()

const formatJSON = (obj: any) => {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return String(obj)
  }
}

const formatTime = (timestamp: Date) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}
</script>

<style scoped>
.execution-result {
  padding: 16px;
}

.no-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
  gap: 12px;
}

.no-result .el-icon {
  font-size: 48px;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-section {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.json-display {
  background: #fff;
  padding: 12px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #303133;
  overflow-x: auto;
  margin: 0;
}

.error-section {
  background: #fef0f0;
  border: 1px solid #fde2e2;
}

.error-message {
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'Monaco', 'Menlo', monospace;
}
</style>
