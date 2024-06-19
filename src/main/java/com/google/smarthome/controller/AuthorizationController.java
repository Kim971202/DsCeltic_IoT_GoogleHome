package com.google.smarthome.controller;

import com.google.smarthome.utils.Constants;
import com.google.smarthome.utils.CookieStorage;
import com.google.smarthome.utils.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthorizationController {

    private final RestTemplate restTemplate;
    private final CookieStorage cookieStorage;

    @GetMapping("/oauth2/authorize")
    public String authorize(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(name = "response_type") String responseType,
            @RequestParam(name = "client_id") String clientId,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            Model model) throws Exception {

        log.info("@GetMapping(\"/oauth2/authorize\")");
        log.info("responseType:{} clientId:{} redirectUri:{} state:{}", responseType, clientId, redirectUri, state);

        cookieStorage.clear(request, response);
        cookieStorage.setCookie(request, "state", state, response);
        cookieStorage.setCookie(request, "redirect_uri", redirectUri, response);

        return "redirect:/home.html";
    }

    @GetMapping({"/authorization_code"})
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public String authorizationCode(HttpSession session, HttpServletRequest request, String code, Model model, HttpServletResponse response) throws Exception {

        log.info("@GetMapping({\"/authorization_code\"})\n");


        String state = cookieStorage.getCookie(request, "state");
        String redirectUri = cookieStorage.getCookie(request, "redirect_uri");
        log.info("state:{}", state);
        log.info("redirect_uri:{}", redirectUri);

        final String authorizationCode = UUID.randomUUID().toString();
        log.info("authorizationCode:{}", authorizationCode);

        String redirectUrl = redirectUri + "?code=" + authorizationCode + "&state=" + state;

        log.info("redirectUrl:{}", redirectUrl);
        return "redirect:" + redirectUrl;
    }

    /**
     * Authorization 코드 요청에 의한 엑세스 토큰 발급
     */
    @ResponseBody
    @GetMapping({"/token"})
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public String token(String code) throws Exception {

        log.info("@GetMapping({\"/token\"})");


        // 엑세스 토큰 요청
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", Constants.CLIENT_ID);
        params.add("client_secret", Constants.CLIENT_SECRET);
        params.add("grant_type", Constants.GRANT_TYPE_AUTHORIZATION_CODE);
        params.add("scope", Constants.SCOPE_READ);
        params.add("redirect_uri", "https://daesungiot.co.kr" + "/authorization_code");
        params.add("code", code);

        log.info("get App token: {}", JSON.toJson(params, true));

        return getToken(params);
    }

    /**
     * 엑세스 토큰 가져오기dvdvdvdvdv
     */
    @SuppressWarnings("unchecked")
    private String getToken(MultiValueMap<String, String> params) {

        log.info("String getToken(MultiValueMap<String, String> params)");

        try {
            Map<String,String> response = restTemplate.postForObject("https://daesungiot.co.kr" + "/oauth/token", params, Map.class);
            log.info("app /oauth/token response: {}", JSON.toJson(response, true));
            String accessToken = response.get("access_token");
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @GetMapping("/oauth2/callback")
    public String oauth2Callback(@RequestParam("code") String code, @RequestParam("state") String state) {
        String url = "https://oauth2.googleapis.com/token";

        log.info("@GetMapping(\"/oauth2/callback\") CALLED");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", "505891126739-5nst99tq7ib748ovv80s6tdd5c0epcp3.apps.googleusercontent.com");
        body.add("client_secret", "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");
        body.add("redirect_uri", "https://oauth-redirect.googleusercontent.com/r/dsiot-52315");
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            // 액세스 토큰 처리
            return "redirect:/success";
        } else {
            // 오류 처리
            return "redirect:/error";
        }
    }

}
