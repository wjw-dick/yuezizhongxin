const app = getApp()

Page({
  data: {
    rooms: [],
    packages: [],
    active: null,
    level: '',
    levels: [],
    loading: true
  },

  onLoad() {
    this.load()
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const [rooms, packages] = await Promise.all([
        app.request({ url: '/api/open/site/rooms' }),
        app.request({ url: '/api/open/site/packages' })
      ])
      const levels = ['', ...Array.from(new Set((rooms || []).map((r) => r.level).filter(Boolean)))]
      this.setData({ rooms: rooms || [], packages: packages || [], levels, loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  pickLevel(e) {
    this.setData({ level: e.currentTarget.dataset.level })
  },

  openDetail(e) {
    const id = e.currentTarget.dataset.id
    const room = this.data.rooms.find((r) => r.id === id)
    this.setData({ active: room })
  },

  closeDetail() {
    this.setData({ active: null })
  },

  toBook(e) {
    const name = e.currentTarget.dataset.name || (this.data.active && this.data.active.name) || ''
    wx.navigateTo({ url: '/pages/book/book?room=' + encodeURIComponent(name) })
  },

  toCreate(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/create/create?packageId=' + id })
  },

  preview(e) {
    const src = e.currentTarget.dataset.src
    wx.previewImage({ current: src, urls: (this.data.active && this.data.active.images) || [src] })
  }
})
