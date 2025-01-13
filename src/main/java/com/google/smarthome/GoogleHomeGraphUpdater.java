package com.google.smarthome;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController

public class GoogleHomeGraphUpdater {

    /**
     * Reports device state to Google HomeGraph API.
     *
     * @param googleOAuth2AccessToken OAuth2 token for Google API.
     * @param agentUserId The user ID associated with the devices.
     * @param deviceStates A map containing device states with device IDs as keys.
     */

     public static void requestSync(String googleOAuth2AccessToken, String agentUserId) {
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

    public static void reportDeviceState(String googleOAuth2AccessToken, String agentUserId, Map<String, Object> deviceStates) {
        String url = "https://homegraph.googleapis.com/v1/devices:reportStateAndNotification";

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

    public static void main(String[] args) {
        // Example usage
        String googleOAuth2AccessToken = "ya29.c.c0ASRK0GYgkjxwJVMechWoD7bw_nGhMdEyis7SSgJVifCx3JYcGYwXm92id4QUrbUIV2PjVdm28ukylmAix7w9fzZAZqD40cfXxFd2j3R8x99IPto7GD10YkBXq4h5c0aveaWKIr2kh2QGXJNguEARvximWNMzABRSz_kaTIwVdy8tidD4FddlKQeJ5mf3OfoudVyjwtaZY0pDX39M5nerjT0E-1HfNgCz1OUgKi2VJ_KflZBUyr3YCPc4B6qe8HFZD8LNdE9YG61uAN9sPtR33v5nBORHcq1d2nC3_8mQO3udH11R-DwdocY87JzYKWYffrtGmCVAWeCGHEHE6KwYs3yZX_ry-5AIBeCHnrw0k7vscQvHiM_Tg_PLE385AJntb3eYVuRpzju8nfe2aQQ9sIitlOu3f8cIMqh0MrcMvl_davOXZkpiQ1RqkWRMpUc4r92kiZQRmx6Q2d2wzZjrcf-St-M715S11OacoWOu9zxojR_kBW2obqkSWVpQtlJRRWpmxvBBJa3ye7ewsuS_kmtpdZcfuzRisdlty7kS3QjXV0xJt4u1M6YY-vtSlR4buFWfs27BlBnfj_ucjya5bWY0jiQwe4WdBFJzksnuo3UX4t3X7b4VvYzbUa-5zhjJ2nY-0rRvtMkMuca8Bq1w6WtqF4JI8avU9YlRfwUZ2wFeh5n6d8YdQk_nhVwicOOI6VzefBoFmUpgdV4Yyp9Xkuszzr8fSvqoZqWjc_Ver1Jwwniv_VxQigov9qch57tw6I61Y0WhsVyRhe2uSy5pnj64OS0uzjj2hU_jpYw_yxyXMp2mfnYVMbRYMU8QuOVIdUed1uldUbd19VlgYhJ4Z8xo6ZSjdYVM71Zs1pt5RljgIyv-S5ZQcnmJm__3siOx7_4U3Q6glfvOk6tofhY_c2SSn9uag5BY8OUXlX3_aYMI4Y06V5mWF195wcs4qZFRzX4aMqIeqvjMJOj1Jl0Smo681mFfFhO0ym7tapbFWJRwwXItIjBYedI";

        String agentUserId = "yohan2025";

        Map<String, Object> deviceStates = Map.of(
            "0.2.481.1.1.2045534365636f313353.20202020343833464441413731333145", Map.of(
                "currentModeSettings", Map.of("mode_boiler", "02"),
                "online", true,
                "temperatureAmbientCelsius", 30.0,
                "temperatureSetpointCelsius", 30.0,
                "on", false
            ),
            "0.2.481.1.1.204443522d39312f5746.20202020343431613834613134643730", Map.of(
                "currentModeSettings", Map.of("mode_boiler", "02"),
                "online", true,
                "temperatureAmbientCelsius", 30.5,
                "temperatureSetpointCelsius", 30.5,
                "on", false
            )
        );

        reportDeviceState(googleOAuth2AccessToken, agentUserId, deviceStates);

         // 2. RequestSync 요청
        requestSync(googleOAuth2AccessToken, agentUserId);
    }
}