package com.google.smarthome.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.smarthome.contant.MobiusResponse;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.dto.QueryResult;
import com.google.smarthome.dto.ReportStatusResult;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.utils.AcessTokenRequester;
import com.google.smarthome.utils.JSON;
import com.google.smarthome.utils.WebClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Slf4j
@Service
public class FulfillmentService {

    @Autowired
    private GoogleMapper googleMapper;
    @Autowired
    private MobiusService mobiusService;
    GoogleDTO deviceStatus;
    private final AcessTokenRequester accessTokenRequester;

    public FulfillmentService(GoogleMapper googleMapper, MobiusService mobiusService,
            AcessTokenRequester accessTokenRequester) {
        this.googleMapper = googleMapper;
        this.mobiusService = mobiusService;
        this.accessTokenRequester = accessTokenRequester;
    }

    public JSONObject handleSync(JSONObject requestBody, Map<String, String> deviceInfoMap, String userId) {
        log.info("handleSync CALLED");
        log.info("requestBody: " + requestBody);

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        payload.put("agentUserId", userId);

        JSONArray devices = new JSONArray();

        for (Map.Entry<String, String> entry : deviceInfoMap.entrySet()) {
            String deviceId = entry.getKey(); // deviceId
            String modelCode = entry.getValue(); // modelCode

            log.info("Processing device with ID: " + deviceId + " and Model Code: " + modelCode);

            if (deviceId == null) {
                log.error("Null device ID found");
                continue;
            }

            JSONObject device = new JSONObject();
            JSONObject attributes = new JSONObject();
            JSONArray availableModes = new JSONArray();
            String deviceType = "";
            String[][] settings = getBoilerSettings(modelCode);
            availableModes.put(createMode(settings));

            // modelCode에 따라 보일러와 환기 기기를 구분
            if (modelCode.equals("ESCeco13S") || modelCode.equals("DCR-91/WF") || modelCode.contains("MC2600")) {
                deviceType = "action.devices.types.BOILER";
                attributes
                        .put("temperatureStepCelsius", 1)
                        .put("temperatureUnitForUX", "C")
                        .put("temperatureRange", new JSONObject()
                                .put("minThresholdCelsius", 5)
                                .put("maxThresholdCelsius", 80))
                        .put("availableModes", availableModes);

                device.put("traits", new JSONArray()
                        .put("action.devices.traits.OnOff")
                        .put("action.devices.traits.TemperatureControl")
                        .put("action.devices.traits.Modes"))
                        // willReportState 속성을 추가
                        .put("willReportState", true);
            }
            // else if (modelCode.equals("DCR-47/WF")) {
            // deviceType = "action.devices.types.FAN";
            //
            // // 환기 기기에 대한 settings 정의
            // String[][] settings = getVentSettings(modelCode);
            //
            // JSONObject modeFan = createModeFan(settings);
            // availableModes.put(modeFan);
            //
            // attributes.put("availableModes", availableModes);
            //
            // device.put("traits", new JSONArray()
            // .put("action.devices.traits.OnOff")
            // .put("action.devices.traits.Modes")); // FanSpeed와 Modes 추가
            // }

            // 공통으로 device에 추가할 값들
            device.put("id", deviceId);
            device.put("type", deviceType);
            device.put("attributes", attributes);

            // GoogleDTO에서 기기 닉네임 가져오기
            GoogleDTO params = new GoogleDTO();
            params.setUserId(userId);
            params.setDeviceId(deviceId);
            GoogleDTO deviceNick = googleMapper.getNicknameByDeviceId(params);

            // device.put("name", new JSONObject().put("name", "대성" + "-" +
            // deviceNick.getDeviceNickname()));
            device.put("name", new JSONObject().put("name", "대성보일러" + "-" + deviceNick.getDeviceNickname()));

            // GoogleDTO에서 기기 상태 정보 가져오기
            GoogleDTO deviceStatus = googleMapper.getInfoByDeviceId(deviceId);
            if (deviceStatus != null) {
                JSONObject customData = new JSONObject();
                boolean online = googleMapper.getOnlineStatus().getOnline().equals("true");
                customData.put("powerStatus", "on".equals(deviceStatus.getPowrStatus()));
                customData.put("online", online);
                customData.put("ambientTemperature", Double.parseDouble(String.format("%.1f", Double.parseDouble(deviceStatus.getCurrentTemp()))));
                customData.put("setpointTemperature", Double.parseDouble(String.format("%.1f", Double.parseDouble(deviceStatus.getTempStatus()))));
                customData.put("currentMode", deviceStatus.getModeValue());
                device.put("customData", customData);
            } else {
                log.warn("Device status not found for deviceId: " + deviceId);
            }

            devices.put(device);
        }

        payload.put("devices", devices);
        response.put("payload", payload);

        log.info("handleSync response: " + response);
        return response;
    }

    private JSONObject createMode(String[][] settings) {

        JSONObject mode = new JSONObject();
        mode.put("name", "mode_boiler");
        JSONArray nameValues = new JSONArray()
                .put(new JSONObject().put("name_synonym", new JSONArray().put("보일러")).put("lang", "ko"))
                .put(new JSONObject().put("name_synonym", new JSONArray().put("boiler")).put("lang", "en"));
        mode.put("name_values", nameValues);

        JSONArray settingsArray = new JSONArray();
        for (String[] setting : settings) {
            JSONObject settingObject = new JSONObject();
            settingObject.put("setting_name", setting[0]);
            settingObject.put("setting_values", new JSONArray()
                    .put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[1])).put("lang", "ko"))
                    .put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[2])).put("lang", "en")));
            settingsArray.put(settingObject);
        }
        mode.put("settings", settingsArray);
        mode.put("ordered", false);
        return mode;
    }

    // 보일러 모드 설정
    private JSONObject createModeBoiler(String[][] settings) {
        JSONObject modeBoiler = new JSONObject();
        modeBoiler.put("name", "mode_boiler");

        JSONArray nameValuesArray = new JSONArray();
        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("보일러")).put("lang", "ko"));
        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("boiler")).put("lang", "en"));
        modeBoiler.put("name_values", nameValuesArray);

        JSONArray settingsArray = new JSONArray();

        // settings 배열에 맞는 모드 설정 추가
        for (String[] setting : settings) {
            JSONObject settingObject = new JSONObject();
            settingObject.put("setting_name", setting[0]);

            JSONArray settingValues = new JSONArray();
            settingValues
                    .put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[1])).put("lang", "ko"));
            settingValues
                    .put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[2])).put("lang", "en"));

            settingObject.put("setting_values", settingValues);
            settingsArray.put(settingObject);
        }

        modeBoiler.put("settings", settingsArray);
        modeBoiler.put("ordered", false); // ordered 필드 추가
        return modeBoiler;
    }

    // 환기 모드 설정
    private JSONObject createModeFan(String[][] settings) {
        JSONObject modeFan = new JSONObject();
        modeFan.put("name", "mode_fan");

        JSONArray nameValuesArray = new JSONArray();
        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("환기")).put("lang", "ko"));
        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("fan")).put("lang", "en"));
        modeFan.put("name_values", nameValuesArray);

        JSONArray settingsArray = new JSONArray();

        // settings 배열에 맞는 모드 설정 추가
        for (String[] setting : settings) {
            JSONObject settingObject = new JSONObject();
            settingObject.put("setting_name", setting[0]);

            JSONArray settingValues = new JSONArray();
            settingValues
                    .put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[1])).put("lang", "ko"));
            settingValues
                    .put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[2])).put("lang", "en"));

            settingObject.put("setting_values", settingValues);
            settingsArray.put(settingObject);
        }

        modeFan.put("settings", settingsArray);
        modeFan.put("ordered", false);
        return modeFan;
    }

    public JSONObject handleExecute(JSONObject requestBody, List<String> deviceIds, String userId) {
        log.info("handleExecute CALLED");
        log.info("requestBody: " + requestBody);

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));
        JSONArray commandsArray = new JSONArray();

        JSONArray inputs = requestBody.getJSONArray("inputs");
        inputs.forEach(inputObj -> {
            JSONObject input = (JSONObject) inputObj;
            JSONArray commands = input.getJSONObject("payload").getJSONArray("commands");

            commands.forEach(commandObj -> {
                JSONObject command = (JSONObject) commandObj;
                JSONArray devices = command.getJSONArray("devices");

                devices.forEach(deviceObj -> {
                    JSONObject device = (JSONObject) deviceObj;
                    String deviceId = device.getString("id");
                    JSONArray executions = command.getJSONArray("execution");

                    executions.forEach(execObj -> {
                        JSONObject execCommand = (JSONObject) execObj;
                        String commandName = execCommand.getString("command");
                        log.info("commandName: " + commandName);

                        boolean isSuccess = true;
                        String errorString = "";
                        JSONObject states = new JSONObject();

                        try {
                            switch (commandName) {
                                case "action.devices.commands.OnOff":
                                    boolean on = execCommand.getJSONObject("params").getBoolean("on");
                                    handleOnOffCommand(deviceId, on);
                                    updateDeviceState(userId, deviceId, "on", on);
                                    states.put("on", on);
                                    break;

                                case "action.devices.commands.SetTemperature":
                                    double setTemp = execCommand.getJSONObject("params").getDouble("temperature");
                                    handleTemperatureSetpoint(deviceId, setTemp);
                                    boolean online;
                                    online = googleMapper.getOnlineStatus().getOnline().equals("true");
                                    states.put("online", online);
                                    states.put("thermostatTemperatureSetpoint", setTemp);
                                    break;

                                case "action.devices.commands.TemperatureRelative":
                                    System.out.println("action.devices.commands.TemperatureRelative");
                                    double temp = execCommand.getJSONObject("params").getDouble("TemperatureRelative");
                                    System.out.println("temp: " + temp);
                                    break;

                                case "action.devices.commands.ThermostatTemperatureSetpoint":
                                    double temperature = execCommand.getJSONObject("params")
                                            .getDouble("thermostatTemperatureSetpoint");
                                    handleTemperatureSetpoint(deviceId, temperature);
                                    updateDeviceState(userId, deviceId, "temperatureSetpointCelsius", temperature);
                                    states.put("temperatureSetpointCelsius", temperature);
                                    break;

                                case "action.devices.commands.ThermostatSetMode":
                                    String mode = execCommand.getJSONObject("params").getString("thermostatMode");
                                    log.info("Setting mode of device " + deviceId + " to " + mode);
                                    if (mode.equals("off"))
                                        deviceStatus.setPowrStatus("of");
                                    else if (mode.equals("heat"))
                                        deviceStatus.setPowrStatus("on");
                                    googleMapper.updateDeviceStatus(deviceStatus);
                                    handleDevice(userId, deviceId, mode, "powr");
                                    break;
                                case "action.devices.commands.SetModes":
                                    JSONObject params = execCommand.getJSONObject("params")
                                            .getJSONObject("updateModeSettings");
                                    for (String modeName : params.keySet()) {
                                        String modeValue = params.getString(modeName);
                                        log.info(
                                                "설정 mode of device " + deviceId + " to " + modeName + ": " + modeValue);
                                        switch (modeValue) {
                                            case "061":
                                                deviceStatus.setModeValue("06");
                                                deviceStatus.setSleepCode("01");
                                                break;
                                            case "062":
                                                deviceStatus.setModeValue("06");
                                                deviceStatus.setSleepCode("02");
                                                break;
                                            case "063":
                                                deviceStatus.setModeValue("06");
                                                deviceStatus.setSleepCode("03");
                                                break;
                                        }

                                        handleSetModes(userId, deviceId, modeName, modeValue);
                                        updateDeviceState(userId, deviceId, "currentModeSettings", params.toMap());
                                        googleMapper.updateDeviceStatus(deviceStatus);
                                    }
                                    break;
                                default:
                                    isSuccess = false;
                                    errorString = "Unsupported command: " + commandName;
                                    log.error(errorString);
                            }
                        } catch (Exception e) {
                            isSuccess = false;
                            errorString = e.getMessage();
                            log.error("Error executing command", e);
                        }

                        JSONObject commandResult = new JSONObject();
                        commandResult.put("ids", new JSONArray().put(deviceId));
                        commandResult.put("status", isSuccess ? "SUCCESS" : "ERROR");
                        commandResult.put("states", states);
                        if (!isSuccess) {
                            commandResult.put("errorCode", errorString);
                        }
                        commandsArray.put(commandResult);
                    });
                });
            });
        });

        JSONObject payload = new JSONObject();
        payload.put("commands", commandsArray);
        response.put("payload", payload);

        log.info("handleExecute response: " + response);
        return response;
    }

    // 전원 제어 명령
    private void handleOnOffCommand(String deviceId, boolean on) throws Exception {
        log.info("Setting device " + deviceId + " power to " + (on ? "ON" : "OFF"));
        deviceStatus.setDeviceId(deviceId);
        deviceStatus.setPowrStatus(on ? "on" : "of");
        googleMapper.updateDeviceStatus(deviceStatus);
        handleDevice(deviceStatus.getUserId(), deviceId, on ? "on" : "of", "powr");
    }

    // 온도 설정 명령
    private void handleTemperatureSetpoint(String deviceId, double temperature) throws Exception {
        log.info("Setting temperature for device " + deviceId + " to " + temperature);
        // 기기 별로 적용 최소/최대 온도 다름
        /*
        * DR-910W(난방수온\온돌난방):  40℃~80℃
        * DCR-91WF(난방수온\온돌난방): 50℃~80℃
        * MC2600시리즈(실내난방):      5℃~40℃
        * */
        String functionId = "wtTp";

        if(deviceId.contains("2045534365636f313353")){          // ESCeco13S
            if (temperature < 40 || temperature > 80) {
                throw new IllegalArgumentException("Temperature out of range");
            }
        } else if(deviceId.contains("204443522d39312f5746")){   // DCR-91/WF
            if (temperature < 10 || temperature > 80) {
                throw new IllegalArgumentException("Temperature out of range");
            }
        } else if(deviceId.contains("4d4332363030"))    {
            // MC2600
            if (temperature < 10 || temperature > 80) {
                throw new IllegalArgumentException("Temperature out of range");
            }
            functionId = "htTp";
        }

        deviceStatus.setDeviceId(deviceId);
        deviceStatus.setTempStatus(String.valueOf(temperature));
        googleMapper.updateDeviceStatus(deviceStatus);
        handleDevice(deviceStatus.getUserId(), deviceId, String.valueOf(temperature), functionId); //wtTp OR htTp
    }

    // 모드 설정 명령
    private void handleSetModes(String deviceId, JSONObject modeSettings) throws Exception {
        for (String mode : modeSettings.keySet()) {
            String value = modeSettings.getString(mode);
            log.info("Setting mode for device " + deviceId + ": " + mode + " = " + value);
            deviceStatus.setModeValue(value);
            googleMapper.updateDeviceStatus(deviceStatus);
            handleDeviceWithMode(deviceStatus.getUserId(), deviceId, value, "opMd");
        }
    }

    public JSONObject handleQuery(JSONObject requestBody, List<String> deviceIds) {
        log.info("handleQuery CALLED");
        log.info("requestBody: " + requestBody);

        // Create the response and add requestId first
        JSONObject response = new JSONObject();
        String requestId = requestBody.getString("requestId");
        response.put("requestId", requestId);

        // Initialize payload
        JSONObject payload = new JSONObject();
        JSONObject devices = new JSONObject();

        for (String deviceId : deviceIds) {
            deviceStatus = googleMapper.getInfoByDeviceId(deviceId);

            JSONObject deviceState = new JSONObject();
            Map<String, Object> currentModeSettings = new HashMap<>();
            currentModeSettings.put("mode_boiler", deviceStatus.getModeValue());
            // currentModeSettings.put("mode_boiler", "외출모드");

            boolean deviceOnOff = deviceStatus.getPowrStatus().equals("on");
            boolean online = googleMapper.getOnlineStatus().getOnline().equals("true");

            // Populate deviceState
            deviceState.put("on", deviceOnOff); // The device is ON
            deviceState.put("online", online);
            deviceState.put("onlineStatusDetails", "OK");
            deviceState.put("currentModeSettings", currentModeSettings);
            deviceState.put("temperatureAmbientCelsius",
                    Double.parseDouble(String.format("%.1f", Double.parseDouble(deviceStatus.getCurrentTemp()))));
            deviceState.put("temperatureSetpointCelsius",
                    Double.parseDouble(String.format("%.1f", Double.parseDouble(deviceStatus.getTempStatus()))));
            deviceState.put("status", "SUCCESS");

            devices.put(deviceId, deviceState);
        }

        // Add devices to payload and payload to response
        payload.put("devices", devices);
        response.put("payload", payload);

        log.info("handleQuery response: " + response);

        return response;
    }

    // 보일러 설정에 따라 settings 배열 생성
    private String[][] getBoilerSettings(String modelCode) {
        if (modelCode.equals("ESCeco13S")) {
            return new String[][] {
                    { "01", "실내난방", "Heating_Indoor_Temperature" },
                    { "02", "온돌난방", "Heating_Water_Temperature" },
                    { "03", "외출", "Away" },
                    { "05", "절약난방", "Economy_Heating" },
                    { "07", "온수전용모드", "Hot_Water_Only" }
            };
        } else {
            return new String[][] {
                    { "01", "실내난방", "Heating_Indoor_Temperature" },
                    { "03", "외출모드", "Away" }, // 외출/온수전용 같음
            };
        }
    }

    // 환기 설정에 따라 settings 배열 생성
    private String[][] getVentSettings(String modelCode) {
        if (modelCode.equals("DCR-47/WF")) {
            return new String[][] {
                    { "00", "Auto", "Ventilation_Auto" },
                    { "01", "환기-1단", "Ventilation_Level_1" },
                    { "02", "환기-2단", "Ventilation_Level_2" },
                    { "03", "환기-3단", "Ventilation_Level_3" }
            };
        } else {
            return new String[][] {
                    { "01", "환기-중속", "Ventilation_Medium" },
                    { "02", "환기-터보", "Ventilation_Turbo" }
            };
        }
    }

    // 모드를 설정하는 새로운 메서드
    private void handleSetModes(String userId, String deviceId, String modeName, String modeValue) {
        log.info("Setting mode for device " + deviceId + ": " + modeName + " = " + modeValue);
        try {
            handleDeviceWithMode(userId, deviceId, modeValue, "opMd");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String handleDevice(String userId, String deviceId, String value, String functionId) throws Exception {
        MobiusResponse mobiusResponse;
        ConcurrentHashMap<String, String> conMap = new ConcurrentHashMap<>();

        conMap.put("userId", userId);
        conMap.put("deviceId", deviceId);
        conMap.put("value", value);
        conMap.put("functionId", functionId);
        System.out.println(JSON.toJson(conMap));
        mobiusResponse = mobiusService.createCin("googleAE", "googleCNT", JSON.toJson(conMap));

        return mobiusResponse.getResponseCode();
    }

    private String handleDeviceWithMode(String userId, String deviceId, String value, String functionId)
            throws Exception {
        // 모드를 설정하는 로직을 여기에 구현합니다.
        MobiusResponse mobiusResponse;
        ConcurrentHashMap<String, String> conMap = new ConcurrentHashMap<>();

        conMap.put("userId", userId);
        conMap.put("deviceId", deviceId);
        conMap.put("value", value);
        conMap.put("functionId", functionId);
        System.out.println(JSON.toJson(conMap));
        mobiusResponse = mobiusService.createCin("googleAE", "googleCNT", JSON.toJson(conMap));

        return mobiusResponse.getResponseCode();
    }

    public void sendDataBasedOnQueryResult(String agentUserId, QueryResult.Response queryResponse)
            throws JsonProcessingException {
        log.info("queryResponse");

        final String requestId = queryResponse.getRequestId();
        Map<String, Object> states = new HashMap<>();

        Map<String, Map<String, Object>> devices = queryResponse.getPayload().getDevices();

        Iterator<String> iter = devices.keySet().iterator();
        while (iter.hasNext()) {
            String deviceId = iter.next();
            Map<String, Object> stateValues = new HashMap<>();

            stateValues.putAll(devices.get(deviceId));

            // `currentModeSettings` 값 처리
            if (stateValues.containsKey("currentModeSettings")) {
                Object currentModeSettings = stateValues.get("currentModeSettings");
                try {
                    if (currentModeSettings instanceof JSONObject) {
                        // JSONObject를 Map으로 변환
                        Map<String, Object> parsedSettings = new ObjectMapper()
                                .readValue(currentModeSettings.toString(), Map.class);
                        stateValues.put("currentModeSettings", parsedSettings);
                    } else if (currentModeSettings instanceof String) {
                        // 문자열 JSON 처리
                        Map<String, Object> parsedSettings = new ObjectMapper().readValue((String) currentModeSettings,
                                Map.class);
                        stateValues.put("currentModeSettings", parsedSettings);
                    } else if (!(currentModeSettings instanceof Map)) {
                        log.warn("Unexpected currentModeSettings type: {}", currentModeSettings.getClass());
                        stateValues.put("currentModeSettings", Map.of());
                    }
                } catch (Exception e) {
                    log.error("Failed to parse currentModeSettings for deviceId {}: {}", deviceId, currentModeSettings,
                            e);
                    stateValues.put("currentModeSettings", Map.of());
                }
            }

            // 불필요한 필드 제거
            stateValues.remove("status");
            stateValues.remove("updateModeSettings");
            stateValues.remove("fanSpeed");
            stateValues.remove("temperature");

            states.put(deviceId, stateValues);
        }

        System.out.println(states);
        System.out.println(JSON.toJson(states));

        ReportStatusResult.Request reportStatusResult = ReportStatusResult.Request.builder()
                .requestId(requestId)
                .agentUserId(agentUserId)
//                .agentUserId("AllUserName")
                .payload(ReportStatusResult.Request.Payload.builder()
                        .devices(ReportStatusResult.Request.Payload.Device.builder()
                                .states(states)
                                .build())
                        .build())
                .build();
                
        AcessTokenRequester tokenRequester = new AcessTokenRequester();
        tokenRequester.request();
        String token = tokenRequester.getToken();
        log.info("sendDataBasedOnQueryResult Token: " + token);

        String baseUrl = "https://homegraph.googleapis.com";
        String uri = baseUrl + "/v1/devices:reportStateAndNotification";
        log.info("baseUrl:{}", uri);

        // WebClient를 통한 Google Home Graph API 요청
        WebClientUtils.getSslClient(baseUrl, MediaType.APPLICATION_JSON_VALUE, HttpMethod.POST, token)
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(JSON.toJson(reportStatusResult))
                .retrieve()
                .toEntity(String.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(WebClientRequestException.class, new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE))
                .onErrorReturn(TimeoutException.class, new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT))
                .onErrorReturn(WebClientResponseException.class, new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR))
                .subscribe(new Consumer<ResponseEntity<String>>() {
                    @Override
                    public void accept(ResponseEntity<String> response) {
                        log.info("send ReportStatusResult request: : {}", JSON.toJson(reportStatusResult, true));
                        log.info("send ReportStatusResult status code: {}", response.getStatusCode());
                        log.info("send ReportStatusResult response getBody: {}", response.getBody());
                    }
                });
    }

    public void updateDeviceState(String userId, String deviceId, String attribute, Object value) {
        log.info("Updating device state for UserId: {}, DeviceId: {}, Attribute: {}, Value: {}", userId, deviceId,
                attribute, value);

        try {
            // 기기의 상태를 갱신
            deviceStatus = googleMapper.getInfoByDeviceId(deviceId);

            switch (attribute) {
                case "on":
                    deviceStatus.setPowrStatus((Boolean) value ? "on" : "of");
                    break;
                case "temperatureSetpointCelsius":
                    deviceStatus.setTempStatus(String.valueOf(value));
                    break;
                case "currentModeSettings":
                    if (value instanceof Map) {
                        Map<String, String> modeSettings = (Map<String, String>) value;
                        String modeValue = modeSettings.get("mode_boiler");
                        deviceStatus.setModeValue(modeValue);
                    }
                    break;
                default:
                    log.warn("Unsupported attribute: {}", attribute);
                    return;
            }

            // 데이터베이스 업데이트
            googleMapper.updateDeviceStatus(deviceStatus);

            // Google Home Graph API를 통해 상태 동기화
            QueryResult.Response queryResponse = new QueryResult.Response();
            queryResponse.setRequestId(UUID.randomUUID().toString()); // 고유 요청 ID 생성

            QueryResult.Response.Payload payload = new QueryResult.Response.Payload();
            Map<String, Map<String, Object>> devicesMap = new HashMap<>();
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put(attribute, value);
            devicesMap.put(deviceId, stateMap);

            payload.setDevices(devicesMap);
            queryResponse.setPayload(payload);

            sendDataBasedOnQueryResult(userId, queryResponse);

        } catch (Exception e) {
            log.error("Error updating device state", e);
        }
    }

}