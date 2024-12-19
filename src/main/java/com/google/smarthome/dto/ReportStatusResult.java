package com.google.smarthome.dto;

import lombok.*;

import java.util.Map;

@NoArgsConstructor
public class ReportStatusResult {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private String requestId;
        private String agentUserId;
        private Payload payload;

        @Getter @Setter @Builder
        @NoArgsConstructor @AllArgsConstructor
        public static class Payload {
            private Device devices;

            @Getter @Setter @Builder
            @NoArgsConstructor @AllArgsConstructor
            public static class Device {
                private Map<String, Object> states;
            }
        }
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String requestId;
    }
}