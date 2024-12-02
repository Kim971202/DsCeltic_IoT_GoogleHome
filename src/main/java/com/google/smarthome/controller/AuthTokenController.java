package com.google.smarthome.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.utils.JSON;
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
    @Autowired
    GoogleMapper googleMapper;

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

        log.info("@PostMapping(\"/oauth2/token\") CALLED");

        log.info("grantType: {}", grantType);
        log.info("authorizationCode: {}", authorizationCode);
        log.info("refreshToken: {}", refreshToken);
        log.info("redirectUri: {}", redirectUri);
        log.info("authorization: {}", authorization);

        // Authorization 헤더 디코딩 (Basic Auth)
        if (StringUtils.hasText(authorization)) {
            log.info("authorization basic 방식");
            String decodedValue = new String(Base64Utils.decodeFromString(authorization.split(" ")[1]));
            log.info("decodedValue:{}", decodedValue);
            String[] arrDecode = decodedValue.split(":");
            clientId = arrDecode[0];
            clientSecret = arrDecode[1];
        }

        log.info("clientId: {}", clientId);
        log.info("clientSecret: {}", clientSecret);

        // 새로운 Token 및 Refresh Token 생성
        String newAuthorizationToken = UUID.randomUUID().toString();
        String newRefreshToken = Base64Utils.encodeToUrlSafeString(newAuthorizationToken.getBytes(StandardCharsets.UTF_8));

        log.info("newAuthorizationToken: {}", newAuthorizationToken);
        log.info("newRefreshToken: {}", newRefreshToken);

        if (authorizationCode == null && refreshToken != null) {
            log.info("authorizationCode == null && refreshToken != null");

//            if(refreshToken.length() < 36) {
//                log.error("유효하지 않은 refreshToken: {}", refreshToken);
//                return ResponseEntity.badRequest().body(Error.builder().error("invalid_grant").build());
//            }

            // Redis에서 refreshToken으로 authorizationCode 조회
            String storedAuthorizationCode = redisCommand.getValues(refreshToken);
            System.out.println("storedAuthorizationCode: " + storedAuthorizationCode);

            if (storedAuthorizationCode == null) {
                log.error("유효하지 않은 refreshToken: {}", refreshToken);
                return ResponseEntity.badRequest().body(Error.builder().error("invalid_grant").build());
            }

            // 새로운 authorizationCode 생성 및 저장
            String newAuthorizationCode = UUID.randomUUID().toString();
            redisCommand.setValues(refreshToken, newAuthorizationCode);

            log.info("새 authorizationCode 생성: {}", newAuthorizationCode);
            authorizationCode = newAuthorizationCode;
        } else if (authorizationCode != null && refreshToken == null) {
            log.info("authorizationCode != null && refreshToken == null");

            // Redis에서 authorizationCode로 refreshToken 조회
            String storedRefreshToken = redisCommand.getValues(authorizationCode);
            if (storedRefreshToken != null) {
                refreshToken = storedRefreshToken; // 이미 있는 refreshToken 재사용
            } else {
                // 새로운 refreshToken 생성 및 저장
                refreshToken = newRefreshToken;
                redisCommand.setValues(authorizationCode, refreshToken);
            }
        } else {
            log.info("NO IDEA");
            throw new IllegalArgumentException("Invalid request: Both authorizationCode and refreshToken are null or invalid.");
        }

        // 응답 데이터 생성
        return ResponseEntity.ok().body(TokenBody.builder()
                .tokenType("Bearer")
                .accessToken(authorizationCode)
                .refreshToken(refreshToken)
                .expiresIn(172800) // 2일 (초 단위)
                .build());
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
