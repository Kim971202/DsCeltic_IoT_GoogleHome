package com.google.smarthome.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class QueryResult {
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Request {
        private String requestId; //필수

        private List<Input> inputs;
        @Getter @Setter @Builder
        @NoArgsConstructor @AllArgsConstructor
        public static class Input {
            private String intent;

            private Payload payload;
            @Getter @Setter @Builder
            @NoArgsConstructor @AllArgsConstructor
            public static class Payload {
                private List<Device> devices;
                @Getter @Setter @Builder
                @NoArgsConstructor @AllArgsConstructor
                public static class Device {
                    private String id;
                    private Object customData;
                }
            }
        }
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private String requestId; //필수

        private Payload payload; //필수
        @Getter @Setter @Builder
        @NoArgsConstructor @AllArgsConstructor
        public static class Payload {
            private Map<String, Map<String, Object>> devices; //필수
        }
    }
}
