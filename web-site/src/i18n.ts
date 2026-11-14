import { ref } from 'vue'

export const LANGS = [
  { code: 'zh-CN', label: '中文', short: '中' },
  { code: 'en-US', label: 'English', short: 'EN' }
]

const stored = localStorage.getItem('yzz.lang') || 'zh-CN'
export const lang = ref(stored)

export function setLang(code: string) {
  lang.value = code
  localStorage.setItem('yzz.lang', code)
  document.documentElement.lang = code
}

/** 简单字典：官网固定文案，内容型文案走 CMS 多语言字段 */
const dict: Record<string, Record<string, string>> = {
  'zh-CN': {
    nav_home: '首页',
    nav_rooms: '房型与套餐',
    nav_rehab: '产后康复',
    nav_meals: '月子餐',
    nav_team: '护理团队',
    nav_cases: '案例口碑',
    nav_stores: '门店分布',
    nav_news: '资讯',
    book: '预约参观',
    consult: '咨询热线',
    footer_note: '本页面内容由月子中心运营后台发布，最终解释权归门店所有。'
  },
  'en-US': {
    nav_home: 'Home',
    nav_rooms: 'Rooms & Packages',
    nav_rehab: 'Postpartum Rehab',
    nav_meals: 'Meal Plans',
    nav_team: 'Care Team',
    nav_cases: 'Stories',
    nav_stores: 'Locations',
    nav_news: 'Journal',
    book: 'Book a Visit',
    consult: 'Hotline',
    footer_note: 'Content is published from the operation console.'
  }
}

export function t(key: string) {
  return dict[lang.value]?.[key] || dict['zh-CN'][key] || key
}
