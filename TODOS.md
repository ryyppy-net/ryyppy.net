# TODOs for Google Authentication

## Remaining Tasks

### 1. Registration Page for New Users
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

### 2. ~~Account Linking Confirmation Screen~~ (IMPLEMENTED with automatic linking)
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
✅ **Google One Tap login** - Modern sign-in experience with FedCM support for returning users

**Feature Flagging Implementation:**
- OAuth2 infrastructure (`OAuth2Configuration`) is always enabled
- `GlobalControllerAdvice` auto-detects if Google has valid credentials from `ClientRegistrationRepository`
- Checks that client-id and client-secret are not empty or placeholder values (REPLACE_THIS, CHANGEME, etc.)
- Provides `${googleAuthEnabled}` flag for the login page
- Login page conditionally shows Google button using: `<c:if test="${googleAuthEnabled}">`
- No manual feature flags needed - just configure Google credentials

**Easy Configuration via Environment Variables:**
- Google login: Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` environment variables
- Much simpler than Spring's default long names (e.g., `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`)
- Works in development and production

**Simple JSP API:**
```jsp
<c:if test="${googleAuthEnabled}">
  <a href="/oauth2/authorization/google">Login with Google</a>
</c:if>
```

**Google One Tap Implementation:**
- Uses Google Identity Services (GIS) library on login page
- Shows One Tap popup for returning users (auto_select: true)
- Uses FedCM when browser supports it (use_fedcm_for_prompt: true)
- Falls back to classic OAuth2 button for users who dismiss One Tap
- Backend endpoint `/api/auth/google/one-tap` verifies the JWT token using `google-api-client`
- Duplicates user lookup/creation logic from `CustomOAuth2UserService` (to be deduplicated later)

❌ Registration page for new users
✅ Automatic account linking (implemented without confirmation screen)
