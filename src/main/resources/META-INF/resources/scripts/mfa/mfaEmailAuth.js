console.log("/scripts/mfa/mfaEmailAuth.js");
var XNAT = getObject(XNAT || {});
XNAT.app = getObject(XNAT.app || {});
(function(factory){
    if (typeof define === "function" && define.amd) {
        define(factory);
    }
    else if (typeof exports === "object") {
        module.exports = factory();
    }
    else {
        return factory();
    }

}(function() {
	$(document).ready(function() {
		var multifactorAuthPanel = {
				tag: 'div.mfa-login-box',
				contents:{
					mfaStyleSheet: {
						tag: 'link|rel=stylesheet|type=text/css|href=/style/mfa/multifactorAuth.css'
					},
					mfaHeader: {
						tag: 'h2',
						contents: 'Multi-factor Authentication'
					},
					mfaEmailMessage: {
					    tag: 'div.message',
                        contents: 'An email with the verification code has been sent to your registered email. Enter your 6 digit verification code in the text box below.' +
                            '<br><br>If you did not receive an email, please contact your site administrator'
					},
					authenticatorVerify: {
                        tag: 'div#authenticator-verify',
                        contents:
                            '<p><strong>Enter OTP code from Email:</strong></p>' +
                            '<p><input type="text" id="authenticator-code" size="6" maxlength="6" placeholder="888888" />' +
                            '<span id="mfa-error"><i class="fa fa-exclamation-circle"></i>&nbsp;<span id="error-msg"></span></span></p>' +
                            '<a class="btn1 mfa-verify-btn"" id="mfa-login-btn" href="#!">Verify Code</a>'
                    },
                    authHelpMessage: {
//                        tag: 'div#mfa-email-btn.auth-help.message',
//                        contents:
//                            'Problems authenticating? ' +
//                            '<a href="#!">Resend email</a> or contact your site administrator'
                    }
		        }
		    }

		$("#xnat_power").hide();
		XNAT.spawner.spawn({multifactorAuthPanel}).render($("div#mfa-login-container"));
		$("#authenticator-code").focus();

        // Handles enter key
		$("#authenticator-code").keyup(function(event) {
			if (event.keyCode === 13) {
				$("a.mfa-verify-btn").click();
			}
		});

		$("#mfa-login-btn").click(function(){
		    XNAT.app.MultifactorAuth.mfaLogin();
		});
    });
}));