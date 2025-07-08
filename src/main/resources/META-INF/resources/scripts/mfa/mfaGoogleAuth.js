console.log("/scripts/mfa/mfaGoogleAuth.js");
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
						tag: `link|rel=stylesheet|type=text/css|href="~/style/mfa/multifactorAuth.css"`
					},
					mfaHeader: {
						tag: 'h2',
						contents: 'Multi-factor Authentication'
					},
					mfaMessage: {
                        tag: 'p',
                        contents: 'Check your authenticator app for your 6 digit verification code and enter it in the text box below.'
                    },
					multifactorLoginBox: {
						tag: 'div',
						contents: {
							authenticatorVerify: {
                                tag: 'div#authenticator-verify',
                                contents:
                                    '<p><strong>Enter code from Authenticator:</strong></p>' +
                                    '<p><input type="text" id="authenticator-code" size="6" maxlength="6" placeholder="888888" />' +
                                    '<span id="mfa-error"><i class="fa fa-exclamation-circle"></i>&nbsp;<span id="error-msg"></span></span></p>' +
                                    '<a class="btn1 mfa-verify-btn"" id="mfa-login-btn" onclick="XNAT.app.MultifactorAuth.mfaLogin()" href="#!">Verify Code</a>'
                            },
							authHelpMessage: {
                                tag: 'div#mfa-email-btn.auth-help.message',
                                contents:
                                    'Problems authenticating? As a fallback, you can ' +
                                    '<a href="#!" class="switch-to-email">receive an authentication code via email</a>'
                            }
						}
		            }
		        }
		    }

		$("#xnat_power").hide();
        XNAT.spawner.spawn({multifactorAuthPanel}).render($("div#mfa-login-container"));
        $("#authenticator-code").focus();

        // Handle enter key
        $("#authenticator-code").keyup(function(event) {
            if (event.keyCode === 13) {
                $("a.mfa-verify-btn").click();
            }
        });
		$(".switch-to-email").click(function(event) {
			event.preventDefault();
			XNAT.app.MultifactorAuth.switchToEmail();
		});

		XNAT.app.MultifactorAuth.renderSwitchEmailButton();

        $("#mfa-login-btn").click(function(){
            XNAT.app.MultifactorAuth.mfaLogin();
        });
    });
}));