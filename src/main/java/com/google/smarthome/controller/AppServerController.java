package com.google.smarthome.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.utils.AcessTokenRequester;
import com.google.smarthome.utils.Common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AppServerController {
    
    @Autowired
    private Common common;

    @Autowired
    private GoogleMapper googleMapper;

    @PostMapping(value = "/AppServerToGoogle")
    @ResponseBody
    public void receiveCin(@RequestBody String jsonBody) throws Exception{
        log.info("AppServer Received JSON: " + jsonBody);

        String deviceId = common.readCon(jsonBody, "deviceId");
        
        GoogleDTO result = googleMapper.getInfoByDeviceId(deviceId);

        boolean powerOnOff = false;
        if(result.getPowrStatus().equals("on")) powerOnOff = true;

        Map<String, Object> deviceStates = Map.of(
            result.getDeviceId(), Map.of(
                "currentModeSettings", Map.of("mode_boiler", result.getModeValue()),
                "online", true,
                "temperatureAmbientCelsius", Float.parseFloat(result.getCurrentTemp()),
                "temperatureSetpointCelsius", Float.parseFloat(result.getTempStatus()),
                "on", powerOnOff
            )
        );
        
        reportDeviceState("yohan2025", deviceStates);
        requestSync(getTokeString(), "yohan2025");
    }

    private String getTokeString(){
        // AcessTokenRequester 인스턴스 생성 및 호출
        AcessTokenRequester tokenRequester = new AcessTokenRequester();
        tokenRequester.request(); // 토큰 요청
        String token = tokenRequester.getToken(); // 토큰 가져오기
        System.out.println("Access Token: " + token); // 토큰 출력
        return token;
    }

    public void requestSync(String googleOAuth2AccessToken, String agentUserId) {
        String url = "https://homegraph.googleapis.com/v1/devices:requestSync";
    
        try {
            // RequestSync Payload
            Map<String, Object> payload = Map.of("agentUserId", agentUserId);
    
            // JSON 변환
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(payload);
    
            // HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + googleOAuth2AccessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    
            // 요청 보내기
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            // 응답 처리
            if (response.statusCode() == 200) {
                System.out.println("RequestSync called successfully: " + response.body());
            } else {
                System.err.println("RequestSync failed. Status Code: " + response.statusCode());
                System.err.println("Response: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error requesting sync: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void reportDeviceState(String agentUserId, Map<String, Object> deviceStates) {
        String url = "https://homegraph.googleapis.com/v1/devices:reportStateAndNotification";
        
        String googleOAuth2AccessToken = getTokeString();

        // Payload structure
        Map<String, Object> payload = Map.of(
            "requestId", UUID.randomUUID().toString(),
            "agentUserId", agentUserId,
            "payload", Map.of(
                "devices", Map.of("states", deviceStates)
            )
        );

        try {
            // Convert payload to JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(payload);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + googleOAuth2AccessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            // Send request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Handle response
            if (response.statusCode() == 200) {
                System.out.println("Device state updated successfully: " + response.body());
            } else {
                System.err.println("Failed to update device state. Status Code: " + response.statusCode());
                System.err.println("Response: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error reporting device state: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
