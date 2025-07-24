# MFA Plugin Changelog

The MFA Plugin enables XNAT to require multi-factor authentication for users via multiple channels. 

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