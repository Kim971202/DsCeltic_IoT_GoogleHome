package com.google.smarthome;

import java.util.List;
import java.util.Map;

public class SyncResponseBuilder {

    public static Map<String, Object> buildSyncResponse(String agentUserId) {
        return Map.of(
            "requestId", "unique-request-id", // 요청 ID
            "payload", Map.of(
                "agentUserId", agentUserId,
                "devices", List.of(
                    Map.of(
                        "id", "0.2.481.1.1.204443522d39312f5746.20202020343431613834613134643730",
                        "type", "action.devices.types.BOILER",
                        "traits", List.of(
                            "action.devices.traits.OnOff",
                            "action.devices.traits.TemperatureControl",
                            "action.devices.traits.Modes"
                        ),
                        "name", Map.of(
                            "name", "사용자 정의 이름-DCR-91/WF"
                        ),
                        "attributes", Map.of(
                            "temperatureRange", Map.of(
                                "maxThresholdCelsius", 80,
                                "minThresholdCelsius", 10
                            ),
                            "temperatureUnitForUX", "C",
                            "temperatureStepCelsius", 1
                        )
                    ),
                    Map.of(
                        "id", "0.2.481.1.1.2045534365636f313353.20202020303833413844434146353435",
                        "type", "action.devices.types.BOILER",
                        "traits", List.of(
                            "action.devices.traits.OnOff",
                            "action.devices.traits.TemperatureControl",
                            "action.devices.traits.Modes"
                        ),
                        "name", Map.of(
                            "name", "사용자 정의 이름-DR-910W"
                        ),
                        "attributes", Map.of(
                            "temperatureRange", Map.of(
                                "maxThresholdCelsius", 80,
                                "minThresholdCelsius", 10
                            ),
                            "temperatureUnitForUX", "C",
                            "temperatureStepCelsius", 1
                        )
                    )
                )
            )
        );
    }

    public static void main(String[] args) {
        String agentUserId = "yohan2025";
        Map<String, Object> syncResponse = buildSyncResponse(agentUserId);

        // JSON 변환 및 출력 (테스트용)
        System.out.println(syncResponse);
    }
}