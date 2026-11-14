// 用 Chrome DevTools Protocol 截图：登录后写入 localStorage，再截目标路由
// 用法：node scripts/shot.mjs <输出目录>
import { writeFileSync, mkdirSync } from 'node:fs'
import { join } from 'node:path'

const OUT = process.argv[2] || 'shots'
const BASE = 'http://localhost:5173'
const CDP = 'http://127.0.0.1:9333'

mkdirSync(OUT, { recursive: true })

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function newTab() {
  const res = await fetch(`${CDP}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })
  return res.json()
}

class Cdp {
  constructor(ws) {
    this.ws = ws
    this.id = 0
    this.pending = new Map()
    ws.addEventListener('message', (ev) => {
      const msg = JSON.parse(ev.data)
      if (msg.id && this.pending.has(msg.id)) {
        const { resolve, reject } = this.pending.get(msg.id)
        this.pending.delete(msg.id)
        msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result)
      }
    })
  }
  send(method, params = {}) {
    const id = ++this.id
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject })
      this.ws.send(JSON.stringify({ id, method, params }))
    })
  }
}

async function connect(url) {
  const ws = new WebSocket(url)
  await new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true })
    ws.addEventListener('error', reject, { once: true })
  })
  return new Cdp(ws)
}

const tab = await newTab()
const cdp = await connect(tab.webSocketDebuggerUrl)

await cdp.send('Page.enable')
await cdp.send('Runtime.enable')
await cdp.send('Emulation.setDeviceMetricsOverride', {
  width: 1512,
  height: 950,
  deviceScaleFactor: 1,
  mobile: false
})

async function goto(url, wait = 1800) {
  await cdp.send('Page.navigate', { url })
  await sleep(wait)
}

async function evaluate(expression) {
  const r = await cdp.send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true
  })
  if (r.exceptionDetails) throw new Error(JSON.stringify(r.exceptionDetails))
  return r.result.value
}

async function shot(name) {
  const { data } = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true })
  const file = join(OUT, `${name}.png`)
  writeFileSync(file, Buffer.from(data, 'base64'))
  console.log('saved', file)
}

// 1. 登录页
await goto(`${BASE}/#/login`, 2000)
await shot('01-login')

// 2. 平台登录并截图
await evaluate(`(async () => {
  const res = await fetch('/api/platform/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'platform', password: 'Admin@123456' })
  }).then(r => r.json());
  localStorage.setItem('yzz.platform.token', res.data.token);
  localStorage.setItem('yzz.platform.user', JSON.stringify(res.data));
  return res.code;
})()`)

for (const [route, name] of [
  ['/platform/dashboard', '02-platform-dashboard'],
  ['/platform/tenants', '03-platform-tenants'],
  ['/platform/packages', '04-platform-packages'],
  ['/platform/bills', '05-platform-bills'],
  ['/platform/audits', '06-platform-audits']
]) {
  await goto(`${BASE}/#${route}`, 2000)
  await shot(name)
}

// 3. 租户登录并截图
await evaluate(`(async () => {
  const res = await fetch('/api/tenant/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Tenant-Code': 'demo' },
    body: JSON.stringify({ username: 'admin', password: 'Admin@123456' })
  }).then(r => r.json());
  localStorage.setItem('yzz.token', res.data.token);
  localStorage.setItem('yzz.tenantCode', 'demo');
  localStorage.setItem('yzz.user', JSON.stringify(res.data));
  return res.code;
})()`)

for (const [route, name] of [
  ['/dashboard', '07-tenant-dashboard'],
  ['/system/config', '08-tenant-config'],
  ['/system/dict', '09-tenant-dict'],
  ['/system/storage', '10-tenant-storage']
]) {
  await goto(`${BASE}/#${route}`, 2200)
  await shot(name)
}

process.exit(0)
