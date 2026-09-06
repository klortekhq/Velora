import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const file = path.join(root, 'apple', 'Sources', 'VeloraKit', 'Resources', 'PrivacyInfo.xcprivacy');
if (!fs.existsSync(file)) throw new Error('Apple: falta PrivacyInfo.xcprivacy');

const source = fs.readFileSync(file, 'utf8');
for (const key of ['NSPrivacyTracking', 'NSPrivacyCollectedDataTypes', 'NSPrivacyAccessedAPITypes']) {
  if (!source.includes(`<key>${key}</key>`)) throw new Error(`Apple: falta ${key}`);
}
if (!/<key>NSPrivacyTracking<\/key>\s*<false\s*\/>/.test(source)) {
  throw new Error('Apple: Velora debe declarar NSPrivacyTracking=false');
}
if (!/<key>NSPrivacyCollectedDataTypes<\/key>\s*<array\s*\/>/.test(source)) {
  throw new Error('Apple: el catálogo de datos recopilados debe estar vacío');
}

console.log('Manifiesto de privacidad Apple presente y coherente: sin tracking ni datos recopilados declarados.');
