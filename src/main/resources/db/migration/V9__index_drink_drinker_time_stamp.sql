-- Promilles read a drinker's drinks from the last days, not their whole history.
create index if not exists drink_drinker_id_time_stamp_idx on drink (drinker_id, time_stamp);
