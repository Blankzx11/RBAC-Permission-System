// 仅RBAC0的本地模拟数据，与SQL种子共享同一来源。
window.RBAC_DEMO = {
  "users": [
    {
      "id": "1",
      "username": "platform_admin",
      "displayName": "周齐",
      "platformAdmin": true,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2",
      "username": "system_admin",
      "displayName": "林岚",
      "platformAdmin": false,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "3",
      "username": "auditor",
      "displayName": "许宁",
      "platformAdmin": false,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "4",
      "username": "chenyu",
      "displayName": "陈雨",
      "platformAdmin": false,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "5",
      "username": "lichen",
      "displayName": "李晨",
      "platformAdmin": false,
      "status": "DISABLED",
      "version": 1
    }
  ],
  "systems": [
    {
      "id": "1",
      "code": "EDU",
      "name": "系统 A · 教务管理",
      "description": "教务业务权限边界",
      "status": "ENABLED",
      "version": 1,
      "policyVersion": 1
    },
    {
      "id": "2",
      "code": "RESEARCH",
      "name": "系统 B · 科研管理",
      "description": "科研业务权限边界",
      "status": "ENABLED",
      "version": 1,
      "policyVersion": 1
    }
  ],
  "members": [
    {
      "sysId": "1",
      "userId": "2",
      "displayName": "林岚",
      "status": "ENABLED",
      "version": 1
    },
    {
      "sysId": "1",
      "userId": "3",
      "displayName": "许宁",
      "status": "ENABLED",
      "version": 1
    },
    {
      "sysId": "1",
      "userId": "4",
      "displayName": "陈雨",
      "status": "ENABLED",
      "version": 1
    },
    {
      "sysId": "1",
      "userId": "5",
      "displayName": "李晨",
      "status": "ENABLED",
      "version": 1
    },
    {
      "sysId": "2",
      "userId": "2",
      "displayName": "林岚",
      "status": "ENABLED",
      "version": 1
    },
    {
      "sysId": "2",
      "userId": "3",
      "displayName": "许宁",
      "status": "ENABLED",
      "version": 1
    },
    {
      "sysId": "2",
      "userId": "4",
      "displayName": "陈雨",
      "status": "ENABLED",
      "version": 1
    },
    {
      "sysId": "2",
      "userId": "5",
      "displayName": "李晨",
      "status": "ENABLED",
      "version": 1
    }
  ],
  "roles": [
    {
      "id": "101",
      "sysId": "1",
      "code": "SYS_ADMIN",
      "name": "系统管理员",
      "builtin": true,
      "status": "ENABLED",
      "version": 1,
      "description": "内置受保护角色"
    },
    {
      "id": "102",
      "sysId": "1",
      "code": "MEMBER_MANAGER",
      "name": "成员管理员",
      "builtin": false,
      "status": "ENABLED",
      "version": 1,
      "description": "业务角色"
    },
    {
      "id": "103",
      "sysId": "1",
      "code": "READER",
      "name": "成员查看员",
      "builtin": false,
      "status": "ENABLED",
      "version": 1,
      "description": "业务角色"
    },
    {
      "id": "104",
      "sysId": "1",
      "code": "AUDITOR",
      "name": "审计查看员",
      "builtin": false,
      "status": "ENABLED",
      "version": 1,
      "description": "业务角色"
    },
    {
      "id": "201",
      "sysId": "2",
      "code": "SYS_ADMIN",
      "name": "系统管理员",
      "builtin": true,
      "status": "ENABLED",
      "version": 1,
      "description": "内置受保护角色"
    },
    {
      "id": "202",
      "sysId": "2",
      "code": "MEMBER_MANAGER",
      "name": "成员管理员",
      "builtin": false,
      "status": "ENABLED",
      "version": 1,
      "description": "业务角色"
    },
    {
      "id": "203",
      "sysId": "2",
      "code": "READER",
      "name": "成员查看员",
      "builtin": false,
      "status": "ENABLED",
      "version": 1,
      "description": "业务角色"
    },
    {
      "id": "204",
      "sysId": "2",
      "code": "AUDITOR",
      "name": "审计查看员",
      "builtin": false,
      "status": "ENABLED",
      "version": 1,
      "description": "业务角色"
    }
  ],
  "permissions": [
    {
      "id": "1001",
      "sysId": "1",
      "code": "menu:members",
      "name": "成员管理",
      "type": "MENU",
      "parentId": null,
      "routePath": "/members",
      "sortOrder": 1,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1002",
      "sysId": "1",
      "code": "menu:roles",
      "name": "角色管理",
      "type": "MENU",
      "parentId": null,
      "routePath": "/roles",
      "sortOrder": 2,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1003",
      "sysId": "1",
      "code": "menu:permissions",
      "name": "权限目录",
      "type": "MENU",
      "parentId": null,
      "routePath": "/permissions",
      "sortOrder": 3,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1004",
      "sysId": "1",
      "code": "menu:audit",
      "name": "审计日志",
      "type": "MENU",
      "parentId": null,
      "routePath": "/audit",
      "sortOrder": 4,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1010",
      "sysId": "1",
      "code": "sys:member:read",
      "name": "查询成员",
      "type": "ACTION",
      "parentId": "1001",
      "routePath": null,
      "sortOrder": 10,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1011",
      "sysId": "1",
      "code": "sys:member:create",
      "name": "添加成员",
      "type": "ACTION",
      "parentId": "1001",
      "routePath": null,
      "sortOrder": 11,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1012",
      "sysId": "1",
      "code": "sys:member:update",
      "name": "更新成员",
      "type": "ACTION",
      "parentId": "1001",
      "routePath": null,
      "sortOrder": 12,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1013",
      "sysId": "1",
      "code": "sys:member:assign",
      "name": "分配成员角色",
      "type": "ACTION",
      "parentId": "1001",
      "routePath": null,
      "sortOrder": 13,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1014",
      "sysId": "1",
      "code": "sys:role:read",
      "name": "查询角色",
      "type": "ACTION",
      "parentId": "1002",
      "routePath": null,
      "sortOrder": 14,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1015",
      "sysId": "1",
      "code": "sys:role:create",
      "name": "创建角色",
      "type": "ACTION",
      "parentId": "1002",
      "routePath": null,
      "sortOrder": 15,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1016",
      "sysId": "1",
      "code": "sys:role:update",
      "name": "更新角色",
      "type": "ACTION",
      "parentId": "1002",
      "routePath": null,
      "sortOrder": 16,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1017",
      "sysId": "1",
      "code": "sys:role:delete",
      "name": "删除角色",
      "type": "ACTION",
      "parentId": "1002",
      "routePath": null,
      "sortOrder": 17,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1018",
      "sysId": "1",
      "code": "sys:role:grant",
      "name": "分配角色权限",
      "type": "ACTION",
      "parentId": "1002",
      "routePath": null,
      "sortOrder": 18,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1019",
      "sysId": "1",
      "code": "sys:permission:read",
      "name": "查询权限",
      "type": "ACTION",
      "parentId": "1003",
      "routePath": null,
      "sortOrder": 19,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1020",
      "sysId": "1",
      "code": "sys:permission:create",
      "name": "创建权限",
      "type": "ACTION",
      "parentId": "1003",
      "routePath": null,
      "sortOrder": 20,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1021",
      "sysId": "1",
      "code": "sys:permission:update",
      "name": "更新权限",
      "type": "ACTION",
      "parentId": "1003",
      "routePath": null,
      "sortOrder": 21,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1022",
      "sysId": "1",
      "code": "sys:permission:delete",
      "name": "删除权限",
      "type": "ACTION",
      "parentId": "1003",
      "routePath": null,
      "sortOrder": 22,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "1023",
      "sysId": "1",
      "code": "sys:audit:read",
      "name": "查询审计",
      "type": "ACTION",
      "parentId": "1004",
      "routePath": null,
      "sortOrder": 23,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2001",
      "sysId": "2",
      "code": "menu:members",
      "name": "成员管理",
      "type": "MENU",
      "parentId": null,
      "routePath": "/members",
      "sortOrder": 1,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2002",
      "sysId": "2",
      "code": "menu:roles",
      "name": "角色管理",
      "type": "MENU",
      "parentId": null,
      "routePath": "/roles",
      "sortOrder": 2,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2003",
      "sysId": "2",
      "code": "menu:permissions",
      "name": "权限目录",
      "type": "MENU",
      "parentId": null,
      "routePath": "/permissions",
      "sortOrder": 3,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2004",
      "sysId": "2",
      "code": "menu:audit",
      "name": "审计日志",
      "type": "MENU",
      "parentId": null,
      "routePath": "/audit",
      "sortOrder": 4,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2010",
      "sysId": "2",
      "code": "sys:member:read",
      "name": "查询成员",
      "type": "ACTION",
      "parentId": "2001",
      "routePath": null,
      "sortOrder": 10,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2011",
      "sysId": "2",
      "code": "sys:member:create",
      "name": "添加成员",
      "type": "ACTION",
      "parentId": "2001",
      "routePath": null,
      "sortOrder": 11,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2012",
      "sysId": "2",
      "code": "sys:member:update",
      "name": "更新成员",
      "type": "ACTION",
      "parentId": "2001",
      "routePath": null,
      "sortOrder": 12,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2013",
      "sysId": "2",
      "code": "sys:member:assign",
      "name": "分配成员角色",
      "type": "ACTION",
      "parentId": "2001",
      "routePath": null,
      "sortOrder": 13,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2014",
      "sysId": "2",
      "code": "sys:role:read",
      "name": "查询角色",
      "type": "ACTION",
      "parentId": "2002",
      "routePath": null,
      "sortOrder": 14,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2015",
      "sysId": "2",
      "code": "sys:role:create",
      "name": "创建角色",
      "type": "ACTION",
      "parentId": "2002",
      "routePath": null,
      "sortOrder": 15,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2016",
      "sysId": "2",
      "code": "sys:role:update",
      "name": "更新角色",
      "type": "ACTION",
      "parentId": "2002",
      "routePath": null,
      "sortOrder": 16,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2017",
      "sysId": "2",
      "code": "sys:role:delete",
      "name": "删除角色",
      "type": "ACTION",
      "parentId": "2002",
      "routePath": null,
      "sortOrder": 17,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2018",
      "sysId": "2",
      "code": "sys:role:grant",
      "name": "分配角色权限",
      "type": "ACTION",
      "parentId": "2002",
      "routePath": null,
      "sortOrder": 18,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2019",
      "sysId": "2",
      "code": "sys:permission:read",
      "name": "查询权限",
      "type": "ACTION",
      "parentId": "2003",
      "routePath": null,
      "sortOrder": 19,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2020",
      "sysId": "2",
      "code": "sys:permission:create",
      "name": "创建权限",
      "type": "ACTION",
      "parentId": "2003",
      "routePath": null,
      "sortOrder": 20,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2021",
      "sysId": "2",
      "code": "sys:permission:update",
      "name": "更新权限",
      "type": "ACTION",
      "parentId": "2003",
      "routePath": null,
      "sortOrder": 21,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2022",
      "sysId": "2",
      "code": "sys:permission:delete",
      "name": "删除权限",
      "type": "ACTION",
      "parentId": "2003",
      "routePath": null,
      "sortOrder": 22,
      "status": "ENABLED",
      "version": 1
    },
    {
      "id": "2023",
      "sysId": "2",
      "code": "sys:audit:read",
      "name": "查询审计",
      "type": "ACTION",
      "parentId": "2004",
      "routePath": null,
      "sortOrder": 23,
      "status": "ENABLED",
      "version": 1
    }
  ],
  "userRoles": {
    "1:2": [
      "101"
    ],
    "1:3": [
      "104"
    ],
    "1:4": [
      "103"
    ],
    "1:5": [],
    "2:2": [
      "203"
    ],
    "2:3": [
      "201"
    ],
    "2:4": [
      "203"
    ],
    "2:5": []
  },
  "rolePermissions": {
    "101": [
      "1001",
      "1002",
      "1003",
      "1004",
      "1010",
      "1011",
      "1012",
      "1013",
      "1014",
      "1015",
      "1016",
      "1017",
      "1018",
      "1019",
      "1020",
      "1021",
      "1022",
      "1023"
    ],
    "102": [
      "1001",
      "1010",
      "1011",
      "1012",
      "1013"
    ],
    "103": [
      "1001",
      "1010"
    ],
    "104": [
      "1004",
      "1023"
    ],
    "201": [
      "2001",
      "2002",
      "2003",
      "2004",
      "2010",
      "2011",
      "2012",
      "2013",
      "2014",
      "2015",
      "2016",
      "2017",
      "2018",
      "2019",
      "2020",
      "2021",
      "2022",
      "2023"
    ],
    "202": [
      "2001",
      "2010",
      "2011",
      "2012",
      "2013"
    ],
    "203": [
      "2001",
      "2010"
    ],
    "204": [
      "2004",
      "2023"
    ]
  },
  "logs": [
    {
      "id": "9001",
      "sysId": "1",
      "actorId": "1",
      "actorName": "周齐",
      "action": "MEMBER_ROLES_REPLACED",
      "targetType": "MEMBER",
      "targetId": "4",
      "before": {
        "roleIds": []
      },
      "after": {
        "roleIds": [
          "103"
        ]
      },
      "requestId": "demo-init-001",
      "createdAt": "2026-10-20T02:00:00Z"
    }
  ]
};
