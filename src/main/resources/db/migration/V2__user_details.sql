create table if not exists users(
    username varchar(50) not null primary key,
    issuer varchar(200) not null,
    password varchar(500),
    enabled boolean not null
);

create table if not exists authorities (
    username varchar(50) not null,
    authority varchar(50) not null,
    constraint fk_authorities_users foreign key(username) references users(username)
);
create unique index ix_auth_username on authorities (username,authority);