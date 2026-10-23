"""Check design cross-references; this does not replace a MySQL/server acceptance test."""
from pathlib import Path
import ast,csv,json,re,hashlib,xml.etree.ElementTree as ET
from contract import OPS
ROOT=Path(__file__).resolve().parents[1]
(ROOT/'validation').mkdir(exist_ok=True)
if not (ROOT/'validation/report.md').exists():
    (ROOT/'validation/report.md').write_text('验证执行中\n',encoding='utf-8')
checks=[]
def check(name,ok,detail=''):
    checks.append((name,bool(ok),detail))

spec=json.loads((ROOT/'api/openapi.json').read_text(encoding='utf-8'))
schemas=spec['components']['schemas']
references=[]
def visit(x):
    if isinstance(x,dict):
        if '$ref' in x:references.append(x['$ref'])
        for v in x.values():visit(v)
    elif isinstance(x,list):
        for v in x:visit(v)
visit(spec)
check('OpenAPI 内部模型引用',all(r.startswith('#/components/schemas/') and r.rsplit('/',1)[1] in schemas for r in references),f'{len(references)} 个引用')
check('模型必填字段存在',all(set(s.get('required',[]))<=set(s.get('properties',{})) for s in schemas.values()),f'{len(schemas)} 个模型')
ids=[];path_ok=True;etag_ok=True
for path,ops in spec['paths'].items():
    expected=set(re.findall(r'{(\w+)}',path))
    for method,op in ops.items():
        ids.append(op['operationId'])
        params={p['name'] for p in op['parameters'] if p['in']=='path' and p.get('required')}
        path_ok &= params==expected
        if method in ['patch','put'] and path!='/api/v1/me/password':
            etag_ok &= any(p['name']=='If-Match' and p.get('required') for p in op['parameters'])
check('operationId 唯一与RBAC0端点数量',len(ids)==len(set(ids))==len(OPS)==38,f'{len(ids)} 个端点')
check('路径参数完整且必填',path_ok)
check('并发修改接口要求 If-Match',etag_ok)
check('仅登录接口公开',sum(op.get('security')==[] for ops in spec['paths'].values() for op in ops.values())==1)
safe_views=[s for name,s in schemas.items() if name.endswith('View') or name.endswith('Page')]
check('响应模型没有密码或token敏感字段',all(not any('password' in k.lower() or 'hash' in k.lower() for k in s.get('properties',{})) for s in safe_views))

sql=(ROOT/'database/001_rbac0.sql').read_text(encoding='utf-8')
future=''
tables=re.findall(r'CREATE TABLE (\w+)',sql+future)
check('仅11张RBAC0表且无扩展SQL',len(re.findall(r'CREATE TABLE ',sql))==11 and not (ROOT/'database/002_rbac1_rbac3.sql').exists())
sql_statements=re.sub(r'--[^\n]*','',sql+future)
check('SQL 无破坏性建表操作',not re.search(r'\b(DROP|TRUNCATE)\b',sql_statements,re.I))
check('授权关联含同系统复合外键',all(x in sql for x in [
    'FOREIGN KEY (sys_id,user_id) REFERENCES iam_member(sys_id,user_id)',
    'FOREIGN KEY (sys_id,role_id) REFERENCES iam_role(sys_id,id)',
    'FOREIGN KEY (sys_id,permission_id) REFERENCES iam_permission(sys_id,id)']))
fk_valid=True
for target,cols in re.findall(r'REFERENCES (\w+)\(([^)]+)\)',sql+future):
    match=re.search(r'CREATE TABLE '+target+r'\s*\((.*?)\) ENGINE',sql+future,re.S)
    fields=set(re.findall(r'^\s*(\w+)\s+(?:BIGINT|VARCHAR|CHAR|TINYINT|INT|DATETIME|BOOLEAN|JSON)',match[1],re.M)) if match else set()
    fk_valid &= set(cols.split(','))<=fields
check('外键引用的表和列存在',fk_valid)
seed=(ROOT/'database/003_demo_seed.sql').read_text(encoding='utf-8')
check('演示 SQL 不含可登录明文密码',seed.count('!DEMO_LOGIN_DISABLED_REPLACE_WITH_HASH!')==5)
text=(ROOT/'prototype/demo-data.js').read_text(encoding='utf-8')
data=json.loads(text.split('window.RBAC_DEMO = ',1)[1].rsplit(';',1)[0])
scope=True
rolemap={r['id']:r for r in data['roles']};pmap={p['id']:p for p in data['permissions']}
for key,role_ids in data['userRoles'].items():
    sid,uid=key.split(':')
    scope &= all(rolemap[r]['sysId']==sid for r in role_ids)
    scope &= any(m['sysId']==sid and m['userId']==uid for m in data['members'])
for rid,pids in data['rolePermissions'].items():scope &= all(pmap[p]['sysId']==rolemap[rid]['sysId'] for p in pids)
check('演示授权数据无跨系统关联',scope)
codes={p['code'] for p in data['permissions']}
check('端点权限码可对应权限目录',all(o['permission'] in codes or o['permission'] in ['PUBLIC','AUTHENTICATED','SYSTEM_MEMBER','PLATFORM_ADMIN'] for o in OPS))
trace=list(csv.DictReader((ROOT/'docs/traceability.csv').open(encoding='utf-8-sig')))
check('FR-01至FR-11均有追踪', {r['需求'] for r in trace}=={f'FR-{i:02d}' for i in range(1,12)} and all(r['对应API'] and r['模块'] and r['验收用例'] for r in trace))
includes=re.findall(r'include::([^\[]+)',(ROOT/'docs/index.adoc').read_text(encoding='utf-8'))
check('AsciiDoc include 文件存在',all((ROOT/'docs'/x).exists() for x in includes),f'{len(includes)} 份文档')
missing=[]
for path in [ROOT/'docs/index.html',ROOT/'prototype/index.html']:
    source=path.read_text(encoding='utf-8')
    for target in re.findall(r'(?:href|src)="([^"#]+)"',source):
        if re.match(r'(https?:|data:|javascript:)',target):continue
        if not (path.parent/target).exists():missing.append(str(path.relative_to(ROOT))+': '+target)
check('HTML 本地链接与资源存在',not missing,'; '.join(missing))
svg_ok=True
for path in (ROOT/'docs/images').glob('*.svg'):
    try:ET.parse(path)
    except ET.ParseError:svg_ok=False
check('SVG 图形 XML 可解析',svg_ok)
check('UML 源文件齐全',len(list((ROOT/'uml').glob('*.puml')))==5)

operations=[op for ops in spec['paths'].values() for op in ops.values()]
check('所有端点仅标记RBAC0',all(o['x-stage']=='RBAC0' for o in operations))
forbidden=['/junior-roles','/constraint-checks','/mutex-sets','/member-limit','/prerequisites','/active-roles']
check('无角色继承和职责分离端点',not any(x in p for p in spec['paths'] for x in forbidden))
reachable=set()
def walk_schema(name):
    if name in reachable:return
    reachable.add(name)
    def walk(x):
        if isinstance(x,dict):
            if '$ref' in x:walk_schema(x['$ref'].rsplit('/',1)[1])
            for v in x.values():walk(v)
        elif isinstance(x,list):
            for v in x:walk(v)
    walk(schemas[name])
for ops in spec['paths'].values():
    for op in ops.values():
        def refs_in(x):
            if isinstance(x,dict):
                if '$ref' in x:walk_schema(x['$ref'].rsplit('/',1)[1])
                for v in x.values():refs_in(v)
            elif isinstance(x,list):
                for v in x:refs_in(v)
        refs_in(op)
check('没有残留的未引用模型',reachable==set(schemas),', '.join(set(schemas)-reachable))
check('成员角色显示字段在响应中定义且必填',{'roleIds','roleNames','accountStatus'}<=set(schemas['MemberView']['required']))
check('授权接口独立返回可用候选及原已选项目',all('options' in schemas[n]['required'] for n in ['RoleIdsView','PermissionIdsView']) and 'assignable' in schemas['AssignmentRoleOption']['required'] and 'grantable' in schemas['AssignmentPermissionOption']['required'])
member_sort=next(p for p in spec['paths']['/api/v1/systems/{sysId}/members']['get']['parameters'] if p['name']=='sort')['schema']
check('成员排序字段存在于成员模型',member_sort['default']=='userId' and set(member_sort['enum'])<=set(schemas['MemberView']['properties']))
check('MENU与ACTION创建分支完整',len(schemas['PermissionCreate'].get('oneOf',[]))==2 and 'routePath' in schemas['PermissionCreate']['oneOf'][0]['required'])
js=(ROOT/'prototype/app.js').read_text(encoding='utf-8')
prototype_routes=set(ast.literal_eval(re.search(r'const routes=(\[[^;]+\]);',js)[1]))
api_routes={r for r in schemas['PermissionCreate']['properties']['routePath']['enum'] if r is not None}
sql_routes=set(re.findall(r"'([^']+)'",re.search(r'route_path IN \(([^)]+)\)',sql)[1]))
check('原型API与SQL的菜单路由白名单一致',prototype_routes==api_routes==sql_routes)
check('SQL路由检查显式拒绝MENU空路由',"type='MENU' AND route_path IS NOT NULL" in sql)
check('SQL不残留角色子集激活状态','role_mode' not in sql and 'EXPLICIT' not in sql)
check('原型不残留扩展页面和权限',not any(x in js for x in ['function hierarchy','function constraints','checkEdge','checkConstraint']) and not any('hierarchy' in p['code'] or 'constraint' in p['code'] for p in data['permissions']))
check('演示系统角色权限数量正确',len(data['systems'])==2 and len(data['roles'])==8 and len(data['permissions'])==36)
check('角色权限36项均与数据库种子对应',all(p['code'] in seed for p in data['permissions']))
check('原型与SQL共享初始化审计',"INSERT INTO iam_audit_log" in seed and all(l['requestId'] in seed for l in data['logs']))
check('SQL显式UTC与会话时间检查',"SET time_zone = '+00:00'" in sql and "SET time_zone = '+00:00'" in seed and 'expires_at > created_at' in sql)
review=(ROOT/'database/004_rbac0_review.sql').read_text(encoding='utf-8')
check('数据库核对脚本只读',not re.search(r'\b(INSERT|UPDATE|DELETE|DROP|TRUNCATE|ALTER|CREATE)\b',re.sub(r'--[^\n]*','',review),re.I))
check('全部API都可在追踪矩阵中找到',all((o['method'].upper()+' '+o['path']) in '\n'.join(r['对应API'] for r in trace) for o in OPS))
check('字典覆盖全部且仅RBAC0表',set(re.findall(r'^## (iam_\w+)$',(ROOT/'database/data-dictionary.md').read_text(encoding='utf-8'),re.M))==set(tables))

check('可选字段模型不输出非法required空数组',all('required' not in s or len(s['required'])>0 for s in schemas.values()))
division=(ROOT/'docs/07-RBAC0六人分工.adoc').read_text(encoding='utf-8')
planned=re.findall(r'^\|((?:docs|uml)/detail-design/([A-F])-[^|\n]+?)\s*\|',division,re.M)
check('六人各5份明确设计文件且无源码测试构建文件',len(planned)==30 and len({p for p,_ in planned})==30 and all(sum(a==person for _,a in planned)==5 for person in 'ABCDEF') and all(Path(p).suffix in ['.adoc','.puml'] for p,_ in planned))
manifest=json.loads((ROOT/'docs/sources/source-manifest.json').read_text(encoding='utf-8'))
pdf_sources=[s for s in manifest if s['Name'].endswith('.pdf')]
check('两份课程PDF副本与SHA256可核对',len(pdf_sources)==2 and all(hashlib.sha256((ROOT/'docs/sources'/s['Name']).read_bytes()).hexdigest()==s['SHA256'] for s in pdf_sources))
api_report=json.loads((ROOT/'validation/openapi-report.json').read_text(encoding='utf-8'))
check('OpenAPI标准校验通过且对应本版38接口',api_report.get('passed') and api_report.get('operations')==38 and api_report.get('version')==spec['info']['version'] and api_report.get('inputSha256')==hashlib.sha256((ROOT/'api/openapi.json').read_bytes()).hexdigest())
build=json.loads((ROOT/'validation/vue-build.json').read_text(encoding='utf-8'))
check('Vue构建通过且输入与离线产物未过期',build.get('passed') and all(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==digest for p,digest in build['inputSha256'].items()) and all(hashlib.sha256((ROOT/a['path']).read_bytes()).hexdigest()==a['sha256'] for a in build['artifacts']))
vue_report=json.loads((ROOT/'validation/vue-browser-report.json').read_text(encoding='utf-8'))
check('Vue离线交互全部通过且无网络或脚本异常',bool(vue_report.get('results')) and all(r['passed'] for r in vue_report['results']) and not vue_report['errors'] and not vue_report['requests'])

passed=sum(1 for _,ok,_ in checks if ok)
report=['# RBAC0设计包验证记录','','日期：2026-10-21（Asia/Shanghai）','',
        f'静态设计一致性检查：{passed}/{len(checks)} 通过。','',
        '|检查|结果|说明|','|---|---|---|']
report.extend(f'|{name}|{"通过" if ok else "失败"}|{detail}|' for name,ok,detail in checks)
report.extend(['','浏览器交互验证另见 [browser-report.json](browser-report.json) 和原型截图。',
               'Vue离线原型另见 [vue-browser-report.json](vue-browser-report.json)；实际类型/构建记录见 [vue-build.json](vue-build.json)；完整OpenAPI规范校验见 [openapi-report.json](openapi-report.json)。',
               '', '未执行：真实 MySQL 建表和种子装载、服务端集成测试、权限攻防、真实并发事务、JaCoCo、服务器部署和压力测试。',
               '当前环境未发现 MySQL/Docker 可执行文件；以上 SQL 检查属于结构和引用检查，不能代替目标数据库执行。',
               '音频没有转写，智能纪要未与原录音逐字核验。'])
browser_report=ROOT/'validation/browser-report.json'
if browser_report.exists():
    b=json.loads(browser_report.read_text(encoding='utf-8'))
    report.insert(6,'浏览器原型交互检查：'+str(sum(r['passed'] for r in b['results']))+'/'+str(len(b['results']))+' 通过，脚本异常 '+str(len(b['errors']))+'。此结果仅针对模拟原型。\n')
(ROOT/'validation').mkdir(exist_ok=True)
(ROOT/'validation/report.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
print(json.dumps({'passed':passed,'total':len(checks),'failed':[name for name,ok,_ in checks if not ok]},ensure_ascii=False))
if passed!=len(checks):raise SystemExit(1)
