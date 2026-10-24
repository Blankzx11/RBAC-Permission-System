-- 新建开发库演示数据；先执行001。密码占位符不能用于登录，必须通过安全初始化器设置哈希。

-- 仅RBAC0。数据初始化属于受控迁移，生产业务变更必须写审计。

SET time_zone = '+00:00';

START TRANSACTION;

INSERT INTO iam_user(id,username,display_name,password_hash,platform_admin,status) VALUES
('1','platform_admin','周齐','!DEMO_LOGIN_DISABLED_REPLACE_WITH_HASH!',TRUE,'ENABLED'),
('2','system_admin','林岚','!DEMO_LOGIN_DISABLED_REPLACE_WITH_HASH!',FALSE,'ENABLED'),
('3','auditor','许宁','!DEMO_LOGIN_DISABLED_REPLACE_WITH_HASH!',FALSE,'ENABLED'),
('4','chenyu','陈雨','!DEMO_LOGIN_DISABLED_REPLACE_WITH_HASH!',FALSE,'ENABLED'),
('5','lichen','李晨','!DEMO_LOGIN_DISABLED_REPLACE_WITH_HASH!',FALSE,'DISABLED');

INSERT INTO iam_system(id,code,name,description) VALUES
('1','EDU','系统 A · 教务管理','教务业务权限边界'),
('2','RESEARCH','系统 B · 科研管理','科研业务权限边界');

INSERT INTO iam_member(sys_id,user_id,display_name,created_by) VALUES
('1','2','林岚','1'),
('1','3','许宁','1'),
('1','4','陈雨','1'),
('1','5','李晨','1'),
('2','2','林岚','1'),
('2','3','许宁','1'),
('2','4','陈雨','1'),
('2','5','李晨','1');

INSERT INTO iam_role(id,sys_id,code,name,builtin,created_by) VALUES
('101','1','SYS_ADMIN','系统管理员',TRUE,'1'),
('102','1','MEMBER_MANAGER','成员管理员',FALSE,'1'),
('103','1','READER','成员查看员',FALSE,'1'),
('104','1','AUDITOR','审计查看员',FALSE,'1'),
('201','2','SYS_ADMIN','系统管理员',TRUE,'1'),
('202','2','MEMBER_MANAGER','成员管理员',FALSE,'1'),
('203','2','READER','成员查看员',FALSE,'1'),
('204','2','AUDITOR','审计查看员',FALSE,'1');

INSERT INTO iam_permission(id,sys_id,code,name,type,parent_id,route_path,sort_order,created_by) VALUES
('1001','1','menu:members','成员管理','MENU',NULL,'/members','1','1'),
('1002','1','menu:roles','角色管理','MENU',NULL,'/roles','2','1'),
('1003','1','menu:permissions','权限目录','MENU',NULL,'/permissions','3','1'),
('1004','1','menu:audit','审计日志','MENU',NULL,'/audit','4','1'),
('1010','1','sys:member:read','查询成员','ACTION','1001',NULL,'10','1'),
('1011','1','sys:member:create','添加成员','ACTION','1001',NULL,'11','1'),
('1012','1','sys:member:update','更新成员','ACTION','1001',NULL,'12','1'),
('1013','1','sys:member:assign','分配成员角色','ACTION','1001',NULL,'13','1'),
('1014','1','sys:role:read','查询角色','ACTION','1002',NULL,'14','1'),
('1015','1','sys:role:create','创建角色','ACTION','1002',NULL,'15','1'),
('1016','1','sys:role:update','更新角色','ACTION','1002',NULL,'16','1'),
('1017','1','sys:role:delete','删除角色','ACTION','1002',NULL,'17','1'),
('1018','1','sys:role:grant','分配角色权限','ACTION','1002',NULL,'18','1'),
('1019','1','sys:permission:read','查询权限','ACTION','1003',NULL,'19','1'),
('1020','1','sys:permission:create','创建权限','ACTION','1003',NULL,'20','1'),
('1021','1','sys:permission:update','更新权限','ACTION','1003',NULL,'21','1'),
('1022','1','sys:permission:delete','删除权限','ACTION','1003',NULL,'22','1'),
('1023','1','sys:audit:read','查询审计','ACTION','1004',NULL,'23','1'),
('2001','2','menu:members','成员管理','MENU',NULL,'/members','1','1'),
('2002','2','menu:roles','角色管理','MENU',NULL,'/roles','2','1'),
('2003','2','menu:permissions','权限目录','MENU',NULL,'/permissions','3','1'),
('2004','2','menu:audit','审计日志','MENU',NULL,'/audit','4','1'),
('2010','2','sys:member:read','查询成员','ACTION','2001',NULL,'10','1'),
('2011','2','sys:member:create','添加成员','ACTION','2001',NULL,'11','1'),
('2012','2','sys:member:update','更新成员','ACTION','2001',NULL,'12','1'),
('2013','2','sys:member:assign','分配成员角色','ACTION','2001',NULL,'13','1'),
('2014','2','sys:role:read','查询角色','ACTION','2002',NULL,'14','1'),
('2015','2','sys:role:create','创建角色','ACTION','2002',NULL,'15','1'),
('2016','2','sys:role:update','更新角色','ACTION','2002',NULL,'16','1'),
('2017','2','sys:role:delete','删除角色','ACTION','2002',NULL,'17','1'),
('2018','2','sys:role:grant','分配角色权限','ACTION','2002',NULL,'18','1'),
('2019','2','sys:permission:read','查询权限','ACTION','2003',NULL,'19','1'),
('2020','2','sys:permission:create','创建权限','ACTION','2003',NULL,'20','1'),
('2021','2','sys:permission:update','更新权限','ACTION','2003',NULL,'21','1'),
('2022','2','sys:permission:delete','删除权限','ACTION','2003',NULL,'22','1'),
('2023','2','sys:audit:read','查询审计','ACTION','2004',NULL,'23','1');

INSERT INTO iam_role_permission(sys_id,role_id,permission_id,created_by) VALUES
('1','101','1001','1'),
('1','101','1002','1'),
('1','101','1003','1'),
('1','101','1004','1'),
('1','101','1010','1'),
('1','101','1011','1'),
('1','101','1012','1'),
('1','101','1013','1'),
('1','101','1014','1'),
('1','101','1015','1'),
('1','101','1016','1'),
('1','101','1017','1'),
('1','101','1018','1'),
('1','101','1019','1'),
('1','101','1020','1'),
('1','101','1021','1'),
('1','101','1022','1'),
('1','101','1023','1'),
('1','102','1001','1'),
('1','102','1010','1'),
('1','102','1011','1'),
('1','102','1012','1'),
('1','102','1013','1'),
('1','103','1001','1'),
('1','103','1010','1'),
('1','104','1004','1'),
('1','104','1023','1'),
('2','201','2001','1'),
('2','201','2002','1'),
('2','201','2003','1'),
('2','201','2004','1'),
('2','201','2010','1'),
('2','201','2011','1'),
('2','201','2012','1'),
('2','201','2013','1'),
('2','201','2014','1'),
('2','201','2015','1'),
('2','201','2016','1'),
('2','201','2017','1'),
('2','201','2018','1'),
('2','201','2019','1'),
('2','201','2020','1'),
('2','201','2021','1'),
('2','201','2022','1'),
('2','201','2023','1'),
('2','202','2001','1'),
('2','202','2010','1'),
('2','202','2011','1'),
('2','202','2012','1'),
('2','202','2013','1'),
('2','203','2001','1'),
('2','203','2010','1'),
('2','204','2004','1'),
('2','204','2023','1');

INSERT INTO iam_user_role(sys_id,user_id,role_id,created_by) VALUES
('1','2','101','1'),
('1','3','104','1'),
('1','4','103','1'),
('2','2','203','1'),
('2','3','201','1'),
('2','4','203','1');

INSERT INTO iam_audit_log(id,sys_id,actor_id,actor_name,action,target_type,target_id,before_json,after_json,request_id,created_at) VALUES
('9001','1','1','周齐','MEMBER_ROLES_REPLACED','MEMBER','4','{"roleIds": []}','{"roleIds": ["103"]}','demo-init-001','2026-10-20 02:00:00.000');

COMMIT;
