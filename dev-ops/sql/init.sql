-- 系统用户表：只存储最基本的用户信息
CREATE TABLE IF NOT EXISTS `tb_user`
(
    `id`              varchar(32)  NOT NULL COMMENT '主键',
    `CREATE_USER`     varchar(32)           DEFAULT NULL COMMENT '创建人',
    `CREATE_TIME`     datetime              DEFAULT NULL COMMENT '创建时间',
    `UPDATE_USER`     varchar(32)           DEFAULT NULL COMMENT '更新人',
    `UPDATE_TIME`     datetime              DEFAULT NULL COMMENT '更新时间',
    `is_delete`       tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    `username`        varchar(64)  NOT NULL COMMENT '用户名',
    `account`         varchar(64)  NOT NULL COMMENT '登录账号',
    `email`           varchar(128)          DEFAULT NULL COMMENT '邮箱',
    `phone`           varchar(32)           DEFAULT NULL COMMENT '手机号',
    `password`        varchar(255) NOT NULL COMMENT '密码',
    `avatar_url`      varchar(512)          DEFAULT NULL COMMENT '头像地址',
    `wx_open_id`      varchar(128)          DEFAULT NULL COMMENT '微信 OpenID',
    `user_roles`      json                  DEFAULT NULL COMMENT '用户角色列表',
    `last_login_time` datetime              DEFAULT NULL COMMENT '最后登录时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tb_user_account` (`account`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '系统用户表';
