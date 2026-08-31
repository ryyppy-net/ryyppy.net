-- Baseline snapshot of the production schema as it existed before Flyway was
-- introduced. Generated from a DDL dump of the production database
-- (chosen-quagga-4227, database ryyppynet-prod) on 2026-08-31.
--
-- This migration is informational: Flyway never executes it against
-- production, because `flyway:baseline` marks V1 as already applied there.
-- It only actually runs against a fresh, empty database (local dev, CI).

create sequence hibernate_sequence;

create sequence drink_seq
    increment by 50;

create sequence party_seq
    increment by 50;

create sequence users_seq
    increment by 50;

create table party
(
    id         bigint not null
        primary key,
    name       varchar(255),
    start_time timestamp
);

create table users
(
    id          bigint  not null
        primary key,
    auth_method varchar(255),
    email       varchar(255),
    guest       boolean not null,
    name        varchar(255),
    open_id     varchar(255),
    passphrase  varchar(255),
    password    varchar(255),
    sex         varchar(255),
    weight      real    not null
);

create table drink
(
    id         bigint not null
        primary key,
    alcohol    real   not null,
    time_stamp timestamp,
    drinker_id bigint
        constraint fkc82kvwq6ppaugo6wk2b90gula
            references users
);

create table participants
(
    party_id       bigint not null
        constraint fkolhjebbydtxth5axxetem5wnm
            references party,
    participant_id bigint not null
        constraint fk7e4xjp7oyhhnhhv3wu2evc05
            references users
);

-- Hibernate's auto-generated global temp tables for bulk HQL update/delete
-- operations (one per entity: users, drink, party).
create table hte_users
(
    guest       boolean,
    id          integer,
    rn_         integer  not null,
    weight      real,
    hib_sess_id char(36) not null,
    auth_method varchar(255),
    email       varchar(255),
    name        varchar(255),
    open_id     varchar(255),
    passphrase  varchar(255),
    password    varchar(255),
    sex         varchar(255),
    primary key (rn_, hib_sess_id)
);

create table hte_drink
(
    alcohol     real,
    drinker_id  integer,
    id          integer,
    rn_         integer  not null,
    time_stamp  timestamp(6),
    hib_sess_id char(36) not null,
    primary key (rn_, hib_sess_id)
);

create table hte_party
(
    id          integer,
    rn_         integer  not null,
    start_time  timestamp(6),
    hib_sess_id char(36) not null,
    name        varchar(255),
    primary key (rn_, hib_sess_id)
);

-- Spring Session JDBC schema (normally managed by
-- spring.session.jdbc.initialize-schema; now owned by this migration
-- instead, see application.yml).
create table spring_session
(
    primary_id            char(36) not null
        constraint spring_session_pk
            primary key,
    session_id            char(36) not null
        constraint spring_session_ix1
            unique,
    creation_time         bigint   not null,
    last_access_time      bigint   not null,
    max_inactive_interval bigint   not null,
    expiry_time           bigint   not null,
    principal_name        varchar(100)
);

create index spring_session_ix3
    on spring_session (principal_name);

create index spring_session_ix2
    on spring_session (expiry_time);

create table spring_session_attributes
(
    session_primary_id char(36)     not null
        constraint spring_session_attributes_fk
            references spring_session
            on delete cascade,
    attribute_name     varchar(200) not null,
    attribute_bytes    bytea        not null,
    constraint spring_session_attributes_pk
        primary key (session_primary_id, attribute_name)
);
