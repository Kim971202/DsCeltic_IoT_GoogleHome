package com.google.smarthome.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.smarthome.utils.RedisCommand;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthTokenController {

    @Value("${custom.google.oauth2.client_id}")
    private String googleOauth2ClientId;
    @Value("${custom.google.oauth2.client_secret}")
    private String googleOauth2ClientSecret;
    @Value("${custom.google.oauth2.project_id}")
    private String googleOauth2ProjectId;
    @Value("${custom.google.oauth2.expires}")
    private int googleOauth2Expires;
    @Autowired
    RedisCommand redisCommand;
    @ResponseBody
    @PostMapping("/oauth2/token")
    public ResponseEntity<?> authorize(
            @RequestParam(name = "grant_type") String grantType,
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "client_secret", required = false) String clientSecret,
            @RequestParam(name = "code", required = false) String authorizationCode,
            @RequestParam(name = "refresh_token", required = false) String refreshToken,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            HttpServletRequest request) throws Exception {

        // Authorization 헤더 처리
        if (StringUtils.hasText(authorization)) {
            log.info("authorization basic 방식");
            if (authorization.startsWith("Basic ")) {
                String base64Credentials = authorization.substring(6);
                String decodedValue = new String(Base64Utils.decodeFromString(base64Credentials));
                log.info("decodedValue: {}", decodedValue);

                String[] credentials = decodedValue.split(":", 2); // split with limit
                if (credentials.length == 2) {
                    clientId = credentials[0];
                    clientSecret = credentials[1];
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "invalid_request", "error_description", "Invalid Authorization header format."));
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "invalid_request", "error_description", "Authorization header must start with 'Basic '."));
            }
        }

        log.info("clientId: {}", clientId);
        log.info("clientSecret: {}", clientSecret);

        // 토큰 생성
        String newAccessToken = UUID.randomUUID().toString();
        String newRefreshToken = Base64Utils.encodeToUrlSafeString(newAccessToken.getBytes(StandardCharsets.UTF_8));

        // grant_type 처리
        switch (grantType) {
            case "authorization_code":
                if (authorizationCode == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "invalid_request", "error_description", "Authorization code is required."));
                }
                log.info("grant_type=authorization_code");
                redisCommand.setValues(authorizationCode, clientId); // Redis에 저장
                return ResponseEntity.ok(TokenBody.builder()
                        .tokenType("Bearer")
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .expiresIn(googleOauth2Expires)
                        .build());

            case "refresh_token":
                if (refreshToken == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "invalid_request", "error_description", "Refresh token is required."));
                }
                log.info("grant_type=refresh_token");

                // Refresh Token 검증
                String storedClientId = redisCommand.getValues(refreshToken);
                if (storedClientId == null || !storedClientId.equals(clientId)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "invalid_grant", "error_description", "Invalid refresh token."));
                }

                return ResponseEntity.ok(TokenBody.builder()
                        .tokenType("Bearer")
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken) // Refresh Token은 갱신하지 않음
                        .expiresIn(googleOauth2Expires)
                        .build());

            default:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "unsupported_grant_type", "error_description", "Grant type not supported."));
        }

    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder @Getter @Setter @ToString
    public static class TokenBody {
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("refresh_token")
        private String refreshToken;
        @JsonProperty("expires_in")
        private int expiresIn;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @NoArgsConstructor @AllArgsConstructor
    @Builder @Getter
    @Setter
    @ToString
    public static class Error {
        private String error;
    }
}
