package com.google.smarthome.mapper;

import com.google.smarthome.dto.GoogleDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoogleMapper {

    public GoogleDTO getInfoByDeviceId(String deviceId);
    public GoogleDTO getGoogleAuthInfo(String deviceId);
    public int updateGoogleAuthInfo(GoogleDTO value);
    public int updateDeviceStatus(GoogleDTO value);
    public GoogleDTO getAccountByUserId(String userId);
    public List<GoogleDTO> getDeviceIdByUserId(String userId);
    public GoogleDTO getNicknameByDeviceId(GoogleDTO params);
}
