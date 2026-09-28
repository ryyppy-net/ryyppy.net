import { createRequire } from 'module';
const require = createRequire(new URL('../../e2e/package.json', import.meta.url));
const { chromium } = require('playwright');
import { spawn } from 'child_process';
const FPS=30, DUR=15, only=process.argv[2];
const browser = await chromium.launch({ executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE, args:['--allow-file-access-from-files'] });
const page = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
page.on('pageerror', e => console.error('PAGEERR', e.message));
await page.goto('file://' + process.cwd() + '/ad.html');
await page.evaluate(() => window.ready);
if (only) { for (const t of only.split(',')) { await page.evaluate(t => render(t), +t); await page.screenshot({ path: `preview/t${t}.png` }); } await browser.close(); process.exit(0); }
const ff = spawn(process.env.FF, ['-y','-loglevel','error','-f','image2pipe','-framerate',String(FPS),'-c:v','mjpeg','-i','-','-c:v','libx264','-pix_fmt','yuv420p','-crf','18','-preset','slow','video.mp4'], { stdio:['pipe','inherit','inherit'] });
for (let f=0; f<FPS*DUR; f++) {
  await page.evaluate(t => render(t), f/FPS);
  const buf = await page.screenshot({ type:'jpeg', quality: 92 });
  if (!ff.stdin.write(buf)) await new Promise(r=>ff.stdin.once('drain',r));
}
ff.stdin.end(); await new Promise(r=>ff.on('close',r)); await browser.close();
