package com.radiologics.mfa.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude
public class MfaModel {
    private String username;
    private Boolean mfaExempted;
    private Boolean mfaNeedsDeviceRegistration;
    private Boolean mfaRegistered;
    private String mfaPreferred;
    private String secret;
    private String qrCodeUrl;
}
