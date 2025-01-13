package com.google.smarthome;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class GoogleAuthorizationCodeFetcher {

    public static void main(String[] args) {
        // Replace these with your credentials
        String clientId = "505891126739-acrkfeg05di1guvb6bpunj3cofb1lp41.apps.googleusercontent.com";
        String redirectUri = "https://oauth-redirect.googleusercontent.com/r/dsiot-52315";
        String scope = "https://www.googleapis.com/auth/homegraph"; // Scope for the required permissions
        String authUri = "https://accounts.google.com/o/oauth2/auth";

        // Construct the authorization URL
        String authorizationUrl = authUri + "?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + scope +
                "&access_type=offline" +
                "&prompt=consent";

        System.out.println("Open the following URL in your browser to authorize the application:");
        System.out.println(authorizationUrl);

        // Automatically open the browser (if supported)
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(authorizationUrl));
            } catch (IOException | URISyntaxException e) {
                System.err.println("Failed to open the browser. Please open the URL manually.");
            }
        }

        // Prompt user to enter the authorization code
        System.out.println("\nAfter authorizing, Google will redirect you to the Redirect URI. Copy the 'code' parameter from the URL and paste it below.");
        System.out.print("Enter the authorization code: ");
        Scanner scanner = new Scanner(System.in);
        String authorizationCode = scanner.nextLine();

        System.out.println("Authorization Code: " + authorizationCode);

        // Use this authorization code in the token exchange process
    }
}
