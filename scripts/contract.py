"""Design-time OpenAPI definitions. No production server is implemented here."""
import copy

def ref(name):
    return {'$ref': '#/components/schemas/' + name}

def obj(props, required=()):
    schema = {'type': 'object', 'additionalProperties': False, 'properties': props}
    # OAS 3.0 requires a non-empty required array when the keyword is present.
    # PATCH fields are optional individually; minProperties=1 is defined below.
    if required:
        schema['required'] = list(required)
    return schema

def text(maximum=64, minimum=1, **kw):
    return dict(type='string', minLength=minimum, maxLength=maximum, **kw)

ID = dict(type='string', pattern='^[1-9][0-9]{0,18}$', example='102', description='正的有符号BIGINT十进制字符串，最大9223372036854775807，服务端校验范围。')
VER = dict(type='string', pattern='^[1-9][0-9]{0,18}$', example='7', description='正的有符号BIGINT版本字符串，最大9223372036854775807。')
STATUS = dict(type='string', enum=['ENABLED', 'DISABLED'])
TIME = dict(type='string', format='date-time')
CODE = text(40, 2, pattern='^[A-Z][A-Z0-9_]+$')
PERMCODE = text(80, 3, pattern='^[a-z][a-z0-9:_]+$')
USERNAME = text(32, 3, pattern='^[a-z0-9_]+$')
PASSWORD = text(64, 12, format='password', writeOnly=True,
                description='至少包含字母与数字；服务端校验，不回显。')
IDS = dict(type='array', items=ID, uniqueItems=False, maxItems=1000,
           description='服务端去重排序；空数组表示清空，未知或异系统ID整批拒绝。')

SCHEMAS = {
 'Error': obj({'code': text(64), 'message': text(500), 'requestId': text(80),
               'details': {'type': 'object', 'additionalProperties': True}},
              ['code', 'message', 'requestId']),
 'LoginRequest': obj({'username': USERNAME, 'password': text(64, 1, format='password', writeOnly=True),
                      'sysId': dict(ID, nullable=True)}, ['username', 'password']),
 'ContextRequest': obj({'sysId': dict(ID, nullable=True)}, ['sysId']),
 'SessionCreated': obj({'accessToken': text(4096), 'tokenType': {'type':'string','enum':['Bearer']},
                         'expiresIn': {'type':'integer','example':1800}, 'sessionId': text(36),
                         'sysId': dict(ID, nullable=True)},
                        ['accessToken','tokenType','expiresIn','sessionId','sysId']),
 'UserCreate': obj({'username': USERNAME, 'displayName': text(), 'initialPassword': PASSWORD,
                     'platformAdmin': {'type':'boolean','default':False}},
                    ['username','displayName','initialPassword']),
 'UserPatch': obj({'displayName': text(), 'status': STATUS, 'platformAdmin': {'type':'boolean'}}),
 'UserView': obj({'id': ID, 'username': USERNAME, 'displayName': text(), 'status':STATUS,
                   'platformAdmin': {'type':'boolean'}, 'version': VER, 'createdAt':TIME},
                  ['id','username','displayName','status','platformAdmin','version','createdAt']),
 'SystemCreate': obj({'code':CODE,'name':text(),'description':text(500,0)}, ['code','name']),
 'SystemPatch': obj({'name':text(),'description':text(500,0),'status':STATUS}),
 'SystemView': obj({'id':ID,'code':CODE,'name':text(),'description':text(500,0),'status':STATUS,
                     'policyVersion':VER,'version':VER,'createdAt':TIME},
                    ['id','code','name','status','policyVersion','version','createdAt']),
 'MemberCreate': obj({'username':USERNAME,'displayName':text()}, ['username','displayName']),
 'MemberPatch': obj({'displayName':text(),'status':STATUS}),
 'MemberView': obj({'userId':ID,'sysId':ID,'username':USERNAME,'displayName':text(),
                     'status':STATUS,'accountStatus':STATUS,'roleIds':IDS,'roleNames':{'type':'array','items':text()},'version':VER,'createdAt':TIME},
                    ['userId','sysId','username','displayName','status','accountStatus','roleIds','roleNames','version','createdAt']),
 'RoleCreate': obj({'code':CODE,'name':text(),'description':text(500,0)}, ['code','name']),
 'RolePatch': obj({'name':text(),'description':text(500,0),'status':STATUS}),
 'RoleView': obj({'id':ID,'sysId':ID,'code':CODE,'name':text(),'description':text(500,0),
                   'status':STATUS,'builtin':{'type':'boolean'},'version':VER,'createdAt':TIME},
                  ['id','sysId','code','name','status','builtin','version','createdAt']),
 'PermissionCreate': obj({'code':PERMCODE,'name':text(),'type':{'type':'string','enum':['MENU','ACTION']},
                           'parentId':dict(ID,nullable=True),'routePath':{'type':'string','nullable':True,'enum':[None,'/overview','/members','/roles','/permissions','/audit','/profile']},
                           'sortOrder':{'type':'integer','minimum':0,'maximum':9999,'default':0}},
                          ['code','name','type']),
 'PermissionPatch': obj({'name':text(),'parentId':dict(ID,nullable=True),
                          'routePath':{'type':'string','nullable':True,'enum':[None,'/overview','/members','/roles','/permissions','/audit','/profile']},'sortOrder':{'type':'integer','minimum':0,'maximum':9999},
                          'status':STATUS}),
 'PermissionView': obj({'id':ID,'sysId':ID,'code':PERMCODE,'name':text(),
                         'type':{'type':'string','enum':['MENU','ACTION']},'parentId':dict(ID,nullable=True),
                         'routePath':{'type':'string','nullable':True,'enum':[None,'/overview','/members','/roles','/permissions','/audit','/profile']},'sortOrder':{'type':'integer','minimum':0,'maximum':9999},
                         'status':STATUS,'version':VER,'createdAt':TIME},
                        ['id','sysId','code','name','type','parentId','routePath','sortOrder','status','version','createdAt']),
 'RoleIdsRequest': obj({'roleIds':IDS}, ['roleIds']),
 'RoleIdsView': obj({'roleIds':IDS,'version':VER}, ['roleIds','version']),
 'PermissionIdsRequest': obj({'permissionIds':IDS}, ['permissionIds']),
 'PermissionIdsView': obj({'permissionIds':IDS,'version':VER}, ['permissionIds','version']),
 'PasswordRequest': obj({'oldPassword':text(64,1,format='password',writeOnly=True), 'newPassword':PASSWORD},
                         ['oldPassword','newPassword']),
 'MeView': obj({'userId':ID,'username':USERNAME,'displayName':text(),'sysId':dict(ID,nullable=True),
                 'platformAdmin':{'type':'boolean'},'roleIds':IDS,'policyVersion':dict(VER,nullable=True)},
                ['userId','username','displayName','sysId','platformAdmin','roleIds','policyVersion']),
 'EffectivePermissions': obj({'sysId':ID,'userId':ID,'policyVersion':VER,
                              'permissionCodes':{'type':'array','items':PERMCODE},
                              'directRoleIds':IDS},
                             ['sysId','userId','policyVersion','permissionCodes','directRoleIds']),
 'MenuItem': obj({'id':ID,'parentId':dict(ID,nullable=True),'name':text(),
                   'routePath':{'type':'string','nullable':True,'enum':[None,'/overview','/members','/roles','/permissions','/audit','/profile']},'navigable':{'type':'boolean'},
                   'children':{'type':'array','items':ref('MenuItem')}},
                  ['id','name','navigable','children']),
 'MenuList': {'type':'array','items':ref('MenuItem')},
 'SystemList': {'type':'array','items':ref('SystemView')},
 'AuditView': obj({'id':ID,'sysId':dict(ID,nullable=True),'actorId':dict(ID,nullable=True),
                    'actorName':text(),'action':text(80),'targetType':text(40),'targetId':text(80),
                    'before':{'type':'object','nullable':True,'additionalProperties':True},
                    'after':{'type':'object','nullable':True,'additionalProperties':True},
                    'requestId':text(80),'createdAt':TIME,'ip':text(45)},
                   ['id','sysId','actorId','actorName','action','targetType','targetId','before','after','requestId','createdAt']),
}
for n in ['UserPatch','SystemPatch','MemberPatch','RolePatch','PermissionPatch']:
    SCHEMAS[n]['minProperties'] = 1
for n in ['User','System','Member','Role','Permission','Audit']:
    SCHEMAS[n+'Page'] = obj({'items':{'type':'array','items':ref(n+'View')},
                             'total':{'type':'integer','format':'int64','minimum':0},
                             'page':{'type':'integer','minimum':1},'pageSize':{'type':'integer','minimum':1,'maximum':100}},
                            ['items','total','page','pageSize'])


SCHEMAS['PermissionCreate']['description']='MENU必须有受控routePath；ACTION的routePath省略或为null。父节点必须为本系统MENU，父链不得成环。'
SCHEMAS['PermissionCreate']['oneOf']=[
    {'properties':{'type':{'enum':['MENU']},'routePath':{'type':'string','enum':['/overview','/members','/roles','/permissions','/audit','/profile']}},'required':['type','routePath']},
    {'properties':{'type':{'enum':['ACTION']},'routePath':{'type':'string','nullable':True,'enum':[None]}},'required':['type']}
]
SCHEMAS['PermissionPatch']['description']='type/code不可改；按现有类型重新校验路由、同系统MENU父节点和菜单无环。'
SCHEMAS['MeView']['description']='roleIds为当前系统全部有效直接角色；平台会话为[]。RBAC0无角色继承和互斥规则。'
SCHEMAS['MemberView']['description']='roleIds/roleNames为顺序对应的全部直接指派角色（含停用角色）；有效性由角色状态另行判定。'
SCHEMAS['AssignmentRoleOption']=obj({'id':ID,'code':CODE,'name':text(),'status':STATUS,'builtin':{'type':'boolean'},'assignable':{'type':'boolean'},'reason':text(200,0)},['id','code','name','status','builtin','assignable'])
SCHEMAS['AssignmentPermissionOption']=obj({'id':ID,'code':PERMCODE,'name':text(),'type':{'type':'string','enum':['MENU','ACTION']},'parentId':dict(ID,nullable=True),'status':STATUS,'grantable':{'type':'boolean'},'reason':text(200,0)},['id','code','name','type','parentId','status','grantable'])
SCHEMAS['RoleIdsView']['properties']['options']={'type':'array','items':ref('AssignmentRoleOption'),'description':'本次可指派角色及原已选角色；原已选但停用/不可委派的角色仍返回，不能因候选隐藏而丢失。'}
SCHEMAS['RoleIdsView']['required'].append('options')
SCHEMAS['PermissionIdsView']['properties']['options']={'type':'array','items':ref('AssignmentPermissionOption'),'description':'本次可授予权限及原已选权限；停用或不可委派项保留状态和原因。'}
SCHEMAS['PermissionIdsView']['required'].append('options')

OPS=[]
def add(method,path,title,permission,request=None,response=None,stage='RBAC0',etag=False,status=200,filters=()):
    OPS.append(dict(method=method,path='/api/v1'+path,title=title,permission=permission,
                    request=request,response=response,stage=stage,etag=etag,status=status,filters=list(filters)))

add('post','/auth/sessions','创建登录会话','PUBLIC','LoginRequest','SessionCreated',status=201)
add('delete','/auth/sessions/current','登出当前会话','AUTHENTICATED',status=204)
add('post','/auth/contexts','切换上下文并撤销旧会话','AUTHENTICATED','ContextRequest','SessionCreated',status=201)
add('get','/me','获取本人身份','AUTHENTICATED',response='MeView')
add('get','/me/systems','获取本人可进入的子系统','AUTHENTICATED',response='SystemList')
add('get','/me/menus','获取本人当前系统菜单','SYSTEM_MEMBER',response='MenuList')
add('get','/me/permissions','获取本人当前系统有效权限','SYSTEM_MEMBER',response='EffectivePermissions')
add('put','/me/password','修改本人密码并使旧会话失效','AUTHENTICATED','PasswordRequest',status=204)

def crud(base,singular,perm,label,delete=True,platform=False):
    fields=['keyword','status']
    permission='PLATFORM_ADMIN' if platform else perm+':read'
    add('get',base,'分页查询'+label,permission,response=singular+'Page',filters=fields)
    add('post',base,'创建'+label,'PLATFORM_ADMIN' if platform else perm+':create',
        singular+'Create',singular+'View',status=201)
    idname={'User':'userId','System':'sysId','Member':'userId','Role':'roleId','Permission':'permissionId'}[singular]
    path=base+'/{'+idname+'}'
    add('get',path,'读取'+label+'与ETag',permission,response=singular+'View')
    add('patch',path,'局部修改'+label,'PLATFORM_ADMIN' if platform else perm+':update',
        singular+'Patch',singular+'View',etag=True)
    if delete:
        add('delete',path,'删除未关联的'+label,perm+':delete',etag=True,status=204)

crud('/platform/users','User',None,'全局账号',False,True)
crud('/platform/systems','System',None,'子系统',False,True)
BASE='/systems/{sysId}'
crud(BASE+'/members','Member','sys:member','系统成员',False)
crud(BASE+'/roles','Role','sys:role','角色')
crud(BASE+'/permissions','Permission','sys:permission','权限')
for suffix,perm,req,res,label in [('/members/{userId}/roles','sys:member:assign','RoleIdsRequest','RoleIdsView','成员角色'),
                                  ('/roles/{roleId}/permissions','sys:role:grant','PermissionIdsRequest','PermissionIdsView','角色权限')]:
    add('get',BASE+suffix,'读取'+label+'集合与ETag',perm,response=res)
    add('put',BASE+suffix,'全量替换'+label+'集合',perm,req,res,etag=True)
for scope,perm in [('/platform','PLATFORM_ADMIN'),(BASE,'sys:audit:read')]:
    add('get',scope+'/audit-logs','分页查询审计日志',perm,response='AuditPage',
        filters=['from','to','actorId','action']+(['sysId'] if scope=='/platform' else []))
    add('get',scope+'/audit-logs/{logId}','读取审计详情',perm,response='AuditView')

def build_openapi():
    spec={'openapi':'3.0.3','info':{'title':'多系统 RBAC 权限管理 API 设计','version':'1.2.0',
          'description':'仅RBAC0的多系统设计契约，未实现后端。所有ID和版本为1至9223372036854775807的十进制字符串；角色与成员的授权集合共用所属根资源version。'},
          'servers':[{'url':'http://localhost:8080','description':'待实现的本地开发服务器'}],
          'security':[{'bearerAuth':[]}],'paths':{},
          'components':{'securitySchemes':{'bearerAuth':{'type':'http','scheme':'bearer','bearerFormat':'JWT'}},
                        'schemas':copy.deepcopy(SCHEMAS)}}
    import re
    for i,o in enumerate(OPS,1):
        params=[]
        for p in re.findall(r'\{(\w+)\}',o['path']):
            params.append({'name':p,'in':'path','required':True,'schema':ID})
        if o['etag']:
            params.append({'name':'If-Match','in':'header','required':True,
                           'schema':{'type':'string','pattern':'^"[1-9][0-9]*"$','example':'"7"'},
                           'description':'根资源版本；成员角色集合共用成员版本，角色权限集合共用角色版本。'})
        if o['response'] and o['response'].endswith('Page'):
            params.extend([{'name':'page','in':'query','schema':{'type':'integer','minimum':1,'default':1}},
                           {'name':'pageSize','in':'query','schema':{'type':'integer','minimum':1,'maximum':100,'default':20}}])
        for f in o['filters']:
            fs = TIME if f in ['from','to'] else STATUS if f=='status' else ID if f in ['sysId','actorId'] else text(64,0)
            params.append({'name':f,'in':'query','schema':fs})
        if o['response'] in ['UserPage','SystemPage','MemberPage','RolePage','PermissionPage']:
            names={'UserPage':['id','createdAt','username'],'SystemPage':['id','createdAt','name'],
                   'MemberPage':['userId','createdAt','username'],'RolePage':['id','createdAt','name'],
                   'PermissionPage':['id','createdAt','name']}[o['response']]
            params.extend([{'name':'sort','in':'query','schema':{'type':'string','enum':names,'default':names[0]}},
                           {'name':'order','in':'query','schema':{'type':'string','enum':['asc','desc'],'default':'desc'}}])
        success={'description':'成功；集合更新返回最终去重集合。'}
        if o['response']:
            success['content']={'application/json':{'schema':obj({'data':ref(o['response']),'requestId':text(80)},['data','requestId'])}}
        headers={}
        if o['status']==201:
            headers['Location']={'description':'新资源URI；会话返回 /api/v1/auth/sessions/current。','schema':{'type':'string'}}
        if o['status'] != 204 and o['response'] and 'version' in SCHEMAS.get(o['response'],{}).get('properties',{}):
            headers['ETag']={'description':'强版本ETag，例如 "7"；角色子资源共用角色根版本。','schema':{'type':'string'}}
        if headers: success['headers']=headers
        responses={str(o['status']):success}
        error_statuses=['400','401','403','404','422','429','503']
        if o['method']!='get':error_statuses.append('409')
        if o['etag']:error_statuses.extend(['412','428'])
        if o['permission']=='PUBLIC':error_statuses.append('423')
        for status in error_statuses:
            responses[status]={'description':{'401':'未认证或会话无效','403':'权限或上下文不符','404':'当前范围不存在',
                '409':'唯一性/引用/保护/约束冲突','412':'版本冲突','428':'缺少If-Match',
                '422':'字段不合法','423':'账号锁定','429':'限流','503':'安全依赖不可用','400':'请求格式错误'}[status],
                'content':{'application/json':{'schema':ref('Error')}}}
        responses['429']['headers']={'Retry-After':{'schema':{'type':'integer','minimum':1},'description':'建议重试秒数'}}
        op={'operationId':f'operation_{i:03d}','summary':o['title'],'tags':[o['stage']],
             'x-stage':o['stage'],'x-permission':o['permission'],'parameters':params,'responses':responses,
             'description':({'PLATFORM_ADMIN':'仅启用的平台管理员的平台会话；sysId为空。','PUBLIC':'校验凭据；无sysId仅平台管理员可登录平台。','AUTHENTICATED':'有效会话即可；平台与系统会话均可调用；切换上下文仍需重新校验资格。','SYSTEM_MEMBER':'仅绑定启用子系统的有效成员会话，或该系统上下文的平台管理员。'}.get(o['permission'],'路径sysId必须等于会话sysId，随后执行操作权限和对象范围校验。'))}
        if o['request']:
            op['requestBody']={'required':True,'content':{'application/json':{'schema':ref(o['request'])}}}
        if o['permission']=='PUBLIC': op['security']=[]
        spec['paths'].setdefault(o['path'],{})[o['method']]=op
    return spec
