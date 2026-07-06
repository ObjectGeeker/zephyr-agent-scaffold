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

create table tb_file
(
    id           varchar(50)                          not null comment '主键'
        primary key,
    storage_type varchar(32)                          null comment '存储类型',
    bucket       varchar(128)                         null comment '存储桶名称',
    object_key   varchar(512)                         not null comment '对象存储路径',
    file_name    varchar(255)                         null comment '存储文件名',
    origin_name  varchar(255)                         null comment '用户原始文件名',
    file_size    bigint                               null comment '文件大小（字节）',
    content_type varchar(128)                         null comment 'MIME 类型',
    file_hash    varchar(64)                          null comment '文件哈希（SHA-256）',
    user_id      varchar(50)                          null comment '所属用户 ID',
    biz_type     varchar(64)                          null comment '业务类型',
    biz_id       varchar(50)                          null comment '业务关联 ID',
    status       varchar(20)                          null comment '文件状态',
    is_temp      tinyint(1) default 0                 not null comment '是否临时文件：0-否 1-是',
    create_time  datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time  datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    create_by    varchar(50)                          null comment '创建人用户 ID',
    update_by    varchar(50)                          null comment '更新人用户 ID',
    deleted      tinyint    default 0                 not null comment '逻辑删除：0-未删除 1-已删除'
)
    comment '系统文件元数据表' collate = utf8mb4_unicode_ci;

create index idx_file_object_key
    on tb_file (object_key);

create index idx_file_hash
    on tb_file (file_hash);

create index idx_file_user_id
    on tb_file (user_id);

create index idx_file_biz
    on tb_file (biz_type, biz_id);

create index idx_file_deleted
    on tb_file (deleted);

create table tb_session
(
    id               varchar(50)                           not null comment '主键，同时作为 Agent threadId'
        primary key,
    session_name     varchar(128)                          null comment '会话名称',
    user_id          varchar(50)                           not null comment '所属用户 ID',
    agent_id         varchar(50)                           null comment '智能体 ID',
    long_time_memory text                                  null comment '会话级长期记忆总结',
    summary_count    int         default 0                 not null comment '记忆总结次数',
    last_message_at  datetime                              null comment '最后一条消息时间',
    status           varchar(20) default 'active'          not null comment '会话状态：active-活跃 archived-归档',
    create_time      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    create_by        varchar(50)                           null comment '创建人用户 ID',
    update_by        varchar(50)                           null comment '更新人用户 ID',
    deleted          tinyint     default 0                 not null comment '逻辑删除：0-未删除 1-已删除'
)
    comment 'Agent 会话表' collate = utf8mb4_unicode_ci;

create index idx_session_user_list
    on tb_session (user_id, deleted, last_message_at);

create table tb_message
(
    id              varchar(50)                        not null comment '主键'
        primary key,
    session_id      varchar(50)                        not null comment '所属会话 ID',
    role            varchar(20)                        not null comment '消息角色：user/assistant/system/tool',
    message_content text                               null comment '消息内容',
    attachment      text                               null comment '附件 fileId 列表（JSON 数组）',
    metadata        text                               null comment '工具调用等扩展信息（JSON）',
    message_index   int                                not null comment '会话内消息序号',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    create_by       varchar(50)                        null comment '创建人用户 ID',
    update_by       varchar(50)                        null comment '更新人用户 ID',
    deleted         tinyint  default 0                 not null comment '逻辑删除：0-未删除 1-已删除',
    constraint uk_session_msg_index
        unique (session_id, message_index)
)
    comment 'Agent 消息记录表' collate = utf8mb4_unicode_ci;

create index idx_message_session
    on tb_message (session_id, deleted, message_index);