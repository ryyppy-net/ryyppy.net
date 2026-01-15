# TODOs for Google Authentication

## Remaining Tasks

### 1. Social Connection Entity and Table
Create a separate entity to connect users with 0 or more social providers instead of storing provider-specific fields in the User entity.

**Implementation:**
- Create `SocialConnection` entity with fields:
  - `id` (primary key)
  - `userId` (foreign key to User)
  - `provider` (enum: GOOGLE, FACEBOOK, GITHUB, etc.)
  - `providerId` (unique ID from the provider)
  - `email` (email from the provider)
  - `connectedAt` (timestamp)
- Create `SocialConnectionDAO` repository
- Update `CustomOAuth2UserService` to create/lookup SocialConnection instead of direct User lookup
- Update `SpringSecurityCurrentUserImpl` to lookup user via SocialConnection

**Benefits:**
- Users can connect multiple social accounts
- Add new providers without schema changes
- Users can disconnect/reconnect accounts
- Standard OAuth2 pattern

### 2. Registration Page for New Users
Currently, new users are auto-created with default values (weight=70kg, sex=MALE). Instead, redirect new users to a registration form to collect necessary information for BAC calculations.

**Implementation:**
- Detect when OAuth2 login is from a new user (no existing User or SocialConnection)
- Store OAuth2 user info in session temporarily
- Redirect to registration page instead of auto-creating user
- Registration form should collect:
  - Name (pre-filled from Google)
  - Email (pre-filled from Google, read-only)
  - Weight
  - Sex
- On form submit, create User and SocialConnection
- Complete authentication and redirect to app

### 3. ~~Account Linking Confirmation Screen~~ (IMPLEMENTED with automatic linking)
✅ **Current Implementation**: Automatic account linking without confirmation screen
- When a user logs in with Google and their email matches an existing account, the accounts are automatically linked
- Google's sub (user ID) is stored in the openId field
- This is safe because Google verifies email addresses

**Future Enhancement (Optional):**
If you want to add a confirmation screen for extra security:
- Detect when OAuth2 email matches existing User but openId is null
- Store OAuth2 user info in session temporarily
- Redirect to confirmation screen showing:
  - "An account with email {email} already exists"
  - "Do you want to link your Google account to this existing account?"
  - Options: "Yes, link accounts" / "No, cancel"
- If confirmed: Store Google ID in openId field
- If cancelled: Clear session and redirect to login page

**Security Note:**
- Current automatic linking is safe because Google verifies email ownership
- Optional enhancement: Could require password confirmation for extra security

## Current Status

✅ Basic Google OAuth2 login working
✅ User lookup by Google ID (openId/sub claim) with email fallback
✅ **Automatic account linking** - Existing users can log in with Google and accounts are automatically linked
✅ Auto-creation of new users with defaults (weight=70kg, sex=MALE, authMethod=OPENID, openId=Google's sub ID)
✅ OAuth2 configuration and security setup
✅ Login page with official Google-branded button
✅ Debug logging enabled
✅ **Feature flagging based on configured providers** - Automatically detects which OAuth2 providers have valid credentials and shows corresponding login buttons

**Feature Flagging Implementation:**
- OAuth2 infrastructure (`OAuth2Configuration`) is always enabled and provider-agnostic
- `GlobalControllerAdvice` auto-detects providers with valid credentials from `ClientRegistrationRepository`
- Checks that client-id and client-secret are not empty or placeholder values (REPLACE_THIS, CHANGEME, etc.)
- Provides simple boolean flags for each provider: `${googleAuthEnabled}`, `${githubAuthEnabled}`, `${facebookAuthEnabled}`
- Login page conditionally shows buttons using simple checks: `<c:if test="${googleAuthEnabled}">`
- No manual feature flags needed - just configure provider credentials
- Easily extensible: add new `@ModelAttribute` method for each provider

**Easy Configuration via Environment Variables:**
- Google login: Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` environment variables
- GitHub login: Set `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` environment variables
- Facebook login: Set `FACEBOOK_CLIENT_ID` and `FACEBOOK_CLIENT_SECRET` environment variables
- Much simpler than Spring's default long names (e.g., `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`)
- Works in development and production

**Simple JSP API:**
```jsp
<c:if test="${googleAuthEnabled}">
  <a href="/oauth2/authorization/google">Login with Google</a>
</c:if>
<c:if test="${githubAuthEnabled}">
  <a href="/oauth2/authorization/github">Login with GitHub</a>
</c:if>
```

❌ Social connection entity/table
❌ Registration page for new users
✅ Automatic account linking (implemented without confirmation screen)
