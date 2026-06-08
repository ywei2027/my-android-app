const { chromium } = require('playwright');
const path = require('path');

const HTML_FILE = path.resolve(__dirname, '../docs/ui-preview/login.html');
const SCREENSHOT_DIR = path.resolve(__dirname, '../docs/ui-preview/screenshots');
const BASE_URL = `file://${HTML_FILE.replace(/\\/g, '/')}`;

const scenarios = [
  { name: 'login_idle', desc: 'Idle — 默认空状态' },
  { name: 'login_editing', desc: 'Editing — 已填邮箱密码，按钮启用' },
  { name: 'login_email_error', desc: 'Editing — 邮箱格式错误' },
  { name: 'login_loading', desc: 'Loading — 登录中' },
  { name: 'login_error_401', desc: 'Error — 401 凭据错误' },
  { name: 'login_error_403', desc: 'Error — 403 账户锁定' },
  { name: 'login_error_429', desc: 'Error — 429 限流' },
  { name: 'login_timeout', desc: 'Timeout — 网络超时' },
  { name: 'login_dark', desc: 'Dark Mode — 深色模式' },
];

(async () => {
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 375, height: 812 },
    deviceScaleFactor: 2,
  });
  const page = await context.newPage();

  for (const scenario of scenarios) {
    console.log(`Capturing: ${scenario.name} — ${scenario.desc}`);

    // Navigate to page
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });

    // Wait for the page to be ready
    await page.waitForSelector('#phoneFrame', { state: 'visible' });

    // Apply scenario via select dropdown
    await page.selectOption('#scenarioSelect', getScenarioValue(scenario.name));

    // Small delay for any animations
    await page.waitForTimeout(300);

    // Screenshot
    await page.screenshot({
      path: path.join(SCREENSHOT_DIR, `${scenario.name}.png`),
      fullPage: false,
    });
    console.log(`  ✓ saved: ${scenario.name}.png`);
  }

  // Also capture the full page with the control panel visible (wider)
  const wideContext = await browser.newContext({
    viewport: { width: 800, height: 900 },
    deviceScaleFactor: 2,
  });
  const widePage = await wideContext.newPage();
  await widePage.goto(BASE_URL, { waitUntil: 'networkidle' });
  await widePage.waitForSelector('#phoneFrame', { state: 'visible' });
  await widePage.selectOption('#scenarioSelect', 'editing');
  await widePage.waitForTimeout(300);
  await widePage.screenshot({
    path: path.join(SCREENSHOT_DIR, 'login_full_page.png'),
    fullPage: false,
  });
  console.log('  ✓ saved: login_full_page.png (wide with control panel)');

  await browser.close();
  console.log('\nAll screenshots captured successfully!');
})();

function getScenarioValue(name) {
  const map = {
    'login_idle': 'idle',
    'login_editing': 'editing',
    'login_email_error': 'emailError',
    'login_loading': 'loading',
    'login_error_401': 'error401',
    'login_error_403': 'error403',
    'login_error_429': 'error429',
    'login_timeout': 'timeout',
    'login_dark': 'dark',
  };
  return map[name] || 'idle';
}
