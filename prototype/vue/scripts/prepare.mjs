// Preserve one authoritative interaction engine instead of maintaining two diverging prototypes.
import { readFileSync, writeFileSync } from 'node:fs'
const source = readFileSync(new URL('../../app.js', import.meta.url), 'utf8')
const seed = readFileSync(new URL('../../demo-data.js', import.meta.url), 'utf8')
const html = readFileSync(new URL('../../index.html', import.meta.url), 'utf8')
writeFileSync(new URL('../src/legacy.css', import.meta.url), html.match(/<style>([\s\S]*?)<\/style>/)[1])
const header = `// GENERATED from prototype/app.js and demo-data.js by scripts/prepare.mjs.\n// Vue/TS host reuses the JavaScript prototype engine; not a full TypeScript rewrite.\nexport function mountPrototype(onUpdate){\n${seed}\nconst stop = new AbortController();\nconst listenDocument = (type, handler) => document.addEventListener(type, handler, {signal:stop.signal});\n`
const footer = `\nconst baseRender=render;\nrender=function(){baseRender();const u=actor();onUpdate({displayName:u?.displayName||'',systemName:sys()?.name||'平台控制台',loggedIn:!!state.uid,roleCount:state.sysId?assigned().length:0,permissionCount:codes().size,annotations:state.annotate});};\nrender();\nreturn {setApiAnnotations(enabled){state.annotate=enabled;render();},dispose(){stop.abort();clearTimeout(window.toastTimer);modal.close();}};\n}\n`
writeFileSync(new URL('../src/engine.js', import.meta.url), header + source.replace("document.addEventListener('click',", "listenDocument('click',").replace(/render\(\);\s*$/, '') + footer)
