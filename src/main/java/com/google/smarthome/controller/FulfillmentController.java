package com.google.smarthome.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.dto.QueryResult;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.service.FulfillmentService;
import com.google.smarthome.utils.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FulfillmentController {

    @Autowired
    private GoogleMapper googleMapper;
    @Autowired
    private FulfillmentService fulfillmentService;
    @Autowired
    RedisCommand redisCommand;

    @PostMapping("/access/api/fulfillment")
    public String handleFulfillment(@RequestBody String request, HttpServletRequest httpRequest) throws JsonProcessingException {
        log.info("POST /api/fulfillment CALLED");
        log.info("Request Body: " + request);

        String googleAuth = httpRequest.getHeader("authorization").replace("Bearer ", "");
        log.info("googleAuth: " + googleAuth);

        String userId = redisCommand.getValues(googleAuth);
        log.info("userId: " + userId);

        // 해당 사용자가 각방이 있는 지 확인
        // 4d4332363030 (MC2600) 여부로 확인
        List<GoogleDTO> devices = googleMapper.getDeviceIdByUserId(userId);

        System.out.println("devices: " + devices);

        for(GoogleDTO googleDTO : devices){
            if(googleDTO.getDeviceId().contains("44522d33303057")){ // DR-300W
                List<GoogleDTO> additionalDevices = googleMapper.getEachRoomDeviceIdByUserId(googleDTO.getDeviceId());
                if (additionalDevices != null) {
                    devices.addAll(additionalDevices);
                }
            }
            System.out.println("googleDTO.getDeviceId(): " + googleDTO.getDeviceId());
        }

        if (devices.isEmpty()) {
            log.error("No devices found for userId: " + userId);
            return new JSONObject().put("error", "No devices found").toString();
        }

        devices.forEach(device -> log.info("Device for userId {}: DeviceId: {}, DeviceModelCode: {}",
                userId, device.getDeviceId(), device.getDeviceModelCode()));

        JSONObject requestBody = new JSONObject(request);
        String intent = requestBody.getJSONArray("inputs").getJSONObject(0).getString("intent");

        JSONObject response = new JSONObject();

        List<String> deviceIds = devices.stream()
                .map(GoogleDTO::getDeviceId)
                .collect(Collectors.toList());

        Map<String, String> deviceInfoMap = new LinkedHashMap<>();
        devices.forEach(device -> deviceInfoMap.put(device.getDeviceId(), device.getDeviceModelCode()));

        // 출력
        deviceInfoMap.forEach((deviceId, modelCode) -> {
            System.out.println("Device ID: " + deviceId + ", Model Code: " + modelCode);
        });

        switch (intent) {
            case "action.devices.SYNC":
                response = fulfillmentService.handleSync(requestBody, deviceInfoMap, userId);
                break;
            case "action.devices.EXECUTE":
                response = fulfillmentService.handleExecute(requestBody, deviceIds, userId);
                break;
            case "action.devices.QUERY":
                response = fulfillmentService.handleQuery(requestBody, deviceIds);

                // --- 추가된 부분: sendDataBasedOnQueryResult 호출 ---
                QueryResult.Response queryResponse = prepareQueryResponse(requestBody, response);
                fulfillmentService.sendDataBasedOnQueryResult(userId, queryResponse);
                break;
            default:
                response.put("error", "Unknown intent");
        }
        return response.toString();
    }

    private QueryResult.Response prepareQueryResponse(JSONObject requestBody, JSONObject queryResponse) {
        QueryResult.Response response = new QueryResult.Response();
        response.setRequestId(requestBody.getString("requestId"));

        QueryResult.Response.Payload payload = new QueryResult.Response.Payload();
        Map<String, Map<String, Object>> devicesMap = new LinkedHashMap<>();

        JSONObject responseDevices = queryResponse.getJSONObject("payload").getJSONObject("devices");
        for (String deviceId : responseDevices.keySet()) {
            JSONObject state = responseDevices.getJSONObject(deviceId);
            Map<String, Object> stateMap = new HashMap<>();

            for (String key : state.keySet()) {
                stateMap.put(key, state.get(key));
            }
            devicesMap.put(deviceId, stateMap);
        }
        payload.setDevices(devicesMap);
        response.setPayload(payload);

        return response;
    }
}