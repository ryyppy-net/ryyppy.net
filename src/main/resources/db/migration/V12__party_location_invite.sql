-- A party's one location invite: where the party is and how far from it someone may join.
create table party_location_invite
(
    party_id   bigint           not null primary key references party,
    latitude   double precision not null,
    longitude  double precision not null,
    radius_m   integer          not null,
    created_by bigint           not null references users,
    created_at timestamp        not null
);
