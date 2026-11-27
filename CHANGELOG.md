# MFA Plugin Changelog

The MFA Plugin enables XNAT to require multi-factor authentication for users via multiple channels. 

## MFA Plugin Version 1.6.0
Unreleased

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
  the MFA pages. The verification pages are now named `MfaEmailVerification.vm`, `MfaSMSVerification.vm`,
  `MfaGoogleAuthenticatorVerification.vm` and `MfaVerificationError.vm`, which XNAT recognises as login pages that
  should not load the data type cache.
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