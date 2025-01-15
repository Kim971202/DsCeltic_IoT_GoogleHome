package com.google.smarthome.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

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

    private static final long REPORT_INTERVAL_SECONDS = 30; // 상태 보고 주기: 30초
    private Instant lastReportTime = Instant.MIN;

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

        // DB에서 기기 상태 가져오기
        log.info("Querying device state from DB for deviceId: " + deviceId);
        GoogleDTO result = googleMapper.getInfoByDeviceId(deviceId);
        if (result == null) {
            log.error("No device found in DB for deviceId: " + deviceId);
            return;
        }
        log.info("Retrieved state from DB: " + result);

        // 기기 상태 생성
        boolean powerOnOff = "on".equals(result.getPowrStatus());
        Map<String, Object> deviceStates = Map.of(
                result.getDeviceId(), Map.of(
                        "currentModeSettings", Map.of("mode_boiler", result.getModeValue()),
                        "online", true,
                        "temperatureAmbientCelsius", Float.parseFloat(result.getCurrentTemp()),
                        "temperatureSetpointCelsius", Float.parseFloat(result.getTempStatus()),
                        "on", powerOnOff));

        log.info("Constructed device state: " + deviceStates);

        // Google에 상태 보고
        reportDeviceState(accessToken, "yohan2025", deviceStates);

         // Google과 동기화 요청
         requestSync(accessToken, "yohan2025");
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

    public void requestSync(String googleOAuth2AccessToken, String agentUserId) {
        String url = "https://homegraph.googleapis.com/v1/devices:requestSync";

        try {
            Map<String, Object> payload = Map.of(
                "agentUserId", agentUserId,
                "async", false
                );
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + googleOAuth2AccessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
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

// public class AppServerController {

// @Autowired
// private Common common;

// @Autowired
// private GoogleMapper googleMapper;

// @PostMapping(value = "/AppServerToGoogle")
// @ResponseBody
// public void receiveCin(@RequestBody String jsonBody) throws Exception{
// log.info("AppServer Received JSON: " + jsonBody);

// String accessToken = getTokeString();

// String deviceId = common.readCon(jsonBody, "deviceId");
// log.info("Extracted deviceId: " + deviceId);

// GoogleDTO result = new GoogleDTO();
// result = googleMapper.getInfoByDeviceId(deviceId);
// boolean powerOnOff = "on".equals(result.getPowrStatus());
// log.info("Device power status: " + result.getPowrStatus());

// Map<String, Object> deviceStates = Map.of(
// result.getDeviceId(), Map.of(
// "currentModeSettings", Map.of("mode_boiler", result.getModeValue()),
// "online", true,
// "temperatureAmbientCelsius", Float.parseFloat(result.getCurrentTemp()),
// "temperatureSetpointCelsius", Float.parseFloat(result.getTempStatus()),
// "on", powerOnOff));

// // Log the constructed device state
// log.info("Constructed device state: " + deviceStates);

// // Request Google to sync devices
// requestSync(accessToken, "yohan2025");

// // Report device state to Google
// reportDeviceState(accessToken, "yohan2025", deviceStates);
// }

// private String getTokeString(){
// // AcessTokenRequester 인스턴스 생성 및 호출
// AcessTokenRequester tokenRequester = new AcessTokenRequester();
// tokenRequester.request(); // 토큰 요청
// String token = tokenRequester.getToken(); // 토큰 가져오기
// System.out.println("Access Token: " + token); // 토큰 출력
// return token;
// }

// public void requestSync(String googleOAuth2AccessToken, String agentUserId) {
// String url = "https://homegraph.googleapis.com/v1/devices:requestSync";

// try {
// // RequestSync Payload
// Map<String, Object> payload = Map.of("agentUserId", agentUserId);

// // JSON 변환
// ObjectMapper objectMapper = new ObjectMapper();
// String requestBody = objectMapper.writeValueAsString(payload);

// // HTTP 요청 생성
// HttpRequest request = HttpRequest.newBuilder()
// .uri(URI.create(url))
// .header("Authorization", "Bearer " + googleOAuth2AccessToken)
// .header("Content-Type", "application/json")
// .POST(HttpRequest.BodyPublishers.ofString(requestBody))
// .build();

// // 요청 보내기
// HttpClient client = HttpClient.newHttpClient();
// HttpResponse<String> response = client.send(request,
// HttpResponse.BodyHandlers.ofString());

// // 응답 처리
// if (response.statusCode() == 200) {
// System.out.println("RequestSync called successfully: " + response.body());
// } else {
// System.err.println("RequestSync failed. Status Code: " +
// response.statusCode());
// System.err.println("Response: " + response.body());
// }
// } catch (Exception e) {
// System.err.println("Error requesting sync: " + e.getMessage());
// e.printStackTrace();
// }
// }

// public void reportDeviceState(String googleOAuth2AccessToken, String
// agentUserId, Map<String, Object> deviceStates) {
// String url =
// "https://homegraph.googleapis.com/v1/devices:reportStateAndNotification";

// // Payload structure
// // ReportStatusResult 객체 생성
// ReportStatusResult.Request reportStatusResult =
// ReportStatusResult.Request.builder()
// .requestId(UUID.randomUUID().toString())
// .agentUserId(agentUserId)
// .payload(ReportStatusResult.Request.Payload.builder()
// .devices(ReportStatusResult.Request.Payload.Device.builder()
// .states(deviceStates)
// .build())
// .build())
// .build();

// try {
// ObjectMapper objectMapper = new ObjectMapper();
// String requestBody = objectMapper.writeValueAsString(reportStatusResult);

// log.info("Sending ReportStatusResult request:");
// log.info("URL: " + url);
// log.info("Headers: Authorization=Bearer " + googleOAuth2AccessToken + ",
// Content-Type=application/json");
// log.info("Body: " + requestBody);

// HttpRequest request = HttpRequest.newBuilder()
// .uri(URI.create(url))
// .header("Authorization", "Bearer " + googleOAuth2AccessToken)
// .header("Content-Type", "application/json")
// .POST(HttpRequest.BodyPublishers.ofString(requestBody))
// .build();

// HttpClient client = HttpClient.newHttpClient();
// HttpResponse<String> response = client.send(request,
// HttpResponse.BodyHandlers.ofString());

// if (response.statusCode() == 200) {
// log.info("Device state updated successfully: " + response.body());
// } else {
// log.error("Failed to update device state. Status Code: " +
// response.statusCode());
// log.error("Response: " + response.body());
// }
// } catch (Exception e) {
// log.error("Error reporting device state: " + e.getMessage(), e);
// }
// }

// }