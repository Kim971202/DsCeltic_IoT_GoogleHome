package com.google.smarthome.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
public class GoogleDTO implements Serializable {

    private static final long serialVersionUID = 54436712726576487L;

    // Device
    private String powrStatus;
    private String tempStatus;

    // User
    private String userId;
    private String userPassword;
    private String authorizationCode;
    private String googleState;
    private String googleCount;
    private String deviceNickname;
    private String addressNickname;

    // Mode
    private String modeValue;
    private String sleepCode;

    private String deviceId;
    private String deviceModelCode;

    // Method to convert single deviceId to a list
    public List<String> getDeviceIds() {
        List<String> deviceIds = new ArrayList<>();
        deviceIds.add(this.deviceId);
        return deviceIds;
    }

    public List<String> getDeviceModelCodes() {
        List<String> modelCodes = new ArrayList<>();
        modelCodes.add(this.deviceModelCode);
        return modelCodes;
    }
}
