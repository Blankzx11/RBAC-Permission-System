import { spawnSync } from 'node:child_process'
import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { createHash } from 'node:crypto'
const project=fileURLToPath(new URL('../',import.meta.url)), root=new URL('../../../',import.meta.url)
const lock=JSON.parse(readFileSync(new URL('../package-lock.json',import.meta.url),'utf8'))
const report={date:new Date().toISOString(),command:'npm run build',node:process.version,passed:false,steps:[],versions:{},inputSha256:{},artifacts:[],scope:'Vue/TypeScript host and typed JS boundary; JS interaction engine internals are not statically typechecked. Not a Java backend build.'}
for(const name of ['vue','typescript','tailwindcss','vite','vue-tsc'])report.versions[name]=lock.packages['node_modules/'+name].version
try{
 await import('./prepare.mjs')
 const steps=[['Vue/TS typecheck','../node_modules/vue-tsc/bin/vue-tsc.js',['--noEmit']],['Vite + Tailwind offline bundle','../node_modules/vite/bin/vite.js',['build']]]
 for(const [name,file,args] of steps){
  const result=spawnSync(process.execPath,[fileURLToPath(new URL(file,import.meta.url)),...args],{cwd:project,encoding:'utf8'})
  const output=(result.stdout||'')+(result.stderr||'');process.stdout.write(output)
  report.steps.push({name,exitCode:result.status,output})
  if(result.error||result.status!==0)throw result.error||new Error(name+' failed')
 }
 await import('./offline-entry.mjs')
 for(const rel of ['prototype/app.js','prototype/demo-data.js','prototype/index.html','prototype/vue/src/App.vue','prototype/vue/src/types.ts','prototype/vue/src/engine.d.ts','prototype/vue/src/style.css','prototype/vue/scripts/prepare.mjs','prototype/vue/scripts/build.mjs','prototype/vue/scripts/offline-entry.mjs','prototype/vue/src/main.ts','prototype/vue/vite.config.ts','prototype/vue/tsconfig.json','prototype/vue/package.json','prototype/vue/package-lock.json'])report.inputSha256[rel]=createHash('sha256').update(readFileSync(new URL(rel,root))).digest('hex')
 for(const file of ['index.html','rbac0-prototype.js','rbac0-prototype.css']){const url=new URL('../dist/'+file,import.meta.url);if(!existsSync(url))throw new Error('Missing artifact '+file);report.artifacts.push({path:'prototype/vue/dist/'+file,sha256:createHash('sha256').update(readFileSync(url)).digest('hex')})}
 report.passed=true
}catch(error){report.error=String(error);process.exitCode=1}
writeFileSync(new URL('validation/vue-build.json',root),JSON.stringify(report,null,2)+'\n')
console.log(JSON.stringify({passed:report.passed,versions:report.versions,error:report.error}))
