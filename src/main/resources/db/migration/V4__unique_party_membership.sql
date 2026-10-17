-- A user is in a party once.
alter table participants add constraint participants_party_participant_key unique (party_id, participant_id);
