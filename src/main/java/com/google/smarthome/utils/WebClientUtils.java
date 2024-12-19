package com.google.smarthome.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.UriSpec;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class WebClientUtils {

    public static UriSpec<RequestBodySpec> getSslClient(String baseUrl, String contentType, HttpMethod method) {
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

            UriSpec<RequestBodySpec> uriSpec = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, contentType)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build()
                    .method(method);

            return uriSpec;
        } catch(Exception e) {
            log.error("", e);
        }

        return null;
    }

    public static UriSpec<RequestBodySpec> getSslClient(String baseUrl, String contentType, HttpMethod method, String token) {

        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(t -> t.sslContext(sslContext))
                    //.wiretap("getSslClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
                    ;

            UriSpec<RequestBodySpec> uriSpec = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, contentType)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build()
                    .method(method);

            return uriSpec;
        } catch(Exception e) {
            log.error("", e);
        }

        return null;
    }
}