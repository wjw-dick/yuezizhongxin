<template>
  <div class="uploader">
    <div
      class="dropzone"
      :class="{ dragging }"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
      @click="pick"
    >
      <Icon name="upload" :size="20" />
      <div class="dz-text">
        <strong>拖拽图片到此处，或点击选择</strong>
        <span>{{ multiple ? `最多 ${limit} 张，` : '' }}支持 jpg / png / webp，单张不超过 20MB</span>
      </div>
      <input ref="inputRef" type="file" accept="image/*" :multiple="multiple" hidden @change="onPick" />
    </div>

    <div v-if="items.length" class="grid">
      <div
        v-for="(item, index) in items"
        :key="item.uid"
        class="thumb"
        draggable="true"
        @dragstart="dragIndex = index"
        @dragover.prevent
        @drop.prevent="reorder(index)"
      >
        <el-image
          v-if="!item.error"
          :src="item.url"
          fit="cover"
          :preview-src-list="previewList"
          :initial-index="index"
          preview-teleported
          class="thumb-img"
        />
        <div v-if="item.progress < 100 && !item.error" class="progress">
          <div class="bar" :style="{ width: item.progress + '%' }" />
          <span>{{ item.progress }}%</span>
        </div>
        <div v-if="item.error" class="failed" @click.stop="retry(item)">
          <Icon name="refresh" :size="16" />
          <span>上传失败，点击重试</span>
        </div>
        <button class="remove" title="删除" @click.stop="remove(index)">
          <Icon name="close" :size="12" />
        </button>
        <span v-if="multiple" class="order">{{ index + 1 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import Icon from './Icon.vue'
import { uploadFile } from '../api/resource'

interface Item {
  uid: string
  key?: string
  url: string
  progress: number
  error?: boolean
  file?: File
}

const props = withDefaults(
  defineProps<{
    modelValue?: string | string[] | null
    multiple?: boolean
    limit?: number
    dir?: string
  }>(),
  { modelValue: '', multiple: false, limit: 9, dir: 'common' }
)

const emit = defineEmits<{ (e: 'update:modelValue', value: string | string[]): void }>()

const inputRef = ref<HTMLInputElement>()
const dragging = ref(false)
const dragIndex = ref<number | null>(null)
const items = ref<Item[]>([])

const previewList = computed(() => items.value.filter((i) => !i.error).map((i) => i.url))

function toItems(value: string | string[] | null | undefined): Item[] {
  if (!value) {
    return []
  }
  const keys = Array.isArray(value) ? value : String(value).split(',').filter(Boolean)
  return keys.map((key) => ({
    uid: `${key}-${Math.random().toString(36).slice(2, 8)}`,
    key,
    url: key.startsWith('http') || key.startsWith('/') ? key : `/files/${key}`,
    progress: 100
  }))
}

watch(
  () => props.modelValue,
  (value) => {
    const incoming = toItems(value).map((i) => i.key).join(',')
    const current = items.value.filter((i) => i.key).map((i) => i.key).join(',')
    if (incoming !== current) {
      items.value = toItems(value)
    }
  },
  { immediate: true }
)

function sync() {
  const keys = items.value.filter((i) => i.key && !i.error).map((i) => i.key as string)
  emit('update:modelValue', props.multiple ? keys : keys[0] || '')
}

function pick() {
  inputRef.value?.click()
}

function onPick(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files || [])
  handleFiles(files)
  ;(event.target as HTMLInputElement).value = ''
}

function onDrop(event: DragEvent) {
  dragging.value = false
  const files = Array.from(event.dataTransfer?.files || []).filter((f) => f.type.startsWith('image/'))
  handleFiles(files)
}

function handleFiles(files: File[]) {
  const room = props.multiple ? props.limit - items.value.length : 1
  if (!props.multiple && items.value.length) {
    items.value = []
  }
  if (room <= 0) {
    ElMessage.warning(`最多上传 ${props.limit} 张图片`)
    return
  }
  files.slice(0, room).forEach(upload)
}

function upload(file: File) {
  const item: Item = {
    uid: `tmp-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    url: URL.createObjectURL(file),
    progress: 0,
    file
  }
  items.value.push(item)
  uploadFile(file, props.dir, (percent) => {
    item.progress = percent
  })
    .then((stored) => {
      item.key = stored.objectKey
      item.url = stored.url
      item.progress = 100
      item.error = false
      sync()
    })
    .catch((error: Error) => {
      item.error = true
      item.progress = 100
      ElMessage.error(error.message)
    })
}

function retry(item: Item) {
  if (!item.file) {
    items.value = items.value.filter((i) => i.uid !== item.uid)
    return
  }
  item.error = false
  item.progress = 0
  uploadFile(item.file, props.dir, (percent) => {
    item.progress = percent
  })
    .then((stored) => {
      item.key = stored.objectKey
      item.url = stored.url
      item.error = false
      sync()
    })
    .catch(() => {
      item.error = true
    })
}

function remove(index: number) {
  items.value.splice(index, 1)
  sync()
}

function reorder(target: number) {
  if (dragIndex.value === null || dragIndex.value === target) {
    return
  }
  const list = [...items.value]
  const [moved] = list.splice(dragIndex.value, 1)
  list.splice(target, 0, moved)
  items.value = list
  dragIndex.value = null
  sync()
}
</script>

<style scoped>
.uploader {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.dropzone {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-md);
  background: var(--surface-sunken);
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.dropzone:hover,
.dropzone.dragging {
  border-color: var(--brand-500);
  background: var(--brand-50);
}
.dz-text {
  display: flex;
  flex-direction: column;
  line-height: 1.5;
}
.dz-text strong {
  font-size: 13px;
  font-weight: 550;
  color: var(--ink-900);
}
.dz-text span {
  font-size: 12px;
  color: var(--ink-400);
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 10px;
}
.thumb {
  position: relative;
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--line);
  background: var(--surface-sunken);
  cursor: grab;
}
.thumb-img {
  width: 100%;
  height: 100%;
  display: block;
}
.progress {
  position: absolute;
  inset: 0;
  background: rgba(20, 33, 28, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
}
.progress .bar {
  position: absolute;
  left: 0;
  bottom: 0;
  height: 3px;
  background: #6cc3a5;
}
.failed {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: #fbeeed;
  color: #a83c37;
  font-size: 11px;
  cursor: pointer;
}
.remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 50%;
  background: rgba(20, 33, 28, 0.6);
  color: #fff;
  display: grid;
  place-items: center;
  cursor: pointer;
}
.remove:hover {
  background: rgba(192, 74, 68, 0.9);
}
.order {
  position: absolute;
  left: 4px;
  bottom: 4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: rgba(20, 33, 28, 0.55);
  color: #fff;
  font-size: 11px;
  display: grid;
  place-items: center;
}
</style>
