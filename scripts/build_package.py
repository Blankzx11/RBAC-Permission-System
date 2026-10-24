"""Rebuild generated design artifacts using the standard library only."""
from pathlib import Path
import csv
import json
import re
import html
from contract import build_openapi, OPS

ROOT=Path(__file__).resolve().parents[1]
def write(path,content):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content,encoding='utf-8')
def dump(path,content):write(path,json.dumps(content,ensure_ascii=False,indent=2)+'\n')

dump('api/openapi.json',build_openapi())
with (ROOT/'api/endpoints.csv').open('w',newline='',encoding='utf-8-sig') as f:
    w=csv.writer(f);w.writerow(['编号','阶段','方法','路径','用途','权限','请求模型','响应模型','If-Match'])
    for i,o in enumerate(OPS,1):w.writerow([f'API-{i:03d}',o['stage'],o['method'].upper(),o['path'],o['title'],o['permission'],o['request'],o['response'],o['etag']])
catalog=['[cols="1,1,4,3,2",options="header"]','|===','|阶段 |方法 |路径 |用途 |权限']
for o in OPS:
    catalog.append('|'+o['stage']+' |'+o['method'].upper()+' |`'+o['path']+'` |'+o['title']+' |`'+o['permission']+'`')
catalog.append('|===')
write('docs/api-endpoint-catalog.adoc','\n'.join(catalog)+'\n')

# A shared design dataset for SQL seed and the offline prototype.
users=[{'id':str(i),'username':u,'displayName':n,'platformAdmin':i==1,'status':'DISABLED' if i==5 else 'ENABLED','version':1}
       for i,u,n in [(1,'platform_admin','周齐'),(2,'system_admin','林岚'),(3,'auditor','许宁'),(4,'chenyu','陈雨'),(5,'lichen','李晨')]]
systems=[dict(id=str(i),code=c,name=n,description=d,status='ENABLED',version=1,policyVersion=1)
         for i,c,n,d in [(1,'EDU','系统 A · 教务管理','教务业务权限边界'),(2,'RESEARCH','系统 B · 科研管理','科研业务权限边界')]]
menus=[('menu:members','成员管理','/members'),('menu:roles','角色管理','/roles'),
       ('menu:permissions','权限目录','/permissions'),('menu:audit','审计日志','/audit')]
action_labels={'sys:member:read':'查询成员','sys:member:create':'添加成员','sys:member:update':'更新成员',
 'sys:member:assign':'分配成员角色','sys:role:read':'查询角色','sys:role:create':'创建角色',
 'sys:role:update':'更新角色','sys:role:delete':'删除角色','sys:role:grant':'分配角色权限',
 'sys:permission:read':'查询权限','sys:permission:create':'创建权限','sys:permission:update':'更新权限',
 'sys:permission:delete':'删除权限','sys:audit:read':'查询审计'}
permissions=[];roles=[];members=[];rp={};ur={}
for s in systems:
    sid=s['id'];base=int(sid)*1000
    for j,(code,name,path) in enumerate(menus,1):
        permissions.append(dict(id=str(base+j),sysId=sid,code=code,name=name,type='MENU',parentId=None,
                                routePath=path,sortOrder=j,status='ENABLED',version=1))
    for j,(code,name) in enumerate(action_labels.items(),10):
        parent={'member':1,'role':2,'permission':3,'audit':4}[code.split(':')[1]]
        permissions.append(dict(id=str(base+j),sysId=sid,code=code,name=name,type='ACTION',parentId=str(base+parent),
                                routePath=None,sortOrder=j,status='ENABLED',version=1))
    for j,code,name in [(1,'SYS_ADMIN','系统管理员'),(2,'MEMBER_MANAGER','成员管理员'),(3,'READER','成员查看员'),(4,'AUDITOR','审计查看员')]:
        rid=str(int(sid)*100+j)
        roles.append(dict(id=rid,sysId=sid,code=code,name=name,builtin=j==1,status='ENABLED',version=1,description='内置受保护角色' if j==1 else '业务角色'))
        codeset=({'menu:members','sys:member:read','sys:member:create','sys:member:update','sys:member:assign'} if j==2 else
                 {'menu:members','sys:member:read'} if j==3 else {'menu:audit','sys:audit:read'})
        rp[rid]=[p['id'] for p in permissions if p['sysId']==sid and (j==1 or p['code'] in codeset)]
    for u in users[1:]:members.append(dict(sysId=sid,userId=u['id'],displayName=u['displayName'],status='ENABLED',version=1))
ur={'1:2':['101'],'1:3':['104'],'1:4':['103'],'1:5':[],
    '2:2':['203'],'2:3':['201'],'2:4':['203'],'2:5':[]}
data=dict(users=users,systems=systems,members=members,roles=roles,permissions=permissions,
          userRoles=ur,rolePermissions=rp,logs=[dict(id='9001',sysId='1',actorId='1',actorName='周齐',action='MEMBER_ROLES_REPLACED',
          targetType='MEMBER',targetId='4',before={'roleIds':[]},after={'roleIds':['103']},requestId='demo-init-001',createdAt='2026-10-20T02:00:00Z')])
write('prototype/demo-data.js','// 仅RBAC0的本地模拟数据，与SQL种子共享同一来源。\nwindow.RBAC_DEMO = '+json.dumps(data,ensure_ascii=False,indent=2)+';\n')

def q(v):
    if v is None:return 'NULL'
    if isinstance(v,bool):return 'TRUE' if v else 'FALSE'
    return "'"+str(v).replace("'","''")+"'"
sql=['-- 新建开发库演示数据；先执行001。密码占位符不能用于登录，必须通过安全初始化器设置哈希。',
     '-- 仅RBAC0。数据初始化属于受控迁移，生产业务变更必须写审计。',"SET time_zone = '+00:00';",'START TRANSACTION;']
def insert(table,fields,rows):
    if rows:sql.append('INSERT INTO '+table+'('+','.join(fields)+') VALUES\n'+',\n'.join('('+','.join(q(r.get(k)) for k in fields)+')' for r in rows)+';')
insert('iam_user',['id','username','display_name','password_hash','platform_admin','status'],[
 dict(id=u['id'],username=u['username'],display_name=u['displayName'],password_hash='!DEMO_LOGIN_DISABLED_REPLACE_WITH_HASH!',platform_admin=u['platformAdmin'],status=u['status']) for u in users])
insert('iam_system',['id','code','name','description'],systems)
insert('iam_member',['sys_id','user_id','display_name','created_by'],[dict(sys_id=m['sysId'],user_id=m['userId'],display_name=m['displayName'],created_by=1) for m in members])
insert('iam_role',['id','sys_id','code','name','builtin','created_by'],[dict(id=r['id'],sys_id=r['sysId'],code=r['code'],name=r['name'],builtin=r['builtin'],created_by=1) for r in roles])
insert('iam_permission',['id','sys_id','code','name','type','parent_id','route_path','sort_order','created_by'],[
 dict(id=p['id'],sys_id=p['sysId'],code=p['code'],name=p['name'],type=p['type'],parent_id=p['parentId'],route_path=p['routePath'],sort_order=p['sortOrder'],created_by=1)
 for p in permissions if not p.get('stage')])
valid_perms={p['id'] for p in permissions if not p.get('stage')}
insert('iam_role_permission',['sys_id','role_id','permission_id','created_by'],[
 dict(sys_id=find_role['sysId'],role_id=rid,permission_id=pid,created_by=1)
 for rid,ids in rp.items() for pid in ids if pid in valid_perms for find_role in roles if find_role['id']==rid])
insert('iam_user_role',['sys_id','user_id','role_id','created_by'],[
 dict(sys_id=key.split(':')[0],user_id=key.split(':')[1],role_id=rid,created_by=1) for key,ids in ur.items() for rid in ids])
insert('iam_audit_log',['id','sys_id','actor_id','actor_name','action','target_type','target_id','before_json','after_json','request_id','created_at'],[
 dict(id=l['id'],sys_id=l['sysId'],actor_id=l['actorId'],actor_name=l['actorName'],action=l['action'],target_type=l['targetType'],target_id=l['targetId'],
      before_json=json.dumps(l['before'],ensure_ascii=False),after_json=json.dumps(l['after'],ensure_ascii=False),request_id=l['requestId'],created_at='2026-10-20 02:00:00.000')
 for l in data['logs']])
sql.append('COMMIT;')
write('database/003_demo_seed.sql','\n\n'.join(sql)+'\n')

# Every SQL column carries its own dictionary comment. Preserve the full definition for review.
dictionary=['# 数据字典','', '由建表 SQL 自动提取。约束和索引请同时查看原始 SQL。','']
for path in ['database/001_rbac0.sql']:
    src=(ROOT/path).read_text(encoding='utf-8')
    for table,body in re.findall(r'CREATE TABLE (\w+)\s*\((.*?)\) ENGINE',src,re.S):
        dictionary.extend(['## '+table,'','|字段|类型与约束|说明|','|---|---|---|'])
        for line in body.splitlines():
            m=re.match(r"\s*(\w+)\s+(.+?)\s+COMMENT '([^']*)'[,]?$",line)
            if m:dictionary.append('|'+m[1]+'|`'+m[2]+'`|'+m[3]+'|')
        dictionary.append('')
write('database/data-dictionary.md','\n'.join(dictionary))

# Editable diagrams: simple SVG plus matching UML source.
SVG_HEAD='<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="760" viewBox="0 0 1100 760"><defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8" fill="#6c83a3"/></marker></defs><rect width="1100" height="760" fill="#fff"/><style>text{font-family:Segoe UI,Microsoft YaHei,sans-serif;fill:#203650} .title{font-size:24px;font-weight:600}.label{font-size:14px}.small{font-size:12px;fill:#5b708e}.line{stroke:#6c83a3;stroke-width:1.5;fill:none;marker-end:url(#arrow)}</style>'
def t(x,y,s,cls='label',anchor='start'):return f'<text x="{x}" y="{y}" class="{cls}" text-anchor="{anchor}">{html.escape(s)}</text>'
def box(x,y,w,h,title,lines=()):
    out=f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="#f2f6fe" stroke="#9db5d9"/>'+t(x+15,y+26,title)
    for i,line in enumerate(lines):out+=t(x+15,y+52+i*22,line,'small')
    return out
def arrow(x1,y1,x2,y2,label=''):
    return f'<path d="M{x1} {y1} L{x2} {y2}" class="line"/>'+ (t((x1+x2)/2+7,(y1+y2)/2-8,label,'small') if label else '')
def svg(name,body):write('docs/images/'+name+'.svg',SVG_HEAD+body+'</svg>')
er=t(35,42,'多系统 RBAC0 数据关系','title')+t(35,70,'实线表示业务引用；授权关系的每一条边都携带 sys_id。','small')
er+=box(35,110,235,125,'iam_user 全局账号',['PK id / UK username','status / platform_admin','auth_version / version'])
er+=box(805,110,260,125,'iam_system 子系统',['PK id / UK code','status / policy_version','version'])
er+=box(395,110,300,125,'iam_member 成员',['PK (sys_id,user_id)','display_name / status / version'])
er+=arrow(270,170,395,170,'1 → N')+arrow(805,170,695,170,'1 → N')
er+=box(395,300,300,100,'iam_user_role 用户角色',['PK (sys_id,user_id,role_id)','FK member + scoped role'])+arrow(545,235,545,300,'1 → N')
er+=box(805,295,260,110,'iam_role 角色',['PK id / UK (sys_id,code)','UK (sys_id,id) / builtin'])+arrow(805,345,695,345,'1 → N')+arrow(935,235,935,295,'1 → N')
er+=box(805,470,260,105,'iam_role_permission',['PK (sys_id,role_id,permission_id)','FK scoped role + permission'])+arrow(935,405,935,470,'1 → N')
er+=box(395,465,300,115,'iam_permission 权限',['PK id / UK (sys_id,code)','MENU/ACTION / parent_id','parent_id 复合引用同系统'])+arrow(695,520,805,520,'1 → N')
er+=box(35,300,235,110,'iam_session 会话',['PK session_id','user_id / sys_id / auth_version','expires_at / revoked_at'])+arrow(150,235,150,300,'1 → N')
er+=box(35,465,235,115,'iam_audit_log 审计',['sys_id / actor / target','before_json / after_json','request_id / created_at'])
er+=box(35,630,235,90,'iam_login_log',['登录与拒绝安全事件','脱敏快照，不级联删除'])
er+=box(395,630,300,90,'iam_platform_guard',['固定 id=1','平台级管理员保护锁'])
er+=t(805,645,'RBAC0：角色直接关联权限','small')+t(805,675,'菜单parent_id仅表示导航层级','small')
svg('er',er)
arch=t(35,42,'C1 模块和依赖','title')+t(35,74,'单体部署、包边界；箭头表示调用或数据依赖，不表示微服务。','small')
arch+=box(35,115,1030,85,'M08 前端',['页面 / API client / 动态路由 / 按钮权限 / ETag / 统一错误'])+arrow(550,200,550,245,'HTTP')
arch+=box(35,250,1030,85,'M00 API 与契约',['Controller / DTO / requestId / If-Match / ErrorHandler'])+arrow(150,335,150,380)
arch+=box(35,385,235,120,'M01 身份安全门',['账号 / 会话 / authVersion','sysId 上下文 / 每请求校验'])
arch+=box(300,385,500,120,'M02 M03 M04 M05 业务事务脚本',['平台 / 成员 / 目录 / 授权差集','system 锁 → 根版本 → 委派 → 审计'])
arch+=box(830,385,235,120,'M06 权限求解',['系统内角色权限并集','版本缓存 / 菜单投影'])
arch+=arrow(270,445,300,445)+arrow(800,445,830,445)
arch+=box(35,565,765,100,'MySQL 权威状态 + M07 审计',['身份 / 成员 / 角色 / 权限 / 关联 / 会话 / 策略版本 / before-after'])
arch+=box(830,565,235,100,'Redis 派生缓存',['key 含 sysId 与版本','不可用回源主库'])
arch+=arrow(550,505,550,565)+arrow(945,505,945,565)+arrow(865,505,760,565)+t(35,720,'M09：迁移、测试、JaCoCo、部署与性能验证；本包仅覆盖RBAC0。','label')
svg('architecture',arch)
seq=t(35,42,'成员角色替换时序','title')+t(35,72,'一次业务事务包含关联差集、两个版本及成功审计。','small')
lanes=[(90,'浏览器'),(310,'API / 安全门'),(580,'M05 授权脚本'),(910,'MySQL / 审计')]
for x,label in lanes:
    seq+=box(x-65,100,130,45,label)+f'<path d="M{x} 145 V700" stroke="#bbc8da" stroke-dasharray="5 5"/>'
for y,a,b,label in [(195,90,310,'PUT roleIds + If-Match'),(255,310,580,'可信上下文与已解析请求'),(315,580,910,'BEGIN；锁 system → member'),
 (375,910,580,'当前版本与操作者权限快照'),(435,580,910,'同系统 / 委派 / 版本验证'),(495,580,910,'写差集 + 根 version + policyVersion'),
 (555,580,910,'INSERT audit before/after；COMMIT'),(615,580,310,'最终集合与新 ETag'),(675,310,90,'200 + requestId；失败则4xx且回滚')]:
    seq+=arrow(a,y,b,y)+t(min(a,b)+12,y-10,label,'small')
svg('assignment-sequence',seq)
uc=t(35,42,'用例边界与参与者','title')
uc+=box(260,95,805,590,'多系统 RBAC 权限管理平台')
for y,name in [(170,'平台管理员'),(365,'系统管理员'),(560,'普通用户')]:
    uc+=f'<circle cx="100" cy="{y-15}" r="15" fill="none" stroke="#5b7597"/><path d="M100 {y} v42 m-25 -26 h50 m-25 26 l-22 28 m22 -28 l22 28" stroke="#5b7597" fill="none"/>'+t(100,y+95,name,'label','middle')
for coords in [(130,180,310,175),(130,185,665,175),(130,370,310,345),(130,375,665,345),(130,565,310,535),(130,560,665,175),(130,370,665,535)]:uc+=arrow(*coords)
for x,y,name in [(455,175,'账号与子系统管理'),(810,175,'登录与系统切换'),(455,345,'成员与角色权限目录'),(810,345,'授权及审计查询'),(455,535,'个人中心和有效权限'),(810,535,'本人密码修改')]:
    uc+=f'<ellipse cx="{x}" cy="{y}" rx="145" ry="43" fill="#f1f5ff" stroke="#9bb4dc"/>'+t(x,y+5,name,'label','middle')
svg('use-cases',uc)
states=t(35,42,'账号与会话状态','title')+t(35,75,'账号业务状态与临时登录锁定分开保存；会话撤销和过期另行校验。','small')
states+=box(100,150,220,100,'ENABLED',['正常账号，可继续安全门'])+box(760,150,240,100,'DISABLED',['平台停用 / authVersion+1'])
states+=arrow(320,185,760,185,'平台管理员停用')+arrow(760,225,320,225,'平台重新启用 / authVersion+1')
states+=box(100,380,220,105,'未锁定登录',['failed_attempts < 5'])+box(760,380,240,105,'临时锁定登录',['locked_until > now'])
states+=arrow(320,410,760,410,'失败达到阈值，锁定15分钟')+arrow(760,455,320,455,'时间到期，允许重新验证凭据')
states+=box(100,600,220,95,'ACTIVE 会话',['未过期 / 未撤销 / 版本一致'])+box(760,600,240,95,'不可用会话',['登出、改密或过期'])
states+=arrow(320,640,760,640,'撤销 / 到期 / authVersion变化')
svg('states',states)

uml={
 'use-cases':'''@startuml
left to right direction
actor 平台管理员 as P
actor 系统管理员 as A
actor 普通用户 as U
rectangle 多系统RBAC平台 {
usecase "账号与子系统管理" as UC_PLATFORM
usecase "登录与上下文切换" as UC_LOGIN
usecase "成员角色权限管理" as UC_DIRECTORY
usecase "授权与审计" as UC_ASSIGNMENT
usecase "个人中心" as UC_PROFILE
}
P --> UC_PLATFORM
P --> UC_LOGIN
P --> UC_DIRECTORY
P --> UC_ASSIGNMENT
A --> UC_LOGIN
A --> UC_DIRECTORY
A --> UC_ASSIGNMENT
U --> UC_LOGIN
U --> UC_PROFILE
@enduml''',
 'domain':'''@startuml
class User {
  id : Long
  username : String
  authVersion : Long
}
class System {
  id : Long
  policyVersion : Long
}
class Member {
  sysId : Long
  userId : Long
  version : Long
}
class Role {
  sysId : Long
  id : Long
  code : String
  version : Long
}
class Permission {
  sysId : Long
  id : Long
  code : String
  type : PermissionType
}
class Session {
  sessionId : String
  sysId : Long
}
User "1" -- "0..*" Member
System "1" -- "0..*" Member
Member "0..*" -- "0..*" Role : UA同系统
Role "0..*" -- "0..*" Permission : PA同系统
User "1" -- "0..*" Session
note bottom : 这是分析领域关系；C1行为由事务脚本组织。
@enduml''',
 'components':'''@startuml
component "M08 前端" as FE
component "M00 API契约" as API
component "M01 身份安全门" as AUTH
component "M02-M05 用例事务脚本" as APP
component "M06 权限求解" as RES
component "M07 审计写入" as AUDIT
database MySQL
database Redis
FE --> API
API --> AUTH
API --> APP
APP --> RES
APP --> AUDIT
APP --> MySQL
AUDIT --> MySQL
RES --> MySQL
RES --> Redis
@enduml''',
 'assignment-sequence':'''@startuml
actor 管理员 as A
participant API
participant M05
database MySQL as DB
A -> API: PUT roleIds + If-Match
API -> M05: 可信SysContext
M05 -> DB: BEGIN / lock system / lock member
M05 -> DB: 当前读重验操作者权限与版本
M05 -> M05: 归属、委派、候选集合、版本
alt 验证失败
M05 -> DB: ROLLBACK
M05 --> API: 403/404/409/412
else 合法
M05 -> DB: 差集 / version+1 / policyVersion+1
M05 -> DB: INSERT before-after audit
M05 -> DB: COMMIT
M05 --> API: final roleIds + ETag
end
API --> A: HTTP + requestId
@enduml''',
 'states':'''@startuml
[*] --> ENABLED
ENABLED --> DISABLED : 平台停用 / authVersion++
DISABLED --> ENABLED : 平台启用 / authVersion++
state 登录锁定 {
[*] --> 可验证
可验证 --> 临时锁定 : 连续失败5次
临时锁定 --> 可验证 : 15分钟到期
}
state 会话 {
[*] --> ACTIVE
ACTIVE --> REVOKED : 登出或改密
ACTIVE --> EXPIRED : 到期
}
@enduml'''}
for name,src in uml.items():write('uml/'+name+'.puml',src+'\n')

trace=[
 ('FR-01','P01 P02','M01','iam_session iam_user iam_system iam_member','T01 T02 T05','/auth/'),
 ('FR-02','P04','M02','iam_user iam_platform_guard','T12','/platform/users'),
 ('FR-03','P03','M02','iam_system','T02 T05','/platform/systems'),
 ('FR-04','P05','M03','iam_member','T03 T05','/members'),
 ('FR-05','P06','M04','iam_role','T07 T12','/roles'),
 ('FR-06','P07','M04','iam_permission','T05 T12','/permissions'),
 ('FR-07','P08','M05','iam_user_role iam_member iam_system iam_audit_log','T06 T08 T09 T10','/members/{userId}/roles'),
 ('FR-08','P09','M05','iam_role_permission iam_role iam_system iam_audit_log','T06 T07 T08 T10','/roles/{roleId}/permissions'),
 ('FR-09','P02 P05 P11','M06','iam_session iam_member iam_role iam_permission','T03 T04 T11','/me/permissions'),
 ('FR-10','P10','M07','iam_audit_log','T10 T13','/audit-logs'),
 ('FR-11','P11','M01 M08','iam_user iam_session','T02','/me/password'),

]
with (ROOT/'docs/traceability.csv').open('w',newline='',encoding='utf-8-sig') as f:
    w=csv.writer(f);w.writerow(['需求','页面','模块','表','验收用例','对应API'])
    for fr,p,m,tables,tests,suffix in trace:
        patterns=[suffix]
        if fr=='FR-01':patterns+=['/api/v1/me/systems']
        if fr=='FR-09':patterns+=['/api/v1/me/menus']
        endpoints=['%s %s'%(o['method'].upper(),o['path']) for o in OPS if any(p in o['path'] for p in patterns) or (fr=='FR-01' and o['path']=='/api/v1/me')]
        w.writerow([fr,p,m,tables,tests,' ; '.join(endpoints)])

# Small, offline AsciiDoc reading renderer. Original .adoc remains authoritative.
def inline(s):
    s=html.escape(s)
    s=re.sub(r'`([^`]+)`',r'<code>\1</code>',s)
    s=re.sub(r'link:([^\s\[]+)\[([^]]+)\]',r'<a href="../\1">\2</a>',s)
    s=re.sub(r'(https?://[^\s\[]+)\[([^]]+)\]',r'<a href="\1">\2</a>',s)
    return s
def render_adoc(src):
    src=re.sub(r'include::([^\[]+)\[[^]]*\]',lambda m:(ROOT/'docs'/m[1]).read_text(encoding='utf-8'),src)
    lines=src.splitlines();out=[];i=0;list_kind=None
    def close_list():
        nonlocal list_kind
        if list_kind:out.append('</'+list_kind+'>');list_kind=None
    while i<len(lines):
        line=lines[i];i+=1
        if line.startswith(':') or line.startswith('[cols=') or line.startswith('[source,') or not line.strip():
            if not line.strip():close_list()
            continue
        if line=='----':
            close_list();code=[]
            while i<len(lines) and lines[i]!='----':code.append(lines[i]);i+=1
            i+=1;out.append('<pre><code>'+html.escape('\n'.join(code))+'</code></pre>');continue
        if line=='|===':
            close_list();rows=[]
            while i<len(lines) and lines[i]!='|===':
                if lines[i].startswith('|'):rows.append(lines[i][1:].split('|'))
                i+=1
            i+=1
            if rows:
                out.append('<div class="table-scroll"><table><thead><tr>'+''.join('<th>'+inline(c.strip())+'</th>' for c in rows[0])+'</tr></thead><tbody>')
                out.extend('<tr>'+''.join('<td>'+inline(c.strip())+'</td>' for c in r)+'</tr>' for r in rows[1:]);out.append('</tbody></table></div>')
            continue
        m=re.match(r'(=+) (.+)',line)
        if m:
            close_list();level=min(len(m[1])+1,6);out.append(f'<h{level}>'+inline(m[2])+f'</h{level}>');continue
        m=re.match(r'image::([^\[]+)\[([^]]*)\]',line)
        if m:
            close_list();out.append('<figure><img src="images/'+m[1]+'" alt="'+html.escape(m[2])+'"><figcaption>'+html.escape(m[2])+'</figcaption></figure>');continue
        m=re.match(r'([*.]) (.+)',line)
        if m:
            kind='ul' if m[1]=='*' else 'ol'
            if list_kind!=kind:close_list();list_kind=kind;out.append('<'+kind+'>')
            out.append('<li>'+inline(m[2])+'</li>');continue
        close_list();para=[line]
        while i<len(lines) and lines[i].strip() and not re.match(r'^(=|\[|\||image::|[*.] )',lines[i]):para.append(lines[i]);i+=1
        out.append('<p>'+inline(' '.join(para))+'</p>')
    close_list();return '\n'.join(out)
doc_names=re.findall(r'include::([^\[]+)',(ROOT/'docs/index.adoc').read_text(encoding='utf-8'))
docs=[ROOT/'docs'/name for name in doc_names]
body=[];nav=[]
for i,path in enumerate(docs):
    title=path.read_text(encoding='utf-8').splitlines()[0].lstrip('= ')
    nav.append(f'<a href="#doc{i}">{i:02d} {html.escape(title)}</a>')
    body.append(f'<section id="doc{i}">'+render_adoc(path.read_text(encoding='utf-8'))+'</section>')
write('docs/index.html','''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>多系统 RBAC0 设计文档</title><style>
*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;color:#20324a;background:#f5f7fb;font:15px/1.85 "Segoe UI","Microsoft YaHei",sans-serif}header{background:#183557;color:white;padding:40px max(25px,calc((100vw - 1200px)/2))}header h1{font-size:30px;margin:0 0 10px}header p{margin:0;color:#c6d6ec}header a{color:#e4efff} .layout{display:grid;grid-template-columns:240px minmax(0,960px);gap:30px;max-width:1260px;margin:30px auto;padding:0 20px}nav{position:sticky;top:18px;align-self:start;display:grid;gap:8px}nav a{font-size:13px;color:#4c6487;text-decoration:none;padding:6px 0}main{min-width:0}section{background:white;border:1px solid #e0e7f0;border-radius:10px;padding:32px;margin-bottom:25px;scroll-margin-top:15px}h2{font-size:25px;border-bottom:1px solid #dce4ef;padding-bottom:15px;margin:0 0 24px}h3{font-size:20px;margin-top:32px;color:#294d7b}h4{font-size:17px}p{overflow-wrap:anywhere}a{color:#285bdd}code{font:13px/1.7 Consolas,monospace;background:#eff3f9;padding:2px 4px;border-radius:4px;overflow-wrap:anywhere}pre{background:#f0f4fa;padding:17px;overflow:auto;border-radius:8px;white-space:pre-wrap}pre code{padding:0;background:transparent}.table-scroll{overflow:auto}table{width:100%;border-collapse:collapse;font-size:13px;line-height:1.75}th,td{border:1px solid #dce4ee;padding:10px 12px;text-align:left;vertical-align:top;min-width:80px}th{background:#e9f0fb;color:#29466e}tr:nth-child(even){background:#fafbfd}figure{margin:25px 0}figure img{display:block;width:100%;height:auto;background:white}figcaption{font-size:12px;color:#60758f;text-align:center}.links{margin-top:18px;display:flex;gap:20px;flex-wrap:wrap}li{margin:7px 0}@media(max-width:850px){.layout{display:block}nav{position:static;margin-bottom:20px;display:flex;flex-wrap:wrap;gap:8px 18px}section{padding:22px 18px}header h1{font-size:25px}table{min-width:620px}}@media print{header{color:#000;background:#fff;padding:0}header p{color:#333}nav{display:none}.layout{display:block;margin:0;padding:0}section{border:0;padding:0;break-before:page}h2,h3{break-after:avoid}table{font-size:10px}figure{break-inside:avoid}a{color:#000}}
</style></head><body><header><h1>多系统 RBAC0 权限管理系统</h1><p>需求 · 原型 · API · 数据库 · 详细设计与任务分解</p><p>修订稿 V1.2 / 2026-10-21 / 仅多系统 RBAC0</p><div class="links"><a href="../prototype/vue/dist/index.html">打开Vue工程原型 ↗</a><a href="../api/openapi.json">OpenAPI 契约</a><a href="../database/data-dictionary.md">数据字典</a><a href="traceability.csv">需求追踪矩阵</a><a href="../validation/report.md">验证记录</a></div></header><div class="layout"><nav>'''+''.join(nav)+'</nav><main>'+''.join(body)+'</main></div></body></html>')
# Standalone division reader; generated from the same authoritative AsciiDoc.
reader=(ROOT/'docs/index.html').read_text(encoding='utf-8')
division=render_adoc((ROOT/'docs/07-RBAC0六人分工.adoc').read_text(encoding='utf-8'))
standalone=re.sub(r'<nav>.*?</nav>', '<nav><a href="index.html">返回完整设计文档</a><a href="../README.adoc">交付包说明</a></nav>',reader,flags=re.S)
standalone=re.sub(r'<main>.*?</main>',lambda m:'<main><section>'+division+'</section></main>',standalone,flags=re.S)
standalone=standalone.replace('<title>多系统 RBAC0 设计文档</title>','<title>RBAC0 六人详细功能设计分工</title>')
write('docs/07-RBAC0六人分工.html',standalone)
print(json.dumps({'operations':len(OPS),'schemas':len(build_openapi()['components']['schemas']),
                  'documents':len(docs),'RBAC0_tables':11,'prototype_accounts':len(users),
                  'prototype_permissions':len(permissions)},ensure_ascii=False))
