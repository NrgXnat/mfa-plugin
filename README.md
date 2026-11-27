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

### Running behind a proxy that terminates TLS ###

Tell Tomcat that the proxy terminated TLS. Otherwise Tomcat believes the request arrived over plain HTTP and builds
`http://` redirects, and because XNAT 1.10.1 and later send `Content-Security-Policy: form-action 'self'`, the browser
refuses to follow such a redirect after a form submission. The login page then sits at "Submitting..." and the browser
console reports that the form submission violates the `form-action` directive.

Have the proxy pass the original scheme and port:

```
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Port  $server_port;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header Host              $host;
```

and have Tomcat read them, by adding this valve to the `<Host>` element in `server.xml`:

```
<Valve className="org.apache.catalina.valves.RemoteIpValve"
       remoteIpHeader="x-forwarded-for"
       protocolHeader="x-forwarded-proto"
       portHeader="x-forwarded-port" />
```

This affects every XNAT redirect, not just MFA, but MFA shows it first because logging in is the first thing that
redirects after a form submission.

### Password resets and expired passwords ###

* A user who follows a "forgot password" or account reactivation link sets their new password before being asked for a
  multi-factor code. The link itself proves they control the account's email. XNAT ends the session as soon as the
  password changes, so they log in again afterwards and pass MFA then. Until the password changes, that session can
  reach nothing else.
* A user whose password has expired passes MFA first and then changes their password, because XNAT shows them the home
  page as soon as the new password is saved.

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

`./gradlew clean xnatPluginJar`



