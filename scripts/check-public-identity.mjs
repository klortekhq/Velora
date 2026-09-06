import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

const root = process.cwd();
const roots = ['README.md', 'NOTICE', 'THIRD_PARTY_LICENSES.md', 'docs', 'app', 'apple', 'web', '.github'];
const forbidden = /(?:elefin|ruvikitten|moonfin|jellyflix|flixor)/i;
const extensions = new Set(['.md', '.txt', '.json', '.js', '.mjs', '.kt', '.swift', '.html', '.xml', '.yml', '.yaml', '.toml']);
const matches = [];

function visit(relative) {
  const absolute = path.join(root, relative);
  const stat = fs.statSync(absolute);
  if (stat.isDirectory()) {
    for (const entry of fs.readdirSync(absolute)) visit(path.join(relative, entry));
    return;
  }
  if (relative.endsWith('.lock') || !extensions.has(path.extname(relative).toLowerCase())) return;
  const lines = fs.readFileSync(absolute, 'utf8').split(/\r?\n/);
  lines.forEach((line, index) => {
    if (forbidden.test(line)) matches.push(`${path.relative(root, absolute)}:${index + 1}`);
  });
}

for (const entry of roots) visit(entry);
if (matches.length) {
  console.error('Referencias de identidad heredadas detectadas:');
  for (const match of matches) console.error(`- ${match}`);
  process.exit(1);
}

console.log('Identidad pública limpia: Velora/Klørtek sin referencias heredadas.');
