import {cp,rm,mkdir} from 'node:fs/promises';import {resolve,dirname} from 'node:path';import {fileURLToPath} from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');const out=resolve(root,'..','outputs','web');const target=process.argv[2]||'all';
await rm(out,{recursive:true,force:true});await mkdir(out,{recursive:true});await cp(root,out,{recursive:true,filter:p=>!p.includes('node_modules')&&!p.includes(`${resolve(root,'scripts')}`)});console.log(`Velora web preparado en ${out} (${target})`);
