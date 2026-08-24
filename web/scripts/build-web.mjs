import {cp,rm,mkdir,copyFile} from 'node:fs/promises';
import {resolve,dirname,join} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const repo=resolve(root,'..');
const out=resolve(repo,'outputs','web');
const target=process.argv[2]||'all';
const command=(name,args,cwd)=>{const r=spawnSync(name,args,{stdio:'inherit',cwd,shell:process.platform==='win32'});return r.status===0};
const copyCommon=async destination=>{await mkdir(destination,{recursive:true});for(const file of ['app.js','index.html','manifest.webmanifest','styles.css','README.md','package.json'])await copyFile(join(root,file),join(destination,file));};
await rm(out,{recursive:true,force:true});await mkdir(out,{recursive:true});
await copyCommon(out);await cp(join(root,'platforms'),join(out,'platforms'),{recursive:true});

const results=[];
if(target==='samsung'||target==='all'){
  const stage=resolve(out,'samsung');await copyCommon(stage);await copyFile(join(root,'platforms','samsung','config.xml'),join(stage,'config.xml'));await copyFile(join(root,'platforms','samsung','icon.png'),join(stage,'icon.png'));
  results.push(command('tizen',['build-web','--','.'],stage)&&command('tizen',['package','-t','wgt','--','.buildResult'],stage)?'Samsung WGT generado':'Samsung bundle preparado; falta Tizen CLI/perfil de firma');
}
if(target==='webos'||target==='all'){
  const stage=resolve(out,'webos');await copyCommon(stage);await copyFile(join(root,'platforms','webos','appinfo.json'),join(stage,'appinfo.json'));await copyFile(join(root,'platforms','webos','icon.png'),join(stage,'icon.png'));
  if(command('esbuild',['app.js','--target=es2017','--outfile=app.compat.js'],stage)){await copyFile(join(stage,'app.compat.js'),join(stage,'app.js'));await rm(join(stage,'app.compat.js'),{force:true});}
  results.push(command('ares-package',['.'],stage)?'webOS IPK generado':'webOS bundle preparado; falta ares-package o certificado');
}
if(target==='vidaa'||target==='all')results.push('VIDAA bundle preparado; el empaquetado depende del SDK y modelo VIDAA instalado');
console.log(`Velora web preparado en ${out}`);for(const result of results)console.log(`- ${result}`);
