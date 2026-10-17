-- One account per email; logins look it up lower-cased. An empty email is no account.
create unique index users_lower_email_key on users (lower(email)) where email <> '';
