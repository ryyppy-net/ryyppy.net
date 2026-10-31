alter table party add column end_time timestamp;
update party set end_time = start_time + interval '48 hours';
alter table party alter column end_time set not null;
