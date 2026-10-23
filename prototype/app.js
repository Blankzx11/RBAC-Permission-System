'use strict';
const seed=structuredClone(window.RBAC_DEMO), db=structuredClone(seed);
const state={uid:null,sysId:null,page:'overview',keyword:'',annotate:false,viewState:'normal',authVersion:null,logFilters:{}};
let nextId=Date.now();
const newId=()=>String(++nextId);
const demoPasswords=new Map(db.users.map(u=>[u.id,'DemoOnly12345']));
db.users.forEach(u=>u.authVersion??=1);
const routes=['/overview','/members','/roles','/permissions','/audit','/profile'];
const app=document.getElementById('app'), modal=document.getElementById('modal');
const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const find=(xs,id)=>xs.find(x=>x.id===String(id));
const actor=()=>find(db.users,state.uid);
const sys=()=>find(db.systems,state.sysId);
const roles=()=>db.roles.filter(x=>x.sysId===state.sysId);
const perms=()=>db.permissions.filter(x=>x.sysId===state.sysId);
const members=()=>db.members.filter(x=>x.sysId===state.sysId);
const assigned=(uid=state.uid)=>db.userRoles[state.sysId+':'+uid]||[];
function sessionValid(){
 const u=actor();if(!u||u.status!=='ENABLED'||u.authVersion!==state.authVersion)return false;
 if(!state.sysId)return u.platformAdmin;
 return sys()?.status==='ENABLED'&&(u.platformAdmin||members().some(m=>m.userId===u.id&&m.status==='ENABLED'));
}
function codes(uid=state.uid){
 if(!state.sysId||sys()?.status!=='ENABLED')return new Set();
 const user=find(db.users,uid);if(user?.status!=='ENABLED')return new Set();
 if(user.platformAdmin)return new Set(perms().filter(p=>p.status==='ENABLED').map(p=>p.code));
 if(!members().some(m=>m.userId===uid&&m.status==='ENABLED'))return new Set();
 const ids=new Set(assigned(uid).flatMap(id=>{const r=roles().find(x=>x.id===id);return r?.status==='ENABLED'?(db.rolePermissions[id]||[]):[]}));
 return new Set(perms().filter(p=>ids.has(p.id)&&p.status==='ENABLED').map(p=>p.code));
}
const can=code=>sessionValid()&&(actor().platformAdmin||codes().has(code));
const isPlatform=()=>!!actor()?.platformAdmin&&!state.sysId;
const routeBase=()=>'/api/v1/systems/'+state.sysId;
const badge=s=>`<span class="pill ${s==='ENABLED'?'green':'red'}">${s==='ENABLED'?'启用':'停用'}</span>`;
const btn=(label,action,attrs='',cls='small')=>`<button type="button" class="${cls}" data-action="${action}" ${attrs}>${label}</button>`;
const api=(...rows)=>state.annotate?`<div class="api-note">${rows.map(x=>`<code>${esc(x)}</code>`).join('')}</div>`:'';
function toast(msg){const live=document.getElementById('live');live.innerHTML=`<div class="toast">${esc(msg)}</div>`;clearTimeout(window.toastTimer);window.toastTimer=setTimeout(()=>live.innerHTML='',4000)}
function audit(action,target,before,after,targetType='RESOURCE',policyChanged=true){
 const clean=x=>x===null?null:JSON.parse(JSON.stringify(x,(k,v)=>/password|token|secret|hash/i.test(k)?undefined:v));
 db.logs.unshift({id:newId(),sysId:state.sysId,actorId:state.uid,actorName:actor().displayName,action,
 targetType,targetId:String(target),before:clean(before),after:clean(after),requestId:'demo-'+newId(),createdAt:new Date().toISOString()});
 if(policyChanged&&sys())sys().policyVersion++;
}
const pageRoutes={members:'/members',roles:'/roles',permissions:'/permissions',audit:'/audit'};
function hasMenu(page){return actor()?.platformAdmin||perms().some(p=>p.type==='MENU'&&p.routePath===pageRoutes[page]&&p.status==='ENABLED'&&codes().has(p.code));}
function access(){
 const p=state.page;
 if(['systems','users'].includes(p))return isPlatform();
 if(['overview','profile'].includes(p))return true;
 if(p==='audit')return isPlatform()||(hasMenu(p)&&can('sys:audit:read'));
 return !!state.sysId&&hasMenu(p)&&can({members:'sys:member:read',roles:'sys:role:read',permissions:'sys:permission:read'}[p]);
}
function login(){app.innerHTML=`<div class="top"><div class="brand"><span class="brand-mark">RB</span>权限管理平台</div><span class="pill">RBAC0 原型 V1.2</span></div><div class="login-shell"><section class="login-copy"><span class="login-tag">OOAD · SOFTWARE ENGINEERING</span><h1>一个身份<br>各系统独立授权</h1><p>围绕用户、角色与权限，建立清晰可追溯的授权边界。切换演示身份，比较平台管理与子系统管理流程。</p><div class="tagrow"><span class="pill">RBAC0 基线</span><span class="pill">多系统隔离</span><span class="pill">权限变更审计</span></div><p class="help">本页为本地模拟，数据保存在内存；刷新恢复初始状态。未连接后端，不用于安全验证。</p></section><form class="login-box" id="login-form"><h2>进入演示工作台</h2><p class="muted">选择身份与系统上下文</p><label class="field"><span>演示身份</span><select id="login-user">${db.users.map(u=>`<option value="${u.id}">${esc(u.displayName)} · ${u.platformAdmin?'平台管理员':u.id==='2'?'系统 A 管理员':u.id==='3'?'审计用户':'普通用户'}${u.status==='DISABLED'?'（账号停用）':''}</option>`).join('')}</select></label><label class="field"><span>目标上下文</span><select id="login-system"><option value="">平台控制台</option><option value="1">系统 A · 教务管理</option><option value="2">系统 B · 科研管理</option></select></label><p id="login-error" role="alert"></p><button class="primary" type="submit">进入演示</button><hr class="line"><p class="note">真实接口：POST /api/v1/auth/sessions<br>生产环境需验证密码、账号、系统与成员状态。</p></form></div>`}
function addLoginFields(){
 const systemField=document.getElementById('login-system').closest('label');
 systemField.insertAdjacentHTML('beforebegin','<label class="field"><span>用户名</span><input id="login-username" autocomplete="username" value="platform_admin" required></label><label class="field"><span>密码（演示口令）</span><input id="login-password" type="password" autocomplete="current-password" value="DemoOnly12345" required><small>仅原型口令 DemoOnly12345，与后端种子数据无关。</small></label>');
 document.getElementById('login-user').addEventListener('change',e=>document.getElementById('login-username').value=find(db.users,e.target.value).username);
 document.getElementById('login-form').addEventListener('submit',e=>{const u=find(db.users,document.getElementById('login-user').value);if(document.getElementById('login-username').value!==u.username||document.getElementById('login-password').value!==demoPasswords.get(u.id)){e.preventDefault();e.stopPropagation();document.getElementById('login-error').textContent='演示凭据不匹配；初始口令为 DemoOnly12345，新建或改密后使用本页设置的口令。'}});
}
function navigation(){
 let items=[['overview','工作台']];
 if(isPlatform())items.push(['systems','子系统管理'],['users','全局账号']);
 if(state.sysId){for(const [p,n] of [['members','成员管理'],['roles','角色管理'],['permissions','权限目录']])if(hasMenu(p))items.push([p,n]);}
 if(isPlatform()||hasMenu('audit'))items.push(['audit','审计日志']);
 items.push(['profile','个人中心']);
 return items.map(([p,n])=>`<button type="button" class="${state.page===p?'active':''}" data-page="${p}">${n}</button>`).join('');
}
function systemOptions(){let list=actor().platformAdmin?db.systems:db.systems.filter(s=>db.members.some(m=>m.userId===state.uid&&m.sysId===s.id&&m.status==='ENABLED'));return (actor().platformAdmin?'<option value="">平台控制台</option>':'')+list.filter(s=>s.status==='ENABLED').map(s=>`<option value="${s.id}" ${s.id===state.sysId?'selected':''}>${esc(s.name)}</option>`).join('')}
function render(){
 if(state.uid&&!sessionValid()){
  if(actor()?.status==='ENABLED'&&actor().platformAdmin&&actor().authVersion===state.authVersion){state.sysId=null;state.page='overview';toast('系统上下文已停用，已返回平台控制台');}
  else{state.uid=null;state.sysId=null;state.page='overview';toast('会话已失效或账号、成员已停用，请重新登录');}
 }
 if(!state.uid){login();addLoginFields();return;}
 app.innerHTML=`<header class="top"><div class="brand"><span class="brand-mark">RB</span>权限管理平台</div><div class="context"><label for="system-picker" class="muted">当前上下文</label><select id="system-picker">${systemOptions()}</select><span class="pill">${esc(actor().displayName)}</span><label><input type="checkbox" id="api-toggle" ${state.annotate?'checked':''}> API 标注</label>${btn('切换身份','logout')}${btn('退出','logout')}</div></header><div class="ribbon">交互原型 · 内存模拟数据 · 未连接生产 API ${state.sysId?' / 当前策略版本 '+sys().policyVersion:' / 平台会话'}</div><div class="layout"><aside><div class="nav-label">ACCESS MANAGEMENT</div><nav aria-label="主导航">${navigation()}</nav><div class="nav-foot">课程设计评审稿<br>2026 年 10 月 21 日<br>仅 RBAC0 · 多系统隔离</div></aside><main class="content">${content()}</main></div>`;
}
function heading(title,sub,extra=''){return `<div class="heading"><div><div class="eyebrow">${state.sysId?'SYSTEM / '+esc(sys().code):'PLATFORM / CONTROL CENTER'}</div><h1>${title}</h1><p class="muted">${sub}</p></div>${extra}</div>`}
function table(headers,rows,count){return `<div class="table-wrap"><table><thead><tr>${headers.map(x=>`<th>${x}</th>`).join('')}</tr></thead><tbody>${rows.length?rows.join(''):`<tr><td colspan="${headers.length}"><div class="empty"><strong>暂无匹配数据</strong>试着更换筛选条件</div></td></tr>`}</tbody></table></div><div class="table-bottom"><span>共 ${count??rows.length} 条 · 演示仅一页</span><span>第 1 页</span></div>`}
function toolbar(createLabel,type,allowed){return `<div class="toolbar"><input id="search" aria-label="搜索名称或编码" maxlength="64" placeholder="搜索名称、账号或编码" value="${esc(state.keyword)}"><span class="grow"></span>${allowed?btn(createLabel,'create',`data-type="${type}"`,'primary'):''}${btn('演示状态','states')}</div>`}
const matches=x=>JSON.stringify(x).toLowerCase().includes(state.keyword.toLowerCase());
function content(){
 if(!access())return heading('403 · 无操作权限','当前身份没有访问此页面的权限。')+btn('返回工作台','home','','primary');
 const p=state.page;
 if(state.viewState==='error')return heading('服务暂不可用','演示网络错误状态，已有表单内容应当保留。')+`<div class="error" role="alert">503 SERVICE_UNAVAILABLE · 无法验证当前权限，请稍后重试。</div><p></p>${btn('重试','normal','','primary')}`;
 if(state.viewState==='empty')return heading('暂无数据','这是列表空状态演示。')+`<div class="panel"><div class="empty"><strong>还没有可显示的记录</strong>调整条件或创建第一条记录</div></div>${btn('恢复演示数据','normal','','primary')}`;
 if(p==='overview')return overview();
 if(p==='profile')return profile();
 if(p==='audit')return logsPage();
 return listPage(p);
}
function overview(){let nr=state.sysId?roles().length:db.systems.length,nm=state.sysId?members().length:db.users.length,np=state.sysId?perms().length:db.logs.length;return heading('工作台',state.sysId?'管理当前系统的成员、角色和授权。':'从平台控制台维护全局身份与子系统边界。',`<span class="pill green">${state.sysId?'系统上下文':'平台上下文'}</span>`)+api('GET /api/v1/me','GET /api/v1/me/systems',state.sysId?'GET /api/v1/me/permissions':'GET /api/v1/platform/systems')+`<div class="stats"><div class="stat"><span class="muted">${state.sysId?'系统成员':'全局账号'}</span><strong>${nm}</strong><small class="muted">内存演示记录</small></div><div class="stat"><span class="muted">${state.sysId?'角色数量':'业务子系统'}</span><strong>${nr}</strong><small class="muted">按当前上下文统计</small></div><div class="stat"><span class="muted">${state.sysId?'权限定义':'操作审计'}</span><strong>${np}</strong><small class="muted">不包含性能指标</small></div></div><div class="grid"><section class="panel panel-body"><h2>身份与权限边界</h2><p>当前用户：<strong>${esc(actor().displayName)}</strong> <span class="mono">${esc(actor().username)}</span></p><p>当前范围：${state.sysId?esc(sys().name):'全局账号与子系统配置'}</p><p>直接角色：${state.sysId?assigned().map(id=>esc(find(db.roles,id)?.name)).join('、')||'平台特权上下文':'平台管理员'}</p><p class="help">同一个账号可以在系统 A 管理成员，在系统 B 仅拥有查看权限。切换上下文时重新获取菜单与权限。</p></section><section class="panel panel-body"><h2>建议评审流程</h2><ol class="steps"><li>进入系统 A，查看角色和权限目录</li><li>给业务角色配置权限，再指派给成员</li><li>切换普通用户，检查菜单和操作入口</li><li>返回管理员，在审计中核对前后值</li></ol></section></div>`}
function listPage(p){
 const cfg={systems:['子系统管理','创建并维护独立的业务权限边界。','新建子系统','system'],users:['全局账号','全局身份与平台管理员标记仅由平台维护。','新建账号','user'],members:['成员管理','仅维护当前系统内的成员资格，全局账号信息独立管理。','添加成员','member'],roles:['角色管理','角色编码在系统内唯一；内置 SYS_ADMIN 受保护。','新建角色','role'],permissions:['权限目录','菜单和操作独立授权，菜单父子关系不产生权限继承。','新建权限','permission']}[p];
 const [title,sub,label,type]=cfg;
 let data={systems:db.systems,users:db.users,members:members(),roles:roles(),permissions:perms()}[p];
 if(p==='permissions'){
  const ordered=[];const seen=new Set();
  function walk(parent,depth){for(const x of perms().filter(p=>p.parentId===parent).sort((a,b)=>(a.sortOrder||0)-(b.sortOrder||0)||Number(a.id)-Number(b.id))){if(seen.has(x.id))continue;seen.add(x.id);ordered.push({...x,treeDepth:depth});walk(x.id,depth+1);}}
  walk(null,0);data=ordered;
 }
 data=data.filter(matches);
 const createAllowed=['systems','users'].includes(p)?isPlatform():can({members:'sys:member:create',roles:'sys:role:create',permissions:'sys:permission:create'}[p]);
 const base=['systems','users'].includes(p)?'/api/v1/platform/'+p:routeBase()+'/'+p;
 let rows=data.map(x=>{let id=x.id||x.userId,attrs=`data-id="${id}" data-type="${type}"`;let user=type==='member'?find(db.users,x.userId):x;
 const editable=isPlatform()||can({member:'sys:member:update',role:'sys:role:update',permission:'sys:permission:update'}[type]);
 const protect=(type==='role'&&x.builtin)||(type==='member'&&assigned(x.userId).some(r=>find(db.roles,r)?.builtin));
 let actions='';
 if(type==='system')actions+=btn('进入系统','enter',attrs);
 if(editable&&(!protect||actor().platformAdmin))actions+=btn('编辑','edit',attrs);
 if(type==='member'&&can('sys:member:assign')&&(!protect||actor().platformAdmin))actions+=btn('分配角色','assignRoles',attrs);
 if(type==='role'&&can('sys:role:grant')&&(!protect||actor().platformAdmin))actions+=btn('分配权限','assignPerms',attrs);
 if(['role','permission'].includes(type)&&can(type==='role'?'sys:role:delete':'sys:permission:delete')&&!x.builtin)actions+=btn('删除','delete',attrs,'small danger');
 const code=type==='member'?user?.username:x.code||x.username;
 const name=(type==='permission'?'↳ '.repeat(x.treeDepth||0):'')+(x.name||x.displayName);
 const detail=type==='permission'?`${x.type==='MENU'?'页面菜单':'操作权限'} · ${x.routePath||'按钮 / API 共享操作码'}`:type==='member'?assigned(x.userId).map(r=>find(db.roles,r)?.name).join('、')||'未指派角色':type==='role'?(x.builtin?'内置受保护角色':`${(db.rolePermissions[id]||[]).length} 项直接权限`):type==='user'?(x.platformAdmin?'平台管理员':'全局账号'):(x.description||'独立授权边界');
 return `<tr><td><strong>${esc(name)}</strong><small>${esc(detail)}</small></td><td class="mono">${esc(code)}</td><td>${badge(x.status)}${type==='member'?'<small>全局账号：'+(user?.status==='ENABLED'?'启用':'停用')+'</small>':''}</td><td class="mono">v${x.version||1}</td><td><div class="actions">${actions||'<span class="muted">只读</span>'}</div></td></tr>`});
 return heading(title,sub)+api(`GET ${base}`,createAllowed?`POST ${base}`:'只读视图：写操作仍由后端独立鉴权',['role','permission'].includes(type)?'编辑 PATCH / 删除 DELETE + If-Match':'编辑 PATCH + If-Match；停用代替物理删除')+`<div class="panel">${toolbar(label,type,createAllowed)}${table(['名称 / 说明','编码 / 账号','状态','版本','操作'],rows)}</div>`;
}
function logsPage(){
 const f=state.logFilters,scoped=db.logs.filter(l=>isPlatform()||l.sysId===state.sysId);
 const day=x=>new Date(x).toLocaleDateString('sv-SE',{timeZone:'Asia/Shanghai'});
 const start=d=>Date.parse(d+'T00:00:00+08:00');
 let validation='';if(Boolean(f.from)!==Boolean(f.to))validation='开始日期和结束日期必须成对填写';
 if(f.from&&f.to){const days=(start(f.to)-start(f.from))/86400000+1;if(days<1)validation='开始日期不得晚于结束日期';else if(days>90)validation='单次查询最多90个自然日';}
 const today=day(new Date()),defaultFrom=new Date(start(today)-6*86400000).toISOString();
 const lower=f.from?start(f.from):Date.parse(defaultFrom),upper=f.to?start(f.to)+86400000:start(today)+86400000;
 const logs=validation?[]:scoped.filter(l=>Date.parse(l.createdAt)>=lower&&Date.parse(l.createdAt)<upper&&(!f.action||l.action===f.action)&&(!f.actorId||l.actorId===f.actorId)&&(!f.sysId||l.sysId===f.sysId));
 const select=(key,title,items)=>`<label class="field"><span>${title}</span><select data-log-filter="${key}"><option value="">全部</option>${items.map(([v,n])=>`<option value="${esc(v)}" ${f[key]===v?'selected':''}>${esc(n)}</option>`).join('')}</select></label>`;
 const actors=[...new Map(scoped.filter(l=>l.actorId).map(l=>[l.actorId,l.actorName])).entries()];
 const controls=`<div class="toolbar"><label class="field"><span>开始日期（上海时区）</span><input type="date" aria-label="审计开始日期" data-log-filter="from" value="${esc(f.from||'')}"></label><label class="field"><span>结束日期</span><input type="date" aria-label="审计结束日期" data-log-filter="to" value="${esc(f.to||'')}"></label>${select('actorId','操作者',actors)}${select('action','操作类型',[...new Set(scoped.map(l=>l.action))].map(a=>[a,a]))}${isPlatform()?select('sysId','目标子系统',db.systems.map(s=>[s.id,s.name])):''}${btn('清除筛选','clearLogFilters')}</div>`;
 return heading('审计日志','日期为空默认近7个自然日；成对填写，最多90日。成功变更记录只读。')+api('GET '+(isPlatform()?'/api/v1/platform':routeBase())+'/audit-logs?from=...&to=...&actorId=...&action=...','GET .../audit-logs/{logId}')+`<div class="panel">${controls}${validation?'<div class="panel-body error" role="alert">422 VALIDATION_ERROR · '+esc(validation)+'</div>':table(['操作者 / 时间','操作','目标系统','目标','详情'],logs.map(l=>`<tr><td>${esc(l.actorName)}<small>${new Date(l.createdAt).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai'})}</small></td><td class="mono">${esc(l.action)}</td><td>${esc(find(db.systems,l.sysId)?.name||'平台')}</td><td class="mono">${esc(l.targetId)}</td><td>${btn('前后值','log',`data-id="${l.id}"`)}</td></tr>`))}</div>`;
}
function profile(){
 const cs=[...codes()],m=members().find(m=>m.userId===state.uid);
 return heading('个人中心','查看当前上下文的角色与有效权限。')+api('GET /api/v1/me',state.sysId?'GET /api/v1/me/permissions':'平台上下文不调用子系统菜单或权限接口','PUT /api/v1/me/password → 204 后重新登录')+`<div class="grid"><section class="panel panel-body"><h2>${esc(actor().displayName)}</h2><p class="mono">${esc(actor().username)}</p><p>全局账号状态：${badge(actor().status)}</p><p>当前上下文：${esc(sys()?.name||'平台控制台')}</p>${state.sysId?'<p>本地成员状态：'+(m?badge(m.status):'平台特权上下文')+'</p>':''}<p>直接角色：${state.sysId?assigned().map(id=>{const r=roles().find(x=>x.id===id);return esc((r?.name||id)+(r?.status==='DISABLED'?'（停用）':''));}).join('、')||'平台特权 / 无业务角色':'平台管理员'}</p>${btn('修改密码','password','','primary')}<p class="help">系统管理员不能在成员页修改他人全局密码。</p></section><section class="panel panel-body"><h2>有效权限 <span class="pill">${cs.length}</span></h2><div class="tagrow">${cs.map(c=>`<span class="pill mono">${esc(c)}</span>`).join('')||'<p class="muted">平台控制台使用单独的平台权限边界。</p>'}</div></section></div>`;
}
function showModal(body){modal.innerHTML=body;modal.showModal();const f=modal.querySelector('#assignment-form');if(f){f.querySelector('.checks').insertAdjacentHTML('afterend','<div id="assignment-diff" class="choice-summary" aria-live="polite"></div>');updateAssignmentDiff(f);}}
function updateAssignmentDiff(form){
 const kind=form.dataset.kind,id=form.dataset.id,old=kind==='roles'?assigned(id):db.rolePermissions[id]||[],chosen=[...new Set(new FormData(form).getAll('ids'))];
 const added=chosen.filter(x=>!old.includes(x)),removed=old.filter(x=>!chosen.includes(x));
 const pool=kind==='roles'?roles():perms(),names=ids=>ids.slice(0,6).map(id=>pool.find(x=>x.id===id)?.name||id).join('、')+(ids.length>6?'…':'');
 const out=form.querySelector('#assignment-diff');if(out)out.textContent=`当前选择 ${chosen.length} 项；新增 ${added.length} 项${added.length?'（'+names(added)+'）':''}；移除 ${removed.length} 项${removed.length?'（'+names(removed)+'）':''}`;
}
const modalHead=t=>`<div class="dialog-head"><h2 id="modal-title">${t}</h2>${btn('关闭','close')}</div>`;
function getResource(type,id){
 if(type==='member')return members().find(x=>x.userId===id);
 const x=find(db[{user:'users',system:'systems',role:'roles',permission:'permissions'}[type]]||[],id);
 return x&&(!['role','permission'].includes(type)||x.sysId===state.sysId)?x:null;
}
function writable(type,op){return sessionValid()&&(['user','system'].includes(type)?isPlatform():!!state.sysId&&can({member:'sys:member:',role:'sys:role:',permission:'sys:permission:'}[type]+op));}
function roleCodes(id){const r=roles().find(x=>x.id===id);return r?.status==='ENABLED'?(db.rolePermissions[id]||[]).map(id=>perms().find(p=>p.id===id)).filter(p=>p?.status==='ENABLED').map(p=>p.code):[];}
function protectedMember(id){return assigned(id).some(r=>roles().some(x=>x.id===r&&x.builtin));}
function delegableRole(id){return actor().platformAdmin||roleCodes(id).every(code=>codes().has(code));}
function validPassword(p){return typeof p==='string'&&p.length>=12&&p.length<=64&&/[a-zA-Z]/.test(p)&&/[0-9]/.test(p);}
function bootstrapSystem(systemId){
 const templates=seed.permissions.filter(p=>p.sysId===seed.systems[0].id),ids=new Map(templates.map(p=>[p.id,newId()]));
 for(const p of templates)db.permissions.push({...structuredClone(p),id:ids.get(p.id),sysId:systemId,parentId:p.parentId?ids.get(p.parentId):null});
 const id=newId();db.roles.push({id,sysId:systemId,code:'SYS_ADMIN',name:'系统管理员',description:'内置受保护角色',builtin:true,status:'ENABLED',version:1});
 db.rolePermissions[id]=[...ids.values()];
}
function editForm(type,id){if(!writable(type,id?'update':'create'))return toast('403 FORBIDDEN · 无操作权限');let x=id?getResource(type,id):{},edit=!!id;if(id&&!x)return toast('404 NOT_FOUND');if(!actor().platformAdmin&&((type==='role'&&x.builtin)||(type==='member'&&protectedMember(id))))return toast('409 PROTECTED_RESOURCE');let body='';const field=(label,key,value='',opts={})=>`<label class="field ${opts.full?'full':''}"><span>${label}</span><input name="${key}" value="${esc(value)}" ${opts.required===false?'':'required'} ${opts.readonly?'readonly':''} maxlength="${opts.max||64}" ${opts.password?'type="password" minlength="12"':''}></label>`;
 if(type==='user'){body+=field('用户名','username',x.username,{readonly:edit,max:32})+field('显示名','displayName',x.displayName);if(!edit)body+=field('初始密码（仅本页内存，不写日志）','initialPassword','',{password:true,full:true});body+=`<label class="field"><span>平台管理员</span><select name="platformAdmin"><option value="false">否</option><option value="true" ${x.platformAdmin?'selected':''}>是</option></select></label>`;}
 if(type==='system'||type==='role'){body+=field('编码（创建后不可改）','code',x.code,{readonly:edit,max:40})+field('名称','name',x.name)+field('说明','description',x.description,{required:false,full:true,max:500});}
 if(type==='member'){if(!edit)body+=field('已有全局用户名（精确匹配）','username','',{full:true,max:32});body+=field('本系统显示名','displayName',x.displayName,{full:true});}
 if(type==='permission'){body+=field('权限码（创建后不可改）','code',x.code,{readonly:edit,max:80})+field('名称','name',x.name);body+=`<label class="field"><span>类型</span><select name="type" ${edit?'disabled':''}><option value="MENU">MENU 页面</option><option value="ACTION" ${x.type==='ACTION'?'selected':''}>ACTION 操作</option></select></label><label class="field"><span>父菜单</span><select name="parentId"><option value="">无父节点</option>${perms().filter(p=>p.type==='MENU'&&p.id!==id).map(p=>`<option value="${p.id}" ${x.parentId===p.id?'selected':''}>${esc(p.name)}</option>`).join('')}</select></label>`+`<label class="field"><span>本地路由（ACTION选无）</span><select name="routePath"><option value="">无</option>${routes.map(r=>`<option value="${r}" ${x.routePath===r?'selected':''}>${r}</option>`).join('')}</select></label><label class="field"><span>排序（0–9999）</span><input name="sortOrder" type="number" min="0" max="9999" required value="${x.sortOrder||0}"></label>`;}
 if(edit)body+=`<label class="field"><span>状态</span><select name="status"><option value="ENABLED">启用</option><option value="DISABLED" ${x.status==='DISABLED'?'selected':''}>停用</option></select></label>`;
 showModal(modalHead((edit?'编辑':'新建')+{user:'全局账号',system:'子系统',member:'成员',role:'角色',permission:'权限'}[type])+api(`${edit?'PATCH':'POST'} ${['user','system'].includes(type)?'/api/v1/platform/'+(type==='user'?'users':'systems'):routeBase()+'/'+({member:'members',role:'roles',permission:'permissions'}[type])}${edit?'/'+id:''}`,edit?'If-Match: "'+x.version+'"':'201 Created')+`<form id="resource-form" data-type="${type}" data-id="${id||''}" data-version="${x.version||''}" data-sys-id="${state.sysId||''}"><div class="fields-grid">${body}</div><p class="help">停用会停止权限生效；重新启用将恢复原指派。所有写入均为原型内存模拟。</p><div id="form-error" role="alert"></div><div class="dialog-actions">${edit?btn('模拟版本冲突','resourceConflict'):''}${btn('取消','close')}<button type="submit" class="primary">保存</button></div></form>`);
}
function error(msg){document.getElementById('form-error').innerHTML=`<div class="error">${esc(msg)}</div>`}
function saveResource(form){
 const type=form.dataset.type,id=form.dataset.id,old=id?getResource(type,id):null,values=Object.fromEntries(new FormData(form));
 const allowed={user:['username','displayName','platformAdmin',...(id?['status']:['initialPassword'])],system:['code','name','description',...(id?['status']:[])],member:id?['displayName','status']:['username','displayName'],role:['code','name','description',...(id?['status']:[])],permission:['code','name','parentId','routePath','sortOrder',...(id?['status']:['type'])]}[type]||[];
 if(Object.keys(values).some(k=>!allowed.includes(k)))return error('422 VALIDATION_ERROR · 请求包含不允许的字段');
 if(!writable(type,id?'update':'create'))return error('403 FORBIDDEN · 无操作权限');
 if(form.dataset.sysId!==(state.sysId||''))return error('403 SYSTEM_CONTEXT_MISMATCH');
 if(id&&!old)return error('404 NOT_FOUND');
 if(old&&String(old.version)!==form.dataset.version)return error('412 VERSION_CONFLICT · 资源已变化，请刷新后比较再提交');
 if(!actor().platformAdmin&&((type==='role'&&old?.builtin)||(type==='member'&&protectedMember(id))))return error('409 PROTECTED_RESOURCE');
 if(type==='role'&&old&&!delegableRole(id))return error('403 FORBIDDEN · 原角色权限超出可管理范围');
 const nameKey=['system','role','permission'].includes(type)?'name':'displayName';values[nameKey]=(values[nameKey]||'').trim();
 if(!values[nameKey]||values[nameKey].length>64)return error('422 VALIDATION_ERROR · 名称须为1–64字符');
 if((values.description||'').length>500)return error('422 VALIDATION_ERROR · 说明最多500字符');
 if(old&&((values.code&&values.code!==old.code)||(values.username&&values.username!==old.username)))return error('422 VALIDATION_ERROR · 编码或用户名不可修改');
 if(['user','member'].includes(type)&&!id&&!/^[a-z0-9_]{3,32}$/.test(values.username))return error('422 VALIDATION_ERROR · 用户名格式不正确');
 if(type==='user'&&!id&&!validPassword(values.initialPassword))return error('422 VALIDATION_ERROR · 密码须12–64位并含字母数字');
 if(['system','role'].includes(type)&&!/^[A-Z][A-Z0-9_]{1,39}$/.test(values.code))return error('422 VALIDATION_ERROR · 编码格式不正确');
 if(type==='permission'&&!/^[a-z][a-z0-9:_]{2,79}$/.test(values.code))return error('422 VALIDATION_ERROR · 权限码格式不正确');
 if(values.status&&!['ENABLED','DISABLED'].includes(values.status))return error('422 VALIDATION_ERROR · 状态无效');
 const array=type==='member'?db.members:db[{user:'users',system:'systems',role:'roles',permission:'permissions'}[type]];
 if(!id&&type!=='member'&&array.some(x=>(['role','permission'].includes(type)?x.sysId===state.sysId:true)&&(x.code||x.username)===(values.code||values.username)))return error('409 DUPLICATE · 编码或用户名已存在');
 if(type==='member'&&!id){const u=db.users.find(u=>u.username===values.username);if(!u)return error('账号不存在或不可添加，请联系平台管理员');if(members().some(m=>m.userId===u.id))return error('409 DUPLICATE · 已是本系统成员');values.userId=u.id;delete values.username;}
 if(type==='user'){
  if(!['true','false'].includes(values.platformAdmin))return error('422 VALIDATION_ERROR · 平台标记必须为布尔值');
  values.platformAdmin=values.platformAdmin==='true';
  if(old?.platformAdmin&&(!values.platformAdmin||values.status==='DISABLED')&&db.users.filter(u=>u.platformAdmin&&u.status==='ENABLED').length<=1)return error('409 PROTECTED_RESOURCE · 必须保留至少一位启用的平台管理员');
 }
 if(type==='permission'){
  values.type=old?.type||values.type;values.parentId=values.parentId||null;values.routePath=values.routePath||null;values.sortOrder=Number(values.sortOrder);
  if(!['MENU','ACTION'].includes(values.type)||!Number.isInteger(values.sortOrder)||values.sortOrder<0||values.sortOrder>9999)return error('422 VALIDATION_ERROR · 类型或排序不合法');
  if(values.type==='MENU'&&!routes.includes(values.routePath))return error('422 VALIDATION_ERROR · MENU必须选择受控本地路由');
  if(values.type==='ACTION'&&values.routePath)return error('422 VALIDATION_ERROR · ACTION不得设置页面路由');
  if(values.parentId&&!perms().some(p=>p.id===values.parentId&&p.type==='MENU'))return error('404 NOT_FOUND · 父节点必须为本系统菜单');
  let cur=values.parentId,seen=new Set();while(cur){if(cur===id||seen.has(cur))return error('409 MENU_CYCLE · 菜单父链不能成环');seen.add(cur);cur=perms().find(p=>p.id===cur)?.parentId;}
  if(old&&!actor().platformAdmin&&!codes().has(old.code))return error('403 FORBIDDEN · 当前权限超出可管理范围');
 }
 const before=old?structuredClone(old):null,initialPassword=values.initialPassword;delete values.initialPassword;
 if(old){delete values.username;delete values.code;}
 let target=old;
 if(old){
  if(type==='user'&&(old.status!==values.status||old.platformAdmin!==values.platformAdmin))old.authVersion++;
  Object.assign(old,values);old.version++;
  if(type==='system')old.policyVersion++;
 }else{
  target={id:newId(),version:1,status:'ENABLED',createdAt:new Date().toISOString(),...values};
  if(['member','role','permission'].includes(type))target.sysId=state.sysId;
  if(type==='member')delete target.id;
  if(type==='role')target.builtin=false;
  if(type==='system')target.policyVersion=1;
  if(type==='user'){target.authVersion=1;demoPasswords.set(target.id,initialPassword);}
  array.push(target);if(type==='role')db.rolePermissions[target.id]=[];
  if(type==='system')bootstrapSystem(target.id);
 }
 audit((id?'UPDATED_':'CREATED_')+type.toUpperCase(),id||target.userId||target.id,before,target,type.toUpperCase());
 modal.close();toast('已保存 · 仅更新本地演示数据');render();
}
function assignment(kind,id){
 const permission=kind==='roles'?'sys:member:assign':'sys:role:grant';if(!can(permission))return toast('403 FORBIDDEN');
 const target=getResource(kind==='roles'?'member':'role',id);if(!target)return toast('404 NOT_FOUND');
 if(!actor().platformAdmin&&(kind==='roles'?protectedMember(id):target.builtin))return toast('409 PROTECTED_RESOURCE');
 const current=kind==='roles'?assigned(id):db.rolePermissions[id]||[];
 const candidates=(kind==='roles'?roles():perms()).filter(x=>current.includes(x.id)||(x.status==='ENABLED'&&(actor().platformAdmin||(kind==='roles'?!x.builtin&&delegableRole(x.id):codes().has(x.code)))));
 showModal(modalHead(kind==='roles'?'分配成员角色':'分配角色权限')+`<p>${esc(target.displayName||target.name)} <span class="pill">version ${target.version}</span></p>`+api(`GET / PUT ${routeBase()}/${kind==='roles'?'members/'+id+'/roles':'roles/'+id+'/permissions'}`,'If-Match: "'+target.version+'"；完整集合替换，[] 表示清空')+`<form id="assignment-form" data-kind="${kind}" data-id="${id}" data-version="${target.version}" data-sys-id="${state.sysId}"><div class="checks">${candidates.map(x=>`<label class="check"><input type="checkbox" name="ids" value="${x.id}" ${current.includes(x.id)?'checked':''}><span>${esc(x.name)}<small class="mono">${esc(x.code)} ${x.type?'· '+x.type:''}${x.status==='DISABLED'?' · 已停用，取消勾选或先恢复启用':''}</small></span></label>`).join('')}</div><div class="choice-summary">菜单与操作独立授权。已选但停用的项目不会静默丢失，请明确取消或恢复后再保存。提交时重新检查归属、委派及版本。</div><div id="form-error" role="alert"></div><div class="dialog-actions">${btn('清空','clearChecks')}${btn('模拟版本冲突','conflict')}${btn('取消','close')}<button type="submit" class="primary">保存授权</button></div></form>`);
}
function saveAssignment(form){
 const kind=form.dataset.kind,id=form.dataset.id,target=getResource(kind==='roles'?'member':'role',id);
 if(!can(kind==='roles'?'sys:member:assign':'sys:role:grant'))return error('403 FORBIDDEN');
 if(form.dataset.sysId!==state.sysId)return error('403 SYSTEM_CONTEXT_MISMATCH');
 if(!target)return error('404 NOT_FOUND');
 if(String(target.version)!==form.dataset.version)return error('412 VERSION_CONFLICT · 资源已变化，当前勾选保留，请关闭并重新打开比对');
 const ids=[...new Set(new FormData(form).getAll('ids'))].sort((a,b)=>Number(a)-Number(b));
 const old=kind==='roles'?assigned(id):db.rolePermissions[id]||[],pool=kind==='roles'?roles():perms();
 if(ids.length>1000||ids.some(id=>!pool.some(x=>x.id===id)))return error('404 NOT_FOUND · 集合包含未知或异系统对象');
 if(ids.some(id=>pool.find(x=>x.id===id).status!=='ENABLED'))return error('409 RESOURCE_DISABLED · 请明确取消已停用项目');
 if(!actor().platformAdmin){
  if(kind==='roles'&&(protectedMember(id)||[...old,...ids].some(r=>roles().find(x=>x.id===r)?.builtin)))return error('409 PROTECTED_RESOURCE');
  if(kind!=='roles'&&target.builtin)return error('409 PROTECTED_RESOURCE');
  const allowed=kind==='roles'?[...old,...ids].every(delegableRole):[...old,...ids].every(p=>{const x=perms().find(x=>x.id===p);return x?.status!=='ENABLED'||codes().has(x.code);});
  if(!allowed)return error('403 FORBIDDEN · 原或新集合超出操作者可委派范围');
 }
 if(JSON.stringify([...old].sort((a,b)=>Number(a)-Number(b)))===JSON.stringify(ids)){modal.close();toast('集合未变化，未生成重复审计');return;}
 const oldVersion=String(target.version),key=kind==='roles'?'roleIds':'permissionIds';
 if(kind==='roles')db.userRoles[state.sysId+':'+id]=ids;else db.rolePermissions[id]=ids;
 target.version++;audit(kind==='roles'?'MEMBER_ROLES_REPLACED':'ROLE_PERMISSIONS_REPLACED',id,{[key]:[...old],version:oldVersion},{[key]:ids,version:String(target.version)},kind==='roles'?'MEMBER':'ROLE');
 modal.close();toast('授权已保存，已生成前后值审计');render();
}
function referenced(type,id){return type==='role'?Object.values(db.userRoles).some(ids=>ids.includes(id))||(db.rolePermissions[id]||[]).length>0:Object.values(db.rolePermissions).some(ids=>ids.includes(id))||perms().some(p=>p.parentId===id);}
function remove(type,id,confirm=false,expected=null){
 if(!['role','permission'].includes(type)||!writable(type,'delete'))return toast('403 FORBIDDEN');
 const x=getResource(type,id);if(!x)return toast('404 NOT_FOUND');
 if(confirm&&String(x.version)!==expected)return toast('412 VERSION_CONFLICT');
 if(x.builtin||referenced(type,id))return toast('409 RESOURCE_IN_USE · 对象仍有关联或受保护，请先解除引用或停用');
 if(!actor().platformAdmin&&(type==='role'?!delegableRole(id):!codes().has(x.code)))return toast('403 FORBIDDEN · 超出可管理范围');
 if(!confirm)return showModal(modalHead('删除确认')+`<p>确定删除 <strong>${esc(x.name)}</strong>？</p><div class="dialog-actions">${btn('取消','close')}${btn('确认删除','confirmDelete',`data-type="${type}" data-id="${id}" data-version="${x.version}"`,'danger')}</div>`);
 const key=type==='role'?'roles':'permissions';db[key]=db[key].filter(p=>p.id!==id);if(type==='role')delete db.rolePermissions[id];
 audit('DELETED_'+type.toUpperCase(),id,x,null,type.toUpperCase());modal.close();toast('已删除演示记录');render();
}

app.addEventListener('submit',e=>{
 if(e.target.id!=='login-form')return;e.preventDefault();
 const uid=document.getElementById('login-user').value,sid=document.getElementById('login-system').value||null,u=find(db.users,uid);let msg='';
 if(!u.platformAdmin&&!sid)msg='此身份没有平台控制台权限，请选择已加入的子系统';
 if(sid&&(!u.platformAdmin&&!db.members.some(m=>m.userId===uid&&m.sysId===sid&&m.status==='ENABLED')))msg='403 · 没有目标系统的有效成员资格';
 if(sid&&find(db.systems,sid)?.status!=='ENABLED')msg='系统已停用';
 if(u.status!=='ENABLED')msg='账号已停用';
 if(msg){document.getElementById('login-error').textContent=msg;return;}
 Object.assign(state,{uid,sysId:sid,authVersion:u.authVersion,page:'overview',keyword:'',logFilters:{},viewState:'normal'});render();
});
app.addEventListener('change',e=>{
 if(e.target.id==='api-toggle'){state.annotate=e.target.checked;render();}
 if(e.target.dataset.logFilter){state.logFilters[e.target.dataset.logFilter]=e.target.value;render();}
 if(e.target.id==='system-picker'){
  const sid=e.target.value||null,u=actor();
  if(!sessionValid()||(!sid&&!u.platformAdmin)||(sid&&(find(db.systems,sid)?.status!=='ENABLED'||(!u.platformAdmin&&!db.members.some(m=>m.sysId===sid&&m.userId===u.id&&m.status==='ENABLED'))))){toast('403 · 没有目标上下文资格');render();return;}
  state.sysId=sid;state.page='overview';state.keyword='';state.logFilters={};state.viewState='normal';toast('已切换上下文，重新装载菜单与权限（模拟）');render();
 }
});
app.addEventListener('input',e=>{if(e.target.id==='search'){state.keyword=e.target.value.slice(0,64);const pos=e.target.selectionStart;render();const input=document.getElementById('search');input?.focus();input?.setSelectionRange(pos,pos);}});
document.addEventListener('click',e=>{
 const b=e.target.closest('button');if(!b)return;
 if(b.dataset.page){state.page=b.dataset.page;state.keyword='';state.viewState='normal';render();return;}
 const a=b.dataset.action,id=b.dataset.id,type=b.dataset.type;
 if(a==='close'){modal.close();return;}
 if(a==='logout'){modal.close();state.uid=null;state.sysId=null;state.page='overview';state.viewState='normal';render();return;}
 if(a==='home'){state.page='overview';render();}
 if(a==='clearLogFilters'){state.logFilters={};render();}
 if(a==='enter'){if(!isPlatform())return toast('403 FORBIDDEN');if(find(db.systems,id)?.status!=='ENABLED')return toast('系统已停用');state.sysId=id;state.page='overview';render();}
 if(a==='create'||a==='edit')editForm(type,a==='edit'?id:null);
 if(a==='assignRoles')assignment('roles',id);
 if(a==='assignPerms')assignment('permissions',id);
 if(a==='clearChecks'){modal.querySelectorAll('input[type=checkbox]').forEach(x=>x.checked=false);updateAssignmentDiff(modal.querySelector('#assignment-form'));}
 if(a==='conflict'||a==='resourceConflict'){
  const f=modal.querySelector('form');const x=f.id==='assignment-form'?getResource(f.dataset.kind==='roles'?'member':'role',f.dataset.id):getResource(f.dataset.type,f.dataset.id);
  x.version++;error('已模拟另一位管理员提交；当前输入保留，再点保存将收到412。');
 }
 if(a==='delete')remove(type,id);
 if(a==='confirmDelete')remove(type,id,true,b.dataset.version);
 if(a==='states')showModal(modalHead('演示页面状态')+`<p>检查列表的空状态与服务错误。</p><div class="dialog-actions">${btn('空状态','empty')}${btn('错误状态','errorState')}${btn('正常状态','normal')}</div>`);
 if(['empty','errorState','normal'].includes(a)){state.viewState={empty:'empty',errorState:'error',normal:'normal'}[a];modal.close();render();}
 if(a==='log'){
  const l=find(db.logs,id);if(!l||(isPlatform()?false:l.sysId!==state.sysId)||!access())return toast('404 NOT_FOUND');
  showModal(modalHead('变更前后值')+`<p class="mono">${esc(l.action)}</p><p class="muted">${esc(l.actorName)} · ${esc(l.requestId)}</p><div class="diff"><div><h3>变更前</h3><pre>${esc(JSON.stringify(l.before,null,2))}</pre></div><div><h3>变更后</h3><pre>${esc(JSON.stringify(l.after,null,2))}</pre></div></div><div class="dialog-actions">${btn('完成','close')}</div>`);
 }
 if(a==='password')showModal(modalHead('修改本人密码')+`<form id="password-form"><label class="field"><span>当前密码（原型模拟）</span><input name="old" type="password" maxlength="64" required></label><label class="field"><span>新密码，12–64位且含字母数字</span><input name="new" type="password" minlength="12" maxlength="64" required></label><p class="help">口令仅在当前页面内存中生效，刷新恢复；不写入日志或演示数据文件。</p><div id="form-error" role="alert"></div><div class="dialog-actions">${btn('取消','close')}<button class="primary" type="submit">确认并重新登录</button></div></form>`);
});
modal.addEventListener('change',e=>{const f=e.target.closest('#assignment-form');if(f)updateAssignmentDiff(f);});
modal.addEventListener('submit',e=>{
 e.preventDefault();if(e.target.id==='resource-form')saveResource(e.target);if(e.target.id==='assignment-form')saveAssignment(e.target);
 if(e.target.id==='password-form'){
  if(!sessionValid())return error('401 UNAUTHENTICATED');const f=new FormData(e.target),p=f.get('new');
  if(f.get('old')!==demoPasswords.get(state.uid))return error('422 VALIDATION_ERROR · 当前密码不正确');
  if(!validPassword(p))return error('422 VALIDATION_ERROR · 新密码须12–64位并包含字母数字');
  const u=actor();demoPasswords.set(u.id,p);u.authVersion++;u.version++;
  audit('PASSWORD_CHANGED',u.id,{credentialChanged:false},{credentialChanged:true},'USER',false);
  modal.close();state.uid=null;state.sysId=null;toast('模拟密码已修改，请使用新口令登录；刷新恢复初始数据');render();
 }
});
render();
