package com.google.smarthome.utils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class AcessTokenRequester {
    private String accessToken;
    private GoogleCredential credentials;

    public void request() {
        try {
            GoogleCredential credentials = getCredentials();

            if( trueIfHasToken() ) {
                log.debug("getAccessToken: " + credentials.getAccessToken());
                log.debug("getExpiresInSeconds: " + credentials.getExpiresInSeconds());

                log.debug("before: {}", accessToken);
                accessToken = credentials.getAccessToken();
                log.debug("after: {}", accessToken);
            }
        } catch (Exception e) {
            log.debug("", e);
        }
    }

    private GoogleCredential getCredentials() throws Exception {
        if( credentials == null ) {
            credentials = GoogleCredential.fromStream(getServiceKeyValueOfTypeJson()).createScoped(getScope());
        }

        return credentials;
    }

    private InputStream getServiceKeyValueOfTypeJson() throws Exception {
        //XXX google 체크. 운영배포용 json 파일 사용하고 있는지
        return new ClassPathResource("json/dsiot-52315-35e8e30600e0.json").getInputStream();
//		return new ClassPathResource("json/test-local-front-9a220d5fda9e.json").getInputStream();
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
