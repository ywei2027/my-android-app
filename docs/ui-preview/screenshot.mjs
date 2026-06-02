import { chromium } from 'playwright';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';
import { mkdirSync } from 'fs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const previewDir = __dirname;
const screenshotDir = join(__dirname, 'screenshots');

mkdirSync(screenshotDir, { recursive: true });

const pages = [
  'p1_loading',
  'p1_empty',
  'p1_list',
  'p1_search',
  'p1_search_empty',
  'p1_error',
  'p1_delete_dialog',
  'p2_edit_new',
  'p2_edit_existing',
  'p2_discard_dialog',
  'p2_save_failed',
];

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: 375, height: 812 },
  deviceScaleFactor: 2,
});

for (const name of pages) {
  const page = await context.newPage();
  const htmlPath = join(previewDir, `${name}.html`);
  const fileUrl = `file:///${htmlPath.replace(/\\/g, '/')}`;
  await page.goto(fileUrl, { waitUntil: 'networkidle' });
  // Wait for any animations to settle
  await page.waitForTimeout(500);
  const screenshotPath = join(screenshotDir, `${name}.png`);
  await page.screenshot({ path: screenshotPath, fullPage: false });
  console.log(`Screenshot saved: ${name}.png`);
  await page.close();
}

await browser.close();
console.log('All screenshots complete.');
