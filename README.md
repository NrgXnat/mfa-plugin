# README #

This is an XNAT plugin to enable Multifactor Authentication (MFA) via 

* Code sent via Google Authenticator 
* Code sent via Email 
* Code sent via SMS

### Notes: ###

* Site level configuration property called `requireMfa` dictates whether users of the site are required to pass MFA to login.
* Site level configuration property called `requireAdminMfa` dictates if the site admin(s) also need to perform MFA to login.
* Individual accounts can be opted out of MFA
* Google Authenticator MFA needs a device registration step, whereas EMAIL and SMS do not require any device registration.
* Phone number for SMS ought to be entered as '+' then the country's calling code and phone number. E.g. `+19998887777`

User Interface:

[Admin user] Administer -> Plugin Settings -> Multifactor Authentication

[Admin user]  Administer -> Users -> [select user account] -> Advanced Settings  

[user] User profile (top right, click on username) -> Multifactor Authentication (read only)

### Known issues: ###
* No way to set/change/view a phone number for existing user
* User cannot change their MFA method, this is only available to admins through Administer -> Users
* Only admins can trigger an MFA reset to prompt the user to reconfigure their MFA (e.g., in event of a new device)

### CLI ###

For SMS and Email, `POST - /xapi/mfa/send_code` will send a code to email or phone:
```
curl -X POST -w "%{http_code}" -u testUser:testUser123 https://host.xnat.com/xapi/mfa/send_code
```

For Google Authenticator, simply visit the app to obtain the code.

Then, `POST - /xapi/mfa/verify` will create a new user session and return the jsession cookie:
```
curl -X POST -u testUser:testUser123 https://host.xnat.com/xapi/mfa/verify -H "Content-Type:application/json" -d "{'token':'907813'}" -s | jq
```
Sample response:
```
{
  "message": "Token Verified",
  "jsessionId": "530AAE86A816F32CA9BD1964B07C376F",
  "csrfToken": "35009dc6-f1a6-4e29-a6e5-62a9fb79cc26"
}
```

For scripted interactions with XNAT, use service accounts exempted from MFA or retrieve an alias token.

### Building the jar ###

`./gradlew clean fatJar`



