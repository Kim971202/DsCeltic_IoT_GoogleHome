package com.google.smarthome.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Getter
@Setter
public class GoogleDTO implements Serializable {

    private static final long serialVersionUID = 54436712726576487L;

    private String powrStatus;
    private String tempStatus;

    private String deviceId;

}
