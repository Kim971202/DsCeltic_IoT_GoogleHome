package com.google.smarthome.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FanSpeed {

    private List<Speed> speeds;
    private boolean ordered;

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Speed {
        private String speed_name;
        private List<SpeedValue> speed_values;

        @Getter @Setter @Builder
        @NoArgsConstructor @AllArgsConstructor
        public static class SpeedValue {
            private List<String> speed_synonym;
            private String lang;
        }
    }
}
