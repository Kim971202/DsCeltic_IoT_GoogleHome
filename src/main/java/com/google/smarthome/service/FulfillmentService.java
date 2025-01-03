package com.google.smarthome.service;

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
import java.util.List;
import java.util.Map;
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

    public FulfillmentService(GoogleMapper googleMapper, MobiusService mobiusService, AcessTokenRequester accessTokenRequester) {
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
            String deviceId = entry.getKey();  // deviceId
            String modelCode = entry.getValue();  // modelCode

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
            if (modelCode.equals("ESCeco13S") || modelCode.equals("DCR-91/WF")) {
                deviceType = "action.devices.types.BOILER";
                attributes
                        .put("temperatureStepCelsius", 1)
                        .put("temperatureUnitForUX", "C")
                        .put("temperatureRange", new JSONObject()
                                .put("minThresholdCelsius", 10)
                                .put("maxThresholdCelsius", 80))
                        .put("availableModes", availableModes);

                device.put("traits", new JSONArray()
                        .put("action.devices.traits.OnOff")
                        .put("action.devices.traits.TemperatureControl")
                        .put("action.devices.traits.Modes"));
            }
//            else if (modelCode.equals("DCR-47/WF")) {
//                deviceType = "action.devices.types.FAN";
//
//                // 환기 기기에 대한 settings 정의
//                String[][] settings = getVentSettings(modelCode);
//
//                JSONObject modeFan = createModeFan(settings);
//                availableModes.put(modeFan);
//
//                attributes.put("availableModes", availableModes);
//
//                device.put("traits", new JSONArray()
//                        .put("action.devices.traits.OnOff")
//                        .put("action.devices.traits.Modes"));  // FanSpeed와 Modes 추가
//            }

            // 공통으로 device에 추가할 값들
            device.put("id", deviceId);
            device.put("type", deviceType);
            device.put("attributes", attributes);

            // GoogleDTO에서 기기 닉네임 가져오기
            GoogleDTO params = new GoogleDTO();
            params.setUserId(userId);
            params.setDeviceId(deviceId);
            GoogleDTO deviceNick = googleMapper.getNicknameByDeviceId(params);

//            device.put("name", new JSONObject().put("name", "대성" + "-" + deviceNick.getDeviceNickname()));
            device.put("name", new JSONObject().put("name", "대성보일러"));
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
            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[1])).put("lang", "ko"));
            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[2])).put("lang", "en"));

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
            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[1])).put("lang", "ko"));
            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[2])).put("lang", "en"));

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
                                    states.put("on", on);
                                    break;

                                case "action.devices.commands.SetTemperature":
                                    double setTemp = execCommand.getJSONObject("params").getDouble("temperature");
                                    handleTemperatureSetpoint(deviceId, setTemp);
                                    states.put("online", true);
                                    states.put("thermostatTemperatureSetpoint", setTemp);
                                    break;

                                case "action.devices.commands.TemperatureRelative":
                                    System.out.println("action.devices.commands.TemperatureRelative");
                                    double temp = execCommand.getJSONObject("params").getDouble("TemperatureRelative");
                                    System.out.println("temp: " + temp);
                                    break;

                                case "action.devices.commands.ThermostatTemperatureSetpoint":
                                    double temperature = execCommand.getJSONObject("params").getDouble("thermostatTemperatureSetpoint");
                                    handleTemperatureSetpoint(deviceId, temperature);
                                    states.put("temperatureSetpointCelsius", temperature);
                                    break;

                                case "action.devices.commands.ThermostatSetMode":
                                    String mode = execCommand.getJSONObject("params").getString("thermostatMode");
                                    log.info("Setting mode of device " + deviceId + " to " + mode);
                                    if (mode.equals("off")) deviceStatus.setPowrStatus("of");
                                    else if (mode.equals("heat")) deviceStatus.setPowrStatus("on");
                                    googleMapper.updateDeviceStatus(deviceStatus);
                                    handleDevice(userId, deviceId, mode, "powr");
                                    break;
                                case "action.devices.commands.SetModes":
                                    JSONObject params = execCommand.getJSONObject("params").getJSONObject("updateModeSettings");
                                    for (String modeName : params.keySet()) {
                                        String modeValue = params.getString(modeName);
                                        log.info("설정 mode of device " + deviceId + " to " + modeName + ": " + modeValue);
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
        if (temperature < 10 || temperature > 80) {
            throw new IllegalArgumentException("Temperature out of range");
        }
        deviceStatus.setDeviceId(deviceId);
        deviceStatus.setTempStatus(String.valueOf(temperature));
        googleMapper.updateDeviceStatus(deviceStatus);
        handleDevice(deviceStatus.getUserId(), deviceId, String.valueOf(temperature), "htTp");
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

            // Populate deviceState
            deviceState.put("on", deviceOnOff); // The device is ON
            deviceState.put("online", true);
            deviceState.put("currentModeSettings", currentModeSettings);
            deviceState.put("temperatureAmbientCelsius", Double.parseDouble(deviceStatus.getCurrentTemp()));
            deviceState.put("temperatureSetpointCelsius", Double.parseDouble(deviceStatus.getTempStatus()));
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
            return new String[][]  {
                    {"01", "실내난방", "Heating_Indoor_Temperature"},
                    {"02", "온돌난방", "Heating_Water_Temperature"},
                    {"03", "외출", "Away"},
                    {"05", "절약난방", "Economy_Heating"},
                    {"061", "취침", "Sleep1"},
                    {"07", "온수전용모드", "Hot_Water_Only"}
            };
        } else {
            return new String[][]  {
                    {"01", "실내난방", "Heating_Indoor_Temperature"},
                    {"03", "외출모드", "Away"}, // 외출/온수전용 같음
                    {"08", "빠른온수모드", "FAST_WATER"}
            };
        }
    }

    // 환기 설정에 따라 settings 배열 생성
    private String[][] getVentSettings(String modelCode) {
        if (modelCode.equals("DCR-47/WF")) {
            return new String[][] {
                    {"00", "Auto", "Ventilation_Auto"},
                    {"01", "환기-1단", "Ventilation_Level_1"},
                    {"02", "환기-2단", "Ventilation_Level_2"},
                    {"03", "환기-3단", "Ventilation_Level_3"}
            };
        } else {
            return new String[][] {
                    {"01", "환기-중속", "Ventilation_Medium"},
                    {"02", "환기-터보", "Ventilation_Turbo"}
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

    private String handleDeviceWithMode(String userId, String deviceId, String value, String functionId) throws Exception {
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

    public void sendDataBasedOnQueryResult(String agentUserId, QueryResult.Response queryResponse) {
        log.info("queryResponse");
        System.out.println(queryResponse);

        final String requestId = queryResponse.getRequestId();
        Map<String, Object> states = new HashMap<>();

        Map<String, Map<String, Object>> devices = queryResponse.getPayload().getDevices();

        for (String deviceId : devices.keySet()) {
            Map<String, Object> stateValues = new HashMap<>();

            stateValues.putAll(devices.get(deviceId));

            // currentModeSettings 값 강제 설정
            if (stateValues.containsKey("currentModeSettings")) {
                Map<String, String> currentModeSettings = new HashMap<>();
                currentModeSettings.put("mode_boiler", "02");
                stateValues.put("currentModeSettings", currentModeSettings);
            } else {
                // currentModeSettings가 없는 경우 기본값 추가
                Map<String, String> currentModeSettings = new HashMap<>();
                currentModeSettings.put("mode_boiler", "02");
                stateValues.put("currentModeSettings", currentModeSettings);
            }

            // 오류 방지를 위해 불필요한 필드 제거
            stateValues.remove("status");
            stateValues.remove("updateModeSettings");
            stateValues.remove("fanSpeed");
            stateValues.remove("temperature");

            states.put(deviceId, stateValues);

            log.info("Final stateValues to send: {}", stateValues);
        }
        

        // ReportStatusResult 객체 생성
        ReportStatusResult.Request reportStatusResult = ReportStatusResult.Request.builder()
                .requestId(requestId)
                .agentUserId(agentUserId)
                .payload(ReportStatusResult.Request.Payload.builder()
                        .devices(ReportStatusResult.Request.Payload.Device.builder()
                                .states(states)
                                .build())
                        .build())
                .build();

        // Google OAuth2 액세스 토큰 요청
        String googleOuath2AccessToken = accessTokenRequester.getToken();
        String baseUrl = "https://homegraph.googleapis.com";
        String uri = "/v1/devices:reportStateAndNotification";
        log.info("baseUrl:{}", baseUrl + uri);

        // WebClient를 통한 Google Home Graph API 요청
        WebClientUtils.getSslClient(baseUrl, MediaType.APPLICATION_JSON_VALUE, HttpMethod.POST, googleOuath2AccessToken)
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

}