package com.google.smarthome.contant;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Modes {

    private String name;
    private List<Modes.NameValues> name_values;
    private List<Modes.Settings> settings;
    private boolean ordered;

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class NameValues {
        private List<String> name_synonym;
        private String lang;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class Settings {
        private String setting_name;
        private List<SettingValues> setting_values;
    }

    @Getter @Setter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class SettingValues {
        private List<String> setting_synonym;
        private String lang;
    }

}