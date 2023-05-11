// Copyright 2020 Radiologics, Inc
// Developer: James Ransford <ransfordj@radiologics.com>
// Developer: Mohana Ramaratnam <mohana@radiologics.com>


console.log('/scripts/mfa/multifactorAuth.js');
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
    XNAT.app.MultifactorAuth = {
        mfaLogin:function(){
            XNAT.app.MultifactorAuth.toggleError();
            if($("#authenticator-code").val() == ""){
                XNAT.app.MultifactorAuth.toggleError("Token cannot be blank");
                return;
            }
            XNAT.app.MultifactorAuth.verifyToken($("#authenticator-code").val()).done(function(){
                $("#mfa-login-container").fadeOut(function(){
                    window.location = serverRoot + "/app/template/Index.vm";
                });
            }).fail(function (e){
                XNAT.app.MultifactorAuth.toggleError("Invalid Token");
            });
        },
        renderGoogleAuthSetup:function(){
            XNAT.app.MultifactorAuth.getMfaStatus().done( function(auth, statusText, xhr){
                var status = xhr.status;
                if (status == 200) {
                    XNAT.app.MultifactorAuth.isMfaRequired = !auth.mfaExempted;
                    if (auth.mfaNeedsDeviceRegistration ) {
                        if (!auth.mfaRegistered ) {
                            $("#authenticator-qr").attr('src', auth.qrCodeUrl);
                            $("#authenticator-secret").text(auth.secret.replace(/(.{4})/g, '$1 '));
                            $("#authenticator-setup").show();
                        }else {
                            $("#authenticator-setup").hide();
                        }
                        $("#authenticator-code").focus();
                        // Handle enter key
                        $("#authenticator-code").keyup(function(event) {
                            if (event.keyCode === 13) {
                                $("a.mfa-verify-btn").click();
                            }
                        }
                        );
                    }
                }
            }).fail(function(){
                xmodal.message("Error","Unable to retrieve Multifactor Authetentication config. Please contact your System Administrator.");
            });
        },
        renderUserProfile:function(){
            XNAT.app.MultifactorAuth.getMfaStatus().done( function(auth, statusText, xhr){
                var status = xhr.status;
                if (status == 200) {
                    $("#authenticator-method").html('Preferred MFA Method: '+auth.mfaPreferred);
                    $("#authenticator-exempted").html('Exempted from MFA: ' + auth.mfaExempted);
                    $("#authenticator-registered").html('Registered for MFA: ' + auth.mfaRegistered);
                }
           });
        },

        renderGoogleAuthenticatior:function(auth) {
                $("#authenticator-qr").attr('src', auth.qrCodeUrl);
                $("#authenticator-secret").text(auth.secret.replace(/(.{4})/g, '$1 '));
                if(auth.mfaRegistered){
                    XNAT.app.MultifactorAuth.renderUserProfileEnabled();
                }else{
                    XNAT.app.MultifactorAuth.renderUserProfileDisabled();
                }
                $("#authenticator-setup").fadeIn(function(){
                    if(!auth.mfaRegistered){
                        $("#authenticator-code").focus();
                    }
                });
        },
        renderUserProfileEnabled:function(auth){
            $("#authenticator-verify").hide();
            $("div.multifactor-enabled").show();
            if(!XNAT.app.MultifactorAuth.isMfaRequired){
                $("a.mfa-disable-btn").show();
            }
        },
        renderUserProfileDisabled:function(){
            $("#authenticator-code").val("");
            $("div.multifactor-enabled").hide();
            $("#authenticator-verify").show();
            // Handle enter key
            $("#authenticator-code").keyup(function(event) {
                if (event.keyCode === 13) {
                    $("a.mfa-verify-btn").click();
                }
            });
        },
        enableMfa:function(){
            XNAT.app.MultifactorAuth.toggleError();
            if($("#authenticator-code").val() == ""){
                XNAT.app.MultifactorAuth.toggleError("Token cannot be blank");
                return;
            }
            XNAT.app.MultifactorAuth.verifyToken($("#authenticator-code").val()).done(function(){
                $("#authenticator-verify").fadeOut(function(){
                    XNAT.app.MultifactorAuth.renderUserProfileEnabled();
                });
            }).fail(function(){
                XNAT.app.MultifactorAuth.toggleError("Invalid Token.");
            });
        },
        disableMfa:function(){
            var url = serverRoot + '/xapi/mfa/unregister?XNAT_CSRF='+csrfToken;
            $.post( { url : url}).done( function(o){
                 $("#authenticator-setup").fadeOut(function(){
                     XNAT.app.MultifactorAuth.renderUserProfile();
                });
            }).fail(function (){
                xmodal.message("Error","Unable to unregister from Multi-factor Authentication.");
            });
        },
        getMfaConfig:function(){
            return $.get( { url : serverRoot+'/xapi/mfa?XNAT_CSRF='+csrfToken});
        },
        getMfaStatus:function(){
            return $.get( { url : serverRoot+'/xapi/mfa/status?XNAT_CSRF='+csrfToken});
        },
        verifyToken:function(token){
            return $.ajax(serverRoot + '/xapi/mfa/verify?XNAT_CSRF=' + csrfToken, {
                data: JSON.stringify({token:token}),
                contentType: 'application/json',
                type: 'POST'
            });
        },
        toggleError:function(error){
            if(error){
                $("#error-msg").text(error);
                $("#mfa-error").css('display', 'inline-block');
                $("#authenticator-code").addClass("error");
            }else{
                $("#mfa-error").css('display', 'none');
                $("#authenticator-code").removeClass("error");
            }
        }
    }
}));