# MFA Plugin Changelog

The MFA Plugin enables XNAT to require multi-factor authentication for users via multiple channels. 

## MFA Plugin Version 1.4.0
Released 2024-10-25

* [XNAT-8178](https://radiologics.atlassian.net/browse/XNAT-8178) Update plugin for compatibility with XNAT 1.9.0. No underlying dependency changes required. 


## MFA Plugin Version 1.3.0

* Reverts [XNAT-8088](https://radiologics.atlassian.net/browse/XNAT-8088) to mitigate conflicts in XNAT deployments. Limits support for SMS code send. 


## MFA Plugin Version 1.2.1

* [XNAT-8088](https://radiologics.atlassian.net/browse/XNAT-8088) Adds GET method to /xapi/mfa/verify, which is required by tools that communicate with XNAT to establish authentication 


## MFA Plugin Version 1.2.0

Initial open source release. Released 2023-05-11