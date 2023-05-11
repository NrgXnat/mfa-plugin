# README #

This is an XNAT based plugin to enable Multi-Factor Authentication (MFA)


### XNAT Dependency ###

This plugin requires the ESign Plugin deployed for the XNAT-CR Settings tab to be available

### JAR  Dependency ###

    com.radiologics.xnat_msbase_manifest version:'1.0-BETA'	(for XNAT Trust Federation Backchannel user access)
    
    com.amazonaws.aws-java-sdk-sns  version: '1.11.901'
    com.amazonaws.aws-java-sdk-core version: '1.11.901'
	
    org.jboss.aerogear:aerogear-otp-java:1.0.0'

### What is this plugin for? ###

This plugin enables Multi-factor Authentication. The authentication is possible using a MFAStrategy. The available stratigies are:

a) Code sent via Google Authenticator

b) Code sent via Email

c) Code sent via SMS

Features:

a) Site level configuration property called requireMfa dictates whether users of the site are required to pass MFA to login.
b) Site level configuration propert called requireAdminMfa dictates if the site admin also need to perform MFA to login.
c) Google Authenticator MFA needs a device registration step, where as, strategies like EMAIL and SMS do NOT need any device registration.

User Interface:

a) Admin User -> Administer -> Plugin Settings -> Multifactor Authentication

b) Admin User -> Individual User Profile -> Advanced Setting 

c) XNAT User Profile -> Multifactor Authentication (ONLY for Google Authenticator)


### Building the fat jar ###

gradlew clean fatJar



