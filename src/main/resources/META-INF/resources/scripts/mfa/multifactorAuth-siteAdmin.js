// Copyright 2019 Radiologics, Inc
// Developer: Kate Alpert <kate@radiologics.com>

console.log('multifactorAuth-siteAdmin.js');

var XNAT = getObject(XNAT || {});
XNAT.plugin = getObject(XNAT.plugin || {});
XNAT.plugin.mfa_plugin = getObject(XNAT.plugin.mfa_plugin || {});

(function(factory){
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function() {
    var mfaSiteManager, mfaSiteConfig;
    const loggedInUser = window.username;

    XNAT.plugin.mfa_plugin.mfaSiteManager = mfaSiteManager =
        getObject(XNAT.plugin.mfa_plugin.mfaSiteManager || {});

    XNAT.plugin.mfa_plugin.mfaSiteConfig = mfaSiteConfig =
        getObject(XNAT.plugin.mfa_plugin.mfaSiteConfig|| {});

    XNAT.plugin.mfa_plugin.mfaSiteManager.mfaMethodOptions = mfaMethodOptions = [{label: 'Select', value: null}];
    var preferredMFAForm = document.getElementById('preferred-site-mfa');

    var    xnatUsersList, mfaUsersList;

    XNAT.plugin.mfa_plugin.xnatUsersList = xnatUsersList = [];
    XNAT.plugin.mfa_plugin.mfaUsersList = mfaUsersList  = getObject(XNAT.plugin.mfa_plugin.mfaUsersList || []);

    XNAT.plugin.mfa_plugin.exemptedUsers = exemptedUsers =
        getObject(XNAT.plugin.mfa_plugin.exemptedUsers || {});

    XNAT.plugin.mfa_plugin.settings = {};

   function displayErrors(errorMsg) {
        var errors = [];
        errorMsg.forEach(function(msg){ errors.push(spawn('li', '<b>' + msg.field + '</b> ' + msg.message)) });

        return spawn('div',[
            spawn('p', 'Errors found:'),
            spawn('ul', errors)
        ]);
    }
    function spacer(width){
        return spawn('i.spacer', {
            style: {
                display: 'inline-block',
                width: width + 'px'
            }
        })
    }

    function mfaMethodsUrl(appended){
        appended = appended ? '/' + appended  : '';
        return XNAT.url.restUrl('/xapi/mfa/methods' + appended);
    }

   function siteConfigUrl(appended){
        appended = appended ? '/' + appended  : '';
        return XNAT.url.restUrl('/xapi/mfa/preference' + appended);
    }

   function getMfaPreferredUrl(appended){
        appended = appended ? '/' + appended  : '';
        return XNAT.url.restUrl('/xapi/mfa/preferred' + appended);
   }

   function siteConfigMFAUrl(appended){
       appended = appended ? '/' + appended  : '';
       return XNAT.url.restUrl('/xapi/mfa/configure' + appended);
   }

	function getMFAEmailBackupUrl(){
		return XNAT.url.restUrl('/xapi/mfa/preference');
	}

	function emailBackupUrl(appended){
		appended = appended ? '/' + appended  : '';
		return XNAT.url.restUrl('/xapi/mfa/emailbackup' + appended);
	}

   function getUsersUrl(appended){
        appended = appended ? '/' + appended  : '';
        return XNAT.url.restUrl('/xapi/users' + appended);
    }

   function getMFAExemptUsers(appended){
        appended = appended ? '/' + appended  : '';
        return XNAT.url.restUrl('/xapi/mfa/exempted' + appended);
    }

   function getMFAUsers(appended){
        appended = appended ? '/' + appended  : '';
        return XNAT.url.restUrl('/xapi/mfa/users' + appended);
    }

   function getMFAMultipleExemptUsers(appended){
        return XNAT.url.restUrl('/xapi/mfa/exemptmultiple' + appended);
    }

   function getMFAMultipleEnforceMfa(appended){
        return XNAT.url.restUrl('/xapi/mfa/enforcemfamultiple' + appended);
   }

	function unregisterMfaUser(user){
	   return XNAT.url.restUrl('/xapi/mfa/'+user+'/unregister')
	}

	function setExemptStatusForUser(user,status) {
	    return XNAT.url.csrfUrl('/xapi/mfa/exempt/'+user+'/'+status)
	}

    function errorHandler(e, message){
        message = message ? message + '<br/><br/>' : '';
        var details = e.responseText ? spawn('p',[message, e.responseText]) : '';
        xmodal.alert({
            title: 'Error',
            content: '<p><strong>Error ' + e.status + ': '+ e.statusText+'</strong></p>' + details.html,
            okAction: function () {
                xmodal.closeAll();
            }
        });
    }
     // get the list of available MFA Methods
	 mfaSiteConfig.setSitePreferences = setSitePreferences = function(sitePreferencesJson){
     	if (String(sitePreferencesJson.requireMfa) === "true") {
			$("#requireMfa").prop("checked", true);
		}else {
			$("#requireMfa").prop("checked", false);
		}
		if (String(sitePreferencesJson.requireAdminMfa) === "true") {
			$("#requireAdminMfa").prop("checked", true);
		}else {
			$("#requireAdminMfa").prop("checked", false);
		}

	    mfaSiteConfig.container.parents('.panel').find('.panel-footer').empty().append(spawn('!',[
	        spawn('div.pull-right button.btn.sm.save', {
                onclick: function(e){
                    e.preventDefault();
                    var requireMfaElt = document.getElementById('requireMfa');
                    var requireAdminMfaElt = document.getElementById('requireAdminMfa');
                    var requireMfaEltChecked = requireMfaElt.checked;
                    var requireAdminMfaEltChecked = requireAdminMfaElt.checked;
                    xmodal.loading.open({ title: 'Updating MFA Configuration'});
                    XNAT.xhr.post({
                        url: siteConfigMFAUrl('?requireMfa='+requireMfaEltChecked + '&requireAdminMfa=' + requireAdminMfaEltChecked),
                        async: false,
                        dataType: 'text',
                        success: function () {
                            xmodal.loading.close();
                            XNAT.dialog.closeAll();
                            XNAT.ui.banner.top(2000, 'Updated MFA Configuration', 'success');
                            location.reload();
                        },
                        error: function (e) {
                            xmodal.loading.close();
                            errorHandler(e);
                        }
                    });
                }
            }, 'Save'),
            spawn('div.clearfix.clear')
	    ]));
     };

  // get the list of available MFA Methods
    mfaSiteConfig.getAllData = function(){
        return XNAT.xhr.get({
            url: siteConfigUrl(),
            success: function(data){
				var methods_response_str = JSON.stringify(data);
				sitePreferencesJson = JSON.parse(methods_response_str);
				setSitePreferences(sitePreferencesJson);
				Object.assign(XNAT.plugin.mfa_plugin.settings,sitePreferencesJson);
            },
            error: function(e) {
                errorHandler(e);
            }
        });
    };

    mfaSiteConfig.init = function(container) {
	        var $siteConfigManager = $$(container||'div#mfa-siteConfig-manager');

	        mfaSiteConfig.container = $siteConfigManager;
			mfaSiteConfig.getAllData();

	        return {
	            element: $siteConfigManager[0],
	            spawned: $siteConfigManager[0],
	            get: function(){
	                return $siteConfigManager[0]
	            }
	        };
	    };


    mfaSiteConfig.init();

    mfaSiteManager.populateMFAMethods = populateMFAMethods = function(mfaChoices, preferred) {
    	$.each(mfaChoices.mfa_methods, function(i, e) {
			if (e === preferred) {
				mfaMethodOptions.push({
							value: e,
							label: e,
							element: {
								classes: 'mfa-method-' + e,
								selected: "true"
							}
					});
			}else {
				mfaMethodOptions.push({
							value: e,
							label: e,
							element: {
								classes: 'mfa-method-' + e
							}
					});
			}
		});

		mfaSiteManager.container.append(
            spawn('mfa-settings-form', [
                XNAT.ui.panel.select.single({
                    name: 'preferredMfaMethod',
                    label: 'Preferred MFA Method',
                    description: 'Select the site wide preferred MFA Method',
                    options: mfaSiteManager.mfaMethodOptions
                }).element,
                XNAT.ui.panel.input.switchbox({
                    name: 'enableMfaEmailBackup',
                    id: 'enableMfaEmailBackup',
                    label: 'Allow Email As Backup MFA Option',
                    description: 'User can choose email as the MFA method during login',
                }).element,
                XNAT.ui.panel.input.switchbox({
                    name: 'mfaAdminEmailNotification',
                    id: 'mfaAdminEmailNotification',
                    label: 'Admin Email MFA Notification',
                    description: 'Send an email notification to admin if the email MFA has been used by a user',
                }).element
            ])
        );

        mfaSiteManager.container.parents('.panel').find('.panel-footer').empty().append(spawn('!',[
            spawn('div.pull-right button.btn.sm.save', {
                onclick: function(e){
                    e.preventDefault();
                    var selectedPreferredMFAElt = document.getElementById('preferred-mfa-method');
                    var selectedPreferredMFAEltValue = selectedPreferredMFAElt.value;

                    var emailAppend = '';
                    if (document.getElementById('enableMfaEmailBackup').checked) {
                        emailAppend ='?emailBackup=true';
                    } else {
                        emailAppend ='?emailBackup=false';
                    }
                    if (document.getElementById('mfaAdminEmailNotification').checked) {
                        emailAppend +='&mfaAdminEmailNotification=true';
                    } else {
                        emailAppend +='&mfaAdminEmailNotification=false';
                    }
                    XNAT.xhr.post({
                        url: getMfaPreferredUrl(selectedPreferredMFAEltValue + '?switchAll=false'),
                        success: function () {
                            XNAT.xhr.post({
                                url: emailBackupUrl(emailAppend),
                                success: function () {
                                    XNAT.ui.banner.top(2000,'MFA site preferences updated','success');
                                },
                                error: function (e) {
                                    XNAT.ui.banner.top(2000,'An error occurred','error');
                                    errorHandler(e);
                                }
                            });
                        },
                        error: function (e) {
                            XNAT.ui.banner.top(2000,'An error occurred','error');
                            errorHandler(e);
                        }
                    });
                }
            }, 'Save'),
            spawn('div.clearfix.clear')
        ]));
	};

	mfaSiteManager.getEmailBackup = getEmailBackup = function() {
		return XNAT.xhr.get({
			url: getMFAEmailBackupUrl(),
			success: function (data) {
				if (data && data['emailBackupEnabled']) {
					$("#enableMfaEmailBackup").prop('checked', true);
				}
				if (data && data['mfaAdminEmailNotificationEnabled']) {
					$("#mfaAdminEmailNotification").prop('checked', true);
				}
			},
			error: function (e) {
				errorHandler(e);
			}
		});
	};

   // get the list of available MFA Methods
     mfaSiteManager.getPreferredMFA = getPreferredMFA = function(mfaChoices){
         return XNAT.xhr.get({
             url: getMfaPreferredUrl(),
             success: function(data){
 				populateMFAMethods(mfaChoices, data);
             },
             error: function(e) {
                 errorHandler(e);
             }
         });
     };


   // get the list of available MFA Methods
    mfaSiteManager.getAll = function(){
        return XNAT.xhr.get({
            url: mfaMethodsUrl(),
            dataType: 'json',
            success: function(data){
				var methods_response_str = JSON.stringify(data);
				var mfaChoices = JSON.parse(methods_response_str);
				getPreferredMFA(mfaChoices)
				mfaSiteManager.getEmailBackup();
            },
            error: function(e) {
                errorHandler(e);
            }
        });
    };


    mfaSiteManager.init = function(container) {
	        var $manager = $$(container||'div#preferredSiteMfa-manager');

	        mfaSiteManager.container = $manager;
			mfaSiteManager.getAll();
	        return {
	            element: $manager[0],
	            spawned: $manager[0],
	            get: function(){
	                return $manager[0]
	            }
	        };
	};

    mfaSiteManager.init();

    exemptedUsers.getAll = function(){
	        XNAT.xhr.get({
	            url: getUsersUrl('/profiles'),
	            async: false,
	            dataType: 'json',
	            success: function(data){
	                xnatUsersList = data;
	            },
	            error: function(e) {
	                errorHandler(e);
	            }
	        });

			XNAT.xhr.get({
				url: getMFAUsers(),
				dataType: 'json',
				async: false,
				success: function(data){
					var exemptedUserObj = JSON.parse(JSON.stringify(data));
					mfaUsersList = data['mfa_users'];
				},
				error: function(e) {
					errorHandler(e);
				}
			});
			return;
	 };

	exemptedUsers.isUserExempted = isUserExempted = function(userLogin) {
		var exempted = false;
		$.each( mfaUsersList , function(i,e) {
			if (userLogin == e.username && e.mfaExempted) {
				exempted = true;
			}
		});
		return exempted;
	}

	exemptedUsers.getMFAMethod = getMFAMethod = function(userLogin) {
		var mfaMethod = "--";
		$.each( mfaUsersList , function(i,e) {
			if (userLogin == e.username) {
				mfaMethod = e.preferredMfas;
			}
		});
		return mfaMethod;
	}

	exemptedUsers.getRegistered = getRegistered = function(userLogin) {
		var mfa_registered = "----";
		$.each( mfaUsersList , function(i,e) {
			if (userLogin === e.username) {
				mfa_registered = String(e.mfaRegistered);
			}
		});
		return mfa_registered;
	}

	XNAT.plugin.mfa_plugin.setExemptStatus = function(login,exemptStatus,reload=false) {
	    XNAT.xhr.postJSON({
            url: setExemptStatusForUser(login,exemptStatus),
            success: function(){
                XNAT.ui.banner.top(2000,'Set MFA exempt status to '+exemptStatus.toString()+' for user '+login,'success');
                if (reload) {
                    location.reload();
                } else {
                    XNAT.plugin['mfa_plugin'].exemptedUsers.init();
                }
            },
            failure: function(e){
                XNAT.ui.banner.top(2000,'An error occurred','error');
                console.error(e);
            }
        });
	}

	function exemptedCheckbox(login, exempted, disabled=false) {
	    let checkAdminStatus = (login === loggedInUser);
        const ckbox = spawn('input.exempt-user',{
            type: 'checkbox',
            checked: (exempted) ? 'checked' : false,
            value: 'true',
            classes: 'exempt-'+login,
            onchange: function(){
                const exemptStatus = !exempted;
                const thisInput = this;
                if (checkAdminStatus && !exemptStatus && XNAT.plugin.mfa_plugin.settings['requireAdminMfa'] && XNAT.plugin.mfa_plugin.exemptedUsers.getRegistered(login) === "false") {
                    // If the logged-in user toggles “Exempted” to false AND “Require MFA for Administrators” is true AND “MFA Registered” is false
                    XNAT.ui.dialog.confirm({
                        title: 'MFA Exempt Warning',
                        content: 'Removing your own MFA exemption when "Require MFA for Admins" is set to true and your own MFA registration is not complete will cause the application to immediately log you out and require an updated MFA device registration. ',
                        okLabel: 'Proceed with MFA Exemption Removal',
                        okAction: function(){
                            // add a page reload when removing exempt status.
                            XNAT.plugin.mfa_plugin.setExemptStatus(login,exemptStatus,true);
                        },
                        cancelAction: function(){
                            $(thisInput).prop('checked','checked')
                            return false;
                        }
                    })
                } else {
                    XNAT.plugin.mfa_plugin.setExemptStatus(login,exemptStatus);
                }
            }
        })

        return spawn('div.center', [
            spawn('label.switchbox',{ title: 'Toggle MFA exempt status for '+login },[
                ckbox,
                ['span.switchbox-outer', [['span.switchbox-inner']]]
            ])
        ]);
    }

    XNAT.plugin.mfa_plugin.resetAction = function(login){
        XNAT.xhr.postJSON({
           url: unregisterMfaUser(login),
           success: function(){
               XNAT.ui.banner.top(2000,'Reset MFA device and method for '+login,'success');
               XNAT.plugin['mfa_plugin'].exemptedUsers.init();
           },
           failure: function(e){
               XNAT.ui.banner.top(2000,'An error occurred','error');
               console.error(e);
           }
        });
    }

	function resetCheckbox(login) {
	    let checkAdminStatus = (login === loggedInUser);
        return spawn('div.center',[
            spawn('button.btn.btn-sm.reset-user', {
                title: 'Reset MFA for '+login,
                html: '<i class="fa fa-refresh"></i>',
                addClass: 'reset-'+login,
                onclick: function(){
                    if (checkAdminStatus && XNAT.plugin.mfa_plugin.settings['requireAdminMfa'] && !XNAT.plugin.mfa_plugin.exemptedUsers.isUserExempted(login)) {
                        // If the logged-in user clicks “Reset MFA Method” on their own account AND “Require MFA for Administrators” is true AND “Exempted” is false, confirm the action
                        XNAT.ui.dialog.confirm({
                            title: 'MFA Reset Warning',
                            content: 'Resetting your own MFA method when "Require MFA for Admins" is set to "true" will cause the application to immediately log you out and require an updated MFA device registration. You can avoid this by setting your "Exempt" status to "true" before continuing.',
                            okLabel: 'Proceed with MFA Reset',
                            okAction: function(){
                                XNAT.plugin.mfa_plugin.resetAction(login);
                            },
                            cancelAction: function(){
                                return false;
                            }
                        })
                    } else {
                        XNAT.plugin.mfa_plugin.resetAction(login);
                    }
                }
            })
        ]);
	}

	exemptedUsers.table = function($parent) {
		exemptedUsers.getAll();
		var columnIds = ["login", "mfa_method","registered", "exempted", "reset" ];
		var labelMap = {
		            login: {label: "User Login", checkboxes: false, id: "Login"},
		            mfa_method: {label: "MFA Method", checkboxes: false, id: "Method"},
					registered: {label: "MFA Registered", checkboxes: false, id: "registered"},
		            exempted: {label: "Exempted", checkboxes: true, id: "Exempted"},
					reset: {label: "Reset", checkboxes: true, id: "Reset"},
        };
		// initialize the table - we'll add to it below
        var userTable = XNAT.table({
            className: 'exempted-users-table xnat-table data-table clean',
            style: {
                width: 'auto'
            }
        });
        var $dataRows = [];
        var dataRows = [];
        function cacheRows(){
            if ($dataRows.length === 0 || $dataRows.length !== dataRows.length) {
                $dataRows = dataRows.length ?
                    $(dataRows) :
                    exemptedUsers.container.find('.table-body').find('tr');
            }
            return $dataRows;
        }
        function filterRows(val, name){
            if (!val) { return false }
            val = val.toLowerCase();
            var filterClass = 'filter-' + name;
            // cache the rows if not cached yet
            cacheRows();
            $dataRows.addClass(filterClass).filter(function(){
                return $(this).find('td.' + name).containsNC(val).length
            }).removeClass(filterClass);
            exemptedUsers.$table.find('.selectable-select-all').each(function(){
                setIndeterminate($(this), $(this).data('configId'), $(this).prop('checked'));
            });
        }


        userTable.thead().tr();
        $.each(columnIds, function(i, c) {
            userTable.th('<b>' + labelMap[c].label + '</b>');
        });

        userTable.tr({classes: 'filter'});
         $.each(columnIds, function(i, c) {
		            if (labelMap[c].checkboxes) {
		                userTable.td("", "");
		            } else {
		                document.head.appendChild(spawn('style|type=text/css', 'tr.filter-' + c + '{display:none;}'));
		                var $filterInput = $.spawn('input.filter-data', {
		                    type: 'text',
		                    title: c + ':filter',
		                    placeholder: 'Filter by ' + labelMap[c].label,
		                    style: 'width: 90%;'
		                });
		                $filterInput.on('focus', function(){
		                    $(this).select();
		                    cacheRows();
		                });
		                $filterInput.on('keyup', function(e){
		                    var val = this.value;
		                    var key = e.which;
		                    // don't do anything on 'tab' keyup
		                    if (key == 9) return false;
		                    if (key == 27){ // key 27 = 'esc'
		                        this.value = val = '';
		                    }
		                    if (!val || key == 8) {
		                        $dataRows.removeClass('filter-' + c);
		                    }
		                    if (!val) {
		                        // no value, no filter
		                        return false;
		                    }
		                    filterRows(val, c);
		                });
		                userTable.td({classes: 'filter'}, $filterInput[0]);
		            }

	        });

		userTable.tbody({classes: 'table-body'});

		$.each(xnatUsersList, function (i, e) {
		    e = e.username; // grab username from user profile object
			if (e ==='guest') return true;
			userTable.tr();
			userTable.td({classes: columnIds[0]}, e);
			userTable.td({classes: columnIds[1], id: "method-" + e}, getMFAMethod(e));
			userTable.td({classes: columnIds[2]}, getRegistered(e));
            if (isUserExempted(e)) {
				userTable.td([exemptedCheckbox(e, true, false)]);
			} else {
				userTable.td([exemptedCheckbox(e, false, false)]);
			}
			userTable.td([resetCheckbox(e)]);

		});

		var $manager = $('<div class="data-table-wrapper"></div>');
		$parent.empty().prepend($manager);
		$manager.empty().prepend(userTable.table);
		exemptedUsers.container = $manager;
		exemptedUsers.$table = $(userTable.table);
	}

    $(document).on('click','.reset-mfa-for-all-users',function(){
        function resetAllUsers(){
            var selectedPreferredMFAElt = document.getElementById('preferred-mfa-method');
            var selectedPreferredMFAEltValue = selectedPreferredMFAElt.value;
            XNAT.xhr.post({
                url: getMfaPreferredUrl(selectedPreferredMFAEltValue + '?switchAll=true'),
                dataType: 'text',
                success: function () {
                    XNAT.ui.banner.top(2000, 'Updated preferred MFA method for all users', 'success')
                    XNAT.plugin['mfa_plugin'].exemptedUsers.init();
                },
                error: function (e) {
                    XNAT.ui.banner.top(2000,'An error occurred','error');
                    errorHandler(e);
                }
            });
        }

        // If the admin toggles this setting and "Require MFA for Administrators" is true and they are not exempt, they will be forced to re-enroll in MFA.
        if (XNAT.plugin.mfa_plugin.settings['requireAdminMfa'] && !XNAT.plugin.mfa_plugin.exemptedUsers.isUserExempted(loggedInUser)) {
            XNAT.ui.dialog.confirm({
                title: 'MFA Reset Warning',
                content: 'Resetting MFA methods and devices for all users (including yourself) when "Require MFA for Admins" is set to true will cause the application to immediately log you out and require an updated MFA device registration. You can avoid this by adding an MFA exemption to your account, if desired. ',
                okLabel: 'Proceed',
                okAction: function(){ resetAllUsers(); }
            });
        } else {
            resetAllUsers();
        }

    });

    $(document).on('change','input#requireAdminMfa',function(){
        // If the admin toggles “Require MFA for Administrators” to true AND “MFA Registered” is false AND “Exempted” is false for their login, warn them before proceeding
        if (!XNAT.plugin.mfa_plugin.settings['requireAdminMfa'] && !XNAT.plugin.mfa_plugin.exemptedUsers.isUserExempted(loggedInUser)) {
            var exemptMessage = (XNAT.plugin.mfa_plugin.exemptedUsers.getRegistered(loggedInUser) === "true") ? 'This action will also unset your exempt status. ' : '';
            XNAT.ui.dialog.confirm({
                title: 'MFA Requirement Warning',
                content: 'Requiring MFA for site administrators when your own MFA method is not set will cause the application to immediately log you out and require an updated MFA device registration. ' + exemptMessage,
                okLabel: 'Proceed',
                cancelLabel: 'Undo',
                cancelAction: function(){
                    $('input#requireAdminMfa').prop('checked',false);
                    XNAT.ui.dialog.closeAll();
                }
            })
        }
    })


     exemptedUsers.init = function() {
        var $parent = $(document).find('#mfa-exempt-users');

        if (exemptedUsers.$table) {
            exemptedUsers.$table.remove();
        }
        exemptedUsers.table($parent);
    };

    exemptedUsers.init();
}));