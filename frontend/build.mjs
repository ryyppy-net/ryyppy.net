// Bundles every script the AngularJS page (templates/app/index.html) loads
// into one minified file. They are plain global scripts, not modules, so
// this concatenates them in page order and minifies the result; jQuery must
// stay after Angular so Angular keeps using its built-in jqLite.
import { transformSync } from 'esbuild';
import { execFileSync } from 'child_process';
import fs from 'fs';
import os from 'os';
import path from 'path';

const res = path.resolve('../src/main/resources/public');
const m2 = path.join(os.homedir(), '.m2/repository/org/webjars');
const webjar = (jar, file) => execFileSync('unzip', ['-p', path.join(m2, jar), 'META-INF/resources/webjars/' + file], { encoding: 'utf8', maxBuffer: 1 << 26 });
const local = file => fs.readFileSync(path.join(res, file), 'utf8');

const inputs = [
  ['angular.min.js', () => local('static/vendor/angular/angular.min.js')],
  ['app.js', () => local('app/js/app.js')],
  ['services.js', () => local('app/js/services.js')],
  ['howler.min.js', () => webjar('npm/howler/2.2.4/howler-2.2.4.jar', 'howler/2.2.4/dist/howler.min.js')],
  ['moment.min.js', () => local('static/vendor/moment/moment.min.js')],
  ...['UserCtrl', 'ProfileSettingsCtrl', 'DrinkerCtrl', 'PartyCtrl', 'GeneralPartyAdminCtrl', 'PartyAdminCtrl']
    .map(c => [c + '.js', () => local(`app/js/controllers/${c}.js`)]),
  ['userhistorygraph.js', () => local('app/js/userhistorygraph.js')],
  ['filters.js', () => local('app/js/filters.js')],
  ['directives.js', () => local('app/js/directives.js')],
  ['jquery.min.js', () => webjar('jquery/1.8.3/jquery-1.8.3.jar', 'jquery/1.8.3/jquery.min.js')],
  ['jquery.pnotify.js', () => webjar('pnotify/1.2.0/pnotify-1.2.0.jar', 'pnotify/1.2.0/jquery.pnotify.js')],
  ['jquery.flot.min.js', () => webjar('flot/0.7/flot-0.7.jar', 'flot/0.7/jquery.flot.min.js')],
  ['jquery.flot.crosshair.min.js', () => webjar('flot/0.7/flot-0.7.jar', 'flot/0.7/jquery.flot.crosshair.min.js')],
  ['jquery.flot.resize.min.js', () => webjar('flot/0.7/flot-0.7.jar', 'flot/0.7/jquery.flot.resize.min.js')],
  // Loaded with defer on the other pages, i.e. after everything above.
  ['sound.js', () => local('static/js/sound.js')],
];

// A file-level "use strict" only applies at the start of a script, so the
// app's own files run in sloppy mode once concatenated.
const parts = inputs.map(([name, read]) => {
  const { code } = transformSync(read(), { minify: true, loader: 'js', target: 'es2015', legalComments: 'inline', sourcefile: name });
  return `/* ${name} */\n${code.trim().replace(/;?$/, ';')}`;
});
const outDir = path.join(res, 'app/js/dist');
fs.mkdirSync(outDir, { recursive: true });
const out = path.join(outDir, 'app.bundle.js');
fs.writeFileSync(out, parts.join('\n') + '\n');
console.log(`${out}: ${fs.statSync(out).size} bytes from ${inputs.length} files`);
