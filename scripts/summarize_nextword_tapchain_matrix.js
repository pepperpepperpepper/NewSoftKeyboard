#!/usr/bin/env node
/**
 * Summarizes one or more NextWord tap-chain report.json files (Genymotion/Device Farm)
 * into a small per-device perf/source-mix table.
 *
 * Usage:
 *   node scripts/summarize_nextword_tapchain_matrix.js <report.json|device_dir|matrix_dir>...
 *
 * Where:
 * - report.json: a single report
 * - device_dir: contains report.json (and optionally logcat.txt or FILE/Logcat_*)
 * - matrix_dir: contains multiple device dirs, each with report.json
 */

import fs from 'node:fs';
import path from 'node:path';

function usage() {
  // eslint-disable-next-line no-console
  console.error('Usage: node scripts/summarize_nextword_tapchain_matrix.js <report.json|device_dir|matrix_dir>...');
  process.exit(2);
}

function isDirectory(p) {
  try {
    return fs.statSync(p).isDirectory();
  } catch {
    return false;
  }
}

function isFile(p) {
  try {
    return fs.statSync(p).isFile();
  } catch {
    return false;
  }
}

function findLogcatPath(deviceDir) {
  const direct = path.join(deviceDir, 'logcat.txt');
  if (isFile(direct)) return direct;

  const deviceFarmFileDir = path.join(deviceDir, 'FILE');
  if (isDirectory(deviceFarmFileDir)) {
    const entries = fs.readdirSync(deviceFarmFileDir);
    const logcat = entries.find((name) => name.startsWith('Logcat_'));
    if (logcat) return path.join(deviceFarmFileDir, logcat);
  }

  return null;
}

function expandTargets(argvPaths) {
  /** @type {{ deviceDir: string, reportPath: string }[]} */
  const out = [];

  for (const input of argvPaths) {
    const fullPath = path.resolve(input);
    if (isFile(fullPath) && fullPath.endsWith('.json')) {
      out.push({ deviceDir: path.dirname(fullPath), reportPath: fullPath });
      continue;
    }

    if (!isDirectory(fullPath)) continue;

    const reportInDir = path.join(fullPath, 'report.json');
    if (isFile(reportInDir)) {
      out.push({ deviceDir: fullPath, reportPath: reportInDir });
      continue;
    }

    for (const entry of fs.readdirSync(fullPath, { withFileTypes: true })) {
      if (!entry.isDirectory()) continue;
      const deviceDir = path.join(fullPath, entry.name);
      const reportPath = path.join(deviceDir, 'report.json');
      if (isFile(reportPath)) out.push({ deviceDir, reportPath });
    }
  }

  const unique = new Map();
  for (const item of out) unique.set(item.reportPath, item);
  return Array.from(unique.values()).sort((a, b) => a.deviceDir.localeCompare(b.deviceDir));
}

function percentile(sorted, p) {
  if (sorted.length === 0) return null;
  const clamped = Math.max(0, Math.min(1, p));
  const idx = Math.floor(clamped * (sorted.length - 1));
  return sorted[idx];
}

function stats(values) {
  const numeric = values.filter((v) => Number.isFinite(v));
  if (numeric.length === 0) return null;
  const sorted = [...numeric].sort((a, b) => a - b);
  return {
    n: sorted.length,
    p50: percentile(sorted, 0.5),
    p95: percentile(sorted, 0.95),
    min: sorted[0],
    max: sorted[sorted.length - 1],
  };
}

function formatMsStat(stat) {
  if (!stat) return '-';
  return `${Math.round(stat.p50)} / ${Math.round(stat.p95)}`;
}

function formatPct(numer, denom) {
  if (!denom) return '-';
  return `${Math.round((100 * numer) / denom)}%`;
}

function collectRuns(report) {
  /** @type {{ caseId: string, modeKey: string, run: any }[]} */
  const runs = [];
  const results = report?.results ?? {};
  for (const [caseId, caseVal] of Object.entries(results)) {
    for (const [modeKey, modeVal] of Object.entries(caseVal ?? {})) {
      const list = modeVal?.runs ?? [];
      for (const run of list) runs.push({ caseId, modeKey, run });
    }
  }
  return runs;
}

function extractMatrixStats(deviceDir, report) {
  const runs = collectRuns(report);

  /** @type {any[]} */
  const inferences = [];
  for (const { run } of runs) {
    for (const inf of run?.neuralInferences ?? []) inferences.push(inf);
  }

  const onnx = stats(inferences.map((i) => i?.onnxLatencyMs));
  const pipeline = stats(inferences.map((i) => i?.pipelineLatencyMs));
  const kvHits = inferences.filter((i) => i?.usedKvCache === true).length;

  const hybridRuns = runs.filter((r) => r.modeKey === 'hybrid');
  const topN = 3;
  const hybridInitialSources = [];
  const hybridSettledSources = [];
  const hybridRequestToListenerMs = [];

  for (const { run } of hybridRuns) {
    const initial = run?.nextWordPipeline?.initial;
    const settled = run?.nextWordPipeline?.settled;
    const initialSources = initial?.finalSuggestionSources;
    const settledSources = settled?.finalSuggestionSources;
    if (Array.isArray(initialSources)) {
      hybridInitialSources.push(...initialSources.slice(0, topN));
    }
    if (Array.isArray(settledSources)) {
      hybridSettledSources.push(...settledSources.slice(0, topN));
    }

    const t = run?.hybridNeuralAsync?.settled ?? run?.hybridNeuralAsync?.initial;
    if (!t?.lastRequestScheduled) continue;
    if (t?.lastRequestCacheHit !== false) continue;
    if (!Number.isFinite(t?.lastRequestUptimeMs)) continue;
    if (!Number.isFinite(t?.lastListenerUptimeMs)) continue;
    if (t?.lastRequestGeneration == null || t?.lastListenerGeneration == null) continue;
    if (t.lastRequestGeneration !== t.lastListenerGeneration) continue;
    const delta = t.lastListenerUptimeMs - t.lastRequestUptimeMs;
    if (delta >= 0) hybridRequestToListenerMs.push(delta);
  }

  const hybridDelay = stats(hybridRequestToListenerMs);
  const hybridInitialNeural = hybridInitialSources.filter((s) => s === 'neural');
  const hybridSettledNeural = hybridSettledSources.filter((s) => s === 'neural');

  const logcatPath = findLogcatPath(deviceDir);
  let skippedFramesCount = 0;
  let skippedFramesMax = 0;
  if (logcatPath) {
    const text = fs.readFileSync(logcatPath, 'utf8');
    const re = /Skipped ([0-9]+) frames!/g;
    for (let m = re.exec(text); m; m = re.exec(text)) {
      skippedFramesCount += 1;
      skippedFramesMax = Math.max(skippedFramesMax, Number.parseInt(m[1], 10));
    }
  }

  return {
    device: report?.meta?.device ?? path.basename(deviceDir),
    androidRelease: report?.meta?.android_release ?? '?',
    androidSdk: report?.meta?.android_sdk ?? '?',
    inferencesCount: inferences.length,
    onnx,
    pipeline,
    kvHits,
    kvRate: inferences.length ? kvHits / inferences.length : null,
    hybridInitialNeuralShare: {
      numer: hybridInitialNeural.length,
      denom: hybridInitialSources.length,
    },
    hybridSettledNeuralShare: {
      numer: hybridSettledNeural.length,
      denom: hybridSettledSources.length,
    },
    hybridDelay,
    skippedFramesCount,
    skippedFramesMax,
  };
}

const args = process.argv.slice(2);
if (args.length === 0) usage();

const targets = expandTargets(args);
if (targets.length === 0) {
  // eslint-disable-next-line no-console
  console.error('No report.json files found.');
  process.exit(1);
}

const rows = [];
for (const { deviceDir, reportPath } of targets) {
  const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
  rows.push(extractMatrixStats(deviceDir, report));
}

// eslint-disable-next-line no-console
console.log(
  [
    '| Device | Android | Inferences | ONNX p50/p95 (ms) | Pipeline p50/p95 (ms) | KV‑cache hit | Hybrid top‑3 neural (init→settled) | Hybrid req→listener p50/p95 (ms) | Skipped frames (count/max) |',
    '|---|---:|---:|---:|---:|---:|---:|---:|---:|',
  ].join('\n'),
);
for (const r of rows) {
  const android = `${r.androidRelease} (sdk ${r.androidSdk})`;
  const kv = r.kvRate == null ? '-' : `${Math.round(r.kvRate * 100)}%`;
  const hybridShare = `${formatPct(r.hybridInitialNeuralShare.numer, r.hybridInitialNeuralShare.denom)} → ${formatPct(
    r.hybridSettledNeuralShare.numer,
    r.hybridSettledNeuralShare.denom,
  )}`;
  const skipped = r.skippedFramesCount === 0 ? '0' : `${r.skippedFramesCount} / ${r.skippedFramesMax}`;

  // eslint-disable-next-line no-console
  console.log(
    `| ${r.device} | ${android} | ${r.inferencesCount} | ${formatMsStat(
      r.onnx,
    )} | ${formatMsStat(r.pipeline)} | ${kv} | ${hybridShare} | ${formatMsStat(r.hybridDelay)} | ${skipped} |`,
  );
}
