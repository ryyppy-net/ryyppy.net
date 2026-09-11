# UI inventory and E2E coverage plan

Written ahead of the JSP-to-Thymeleaf migration, to answer two questions:
what UI surface exists, and what regression coverage each page needs before
its template is rewritten.

**Verification status:** everything below comes from static reference
analysis of the repo (controllers, JSPs, Angular routes, static JS). The app
was not booted while writing this — the session had no Docker/Postgres — so
the "no inbound references" findings are grep-complete but not runtime-
confirmed. Each one lists how to confirm it against a running instance.

---

## 1. The UI surface

Three server-rendered entry points feed two frontends, plus one orphan.

### Modern UI (AngularJS) — `/app/index.html#/...`

Served by `DefaultController.appIndex()` → `WEB-INF/jsp/app/index.jsp`, which
inlines every partial into `$templateCache` and embeds the profile, parties,
own drinks and drink-history CSV as `window.__INITIAL_*__`.

| Route | Controller / partial | What it does |
|---|---|---|
| `#/` | `UserCtrl` / `user.html` | Own drinker tile (click to drink), party list, promille history graph, last 5 drinks with delete |
| `#/profile-settings` | `ProfileSettingsCtrl` / `profile_settings.html` | Edit name, email, sex, weight |
| `#/party-admin/` | `GeneralPartyAdminCtrl` / `party_admin_general.html` | Create a party, list own parties, leave a party |
| `#/party-admin/:partyId` | `PartyAdminCtrl` / `party_admin.html` | Add participant (old friend / registered by email / guest), remove participant |
| `#/party/:partyId` | `PartyCtrl` / `party.html` | 3-wide grid of drinker tiles, click to drink with undo countdown and size/ABV editor |

Shared partials: `user_menu.html` and `party_menu.html` (both carry the
"Vanha käyttöliittymä" switch and logout), `user_button.html` (`DrinkerCtrl`).
Backed by the v2 API (`/API/v2/parties`, `/API/v2/profile`).

### Classic UI (jQuery + JSP) — `/ui/...`

| URL | View | What it does |
|---|---|---|
| `/ui/login` | `login.jsp` | Username/password form, total-drink counter, Google sign-in or OAuth relay link, links to registration / privacy / terms |
| `/ui/newuser` | `newuser.jsp` | Registration; live email-availability check via `/ui/checkEmail` |
| `/ui/user` | `user.jsp` | Own drinker button grid, party list with leave, history graph, and three jQuery UI dialogs: add party, edit profile, add/remove drinks |
| `/ui/party?id=N` | `party.jsp` | Participant button grid (polled from `/API/parties/{id}`), group graph dialog, add-drinker dialog (registered by email / guest), kick dialog |
| `/ui/terms` | `terms.jsp` | Terms of service (still JSP) |
| `/ui/privacy` | `templates/privacy.html` | Privacy policy — **already converted to Thymeleaf** (the migration pilot) |

Backed by the legacy `/API/...` XML endpoints via `static/js/common.js`
(`RyyppyAPI`), `userbutton.js`, `userbuttongrid.js`, `party.js`,
`partygraph.js`, `drinkerchecks.js`.

Support views: `error.jsp` (Spring Boot's `error` view name resolves through
the JSP resolver), `loginerror.jsp` (see §2).

---

## 2. Unreferenced / dead surface

Ordered roughly by confidence.

### 2.1 `/static/mob/` — orphan mobile prototype
A jQuery Mobile 1.0b2 + Backbone + Handlebars app. **Zero inbound
references** anywhere in the repo. It is also unfinished: `js/app.js`'s
`App.addDrinker` creates a hardcoded `{id: 1, promilles: 1.2}` model rather
than calling the API, `index.html` has an empty `<title>` and an `<h1>Hello</h1>`
header. Publicly reachable, since `/static/**` is `permitAll`.
*Confirm:* open `/static/mob/index.html` on a running instance.
*Suggested action:* delete the directory. No test.

### 2.2 `/ui/viewParty` — unreferenced duplicate of `/ui/party`
No link anywhere. It renders the same `party.jsp` but puts `users` in the
model, while `party.jsp` reads `${user.id}` — so the kick dialog would list
the current user too. Its `kick` query parameter performs a GET-triggered
unlink with no confirmation.
*Confirm:* `grep -rn viewParty src/` returns only the controller itself.
*Suggested action:* delete the handler.

### 2.3 `/ui/addDrink` — unreferenced and broken
No inbound references, and it ends with `return "redirect:parties"` — there
is no `parties` mapping under `/ui` (or anywhere), so a successful call would
redirect into a 404. The drink *is* added first, so the failure is silent
from the caller's point of view.
*Confirm:* `GET /ui/addDrink?id=<your id>` while logged in → error page, and
the drink shows up anyway.
*Suggested action:* delete the handler (the modern UI and `/ui/user`'s
drinker button both add drinks through the API instead).

### 2.4 `/ui/passphrase`, `/ui/passphrase-generate`, `passphrase.jsp` — orphan page, live API
No page in either UI links to the passphrase view; it is reachable only by
typing the URL. The page's copy button is ZeroClipboard, i.e. Flash
(`static/vendor/zeroclipboard/ZeroClipboard.swf`) — non-functional in any
current browser.

**But the backing API is not dead by the same evidence:** `/API/passphrase/**`
is explicitly `permitAll` in `WebSecurityConfiguration` and exposes
`GET /API/passphrase/{p}`, `.../add-drink/{time}` and `.../undo-drink`. That
is the shape of an external client (a script, a hardware button), which this
repo would not reference. Removing the page would leave existing passphrase
holders with no way to see or rotate their passphrase.
*Decision needed:* either (a) keep the page, link it from the settings
menus, and replace ZeroClipboard with `navigator.clipboard`, or (b) drop the
page and keep the API. Worth checking production logs for
`/API/passphrase/` traffic before choosing.

### 2.5 `/ui/loginerror` + `loginerror.jsp` — unreachable
Nothing routes to it. Form login failure uses Spring Security's default,
`/ui/login?error`; OAuth2 failure goes through `UserNotRegisteredFailureHandler`
to the registration URL. Related gap: `login.jsp` renders no message for
`?error`, so a rejected login silently redraws the form.
*Confirm:* the existing e2e test already asserts the failure lands on
`/ui/login?error`.
*Suggested action:* either wire the failure handler to `/ui/loginerror`, or
delete the view and show an inline error on `login.jsp`. Either way the
"failed login gives no feedback" gap is worth a test.

### 2.6 Smaller orphans
- `GET /API/parties/{partyId}/get-history` — no frontend reference
  (`partygraph.js` uses `/API/users/{id}/show-history`).
- `public/app/index-async.html` — no references.
- `public/static/js/googlegraph.js` — no references.
- Inside live code: `user.jsp`'s `configureDrinksDialogOpened()` calls
  `$("#time").datetimepicker(...)`, but `user.jsp` contains no `#time`
  element — the picker actually in use is `new DateTimePicker('#historyDrinkTime')`.

---

## 3. Existing E2E coverage

`e2e/tests/`, 4 specs / 7 tests:

| Spec | Covers |
|---|---|
| `registration-login.spec.ts` | Register → dashboard; logout → login → dashboard; unknown credentials rejected |
| `party-and-drinking.spec.ts` | Party start-time formatting on the dashboard; create party, drink on the party page, promille changes |
| `classic-ui-toggle.spec.ts` | Modern → `/ui/user` → modern, with a party in the list |
| `ssr-initial-profile.spec.ts` | Dashboard makes no profile/parties/drinks/history XHR on first load |

So the modern dashboard and party page have basic coverage; `/ui/user` is
only ever asserted to render a heading and a party name. Nothing covers
`/ui/party`, the classic dialogs, profile settings in either UI, party admin
in either UI, guest users, drink removal, or terms/privacy/error pages —
which is most of what the migration will touch.

---

## 4. Recommended regression tests

Basic functionality only, no edge cases. Ordered by migration risk: the
classic JSP pages are what gets rewritten, so they come first.

### 4.0 Cheap first net: per-page smoke tests
One spec that visits every server-rendered URL as a logged-in user and
asserts a 200, the expected `<title>`, and one distinctive on-page string:
`/ui/login`, `/ui/newuser`, `/ui/user`, `/ui/party?id=N`, `/ui/terms`,
`/ui/privacy`. Cheap to write, and it catches the most common Thymeleaf
conversion failures (an unresolved expression, a lost `<title>`, a template
that 500s) before any behavioural test runs.

### 4.1 `/ui/login` — `login.jsp`
| # | Test |
|---|---|
| L1 | Renders logo, username + password fields, the "already N drinks" counter with a number, the registration link, and the privacy + terms footer links |
| L2 | Submitting valid credentials lands on `/app/index.html` |
| L3 | An already-logged-in user visiting `/ui/login` is redirected to the dashboard (`AuthenticationController.login`) |
| — | Invalid credentials → `/ui/login?error` — **already covered** |

### 4.2 `/ui/newuser` — `newuser.jsp`
| # | Test |
|---|---|
| N1 | Full registration → dashboard — **already covered** via the `registerUser` helper |
| N2 | Typing an email that already exists marks `#emailCorrect` as an error and leaves `#submitButton` disabled (exercises the `/ui/checkEmail` round trip) |
| N3 | "Takaisin etusivulle" navigates to `/` |

### 4.3 `/ui/user` — `user.jsp` (largest gap)
| # | Test |
|---|---|
| U1 | Page heading is the user's name; `#drinkers` shows their button with a promille reading |
| U2 | Party list shows each party's name and formatted start time, newest first |
| U3 | "Add party" dialog opens; submitting a name creates the party and lands on `/ui/party?id=N` |
| U4 | "Edit profile" dialog opens prefilled with name/email/sex; saving a new name redirects to `/ui/user` and the heading updates |
| U5 | Drinks dialog opens; the remove-drink accordion lists existing drinks; clicking one (accepting the confirm) removes it |
| U6 | Adding a drink at a chosen past time via the datetimepicker redirects to `/ui/user` and the drink appears in the list |
| U7 | Leaving a party (x button + confirm) drops it from the party list |
| U8 | Clicking the drinker button adds a drink and the promille reading changes |
| — | Switch to modern UI — **already covered** |

### 4.4 `/ui/party?id=N` — `party.jsp`
| # | Test |
|---|---|
| P1 | Header fills in with the party name (async from `/API/parties/{id}`); grid shows each participant with a promille reading |
| P2 | Add-guest form (name/sex/weight) puts a new drinker in the grid |
| P3 | Add-registered-user form: typing a known email enables the button (exercises `/ui/getUserByEmail`), submitting adds them to the grid |
| P4 | Kick dialog lists other participants but not the current user; removing one drops them from the grid |
| P5 | Group graph dialog opens and renders a flot canvas |
| P6 | Back button → `/ui/user`; switch-to-modern link → `/app/index.html` |

### 4.5 `/ui/terms` and `/ui/privacy`
| # | Test |
|---|---|
| T1 | Both load **anonymously** (both are `permitAll`), render their heading and body text, and `terms` links to `/ui/privacy` |

`privacy` already has a render-level unit test (`LegalControllerTest`); that
pattern — render the template standalone, assert no `${` leaked and the
layout fragments projected — is worth repeating for every page as it is
converted, alongside the e2e test.

### 4.6 Error pages
| # | Test |
|---|---|
| E1 | Requesting a party you are not a participant of renders `error.jsp`, not a whitelabel page or a stack trace |
| E2 | An unknown `/ui/...` URL renders `error.jsp` |
| E3 | A rejected login gives the user visible feedback (currently it does not — see §2.5; write this once the behaviour is decided) |

### 4.7 Modern UI — unchanged by the migration, but the mode toggle crosses over
| # | Test |
|---|---|
| M1 | Profile settings: change name and weight, save, get redirected to `#/`, dashboard shows the new name |
| M2 | Party admin (general): "Poistu bileistä" removes the party from the list |
| M3 | Party admin (per party): add a guest → appears in the participant list; remove them → disappears |
| M4 | Party admin (per party): add a registered user by email → appears in the participant list |
| M5 | "Vanha ystävä" invitation list populates from past co-participants and clicking one adds them (the only consumer of `/API/v2/parties/{id}/invitations`) |
| M6 | Dashboard: "Poista" on one of the last-5 drinks removes it |
| M7 | Dashboard drink flow: cancelling within the undo countdown posts no drink; editing size/ABV and submitting posts one |
| M8 | Menu navigation across the three user-menu routes and the two party-menu routes |

### 4.8 Cross-cutting
| # | Test |
|---|---|
| X1 | Logout from both UIs lands on `/ui/login` |
| X2 | Unauthenticated `/ui/user`, `/ui/party?id=N` and `/app/index.html` all redirect to `/ui/login` |

### Helpers worth adding to `e2e/tests/helpers.ts`
`loginClassic`, `createPartyClassic`, `addGuestToParty`, `addDrinkAt(time)`,
and a fixture that seeds a user with a couple of drinks — most of the `/ui/user`
tests need one.

---

## 5. Suggested sequencing

1. Decide on §2 removals (`/static/mob/`, `/ui/viewParty`, `/ui/addDrink`) and
   on the passphrase page — deleting a page is cheaper than testing and then
   converting it.
2. Land §4.0 smoke tests for every remaining server-rendered page.
3. Land §4.1–4.6 per page, then convert that page's template.
4. §4.7–4.8 whenever; they are not on the migration's critical path.
