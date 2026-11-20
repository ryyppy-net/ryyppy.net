-- Who started the party; null for parties started before it was recorded.
alter table party add column started_by bigint references users;
