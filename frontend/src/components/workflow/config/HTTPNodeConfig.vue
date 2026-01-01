<template>
  <div class="http-node-config">
    <el-form-item label="请求方法">
      <el-select
        v-model="localConfig.method"
        style="width: 100%"
        @change="emitUpdate"
      >
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
    </el-form-item>

    <el-form-item label="URL">
      <el-input
        v-model="localConfig.url"
        placeholder="https://api.example.com/endpoint"
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="请求头 (JSON)">
      <el-input
        v-model="localConfig.headers"
        type="textarea"
        :rows="3"
        placeholder='{"Content-Type": "application/json"}'
        @change="emitUpdate"
      />
    </el-form-item>

    <el-form-item label="请求体" v-if="localConfig.method !== 'GET'">
      <el-input
        v-model="localConfig.body"
        type="textarea"
        :rows="4"
        placeholder="请求体内容，支持变量 {{variable}}"
        @change="emitUpdate"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { HTTPNodeConfig } from '@/types/workflow'

interface Props {
  config: HTTPNodeConfig
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [config: HTTPNodeConfig]
}>()

const localConfig = ref<HTTPNodeConfig>({ ...props.config })

watch(() => props.config, (newConfig) => {
  localConfig.value = { ...newConfig }
}, { deep: true })

const emitUpdate = () => {
  emit('update', localConfig.value)
}
</script>
