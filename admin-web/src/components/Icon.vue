<template>
  <svg
    :width="sizeValue"
    :height="sizeValue"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="stroke"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path v-for="(d, i) in paths" :key="i" :d="d" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ name: string; size?: number | string; stroke?: number }>(), {
  size: 18,
  stroke: 1.7
})

const sizeValue = computed(() => props.size)

const icons: Record<string, string[]> = {
  dashboard: ['M4 4h7v7H4z', 'M13 4h7v4h-7z', 'M13 11h7v9h-7z', 'M4 14h7v6H4z'],
  building: ['M3 21h18', 'M6 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16', 'M9 7h2', 'M9 11h2', 'M9 15h2', 'M17 21v-7'],
  package: ['M21 8.5 12 3.5 3 8.5l9 5 9-5z', 'M3 8.5v7l9 5 9-5v-7', 'M12 13.5v7'],
  receipt: ['M6 3h12v18l-3-2-3 2-3-2-3 2V3z', 'M9 8h6', 'M9 12h6'],
  shield: ['M12 3.5 19 6v6c0 4.2-2.9 7.3-7 8.8-4.1-1.5-7-4.6-7-8.8V6l7-2.5z', 'M9.5 12l1.8 1.8 3.4-3.6'],
  settings: ['M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z', 'M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z'],
  book: ['M4 4.5A1.5 1.5 0 0 1 5.5 3H19v18H5.5A1.5 1.5 0 0 1 4 19.5v-15z', 'M8 7h7', 'M8 11h7'],
  globe: ['M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z', 'M3.6 9h16.8', 'M3.6 15h16.8', 'M12 3c2.5 2.4 3.8 5.5 3.8 9s-1.3 6.6-3.8 9c-2.5-2.4-3.8-5.5-3.8-9s1.3-6.6 3.8-9z'],
  grid: ['M4 4h7v7H4z', 'M13 4h7v7h-7z', 'M4 13h7v7H4z', 'M13 13h7v7h-7z'],
  cloud: ['M7.5 18.5a4.5 4.5 0 0 1-.6-8.96A5.5 5.5 0 0 1 17.5 9.5a4 4 0 0 1 .5 9h-10.5z'],
  bell: ['M6.5 9a5.5 5.5 0 0 1 11 0c0 4.5 1.8 5.5 1.8 5.5H4.7S6.5 13.5 6.5 9z', 'M10 18.5a2 2 0 0 0 4 0'],
  key: ['M15.5 4a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9z', 'M12.3 12.3 4 20.5', 'M7 17l2 2', 'M5 19l1.5 1.5'],
  users: ['M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z', 'M2.5 20a6.5 6.5 0 0 1 13 0', 'M16 11.2a3 3 0 1 0 0-5.9', 'M17.5 20a5.6 5.6 0 0 0-2-4.3'],
  logout: ['M9.5 21H5.5A1.5 1.5 0 0 1 4 19.5v-15A1.5 1.5 0 0 1 5.5 3h4', 'M16 16.5l4.5-4.5L16 7.5', 'M20.5 12H9'],
  plus: ['M12 5v14', 'M5 12h14'],
  search: ['M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z', 'M16.2 16.2 21 21'],
  refresh: ['M20.5 12a8.5 8.5 0 1 1-2.7-6.2', 'M20.5 4v5h-5'],
  chevron: ['M7 9.5l5 5 5-5'],
  arrow: ['M4.5 12h15', 'M13.5 6l6 6-6 6'],
  check: ['M20 6.5 9.5 17 4 11.5'],
  alert: ['M12 9v4.5', 'M12 17.2v.1', 'M10.4 4 2.6 17.5A1.8 1.8 0 0 0 4.2 20.3h15.6a1.8 1.8 0 0 0 1.6-2.8L13.6 4a1.8 1.8 0 0 0-3.2 0z'],
  clock: ['M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z', 'M12 7.2V12l3.2 2'],
  chart: ['M3.5 3.5v17h17', 'M7.5 15.5l3.2-4.2 3 2.8 4.3-6'],
  filter: ['M4 5h16', 'M7 12h10', 'M10 19h4'],
  edit: ['M4 20h4L20 8l-4-4L4 16v4z', 'M14.5 5.5 18.5 9.5'],
  trash: ['M4.5 7h15', 'M9 7V4.5h6V7', 'M6.5 7l.8 13h9.4l.8-13'],
  building2: ['M4 21V6l7-3 7 3v15', 'M9 9h.01', 'M9 13h.01', 'M9 17h.01', 'M15 9h.01', 'M15 13h.01', 'M15 17h.01'],
  sparkle: ['M12 3.5l1.9 4.6 4.6 1.9-4.6 1.9L12 16.5l-1.9-4.6L5.5 10l4.6-1.9L12 3.5z'],
  layers: ['M12 3.5 3 8l9 4.5L21 8l-9-4.5z', 'M3 13l9 4.5 9-4.5'],
  close: ['M6 6l12 12', 'M18 6 6 18'],
  dot: ['M12 12h.01'],
  upload: ['M12 16V4', 'M7 9l5-5 5 5', 'M4.5 16.5v3A1.5 1.5 0 0 0 6 21h12a1.5 1.5 0 0 0 1.5-1.5v-3'],
  inbox: ['M4 13.5 6 4h12l2 9.5', 'M4 13.5V19a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-5.5', 'M4 13.5h4l1.2 2h5.6l1.2-2h4'],
  wechat: ['M9.5 4C5.9 4 3 6.4 3 9.4c0 1.7.9 3.2 2.4 4.2L5 16l2.6-1.3c.6.2 1.2.3 1.9.3', 'M21 13.4c0-2.6-2.5-4.7-5.5-4.7s-5.5 2.1-5.5 4.7 2.5 4.7 5.5 4.7c.7 0 1.3-.1 1.9-.3L21 20l-.7-2.4c1-.8 1.7-2 1.7-3.4z'],
  calendar: ['M4 6.5A1.5 1.5 0 0 1 5.5 5h13A1.5 1.5 0 0 1 20 6.5v13A1.5 1.5 0 0 1 18.5 21h-13A1.5 1.5 0 0 1 4 19.5v-13z', 'M4 10h16', 'M8 3v4', 'M16 3v4'],
  meal: ['M6 3v8a2 2 0 0 0 2 2v8', 'M10 3v8', 'M8 3v18', 'M17 3c-1.7 1-2.5 2.6-2.5 4.5S15.3 11 17 12v11']
}

const paths = computed(() => icons[props.name] || icons.dot)
</script>
