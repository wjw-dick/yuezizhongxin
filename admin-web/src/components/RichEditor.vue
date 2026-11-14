<template>
  <div class="rich-editor">
    <Toolbar class="toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      v-model="html"
      class="editor"
      :style="{ height: `${minHeight}px` }"
      :default-config="editorConfig"
      mode="default"
      @on-created="onCreated"
      @on-change="onChange"
    />
    <div class="footer">
      <span>支持粘贴 Word / 网页内容自动清洗；图片粘贴即上传至对象存储</span>
      <span>{{ plainLength }} 字</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { ElMessage } from 'element-plus'
import { uploadFile } from '../api/resource'

const props = withDefaults(defineProps<{ modelValue?: string; minHeight?: number; dir?: string }>(), {
  modelValue: '',
  minHeight: 280,
  dir: 'richtext'
})
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const editorRef = shallowRef<IDomEditor>()
const html = ref('')
const plainLength = computed(() => html.value.replace(/<[^>]+>/g, '').length)

watch(
  () => props.modelValue,
  (value) => {
    if (value !== html.value) {
      html.value = value || ''
    }
  },
  { immediate: true }
)

const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['group-video', 'insertVideo', 'uploadVideo', 'fullScreen']
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入内容，支持图片、表格、引用与多级标题',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        try {
          const stored = await uploadFile(file, props.dir)
          insertFn(stored.url, file.name, stored.url)
        } catch (error: any) {
          ElMessage.error(error.message || '图片上传失败')
        }
      }
    },
    // 粘贴网页内容时只保留基础排版，避免带入外部样式与脚本
    // wangEditor 类型定义声明为对象，实际支持布尔开关
    pasteFilterStyle: true as any,
    pasteIgnoreImg: false as any,
    pasteTextHandle: (content: string) => content
  }
}

function onCreated(editor: IDomEditor) {
  editorRef.value = editor
}

function onChange(editor: IDomEditor) {
  const value = editor.isEmpty() ? '' : editor.getHtml()
  emit('update:modelValue', value)
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<style scoped>
.rich-editor {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--surface);
}
.toolbar {
  border-bottom: 1px solid var(--line);
  background: var(--surface-sunken);
}
.editor {
  overflow-y: auto;
}
.footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  border-top: 1px solid var(--line);
  background: var(--surface-sunken);
  color: var(--ink-400);
  font-size: 11px;
}
:deep(.w-e-text-container) {
  background: var(--surface);
}
</style>
