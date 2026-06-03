/**
 * Removes stale Vite prebundle output under .angular/cache.
 * Prevents "file does not exist … vite/deps/chunk-*.js" warnings when hashes drift.
 */
import { existsSync, readdirSync, rmSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const pkgRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
const cacheRoot = join(pkgRoot, '.angular', 'cache');

function clearViteDepsUnder(dir) {
  let entries;
  try {
    entries = readdirSync(dir, { withFileTypes: true });
  } catch {
    return;
  }
  for (const entry of entries) {
    if (!entry.isDirectory()) {
      continue;
    }
    const child = join(dir, entry.name);
    if (entry.name === 'vite') {
      const depsDir = join(child, 'deps');
      if (existsSync(depsDir)) {
        rmSync(depsDir, { recursive: true, force: true });
      }
      continue;
    }
    clearViteDepsUnder(child);
  }
}

if (existsSync(cacheRoot)) {
  clearViteDepsUnder(cacheRoot);
}
