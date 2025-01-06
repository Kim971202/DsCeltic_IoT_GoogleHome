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
    
    @PostMapping("/access/api/fulfillment")
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
        String googleOAuth2AccessToken = "YOUR_GOOGLE_OAUTH2_ACCESS_TOKEN";
        String agentUserId = "yohan2025";

        Map<String, Object> deviceStates = Map.of(
            "0.2.481.1.1.2045534365636f313353.20202020303833413844434146353435", Map.of(
                "currentModeSettings", Map.of("mode_boiler", "02"),
                "online", true,
                "temperatureAmbientCelsius", 15.0,
                "temperatureSetpointCelsius", 25.0,
                "on", false
            ),
            "0.2.481.1.1.204443522d39312f5746.20202020343431613834613134643730", Map.of(
                "currentModeSettings", Map.of("mode_boiler", "02"),
                "online", true,
                "temperatureAmbientCelsius", 17.5,
                "temperatureSetpointCelsius", 12.0,
                "on", false
            )
        );

        reportDeviceState(googleOAuth2AccessToken, agentUserId, deviceStates);
    }
}