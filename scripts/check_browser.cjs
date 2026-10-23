// RBAC0 prototype regression checks. No production server or MySQL is tested here.
const fs=require('fs'),path=require('path'),{pathToFileURL}=require('url');
const {chromium}=require('C:/Users/123/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const root=path.resolve(__dirname,'..'),results=[],errors=[];
let browser,page;
const record=(name,passed)=>{results.push({name,passed:!!passed});if(!passed)throw new Error(name);};
const click=name=>page.getByRole('button',{name,exact:true}).click();
const close=()=>page.getByRole('button',{name:'关闭',exact:true}).first().click();
async function login(uid,sid,password='DemoOnly12345'){
 await page.locator('#login-user').selectOption(uid);await page.locator('#login-system').selectOption(sid);
 await page.locator('#login-password').fill(password);await click('进入演示');
}
async function snapshot(name){await page.locator('.toast').waitFor({state:'hidden'});await page.screenshot({path:path.join(root,'docs/images/'+name+'.png'),fullPage:true});}
async function createRole(code,name){await click('新建角色');await page.locator('[name=code]').fill(code);await page.locator('[name=name]').fill(name);await click('保存');}
async function platform(){await page.locator('#system-picker').selectOption('');}
async function system(sid='1'){await page.locator('#system-picker').selectOption(sid);}
async function formError(text){return (await page.locator('#form-error').textContent()).includes(text);}

(async()=>{
 browser=await chromium.launch({executablePath:'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',headless:true});
 page=await browser.newPage({viewport:{width:1440,height:1050},deviceScaleFactor:1});
 page.setDefaultTimeout(10000);page.on('pageerror',e=>errors.push(e.message));
 await page.goto(pathToFileURL(path.join(root,'prototype/index.html')).href);
 await snapshot('prototype-login');
 await login('1','','wrong');record('错误演示口令拒绝',await page.locator('#login-error').textContent().then(t=>t.includes('凭据不匹配')));
 await login('2','');record('非平台身份不能进入平台',await page.locator('#login-error').textContent().then(t=>t.includes('没有平台')));
 await login('1','');await click('子系统管理');record('初始两个子系统',await page.locator('tbody tr').count()===2);
 record('导航仅有RBAC0功能',await page.getByRole('button',{name:/角色继承|约束预检/}).count()===0);

 await click('全局账号');const rootRow=page.locator('tbody tr').filter({hasText:'platform_admin'});
 await rootRow.getByRole('button',{name:'编辑',exact:true}).click();await page.locator('[name=status]').selectOption('DISABLED');await click('保存');
 record('最后平台管理员不能停用',await formError('必须保留'));await close();
 const chenUser=page.locator('tbody tr').filter({hasText:'chenyu'});await chenUser.getByRole('button',{name:'编辑',exact:true}).click();
 await page.locator('[name=displayName]').fill('未提交名字');await click('模拟版本冲突');await click('保存');
 record('普通编辑也检查版本并保留输入',await formError('412')&&await page.locator('[name=displayName]').inputValue()==='未提交名字');await close();
 await click('新建账号');await page.locator('[name=username]').fill('demo_user');await page.locator('[name=displayName]').fill('测试用户');
 await page.locator('[name=initialPassword]').fill('UserPassword123');await click('保存');
 record('创建账号成功',await page.locator('tbody').textContent().then(t=>t.includes('测试用户')));
 const newUserId=await page.evaluate(()=>db.users.find(u=>u.username==='demo_user').id);

 await click('子系统管理');await click('新建子系统');await page.locator('[name=code]').fill('TEST_SYS');await page.locator('[name=name]').fill('测试子系统');await click('保存');
 const newSysId=await page.locator('#system-picker option').filter({hasText:'测试子系统'}).getAttribute('value');
 await system(newSysId);await click('角色管理');record('新系统初始化受保护管理员角色',await page.locator('tbody tr').count()===1&&await page.locator('tbody').textContent().then(t=>t.includes('SYS_ADMIN')));
 await click('权限目录');record('新系统初始化18项RBAC0权限',await page.locator('tbody tr').count()===18);
 await click('角色管理');await createRole('READER','新系统同编码角色');record('角色编码在不同系统可重复',await page.locator('tbody').textContent().then(t=>t.includes('新系统同编码角色')));

 await system();await page.locator('#api-toggle').check();await click('成员管理');
 record('成员页包含API标注与全局状态',await page.locator('.api-note').textContent().then(t=>t.includes('/systems/1/members'))&&await page.locator('tbody tr').filter({hasText:'lichen'}).textContent().then(t=>t.includes('全局账号：停用')));
 await snapshot('prototype-members-api');
 await click('添加成员');await page.locator('[name=username]').fill('demo_user');await page.locator('[name=displayName]').fill('测试成员');await click('保存');
 record('按已有账号添加成员',await page.locator('tbody').textContent().then(t=>t.includes('测试成员')));
 await page.locator('tbody tr').filter({hasText:'测试成员'}).getByRole('button',{name:'分配角色',exact:true}).click();
 await page.locator('label.check').filter({hasText:'READER'}).locator('input').check();await click('保存授权');

 await click('角色管理');await createRole('READER','重复角色');record('同系统重复角色编码拒绝',await formError('409'));await close();
 await createRole('TEST_ROLE','验收角色');const roleRow=page.locator('tbody tr').filter({hasText:'验收角色'});
 await roleRow.getByRole('button',{name:'分配权限',exact:true}).click();
 await page.locator('label.check').filter({hasText:'menu:audit'}).locator('input').check();await page.locator('label.check').filter({hasText:'sys:audit:read'}).locator('input').check();
 record('授权弹窗显示实时新增差集',await page.locator('#assignment-diff').textContent().then(t=>t.includes('新增 2 项')));
 await snapshot('prototype-grant-api');await click('保存授权');record('角色权限集合可保存',await roleRow.textContent().then(t=>t.includes('2 项')));
 await roleRow.getByRole('button',{name:'分配权限',exact:true}).click();await click('模拟版本冲突');await click('保存授权');record('授权冲突返回412',await formError('412'));await close();
 await createRole('CLEAR_ROLE','清空测试角色');const clearRole=page.locator('tbody tr').filter({hasText:'清空测试角色'});
 await clearRole.getByRole('button',{name:'分配权限',exact:true}).click();await page.locator('label.check').filter({hasText:'menu:members'}).locator('input').check();
 await page.locator('#assignment-form').evaluate(f=>f.insertAdjacentHTML('beforeend','<input type="checkbox" checked name="ids" value="1001">'));await click('保存授权');
 record('重复权限ID去重后仅一项',await clearRole.textContent().then(t=>t.includes('1 项')));
 await clearRole.getByRole('button',{name:'分配权限',exact:true}).click();await click('清空');await click('保存授权');record('空集合明确清空权限',await clearRole.textContent().then(t=>t.includes('0 项')));
 await clearRole.getByRole('button',{name:'删除',exact:true}).click();await click('确认删除');record('未引用业务角色可删除',await clearRole.count()===0);
 await roleRow.getByRole('button',{name:'编辑',exact:true}).click();await page.locator('#resource-form').evaluate(f=>f.insertAdjacentHTML('beforeend','<input name="sysId" value="2">'));await click('保存');record('拒绝修改表单注入的系统字段',await formError('不允许的字段'));await close();
 await click('成员管理');const chen=page.locator('tbody tr').filter({hasText:'chenyu'});
 await chen.getByRole('button',{name:'分配角色',exact:true}).click();
 await page.locator('#assignment-form').evaluate(f=>f.insertAdjacentHTML('beforeend','<input type="checkbox" checked name="ids" value="201">'));
 await click('保存授权');record('注入异系统角色ID整批拒绝',await formError('404'));await close();
 await chen.getByRole('button',{name:'分配角色',exact:true}).click();await page.locator('label.check').filter({hasText:'验收角色'}).locator('input').check();await click('保存授权');
 record('成员角色指派成功',await chen.textContent().then(t=>t.includes('验收角色')));
 const beforeNoop=await page.evaluate(()=>db.logs.length);await chen.getByRole('button',{name:'分配角色',exact:true}).click();await click('保存授权');
 record('相同授权集合不生成重复审计',beforeNoop===await page.evaluate(()=>db.logs.length));

 await click('审计日志');await page.locator('[data-log-filter=action]').selectOption('MEMBER_ROLES_REPLACED');
 record('审计按操作类型过滤',await page.locator('tbody tr').count()===3);
 await page.locator('[data-log-filter=from]').fill('2027-01-01');record('日期必须成对',await page.locator('.content [role=alert]').textContent().then(t=>t.includes('成对')));
 await page.locator('[data-log-filter=to]').fill('2027-01-01');record('有效日期区间支持空结果',await page.locator('.empty').textContent().then(t=>t.includes('暂无匹配')));
 await page.locator('[data-log-filter=from]').fill('2027-01-02');record('日期逆序拒绝',await page.locator('.content [role=alert]').textContent().then(t=>t.includes('晚于')));
 await page.locator('[data-log-filter=from]').fill('2026-01-01');await page.locator('[data-log-filter=to]').fill('2026-10-21');record('超过90日拒绝',await page.locator('.content [role=alert]').textContent().then(t=>t.includes('90')));
 await click('清除筛选');await page.locator('tbody tr').first().getByRole('button',{name:'前后值',exact:true}).click();
 record('审计字段与API一致并含版本',await page.locator('.diff').textContent().then(t=>t.includes('roleIds')&&t.includes('version')));
 await snapshot('prototype-audit');await close();

 await click('权限目录');const codes=await page.locator('tbody tr').allTextContents();
 record('权限目录按菜单父子顺序排列',codes.findIndex(t=>t.includes('menu:members'))<codes.findIndex(t=>t.includes('sys:member:read'))&&codes.findIndex(t=>t.includes('sys:member:read'))<codes.findIndex(t=>t.includes('menu:roles')));
 await click('新建权限');await page.locator('[name=code]').fill('test:action');await page.locator('[name=name]').fill('测试操作');await page.locator('[name=type]').selectOption('ACTION');await page.locator('[name=routePath]').selectOption('/members');await click('保存');
 record('ACTION禁止配置页面路由',await formError('ACTION'));await page.locator('[name=routePath]').selectOption('');await page.locator('[name=sortOrder]').fill('8');await click('保存');
 record('ACTION合法创建且排序值保存',await page.locator('tbody').textContent().then(t=>t.includes('test:action')));
 const memberMenu=page.locator('tbody tr').filter({hasText:'menu:members'});await memberMenu.getByRole('button',{name:'编辑',exact:true}).click();await page.locator('[name=parentId]').selectOption('1002');await click('保存');
 const roleMenu=page.locator('tbody tr').filter({hasText:'menu:roles'});await roleMenu.getByRole('button',{name:'编辑',exact:true}).click();await page.locator('[name=parentId]').selectOption('1001');await click('保存');record('菜单父链成环拒绝',await formError('MENU_CYCLE'));await close();
 await memberMenu.getByRole('button',{name:'编辑',exact:true}).click();await page.locator('[name=parentId]').selectOption('');await click('保存');

 await click('退出');await login('2','1');record('系统管理员不能进入全局账号管理',await page.getByRole('button',{name:'全局账号',exact:true}).count()===0);
 await click('角色管理');const builtin=page.locator('tbody tr').filter({hasText:'SYS_ADMIN'});record('系统管理员不能修改内置角色',await builtin.getByRole('button',{name:'编辑',exact:true}).count()===0&&await builtin.getByRole('button',{name:'分配权限',exact:true}).count()===0);
 await click('退出');await login('4','1');record('新授权影响普通用户菜单',await page.getByRole('button',{name:'审计日志',exact:true}).count()===1);
 await click('成员管理');record('只读成员不显示写操作',await page.getByRole('button',{name:'添加成员',exact:true}).count()===0&&await page.getByRole('button',{name:'分配角色',exact:true}).count()===0);
 await system('2');record('切换系统后权限不串用',await page.getByRole('button',{name:'审计日志',exact:true}).count()===0&&await page.locator('.heading').textContent().then(t=>t.includes('RESEARCH')));
 await page.setViewportSize({width:375,height:1000});await click('个人中心');await snapshot('prototype-mobile');record('375px无页面横向溢出',await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
 await click('成员管理');await click('演示状态');await click('空状态');record('空状态可恢复',await page.locator('.empty').textContent().then(t=>t.includes('还没有')));await click('恢复演示数据');
 await click('演示状态');await click('错误状态');record('503状态可重试',await page.locator('.error').textContent().then(t=>t.includes('503')));await click('重试');

 await page.setViewportSize({width:1440,height:1050});await click('退出');await login('5','1');record('全局停用账号不能登录',await page.locator('#login-error').textContent().then(t=>t.includes('账号已停用')));
 await login(newUserId,'1');record('新用户不能用初始通用口令登录',await page.locator('#login-error').textContent().then(t=>t.includes('凭据不匹配')));
 await login(newUserId,'1','UserPassword123');await click('个人中心');await click('修改密码');await page.locator('[name=old]').fill('wrong');await page.locator('[name=new]').fill('NewUserPassword987');await click('确认并重新登录');record('改密必须验证旧演示口令',await formError('当前密码不正确'));
 await page.locator('[name=old]').fill('UserPassword123');await click('确认并重新登录');record('改密后要求重新登录',await page.locator('#login-form').count()===1);
 await login(newUserId,'1','UserPassword123');record('改密后旧口令失效',await page.locator('#login-error').textContent().then(t=>t.includes('凭据不匹配')));
 await login(newUserId,'1','NewUserPassword987');record('改密后新口令可登录',await page.locator('#system-picker').count()===1);
 record('审计不包含任何新建或修改口令',await page.evaluate(()=>!JSON.stringify(db.logs).includes('NewUserPassword987')&&!JSON.stringify(db.logs).includes('UserPassword123')));

 await click('退出');await login('1','1');await click('角色管理');await roleRow.getByRole('button',{name:'编辑',exact:true}).click();await page.locator('[name=status]').selectOption('DISABLED');await click('保存');
 await click('成员管理');await chen.getByRole('button',{name:'分配角色',exact:true}).click();const disabled=page.locator('label.check').filter({hasText:'验收角色'});
 record('原指派停用角色不被隐藏或静默取消',await disabled.locator('input').isChecked()&&await disabled.textContent().then(t=>t.includes('已停用')));
 await click('保存授权');record('含停用角色的候选集合要求明确处理',await formError('RESOURCE_DISABLED'));await disabled.locator('input').uncheck();await click('保存授权');
 await click('退出');await login('4','1');record('撤销角色后审计菜单消失',await page.getByRole('button',{name:'审计日志',exact:true}).count()===0);
 await click('退出');await login('1','1');await click('成员管理');await chen.getByRole('button',{name:'编辑',exact:true}).click();await page.locator('[name=status]').selectOption('DISABLED');await click('保存');
 await click('退出');await login('4','1');record('本系统成员停用后拒绝登录',await page.locator('#login-error').textContent().then(t=>t.includes('成员资格')));
 await login('4','2');record('成员停用不影响另一系统',await page.locator('#system-picker').count()===1);
 await click('退出');await login('1','');await click('子系统管理');await page.locator('tbody tr').filter({hasText:'RESEARCH'}).getByRole('button',{name:'编辑',exact:true}).click();await page.locator('[name=status]').selectOption('DISABLED');await click('保存');
 await click('退出');await login('4','2');record('系统停用后拒绝进入',await page.locator('#login-error').textContent().then(t=>t.includes('系统已停用')));

 await page.goto(pathToFileURL(path.join(root,'docs/index.html')).href);await page.setViewportSize({width:1440,height:1050});
 record('阅读版章节与源入口一致且全部图片加载',await page.locator('main section').count()===(fs.readFileSync(path.join(root,'docs/index.adoc'),'utf8').match(/include::/g)||[]).length&&await page.locator('nav').textContent().then(t=>t.includes('RBAC0 六人'))&&await page.locator('figure img').evaluateAll(xs=>xs.every(x=>x.complete&&x.naturalWidth>0)));
 await page.screenshot({path:path.join(root,'docs/images/design-reader.png')});
 for(const name of ['er','use-cases','architecture','assignment-sequence','states']){await page.setViewportSize({width:1100,height:760});await page.goto(pathToFileURL(path.join(root,'docs/images/'+name+'.svg')).href);await page.screenshot({path:path.join(root,'validation/'+name+'-preview.png')});}
 record('浏览器无脚本异常',errors.length===0);
})().catch(e=>{errors.push(e.stack||String(e));process.exitCode=1;}).finally(async()=>{
 if(errors.length&&page)await page.screenshot({path:path.join(root,'validation/browser-failure.png'),fullPage:true}).catch(()=>{});
 fs.writeFileSync(path.join(root,'validation/browser-report.json'),JSON.stringify({date:'2026-10-21',scope:'RBAC0',browser:'Installed Edge / headless',results,errors,limitation:'仅模拟原型与文档，未执行生产API或MySQL。'},null,2));
 console.log(JSON.stringify({passed:results.filter(r=>r.passed).length,total:results.length,errors}));await browser?.close();
});
