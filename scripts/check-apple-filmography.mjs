import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const client = fs.readFileSync(
  path.join(root, 'apple/Sources/VeloraKit/JellyfinClient.swift'),
  'utf8'
);
const tests = fs.readFileSync(
  path.join(root, 'apple/Tests/VeloraKitTests/VeloraKitTests.swift'),
  'utf8'
);

const requiredClientMarkers = [
  'public func items(userID: String, forPerson personID: String)',
  'URLQueryItem(name: "StartIndex", value: String(startIndex))',
  'URLQueryItem(name: "Limit", value: String(pageSize))',
  'let maximumItems = 500',
  'page.totalRecordCount',
];
const missing = requiredClientMarkers.filter((marker) => !client.includes(marker));
if (!tests.includes('testPersonFilmographyFollowsJellyfinPagination')) {
  missing.push('testPersonFilmographyFollowsJellyfinPagination');
}
if (missing.length) {
  throw new Error(`Apple filmography pagination contract failed: ${missing.join(', ')}`);
}

console.log('Apple filmography contract passed: paginated, bounded and regression-covered.');
