-- Columns the app looks rows up by; without these each lookup reads the whole table.
create index if not exists drink_drinker_id_idx on drink (drinker_id);
create index if not exists participants_party_id_idx on participants (party_id);
create index if not exists participants_participant_id_idx on participants (participant_id);
create index if not exists users_email_idx on users (email);
create index if not exists users_passphrase_idx on users (passphrase);
create index if not exists users_open_id_idx on users (open_id);
