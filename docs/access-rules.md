# Ryyppy.net access rules: users and parties

You control your own account. You see and act on other people only through the parties you share with them, and everyone outside a party sees nothing of it.

| Term | Meaning |
| --- | --- |
| User | Someone with an account, who signs in with email and password or with Google. |
| Guest | A participant added to a party with just a name, sex and weight. A guest has no account and cannot sign in. |
| Party | A drinking session with a name, a start time, an end time and its participants. |
| Member | A participant of a party, either a user or a guest. |
| Party-mate | A member of a party you are also a member of. |

## Signing in

Everything about drinks, parties and people requires signing in. A visitor who is not signed in is taken to the login page.

Without signing in, a visitor can only:

- read the privacy policy and the terms of use
- register an account with name, sex, weight, email and password
- sign in with email and password, or with Google

Each email address belongs to exactly one account. Signing in with Google uses the account with that email, and creates one if there is none.

## Your own account

Only you can see and change your profile and your full drink log.

- Your profile is your name, email, sex and weight. Only you can change it, and a new email must not belong to another account.
- You can log drinks for yourself, now or at an earlier time, and change or delete them. This works from your own dashboard whether or not a party has ended.
- Only you can see your full list of drinks with their times, and your drinks per day.
- You see the parties you are a member of, and no others.

## Seeing a party

Only a party's members can see it. Anyone else cannot open it, see who is in it or see anyone's readings, even with its link or number.

The one exception is a join link: a signed-in user who holds a valid one sees the party's name, start time and end time, but not who is in it, and can join it. A visitor who opens a join link while signed out sees nothing of the party: they are asked to sign in or register first, and then continue to the link.

A member sees:

- the party's name, start time and end time
- every participant's name and profile picture
- every participant's promille graph from the party's start up to now, or up to its end once it has ended, and their promilles and number of drinks at that moment

These come only from drinks logged during the party, so they show nothing drunk before it started or after it ended, and nothing is projected past now.

The classic party view instead shows each participant's current promilles, drinks since they were last sober, time since their last drink and promille graph for the last five hours, whenever those drinks were logged.

A member does not see other participants' email, weight or individual drink times.

## Who is in a party

All members of a party are equal: any member can add and remove participants, and nobody outside the party can change who is in it.

| Action | Who can do it | How it works |
| --- | --- | --- |
| Create a party | Any signed-in user | You become its first member. The party lasts 1 day unless you give a number of days. |
| Change a party's duration | Any member | In whole days, at least 1. The party ends that many days after it started. |
| Add a user | Any member | Found by their exact email address, or picked from people you have been in parties with. The party then appears in their list. |
| Add a guest | Any member | Give a name, sex and weight. The guest belongs to that party only. |
| Remove a participant | Any member | Any participant, user or guest, including yourself. |
| Share a join link | Any member | A party has one join link at a time. Any member can show it again, replace it with a new one, which stops the old one working, or turn it off. |
| Join with a link | Any signed-in user with a valid link | Adds only yourself; works until the party ends. A link that is unknown, replaced, turned off or past its party's end does not work, and does not say which. |

## Party-mates

Party-mates can keep score for each other, so one phone can log drinks for the whole table. People who share no party with you cannot see or do anything that concerns you.

Joining a party with its link makes you a party-mate of all its members, the same as being added to it.

For a party-mate, you can:

- see their name, current promilles, total drinks, time since their last drink and their promille graph
- log a drink for them through a party you share with them, now or at an earlier time (not from your own dashboard or any user-level endpoint)
- change the alcohol amount of one of their drinks, or delete it

You cannot see a party-mate's full drink log or change their profile. A guest has no account, so the other members of the guest's party keep the guest's drinks.

## Ended parties

Once a party has ended, that is, its end time has been reached, no drink can be added to its members through that party, whoever asks and whether it is their own drink or a party-mate's. The party page of either front end then offers no way to add one, and the server refuses it with 409 Conflict. A drink for someone else can only be added through a party, so it cannot be added to the members of an ended party any other way. This covers only adding a drink through the party: changing or deleting an existing drink stays allowed, so a drink tapped just before the end can still be undone or edited, and you can still log a drink for yourself from your own dashboard, which is not tied to any party.
