-- A party's one active join link; the token is kept so members can show the link again.
create table party_join_link
(
    party_id   bigint      not null primary key references party,
    token      varchar(32) not null unique,
    created_by bigint      not null references users,
    created_at timestamp   not null
);
