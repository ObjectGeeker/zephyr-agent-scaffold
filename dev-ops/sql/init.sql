create table tb_user
(
    id              varchar(50)                           not null comment '主键'
        primary key,
    user_account    varchar(64)                           not null comment '登录账号',
    password        varchar(128)                          not null comment '密码（BCrypt 等哈希，禁止明文）',
    wx_openid       varchar(50)                           null comment '微信 openid',
    status          varchar(10)                           null comment '用户状态',
    roles           varchar(100)                          null comment '用户所属角色',
    username        varchar(64) default ''                not null comment '展示名称',
    email           varchar(128)                          null comment '邮箱',
    phone           varchar(20)                           null comment '手机号',
    avatar          varchar(512)                          null comment '头像 URL',
    last_login_time datetime                              null comment '最后登录时间',
    create_time     datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    create_by       bigint                                null comment '创建人用户 ID',
    update_by       bigint                                null comment '更新人用户 ID',
    deleted         tinyint     default 0                 not null comment '逻辑删除：0-未删除 1-已删除',
    constraint uk_user_account
        unique (user_account)
)
    comment '用户表' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on tb_user (deleted);