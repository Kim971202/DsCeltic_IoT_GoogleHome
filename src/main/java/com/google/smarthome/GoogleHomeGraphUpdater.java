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
        String googleOAuth2AccessToken = "ya29.c.c0ASRK0GYfsKB8ov8lZE2P_Z27G9hVDfBHffwJzY38MTB4DCLpn8vVuOh5i_jsbCjlCeZdXpKQ7cArtL5smaEbYC7OLE5IULBd130pwP74gKxttpVmWBsy-9flViZbwrAlnYlcmk6csWuPR0ixZdwV5WLPjL8FH94uud2ByTb07CVaVRx8yrlnpBAPpKhw6U_O3SucsF5nLpXG2S-KJju4KcvHIFtCxMkpZMvvcmHbtfqcIKOdCu_DfDTCtD2unaX_6r77AxucXWQsG1zEgBPnvLeAraQspKddHx6Y3ENf_HffJtTry2uq1jHQDSPVRngqtLcgbRQOHokyMxGHA3YdINBrsheYG5AQ9NyeeOHbRY6zCMOeC5YyHU7cE385Pyfp_1bI_t9bBlIhVhB-z4FnS1Re0UgzIvtgdtZbrxsfI2YFhpgSV9cgrgWuFQf5ZlX-y3M0_bs3qQms6vmqQn3S4w581-wwb2zRb1Ik2U_xIZYxRYtjB-r3lUYOd4oi7w29ehsFZZ2c7w47d4m96vgj9gpW-zuink5Z_80So7pmIquIg-JYJq3Owdpt89tijp5Y0bbt4k9h5eQiuc4R8nFjVSwBZQdQu16p0y_MMxkS3k5inn8d4nfRdFQ_63yBe46zRRZrwlnWBz9cg5t-Bf0aknB1R9I7tdguo1W4djZgQX8kyIZxgymXbivgBf2brfMWXMrfUMwxwsw6Y4InukVV7xctBa3_sal6Bb-O9qgyhVWX9O6YoUUUUk4U5qJt3fROumvjuqv07YrxtMgpzBlvi7J72QjS-Ofw_a5swuFXzh6M4mJ5Z1kfif8pc1iukzYtuYrxJ0yr_qqx07hVkZqYwJY48W5qWRnJU83u_sz21Bh9Xcha4ghYxzhccjQbSxd8nZaF89wl1XvuYSsdo1W9rR5u9ZbvUclvRyvyXn5FRVVxdUVlgMsY_VssRdJRy5plW0BYFUUYmjxy7a7-4JxpOXizMrU5_JY21eive04BJambtSV4WZfQSta";

        String agentUserId = "yohan2025";

        Map<String, Object> deviceStates = Map.of(
            // "0.2.481.1.1.2045534365636f313353.20202020343833464441413731333145", Map.of(
            //     "currentModeSettings", Map.of("mode_boiler", "02"),
            //     "online", true,
            //     "temperatureAmbientCelsius", 30.0,
            //     "temperatureSetpointCelsius", 30.0,
            //     "on", false
            // ),
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