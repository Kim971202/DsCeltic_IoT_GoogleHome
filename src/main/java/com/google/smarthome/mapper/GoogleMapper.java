package com.google.smarthome.mapper;

import com.google.smarthome.dto.GoogleDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GoogleMapper {

    public GoogleDTO getInfoByDeviceId(String deviceId);
    public GoogleDTO getInfoByEachRoomDeviceId(String deviceId);
    public GoogleDTO getGoogleAuthInfo(String deviceId);
    public int updateGoogleAuthInfo(GoogleDTO value);
    public int updateDeviceStatus(GoogleDTO value);
    public int updateEachRoomDeviceStatus(GoogleDTO value);
    public int updateUserAuthInfo(GoogleDTO value);
    public GoogleDTO getUserIdByAuthorizationCode(String authorizationCode);
    public GoogleDTO getAccountByUserId(String userId);
    public GoogleDTO getNicknameByDeviceId(GoogleDTO params);
    public GoogleDTO getOnlineStatus();
    public GoogleDTO checkGoogleRegistDevice(String deviceId);
    public List<GoogleDTO> getDeviceIdByUserId(String userId);
    public List<GoogleDTO> getEachRoomDeviceIdByUserId(String deviceId);
    public List<GoogleDTO> getGroupIdByUserId(String userId);
    public List<GoogleDTO> getEachRoomDeviceIdList(String deviceId);
}
