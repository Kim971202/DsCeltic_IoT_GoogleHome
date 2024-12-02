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

        log.info("grantType:{}", grantType);
        log.info("authorizationCode:{}", authorizationCode);
        log.info("refreshToken:{}", refreshToken);
        log.info("redirectUri:{}", redirectUri);
        log.info("authorization:{}", authorization);

        //grantType에 따라 client id, secret 값을 꺼내는 위치가 다르기 때문에 동일한 변수를 사용할 수 있도록 처리.
        if( StringUtils.hasText(authorization)) { //basic 방식
            log.info("authorization basic 방식");
            String decodeValue = new String(Base64Utils.decodeFromString(authorization));
            log.info("decodeValue:{}", decodeValue);

            String[] arrDecode = decodeValue.split(":");
            clientId = arrDecode[0];
            clientSecret = arrDecode[1];
        }
        log.info("clientId:{}", clientId);
        log.info("clientSecret:{}", clientSecret);

        if( !googleOauth2ClientId.equals(clientId) || !googleOauth2ClientSecret.equals(clientSecret) ) {
            log.info("클라이언트 정보 잘못됨 clientId:{}, clientSecret:{}", clientId, clientSecret);
            return ResponseEntity.badRequest().body(Error.builder().error("invalid_grant").build());
        }

        if( !"authorization_code".equals(grantType) && !"refresh_token".equals(grantType)) {
            log.info("grantType 코드 잘못됨:{}", grantType);
            return ResponseEntity.badRequest().body(Error.builder().error("invalid_grant").build());
        }

        System.out.println("return null");
        return null;
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
