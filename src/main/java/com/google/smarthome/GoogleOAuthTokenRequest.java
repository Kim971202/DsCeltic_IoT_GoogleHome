package com.google.smarthome;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleOAuthTokenRequest {

    public static String requestTokens(String clientId, String clientSecret, String authorizationCode, String redirectUri) {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";

        try {
            // Build the request body
            String requestBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&code=" + URLEncoder.encode(authorizationCode, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code" +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

            // Create the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the HTTP request
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Handle the response
            if (response.statusCode() == 200) {
                System.out.println("Tokens fetched successfully: " + response.body());
                return response.body(); // Returns the response JSON containing tokens
            } else {
                System.err.println("Failed to fetch tokens. Status Code: " + response.statusCode());
                System.err.println("Response: " + response.body());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error fetching tokens: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Replace these values with your JSON values
        String clientId = "505891126739-acrkfeg05di1guvb6bpunj3cofb1lp41.apps.googleusercontent.com";
        String clientSecret = "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";
        String authorizationCode = "YOUR_AUTHORIZATION_CODE";
        String redirectUri = "https://oauth-redirect.googleusercontent.com/r/dsiot-52315";

        // Request tokens
        String tokensResponse = requestTokens(clientId, clientSecret, authorizationCode, redirectUri);

        if (tokensResponse != null) {
            System.out.println("Tokens Response: " + tokensResponse);
        }
    }
}