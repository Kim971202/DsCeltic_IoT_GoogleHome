package com.google.smarthome.controller;

import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.service.FulfillmentService;
import com.google.smarthome.utils.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FulfillmentController {

    private static final Logger logger = LoggerFactory.getLogger(FulfillmentController.class);

    @Autowired
    private GoogleMapper googleMapper;
    @Autowired
    private FulfillmentService fulfillmentService;
    @Autowired
    RedisCommand redisCommand;

    @PostMapping("/api/fulfillment")
    public String handleFulfillment(@RequestBody String request, HttpServletRequest httpRequest) {
        log.info("POST /api/fulfillment CALLED");
        log.info("Request Body: " + request);

        String googleAuth = httpRequest.getHeader("authorization").replace("Bearer ", "");
        log.info("googleAuth: " + googleAuth);

        String userId = redisCommand.getValues(googleAuth);
        log.info("userId: " + userId);

        List<GoogleDTO> devices = googleMapper.getDeviceIdByUserId(userId);
        if (devices == null || devices.isEmpty()) {
            log.error("No devices found for userId: " + userId);
            return new JSONObject().put("error", "No devices found").toString();
        }

        devices.forEach(device -> log.info("Device for userId {}: {}", userId, device.getDeviceId()));

        JSONObject requestBody = new JSONObject(request);
        String intent = requestBody.getJSONArray("inputs").getJSONObject(0).getString("intent");

        JSONObject response = new JSONObject();
        List<String> deviceIds = devices.stream()
                .map(GoogleDTO::getDeviceId)
                .collect(Collectors.toList());

        log.info("Device IDs: " + deviceIds);

        switch (intent) {
            case "action.devices.SYNC":
                response = fulfillmentService.handleSync(requestBody, deviceIds, userId);
                break;
            case "action.devices.EXECUTE":
                response = fulfillmentService.handleExecute(requestBody, deviceIds, userId);
                break;
            case "action.devices.QUERY":
                response = fulfillmentService.handleQuery(requestBody, deviceIds);
                break;
            default:
                response.put("error", "Unknown intent");
        }
        return response.toString();
    }

}