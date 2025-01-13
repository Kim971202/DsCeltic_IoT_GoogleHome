package com.google.smarthome;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

public class AuthTest {
    public static void main(String[] args) {
        // AcessTokenRequester 인스턴스 생성 및 호출
        AcessTokenRequester tokenRequester = new AcessTokenRequester();
        tokenRequester.request(); // 토큰 요청
        String token = tokenRequester.getToken(); // 토큰 가져오기
        System.out.println("Access Token: " + token); // 토큰 출력
    }
}

class AcessTokenRequester {
    private String accessToken;
    private GoogleCredential credentials;

    public void request() {
        try {
            GoogleCredential credentials = getCredentials();

            if (trueIfHasToken()) {
                System.out.println("getAccessToken: " + credentials.getAccessToken());
                System.out.println("getExpiresInSeconds: " + credentials.getExpiresInSeconds());

                System.out.println("before: " + accessToken);
                accessToken = credentials.getAccessToken();
                System.out.println("after: " + accessToken);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private GoogleCredential getCredentials() throws Exception {
        if (credentials == null) {
            credentials = GoogleCredential.fromStream(getServiceKeyValueOfTypeJson()).createScoped(getScope());
        }
        return credentials;
    }

    private InputStream getServiceKeyValueOfTypeJson() throws Exception {
        // JSON 파일 경로 설정
        return new ClassPathResource("json/dsiot-52315-d59d382966ee.json").getInputStream();
    }

    private List<String> getScope() {
        return List.of("https://www.googleapis.com/auth/homegraph");
    }

    private boolean trueIfHasToken() throws Exception {
        return credentials.refreshToken();
    }

    public String getToken() {
        return accessToken;
    }
}