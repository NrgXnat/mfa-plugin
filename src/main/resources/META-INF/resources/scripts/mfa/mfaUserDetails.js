console.log("/scripts/mfa/mfaUserDetails.js");
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
    XNAT.app.mfa = getObject(XNAT.app.mfa || {});
    XNAT.app.mfa.userDetails = getObject(XNAT.app.mfa.userDetails || {});
    XNAT.app.mfa.userDetails.topXnat = topXNAT = window.top.XNAT;

    $(document).ready(function() {
        var mfaUserDetails = {
            tag: "div",
            contents: {
                mfaMsg: {
                    tag: "div#mfa-message.message.hidden"
                },
                mfaStyleSheet: {
                    tag: "link|rel=stylesheet|type=text/css|href=/style/mfa/multifactorAuth.css"
                },
                mfaSettings: {
                    tag: "div#mfa-user-details",
                    contents: {
                        exemptMfa:{
                            tag: "div.mfa-setting",
                            contents: {
                                exemptSwitch: {
                                    tag: "div",
                                    contents: {
                                        multifactorExemptUser: {
                                            kind: "panel.input.switchbox",
                                            label: "Exempt User: ",
                                            id: "exempt-mfa-user",
                                            onText: "Exempt",
                                            offText: "Not Exempt",
                                        }
                                    }
                                },
                                exemptDescription: {
                                    tag: "div.description",
                                    contents: "Exempt users will not be required to use multi-factor authentication. Changes will take effect immediately."
                                },
                            }
                        },
                        mfaMethod: {
                            tag: "div.mfa-setting",
                            contents: {
                                selectLabel: {
                                    tag : "div.select-label",
                                    contents: "MFA Method:"
                                },
                                methodSelect: {
                                    tag: "select#mfa-method"
                                },
                                methodDescription: {
                                    tag: "div.description",
                                    contents: "Sets the users mfa method. Changes will take effect immediately."
                                }
                            }
                        },
                        resetMfa: {
                            tag: "div.mfa-setting",
                            contents: {
                                resetBtn: {
                                    tag: "button#multi-factor-reset.btn.btn1|type=button",
                                    contents: "Reset User Secret"
                                },
                                resetDescription: {
                                    tag: "div.description",
                                    contents: "In the event the user loses their registered authentication device, their secret can be reset and they will be required to re-configure multi-factor authentication on their next login."
                                }
                            }
                        }
                    }
                }
            }
        }
        XNAT.app.mfa.userDetails.topXnat.spawner.spawn({mfaUserDetails:mfaUserDetails}).render($("div#mfa-settings-container"));

        XNAT.xhr.get( {url : XNAT.url.csrfUrl("/xapi/mfa/" + username + "/status") }).done(function(mfaObj, statusText, xhr){
            if (xhr.status != 200) {
                toggleMessage("Error: Unable to fetch MFA Information.");
                return;
            }

            XNAT.app.mfa.userDetails.toggleMessage();
            $("#exempt-mfa-user").checked(mfaObj.mfaExempted);

            XNAT.xhr.get( { url :  XNAT.url.csrfUrl('/xapi/mfa/methods') }).done(function(methods, statusText, xhr){
                XNAT.app.mfa.userDetails.initMethods(methods, mfaObj.mfaPreferred)
            }).fail(function(){
                XNAT.app.mfa.userDetails.toggleMessage("Error: Failed to retrieve available mfa methods.");
            });

        }).fail(function (e){
            toggleMessage("Error: Unable to fetch MFA Information.");
        });

       $("#exempt-mfa-user").on("change", function(){
           var exempted = this.checked;
           XNAT.xhr.post({ url : XNAT.url.csrfUrl("/xapi/mfa/exempt/" + username + "/" + exempted) }).done( function(o){
               XNAT.app.mfa.userDetails.topXnat.ui.banner.top(2000, exempted ? "User exempted from multi-factor authentication." : "Multi-factor authentication required for user.", "success");
           }).fail(function (e){
               XNAT.app.mfa.userDetails.topXnat.ui.banner.top(3000,"Error: Failed to save exempted status for user.", "error");
           });
       });

      $("#multi-factor-reset").click(function(){
          XNAT.xhr.post({ url : XNAT.url.csrfUrl("/xapi/mfa/" + username + "/unregister") }).done( function(o){
              XNAT.app.mfa.userDetails.topXnat.ui.banner.top(2000,"Secret reset.", "success");
          }).fail(function (e){
               XNAT.app.mfa.userDetails.topXnat.ui.banner.top(3000,"Error: Failed to reset user secret.", "error");
          });
      });

     $("#mfa-method").on("change", function(){
         var method = $(this).val();
         XNAT.xhr.post({ url : XNAT.url.csrfUrl("/xapi/mfa/preferred/user/" + username + "/method/" + method) }).done( function(o){
             topXNAT.ui.banner.top(2000, "Multi-factor authentication method updated to: " + method, "success");
         }).fail(function (e){
             topXNAT.ui.banner.top(3000,"Error: Failed to save multi-factor authentication method.", "error");
         });
     });

      XNAT.app.mfa.userDetails.initMethods = function(response, usersMethod){
        $.each(response["mfa_methods"], function(i, e) {
            var selected = (e == usersMethod);
            $("#mfa-method").append('<option ' + (selected ? 'selected ' : '' ) +'data-method=mfa-method-' + e + 'value=1>' + e + '</option>');
        });
      }

      XNAT.app.mfa.userDetails.toggleMessage = toggleMessage = function(msg){
        if(msg){
            $("#mfa-user-details").hide();
            $("#mfa-message").html(msg);
            $("mfa-message").show();
        }else{
            $("#mfa-message").hide();
            $("#mfa-user-details").show();
        }
      }
    });
}));