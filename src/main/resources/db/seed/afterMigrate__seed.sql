-- Fills the database with fake users, parties and drinks, sized so that missing
-- indexes, N+1 queries and unpaginated lists show. Runs under the `seed` profile
-- after every migrate and does nothing once seeded. See docs/seed-data.md.
do $$
declare
    v_users bigint;
    v_regular bigint;
    v_long_drinker bigint;
begin
    if exists (select 1 from users where email = 'seed-party-regular@example.com') then
        return;
    end if;

    -- Parallel workers would draw from random() in a different order on every run.
    set local max_parallel_workers_per_gather = 0;

    create temp table seed_names on commit drop as
    select array['Matti','Juha','Mikko','Timo','Jari','Antti','Jukka','Mika','Markku','Pekka',
                 'Hannu','Heikki','Sami','Janne','Ville','Tuomas','Lauri','Eero','Onni','Elias',
                 'Aleksi','Joonas','Niko','Petri','Kari','Tapio','Juho','Olli','Väinö','Jyrki'] as male,
           array['Anna','Maria','Laura','Sanna','Johanna','Tiina','Kaisa','Elina','Heidi','Riikka',
                 'Katja','Minna','Päivi','Satu','Anu','Emilia','Aino','Sofia','Venla','Ella',
                 'Iida','Helmi','Jenni','Noora','Saara','Liisa','Hanna','Outi','Sari','Eveliina'] as female,
           array['Korhonen','Virtanen','Mäkinen','Nieminen','Mäkelä','Hämäläinen','Laine','Heikkinen',
                 'Koskinen','Järvinen','Lehtonen','Lehtinen','Saarinen','Salminen','Heinonen','Niemi',
                 'Heikkilä','Kinnunen','Salonen','Turunen','Salo','Laitinen','Tuominen','Rantanen',
                 'Karjalainen','Jokinen','Mattila','Savolainen','Lahtinen','Ahonen','Pääkkönen','Öhman'] as last,
           array['Vappu','Juhannus','Pikkujoulut','Uusivuosi','Mökkiviikonloppu','Saunailta','Synttärit',
                 'Polttarit','Tupaantuliaiset','Approt','Kesäkemut','Rapujuhlat','Laskiainen','Sitsit',
                 'Pelilauantai'] as occasion,
           -- Grams of alcohol: standard drink, 0.5 l beer, glass of wine, light beer.
           array[12, 12, 12, 16, 14, 10]::real[] as portion,
           -- bcrypt of 'seed-password'
           '$2a$10$ZGkaeTmtPNegZgrzYe.cG.y0irneo6aFmgVVPfYDzA7HOVbIDGF/2'::text as password_hash
    from (select setseed(0.42)) seed;

    -- Users -----------------------------------------------------------------

    create temp table seed_user on commit drop as
    select g as n,
           nextval('users_seq') as id,
           case when random() < 0.55 then 'MALE' else 'FEMALE' end as sex,
           random() as kind
    from generate_series(1, round(100000 * ${scale})::int) g;

    select count(*) into v_users from seed_user;

    -- kind: below 0.1 a guest added by name to a party, below 0.3 a Google
    -- account, otherwise an email and password account.
    insert into users (id, name, email, sex, weight, guest, auth_method, open_id, password, passphrase)
    select u.id,
           case when u.sex = 'MALE'
                then nm.male[1 + floor(random() * cardinality(nm.male))::int]
                else nm.female[1 + floor(random() * cardinality(nm.female))::int]
           end || ' ' || nm.last[1 + floor(random() * cardinality(nm.last))::int],
           case when u.kind >= 0.1 then 'seed-user-' || u.n || '@example.com' end,
           u.sex,
           greatest(45, least(150,
               case when u.sex = 'MALE' then 85 else 68 end
               + case when u.sex = 'MALE' then 13 else 11 end
                 * sqrt(-2 * ln(1 - random())) * cos(2 * pi() * random()))),
           u.kind < 0.1,
           case when u.kind < 0.3 then 'OPENID' else 'PASSWORD' end,
           case when u.kind >= 0.1 and u.kind < 0.3 then 'seed-openid-' || u.n end,
           case when u.kind >= 0.3 then nm.password_hash end,
           case when random() < 0.05 then md5(random()::text)::uuid::text end
    from seed_user u, seed_names nm
    order by u.id;

    create temp table seed_extreme on commit drop as
    select nextval('users_seq') as id, x.role, x.email, x.name
    from (values ('regular',      'seed-party-regular@example.com', 'Seed Party Regular'),
                 ('long_drinker', 'seed-long-drinker@example.com',  'Seed Long-time Drinker'),
                 ('huge_host',    'seed-huge-party@example.com',    'Seed Huge Party Host')) x(role, email, name);

    select max(id) filter (where role = 'regular'),
           max(id) filter (where role = 'long_drinker')
    into v_regular, v_long_drinker
    from seed_extreme;

    insert into users (id, name, email, sex, weight, guest, auth_method, password)
    select x.id, x.name, x.email, 'MALE', 85, false, 'PASSWORD', nm.password_hash
    from seed_extreme x, seed_names nm;

    -- Parties ---------------------------------------------------------------

    -- The app stores timestamps as UTC, whatever the session's time zone is.

    -- 2-20 participants, most parties at the small end, about 8 on average.
    create temp table seed_party on commit drop as
    select nextval('party_seq') as id,
           (now() at time zone 'UTC') - random() * interval '5 years' as start_time,
           2 + floor(power(random(), 2) * 19)::int as size,
           'background' as kind
    from generate_series(1, round(40000 * ${scale})::int);

    insert into seed_party
    select nextval('party_seq'), (now() at time zone 'UTC') - random() * interval '6 hours',
           2 + floor(power(random(), 2) * 19)::int, 'active'
    from generate_series(1, 20);

    insert into seed_party
    select nextval('party_seq'), (now() at time zone 'UTC') - random() * interval '5 years',
           2 + floor(power(random(), 2) * 19)::int, 'regular'
    from generate_series(1, 1000);

    insert into seed_party
    select nextval('party_seq'), (now() at time zone 'UTC') - interval '4 hours', 499, 'huge';

    insert into party (id, name, start_time, end_time)
    select p.id,
           case when p.kind = 'huge' then 'Seed Huge Party'
                else nm.occasion[1 + floor(random() * cardinality(nm.occasion))::int]
                     || ' ' || extract(year from p.start_time)
           end,
           p.start_time,
           p.start_time + interval '48 hours'
    from seed_party p, seed_names nm
    order by p.id;

    -- Participants ----------------------------------------------------------

    create temp table seed_participation on commit drop as
    select distinct p.id as party_id, u.id as user_id, p.start_time
    from seed_party p
    cross join lateral (select 1 + floor(random() * v_users)::int as n
                        from generate_series(1, p.size)) pick
    join seed_user u on u.n = pick.n
    where p.kind <> 'huge';

    insert into seed_participation
    select p.id, u.id, p.start_time
    from seed_party p,
         lateral (select id from seed_user order by random() limit p.size) u
    where p.kind = 'huge';

    insert into seed_participation
    select p.id, x.id, p.start_time
    from seed_party p
    join seed_extreme x on (p.kind = 'regular' and x.role = 'regular')
                        or (p.kind = 'huge' and x.role = 'huge_host');

    insert into seed_participation
    select p.id, v_long_drinker, p.start_time
    from (select id, start_time from seed_party where kind = 'background'
          order by random() limit 30) p;

    insert into participants (party_id, participant_id)
    select party_id, user_id from seed_participation order by party_id, user_id;

    -- Drinks ----------------------------------------------------------------

    -- Each participant drinks 0-15 drinks, about 6 on average, starting up to two
    -- hours into the party and spaced 15-60 minutes apart.
    create temp table seed_drink_plan on commit drop as
    select party_id, user_id, start_time,
           floor(power(random(), 1.4) * 16)::int as drinks,
           random() * 120 as first_offset_minutes
    from seed_participation
    where user_id not in (v_regular, v_long_drinker);

    create temp table seed_drink (drinker_id bigint, time_stamp timestamp, alcohol real) on commit drop;

    insert into seed_drink
    select d.user_id,
           d.start_time + interval '1 minute' * (d.first_offset_minutes
               + sum(15 + random() * 45) over (partition by d.party_id, d.user_id order by i)),
           nm.portion[1 + floor(random() * cardinality(nm.portion))::int]
    from seed_drink_plan d
    cross join lateral generate_series(1, d.drinks) i
    cross join seed_names nm;

    -- The party regular has one drink at each of their parties.
    insert into seed_drink
    select user_id, start_time + interval '30 minutes', 12
    from seed_participation
    where user_id = v_regular;

    -- The long-time drinker: 1,500 evenings of 10 drinks over five years.
    insert into seed_drink
    select v_long_drinker,
           s.evening + interval '1 minute' * sum(15 + random() * 45) over (partition by s.k order by i),
           nm.portion[1 + floor(random() * cardinality(nm.portion))::int]
    from (select k, date_trunc('day', (now() at time zone 'UTC') - random() * interval '5 years')
                    + interval '18 hours' + random() * interval '4 hours' as evening
          from generate_series(1, 1500) k) s
    cross join generate_series(1, 10) i
    cross join seed_names nm;

    insert into drink (id, alcohol, time_stamp, drinker_id)
    select nextval('drink_seq'), alcohol, time_stamp, drinker_id
    from (select * from seed_drink where time_stamp <= (now() at time zone 'UTC') order by time_stamp) d;

    analyze users;
    analyze party;
    analyze participants;
    analyze drink;
end
$$;
