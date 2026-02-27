#!/usr/bin/env node
/*
 * Renders an HTML report by embedding the JSON output produced by
 * NextWordTapChainReportUiAutomatorTest into docs/nextword-test-suite/index.html.
 *
 * Usage:
 *   node scripts/render_nextword_tapchain_report.js <template.html> <report.json> <out.html>
 */

import fs from 'node:fs';

function usage() {
  // eslint-disable-next-line no-console
  console.error('Usage: node scripts/render_nextword_tapchain_report.js <template.html> <report.json> <out.html>');
  process.exit(2);
}

const [, , templatePath, reportJsonPath, outPath] = process.argv;
if (!templatePath || !reportJsonPath || !outPath) usage();

const template = fs.readFileSync(templatePath, 'utf8');
if (!template.includes('__AUTO_RESULTS_JSON__')) {
  throw new Error('Template missing __AUTO_RESULTS_JSON__ placeholder.');
}

const report = JSON.parse(fs.readFileSync(reportJsonPath, 'utf8'));
let json = JSON.stringify(report, null, 2);

// Prevent accidental </script> injection when embedding JSON inside a <script> tag.
json = json.replaceAll('<', '\\u003c');

const out = template.replace('__AUTO_RESULTS_JSON__', json);
fs.writeFileSync(outPath, out, 'utf8');

// eslint-disable-next-line no-console
console.log(outPath);
