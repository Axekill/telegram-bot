--liquibase formatted sql

--changeset andrey:2
create table notification_task (
id bigint primary key,
chat_id  bigint,
text_task varchar(4096),
date_time date
);

