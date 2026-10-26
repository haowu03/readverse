import { chromium } from 'playwright-core'
import fs from 'node:fs'

const CHROME = process.env.RV_CHROME || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const ADMIN = process.env.RV_ADMIN_URL || 'http://localhost:5173'
const PORTAL = process.env.RV_PORTAL_URL || 'http://localhost:5174'
const API = process.env.RV_API_URL || 'http://localhost:8080'
const OUT = 'C:\\Users\\16098\\Desktop\\ReadVerse\\docs\\screenshots'
fs.mkdirSync(OUT, { recursive: true })

const errors = []
const browser = await chromium.launch({ executablePath: CHROME, headless: true })
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'zh-CN' })
const page = await context.newPage()
page.on('console', (msg) => {
  if (msg.type() === 'error') errors.push(`console: ${msg.text()}`)
})
page.on('pageerror', (err) => errors.push(`pageerror: ${err.message}`))
page.on('requestfailed', (req) => {
  if (!req.url().includes('favicon')) errors.push(`requestfailed: ${req.url()} ${req.failure()?.errorText}`)
})
page.on('response', (res) => {
  if (res.status() >= 400) errors.push(`http ${res.status()}: ${res.url()}`)
})

async function shot(name) {
  await page.screenshot({ path: `${OUT}\\${name}.png`, fullPage: false })
  console.log('screenshot:', name)
}

try {
  // ---------- Admin ----------
  await page.goto(`${ADMIN}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1200)
  await shot('admin-login')
  const inputs = page.locator('.login-box input')
  await inputs.nth(0).fill('admin')
  await inputs.nth(1).fill('Admin@123')
  await page.locator('.login-btn').click()
  await page.waitForURL('**/dashboard', { timeout: 15000 })
  await page.waitForTimeout(1800)
  await page.getByText('数据看板', { exact: false }).first().waitFor({ timeout: 10000 })
  await shot('admin-dashboard')

  for (const path of ['/content/books', '/content/chapters', '/content/authors', '/content/categories',
    '/operation/sources', '/operation/tasks', '/points/goods', '/points/orders', '/system/users', '/system/roles']) {
    await page.goto(`${ADMIN}${path}`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(900)
    const body = await page.locator('body').innerText()
    console.log(path, body.includes('404') ? '404?' : 'ok', body.slice(0, 60).replace(/\n/g, ' '))
  }
  await page.goto(`${ADMIN}/content/books`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1200)
  await shot('admin-books')

  // ---------- Portal ----------
  await page.goto(`${PORTAL}/`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  await shot('portal-home')

  const bookResp = await page.request.get(`${API}/api/portal/books?size=1`)
  const bookJson = await bookResp.json()
  const book = bookJson.data.records[0]
  const chapterResp = await page.request.get(`${API}/api/portal/books/${book.id}/chapters`)
  const chapterJson = await chapterResp.json()
  const chapter = chapterJson.data[0]

  await page.goto(`${PORTAL}/book/${book.id}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1400)
  await shot('portal-book-detail')

  await page.goto(`${PORTAL}/read/${chapter.id}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)
  await shot('portal-reader')

  await page.goto(`${PORTAL}/login`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(600)
  await page.getByRole('button', { name: /reader \/ Reader@123/ }).click()
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await page.waitForTimeout(1200)
  await page.goto(`${PORTAL}/profile`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1500)
  await shot('portal-profile')

  console.log('FINAL_URL:', page.url())
} catch (e) {
  errors.push('fatal: ' + e.message)
  await shot('error-state')
} finally {
  console.log('--- ERRORS ---')
  console.log(errors.length ? errors.join('\n') : 'none')
  await browser.close()
}
