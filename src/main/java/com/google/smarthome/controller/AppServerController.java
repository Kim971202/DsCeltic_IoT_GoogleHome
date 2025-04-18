package com.google.smarthome.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.dto.ReportStatusResult;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.utils.AcessTokenRequester;
import com.google.smarthome.utils.Common;
import com.google.smarthome.utils.JSON;
import com.google.smarthome.utils.WebClientUtils;

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
    public void receiveCin(@RequestBody String jsonBody) throws Exception {
        log.info("AppServer Received JSON: " + jsonBody);

        // Access token 가져오기
        String accessToken = getTokeString();
        if (accessToken == null || accessToken.isEmpty()) {
            log.error("Failed to retrieve access token.");
            return;
        }

        // deviceId 추출
        String deviceId = common.readCon(jsonBody, "deviceId");
        log.info("Extracted deviceId: " + deviceId);

        // TBR_OPR_ACCOUNT 테이블의 GOOGLE_HOME_STATUS가 00인 사용자의 기기만 허용
        System.out.println("googleMapper.checkGoogleRegistDevice(deviceId).getDeviceCount(): " + 
        googleMapper.checkGoogleRegistDevice(deviceId).getDeviceCount());
        
        if(googleMapper.checkGoogleRegistDevice(deviceId).getDeviceCount().equals("0")){
            return;
        }

        // DB에서 기기 상태 가져오기
        log.info("Querying device state from DB for deviceId: " + deviceId);
        GoogleDTO result = googleMapper.getInfoByDeviceId(deviceId);
        if (result == null) {
            log.error("No device found in DB for deviceId: " + deviceId);
            return;
        }
        log.info("Retrieved state from DB: " + result);
        boolean online = googleMapper.getOnlineStatus().getOnline().equals("true");
        // 기기 상태 생성
        boolean powerOnOff = "on".equals(result.getPowrStatus());
        Map<String, Object> deviceStates = Map.of(
                result.getDeviceId(), Map.of(
                        "currentModeSettings", Map.of("mode_boiler", result.getModeValue()),
                        "online", online,
                        "temperatureAmbientCelsius",
                        Double.parseDouble(String.format("%.1f", Double.parseDouble(result.getCurrentTemp()))),
                        "temperatureSetpointCelsius",
                        Double.parseDouble(String.format("%.1f", Double.parseDouble(result.getTempStatus()))),
                        "on", powerOnOff));

        log.info("Constructed device state: " + deviceStates);

        // Google에 상태 보고
        reportDeviceState(accessToken, "yohan1202", deviceStates);

        // Google과 동기화 요청
        requestSync(accessToken, "yohan1202");
    }

    private String getTokeString() {
        try {
            AcessTokenRequester tokenRequester = new AcessTokenRequester();
            tokenRequester.request();
            String token = tokenRequester.getToken();
            log.info("Access Token: " + token);
            return token;
        } catch (Exception e) {
            log.error("Error retrieving access token: " + e.getMessage(), e);
            return null;
        }
    }

    public String devicesSync(String accessToken, String agentUserId) {
        String SYNC_URL = "https://homegraph.googleapis.com/v1/devices:sync";
    
        try {
            // 요청 본문 생성
            Map<String, String> payload = Map.of(
                "requestId", UUID.randomUUID().toString(),
                "agentUserId", agentUserId
            );
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(payload);
    
            // HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SYNC_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    
            // HTTP 클라이언트 생성 및 요청 전송
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            if (response.statusCode() == 200) {
                log.info("Devices sync successful: " + response.body());
                return response.body(); // JSON 응답 반환
            } else {
                log.error("Devices sync failed. Status Code: " + response.statusCode());
                log.error("Response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            log.error("Error during devices.sync: " + e.getMessage(), e);
            return null;
        }
    }
    
    public String devicesQuery(String accessToken, String agentUserId, List<String> deviceIds) {

        String QUERY_URL = "https://homegraph.googleapis.com/v1/devices:query";
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 요청 본문 생성
            Map<String, Object> payload = Map.of(
                    "requestId", java.util.UUID.randomUUID().toString(),
                    "agentUserId", agentUserId,
                    "inputs", List.of(Map.of(
                            "payload", Map.of(
                                    "devices", deviceIds.stream()
                                            .map(id -> Map.of("id", id))
                                            .collect(Collectors.toList()) // Updated for Java 8 compatibility
                            ))));

            String requestBody = objectMapper.writeValueAsString(payload);

            // HTTP 요청 생성
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(QUERY_URL))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // HTTP 클라이언트 생성 및 요청 전송
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 응답 반환
            if (response.statusCode() == 200) {
                System.out.println("Query Response: " + response.body());
                return response.body();
            } else {
                System.err.println("Query failed. Status Code: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error during devices.query: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void requestSync(String googleOAuth2AccessToken, String agentUserId) {
        String url = "https://homegraph.googleapis.com/v1/devices:requestSync";

        try {
            Map<String, Object> payload = Map.of("agentUserId", agentUserId);
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + googleOAuth2AccessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            System.out.println(request);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("RequestSync called successfully: " + response.body());
            } else {
                log.error("RequestSync failed. Status Code: " + response.statusCode());
                log.error("Response: " + response.body());
            }
        } catch (Exception e) {
            log.error("Error requesting sync: " + e.getMessage(), e);
        }
    }

    public void reportDeviceState(String googleOAuth2AccessToken, String agentUserId,
            Map<String, Object> deviceStates) {

        ReportStatusResult.Request reportStatusResult = ReportStatusResult.Request.builder()
                .requestId(UUID.randomUUID().toString())
                .agentUserId(agentUserId)
                .payload(ReportStatusResult.Request.Payload.builder()
                        .devices(ReportStatusResult.Request.Payload.Device.builder()
                                .states(deviceStates)
                                .build())
                        .build())
                .build();

        String baseUrl = "https://homegraph.googleapis.com";
        String uri = baseUrl + "/v1/devices:reportStateAndNotification";
        log.info("baseUrl:{}", uri);

        // WebClient를 통한 Google Home Graph API 요청
        WebClientUtils.getSslClient(baseUrl, MediaType.APPLICATION_JSON_VALUE, HttpMethod.POST, googleOAuth2AccessToken)
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(JSON.toJson(reportStatusResult))
                .retrieve()
                .toEntity(String.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(WebClientRequestException.class, new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE))
                .onErrorReturn(TimeoutException.class, new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT))
                .onErrorReturn(WebClientResponseException.class, new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR))
                .subscribe(new Consumer<ResponseEntity<String>>() {
                    @Override
                    public void accept(ResponseEntity<String> response) {
                        log.info("WebClient를 통한 Google Home Graph API 요청: {}", JSON.toJson(reportStatusResult, true));
                        log.info("send ReportStatusResult status code: {}", response.getStatusCode());
                        log.info("send ReportStatusResult response getBody: {}", response.getBody());
                    }
                });
    }
}