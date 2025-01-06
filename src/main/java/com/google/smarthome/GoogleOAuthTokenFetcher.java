package com.google.smarthome;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GoogleOAuthTokenFetcher {

    public static String fetchAccessToken(String clientId, String clientSecret, String authorizationCode, String redirectUri) {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";
        
        try {
            // Build request body
            String requestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                                 "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                                 "&code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8) +
                                 "&grant_type=authorization_code" +
                                 "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

            // Create HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            // Send request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Access token fetched successfully: " + response.body());
                return response.body();
            } else {
                System.err.println("Failed to fetch access token. Status Code: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error fetching access token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        String clientId = "505891126739-5nst99tq7ib748ovv80s6tdd5c0epcp3.apps.googleusercontent.com";
        String clientSecret = "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";
        String authorizationCode = "47079965-76fd-4402-8d76-b889552ff36e";
        String redirectUri = "https://oauth-redirect.googleusercontent.com/r/dsiot-52315";

        fetchAccessToken(clientId, clientSecret, authorizationCode, redirectUri);
    }
}
