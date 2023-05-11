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
						tag: 'div',
						contents: '<h2>Multi-factor Authentication</h2>'
					},
					multifactorLoginBox: {
						tag: 'div',
						contents: {
							authenticatorVerify: {
								tag: 'div',
								contents: {
								    mfaMessage: {
								        tag: 'div.message',
								        contents: 'An email with the verification code has been sent to your registered email. Enter your 6 digit verification code in the text box below.'
								    },
								    mfaCodeInput: {
								        tag: 'input#authenticator-code.mfa-input|type="text"|size="11"|maxlength="6"'
								    },
								    mfaSubmit: {
								        tag: 'button#mfa-login-btn.btn.btn1.mfa-input|type="button"',
								        contents: 'Verify'
								    },
//								    mfaReSend: {
//								        tag: 'button#mfa-resend-btn.btn.btn1.mfa-input|type="button"',
//								        contents: 'Resend Code'
//								    },
								    mfaError: {
								        tag: 'span#mfa-error',
								        contents : '<i class="fa fa-asterisk"></i><span id="error-msg"></span>'
								    }
						        }
					        }
				        }
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