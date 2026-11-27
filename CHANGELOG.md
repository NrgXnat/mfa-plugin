# MFA Plugin Changelog

The MFA Plugin enables XNAT to require multi-factor authentication for users via multiple channels. 

## MFA Plugin Version 1.6.0
Unreleased

**Breaking changes**
* The verification pages are renamed. `MfaEmailVerify.vm` is now `MfaEmailVerification.vm`, `MfaSMSVerify.vm` is now
  `MfaSMSVerification.vm`, and `MfaGoogleAuthenticatorVerify.vm` is now `MfaGoogleAuthenticatorVerification.vm`. A site
  that has overridden any of those templates must rename its override, or the override will no longer be used. The
  names matter: XNAT decides from the page name that these are login pages, which is what stops them trying to load
  the data type cache.
* `MFAPreferences.getMfaRedirectPath()` now returns a path within the application, such as `/app/template/Index.vm`,
  rather than a full URL built from the site URL. The same change is visible in the JSON from
  `GET /xapi/mfa/preference`.

**Changed behaviour**
* A user who follows a "forgot password" or account reactivation link sets their new password before being asked for a
  code. The link proves they control the account's email, and XNAT ends the session as soon as the password changes,
  so the code is asked for at the next login. Someone who can read a user's email can therefore force a password
  change on that account. They still cannot log in without the second factor, but they can lock the owner out until
  the owner resets the password again.
* Until that password has been changed, the session can reach nothing but the password page, the login page and
  logging out. Anything else is redirected back to the password page.
* A user whose password has expired must pass MFA before they can set a new one, because XNAT shows the home page as
  soon as the new password is saved. A user who cannot receive their code can no longer change an expired password
  themselves and will need an administrator to set it.
* On the admin user page, the read-only "MFA Registered" switch is hidden for users whose method has nothing to
  register, which is every method except Google Authenticator.
* An email verification link sends a user who has already registered a device to the verification page rather than to
  the registration page it would have bounced them off.
* Visitors who are not logged in are redirected away from the MFA pages whether or not MFA is required site-wide.

**Bugfixes**
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Let users with MFA reset their password. The MFA filter caught
  the submission from the "forgot password" page, so the password was never changed, and the filter and XNAT's
  expired-password filter redirected each other's pages until the verification page's own scripts failed to load,
  leaving a page with no box to enter the code in. The two filters now agree on which pages a user who must change
  their password can reach, and MFA is held until the password has been changed.
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Ask users whose password has expired for their code before
  sending them to the password page, instead of leaving them in the same loop.
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Send one code per login. A page makes several requests at once,
  and each code replaced the one before it, so users received several codes of which only the last one worked.
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Serve XNAT's scripts and stylesheets to users who have not yet
  passed MFA. The MFA pages are ordinary XNAT pages and could only draw their form if the browser already had those
  files in its cache, so they failed in a new or private browser window.
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Add the deployment's context path to every redirect, and match
  the MFA pages by their exact path. On an installation under a path such as `/xnat`, redirects went outside the
  application and the MFA pages were redirected to themselves.
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Stop building redirects from the site URL. When the site URL did
  not match the host the user browsed to, the browser blocked the redirect after the login form was submitted and the
  page sat at "Submitting...".
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Fix the error thrown when a user whose method is email or SMS
  follows an email verification link, and when the site-wide method is email and a visitor loads the login page.
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Load the plugin's stylesheets and scripts correctly on
  installations under a context path, and point the user profile page at a stylesheet that exists.
* [#3](https://github.com/NrgXnat/mfa-plugin/issues/3) Stop the "Refreshing data type cache" message from sitting over
  the MFA pages, which cannot reach the data type cache before MFA is complete. This is why the pages are renamed; see
  the breaking changes above.
* Fix "receive an authentication code via email" on the Google Authenticator page. The code was sent, but the user
  was put straight back on the Google Authenticator page, where the emailed code could not work. The switch now lasts
  for the rest of the session, and the user's own method comes back the next time they log in.
* Hide the read-only "MFA Registered" switch for users whose method has nothing to register.


## MFA Plugin Version 1.5.0
Released 2025-07-25

**New Features and Improvements**
* [MFA-12](https://radiologics.atlassian.net/browse/MFA-12) Allow site admin to reset users' MFA registration
* [MFA-14](https://radiologics.atlassian.net/browse/MFA-14) Replace deprecated Google Authenticator QR chart generation script
* [MFA-15](https://radiologics.atlassian.net/browse/MFA-15) Improve MFA user table by adding registration status, and reflect this in Administer Users as well
* [MFA-16](https://radiologics.atlassian.net/browse/MFA-16) Improve UX of plugin administration and MFA user controls
* [MFA-20](https://radiologics.atlassian.net/browse/MFA-20) Improve UX of MFA registration pages
* [MFA-21](https://radiologics.atlassian.net/browse/MFA-21) Warn site admins before they make changes in MFA administration that inadvertently log themselves out

**Bugfixes**
* [MFA-7](https://radiologics.atlassian.net/browse/MFA-7) Replace deprecated Google Authenticator QR chart generation script
* [MFA-8](https://radiologics.atlassian.net/browse/MFA-8) Deprecate SMS as a supported method (for now)
* [MFA-17](https://radiologics.atlassian.net/browse/MFA-17) Remove guest user from MFA user table
* [MFA-18](https://radiologics.atlassian.net/browse/MFA-18), [MFA-19](https://radiologics.atlassian.net/browse/MFA-19) Fix blocker bugs preventing MFA registration on non-root XNAT installations


## MFA Plugin Version 1.4.0
Released 2024-10-25

* [XNAT-8178](https://radiologics.atlassian.net/browse/XNAT-8178) Update plugin for compatibility with XNAT 1.9.0. No underlying dependency changes required. 


## MFA Plugin Version 1.3.0

* Reverts [XNAT-8088](https://radiologics.atlassian.net/browse/XNAT-8088) to mitigate conflicts in XNAT deployments. Limits support for SMS code send. 


## MFA Plugin Version 1.2.1

* [XNAT-8088](https://radiologics.atlassian.net/browse/XNAT-8088) Adds GET method to /xapi/mfa/verify, which is required by tools that communicate with XNAT to establish authentication 


## MFA Plugin Version 1.2.0

Initial open source release. Released 2023-05-11