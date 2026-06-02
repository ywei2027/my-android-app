import { chromium } from 'playwright';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { mkdirSync } from 'fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const previewDir = __dirname;
const screenshotDir = join(__dirname, 'screenshots');

mkdirSync(screenshotDir, { recursive: true });

// All frames in version-tag-preview.html
const frames = [
  { id: 'frame-main-debug',        name: 'main_debug' },
  { id: 'frame-main-release',      name: 'main_release' },
  { id: 'frame-main-debug-dark',   name: 'main_debug_dark' },
  { id: 'frame-about-debug',       name: 'about_normal_debug' },
  { id: 'frame-about-release',     name: 'about_normal_release' },
  { id: 'frame-about-success',     name: 'about_copy_success' },
  { id: 'frame-about-failure',     name: 'about_copy_failure' },
  { id: 'frame-about-unknown',     name: 'about_unknown_version' },
  { id: 'frame-about-dark',        name: 'about_dark_mode' },
];

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: 375, height: 812 },
  deviceScaleFactor: 2,
});

const page = await context.newPage();
const htmlPath = join(previewDir, 'version-tag-preview.html');
const fileUrl = `file:///${htmlPath.replace(/\\/g, '/')}`;
await page.goto(fileUrl, { waitUntil: 'networkidle' });
await page.waitForTimeout(500);

for (const frame of frames) {
  const el = page.locator(`#${frame.id}`);
  await el.scrollIntoViewIfNeeded();
  await page.waitForTimeout(200);
  const screenshotPath = join(screenshotDir, `${frame.name}.png`);
  await el.screenshot({ path: screenshotPath });
  console.log(`Screenshot saved: ${frame.name}.png`);
}

await browser.close();
console.log('All screenshots complete.');
