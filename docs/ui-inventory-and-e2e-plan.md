# UI inventory and E2E coverage plan

Written ahead of the JSP-to-Thymeleaf migration, to answer two questions:
what UI surface exists, and what regression coverage each page needs before
its template is rewritten.

**Verification status:** every finding below was checked against a running
instance (local PostgreSQL 16 + `mvn spring-boot:run`), driving the pages
with curl and with Playwright/Chromium for the JS-dependent ones. The
existing e2e suite was green (7/7) on the same setup. Findings that are
still a judgement call rather than an observation say so explicitly.

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

All of the following were exercised against a running instance.

### 2.1 `/static/mob/` — orphan mobile prototype — **confirmed**
A jQuery Mobile 1.0b2 + Backbone + Handlebars app with zero inbound
references anywhere in the repo. Verified live: `/static/mob/index.html`
returns 200 (it is public, since `/static/**` is `permitAll`), its page title
is `"Hello"`, and typing a name into its "Add drinker" form renders
`MobTester 1.2` — the hardcoded `{id: 1, promilles: 1.2}` model from
`js/app.js`, with no API call at all. It is an unfinished prototype, not a
working UI.
*Action:* delete the directory. No test.

### 2.2 `/ui/viewParty` — unreferenced duplicate, with a worse bug — **confirmed**
No link anywhere; returns 200 if you type the URL. It renders the same
`party.jsp` as `/ui/party`, but puts `users` in the model where the template
reads `${user.id}`. Verified difference on the same party, same session:

| | entries in the "remove drinker" dialog |
|---|---|
| `/ui/party?id=1` | `GuestBob` |
| `/ui/viewParty?id=1` | `GuestBob`, **`VerifyUser`** (the current user) |

So `/ui/viewParty` offers the logged-in user a button to kick themselves out
of their own party. Its `kick` query parameter also performs a
GET-triggered unlink with no confirmation.
*Action:* delete the handler.

### 2.3 `/ui/addDrink` — unreferenced and broken — **confirmed**
No inbound references, and it ends with `return "redirect:parties"` — no
`parties` mapping exists. Verified: `GET /ui/addDrink?id=1` took the profile
from `totalDrinks = 0` to `totalDrinks = 1`, then returned
`302 → /ui/parties`, which is a **404**. The drink is written and the user is
dropped on an error page.
*Action:* delete the handler. Both UIs add drinks through the API instead.

### 2.4 Passphrase — the page is orphaned, the API works, and nobody has a passphrase
Three separate facts, verified:

1. **The page is unlinked.** No page in either UI links to `/ui/passphrase`;
   it is reachable only by typing the URL. It renders fine and
   `/ui/passphrase-generate` does rotate the value.
2. **The API is fully functional and unauthenticated.** With no cookies at
   all: `GET /API/passphrase/{p}` → `VerifyUser,0.19847844541072845`;
   `.../add-drink/0` took the user from 1 drink to 2; `.../undo-drink` took
   them back to 1. This is a real, working external-client interface.
3. **But almost nobody can have a passphrase.** Registration never generates
   one — `UserServiceImpl.generatePassphrase()` is called only from
   `/ui/passphrase-generate`. In the verification database every user had
   `passphrase = NULL` except the single one where that unlinked URL was hit
   by hand. So the only route to a working passphrase is the orphan page.

The copy button is still ZeroClipboard: the page injects a Flash object into
`#d_clip_container` (confirmed in the DOM), which no current browser will run.

*This is the one item that needs a production data point rather than a
judgement call.* Run:

```sql
SELECT count(*) FROM users WHERE passphrase IS NOT NULL;
```

Zero (or only your own test rows) means the whole feature — page, API,
`findByPassphrase`, the `permitAll` rule — is dead and can go. A non-trivial
count means real clients exist, and the page should be kept, linked from the
settings menus, and have ZeroClipboard replaced with `navigator.clipboard`.

### 2.5 `/ui/loginerror` — unreachable, and login failure is silent — **confirmed**
`/ui/loginerror` returns 200 but nothing routes to it: form-login failure
uses Spring Security's default `/ui/login?error`, and OAuth2 failure goes
through `UserNotRegisteredFailureHandler` to the registration URL.

Verified separately: after submitting bad credentials the browser lands on
`/ui/login?error` and the page contains **no error wording at all** — the
rendered text was searched for `virhe`, `väär`, `error`, `epäonnistu` and
`invalid` and matched none. A rejected login silently redraws the form.
*Action:* either point the failure handler at `/ui/loginerror`, or delete the
view and render an inline message on `login.jsp` for `?error`. Either way
test E3 below covers it.

### 2.6 Smaller orphans
- `GET /API/parties/{partyId}/get-history` — returns 200, no frontend
  reference (`partygraph.js` uses `/API/users/{id}/show-history`).
- `public/app/index-async.html` — no references.
- `public/static/js/googlegraph.js` — no references.
- Inside live code: `user.jsp`'s `configureDrinksDialogOpened()` calls
  `$("#time").datetimepicker(...)` but the page has no `#time` element, so
  that call is a no-op. The picker actually in use is
  `new DateTimePicker('#historyDrinkTime')`, which renders correctly.

### 2.7 Correction to an earlier assumption: the error page is alive
`error.jsp` is reachable and used. A 404 requested with `Accept: text/html`
renders it (`<title>Ryyppy.net - Virhe!</title>`); the same URL with
`Accept: */*` returns JSON instead. Tests for it must therefore go through a
browser, not a bare API client. (The JSON also carried a stack trace here,
but that is `spring-boot-devtools` forcing
`server.error.include-stacktrace=always` in dev — not a production finding.)

---

## 2b. Two things that will bite whoever writes the classic-UI tests

**The header icon buttons are 0×0 anchors.** On `/ui/user` and `/ui/party`
every `<a class="headerButtonA">` wrapper measures 0×0, because the icon
`<div>` inside it is floated and the inline anchor collapses. The 42×42 icon
div is what a user sees and clicks. Playwright will refuse to click the
anchor ("element is not visible") and time out. Click the inner div instead:

| Click this | Not this |
|---|---|
| `#addPartyButton`, `#configureButton`, `#configureDrinksButton` | `#addPartyButtonLink`, `#configureDrinkerButtonLink`, `#configureDrinksButtonLink` |
| `#graphButton`, `#addDrinkerButton`, `#kickDrinkerButton` | `#graphButtonLink`, `#addDrinkerButtonLink`, `#kickDrinkerButtonLink` |

`#uiSwitchButton` is a text link (114×40) and is clickable directly — which
is why the existing `classic-ui-toggle` test works.

**Reopening the group graph throws.** On `/ui/party`, opening the group
graph dialog, closing it, and opening it again reliably throws
`Cannot read properties of undefined (reading 'w')` from
`jquery.flot.resize.min.js`. Cause: `party.jsp` wires only the dialog's
`open:` handler and never `graphDialogClosed()`, so `RyyppyNet.graphVisible`
stays `true` after closing (verified) and the two-minute interval keeps
redrawing a hidden plot. Test P5 below will hit this — worth fixing before
or alongside writing it, rather than writing the test around it.

## 3. Existing E2E coverage

`e2e/tests/`, 4 specs / 7 tests — all green against the verification setup:

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

Everything those missing tests would cover was confirmed to work in a
browser, so none of them is blocked: `/ui/user` renders the heading, the
drinker button with its promille reading and the history graph (2 flot
canvases), its add-party dialog opens and creating a party lands on
`/ui/party?id=N`; `/ui/party` fills its title in asynchronously from
`/API/parties/{id}`, renders the participant grid, and opens both the group
graph (2 canvases) and add-drinker dialogs; the drinks dialog renders its
`#historyDrinkTime` picker and shows "Ei lisättyjä juomia" when empty. The
two caveats in §2b apply.

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

Verified: both return 200 with no session — `/ui/terms` is
`Käyttöehdot - Ryyppy.net` / `<h1>Käyttöehdot</h1>`, `/ui/privacy` is
`Tietosuojaseloste - Ryyppy.net` / `<h1>Tietosuojaseloste</h1>`. Those
titles and headings are the assertion targets.

`privacy` already has a render-level unit test (`LegalControllerTest`); that
pattern — render the template standalone, assert no `${` leaked and the
layout fragments projected — is worth repeating for every page as it is
converted, alongside the e2e test.

### 4.6 Error pages
| # | Test |
|---|---|
| E1 | Requesting a party you are not a participant of renders `error.jsp`, not a whitelabel page or a stack trace |
| E2 | An unknown `/ui/...` URL renders `error.jsp` (verified: a 404 with an HTML `Accept` header returns `<title>Ryyppy.net - Virhe!</title>`; the same URL as an API client returns JSON, so these must run through the browser) |
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

1. Delete the three confirmed-dead handlers (`/static/mob/`,
   `/ui/viewParty`, `/ui/addDrink`) — deleting a page is cheaper than testing
   it and then converting it. Run the passphrase `count(*)` query from §2.4
   against production to decide that one, and pick a direction for
   `/ui/loginerror` (§2.5).
2. Land §4.0 smoke tests for every remaining server-rendered page.
3. Land §4.1–4.6 per page, then convert that page's template.
4. §4.7–4.8 whenever; they are not on the migration's critical path.

Separately, the group-graph reopen bug (§2b) is a real defect on a live page,
independent of the migration.

---

## 6. Reproducing the verification environment

No Docker is needed — a local PostgreSQL works:

```bash
pg_ctlcluster 16 main start          # or pg_ctl -D /var/lib/postgresql/16/main start
sudo -u postgres psql -c "CREATE ROLE ryyppynet LOGIN PASSWORD 'ryyppynet' SUPERUSER;" \
                      -c "CREATE DATABASE ryyppynet OWNER ryyppynet;"
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.docker.compose.enabled=false"
```

Flyway builds the schema on first boot. Then, from `e2e/`:
`SKIP_WEBSERVER=1 npx playwright test`.
