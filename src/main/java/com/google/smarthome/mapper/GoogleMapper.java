package com.google.smarthome.mapper;

import com.google.smarthome.dto.GoogleDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoogleMapper {

    public GoogleDTO getInfoByDeviceId(String deviceId);

    public int updateDeviceStatus(GoogleDTO value);

    public GoogleDTO getAccountByUserId(String userId);

    public GoogleDTO getDeviceIdByUserId(String userId);
}
