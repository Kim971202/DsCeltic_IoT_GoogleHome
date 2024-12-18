//package com.google.smarthome.service;
//
//import com.google.smarthome.contant.MobiusResponse;
//import com.google.smarthome.dto.FanSpeed;
//import com.google.smarthome.dto.GoogleDTO;
//import com.google.smarthome.mapper.GoogleMapper;
//import com.google.smarthome.utils.JSON;
//import lombok.extern.slf4j.Slf4j;
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Slf4j
//@Service
//public class FulfillmentService {
//
//    @Autowired
//    private GoogleMapper googleMapper;
//    @Autowired
//    private MobiusService mobiusService;
//    GoogleDTO deviceStatus;
//
//    public JSONObject handleSync(JSONObject requestBody, Map<String, String> deviceInfoMap, String userId) {
//        log.info("handleSync CALLED");
//        log.info("requestBody: " + requestBody);
//
//        JSONObject response = new JSONObject();
//        response.put("requestId", requestBody.getString("requestId"));
//
//        JSONObject payload = new JSONObject();
//        payload.put("agentUserId", userId);
//
//        JSONArray devices = new JSONArray();
//
//        for (Map.Entry<String, String> entry : deviceInfoMap.entrySet()) {
//            String deviceId = entry.getKey();  // deviceId
//            String modelCode = entry.getValue();  // modelCode
//
//            log.info("Processing device with ID: " + deviceId + " and Model Code: " + modelCode);
//
//            if (deviceId == null) {
//                log.error("Null device ID found");
//                continue;
//            }
//
//            JSONObject device = new JSONObject();
//            JSONObject attributes = new JSONObject();
//            JSONArray availableModes = new JSONArray();
//            String deviceType = "";
//
//            // modelCode에 따라 보일러와 환기 기기를 구분
//            if (modelCode.equals("ESCeco13S") || modelCode.equals("DCR-91/WF")) {
//                deviceType = "action.devices.types.THERMOSTAT";
//                attributes.put("temperatureUnitForUX", "C")
//                        .put("temperatureStepCelsius", 1)
//                        .put("temperatureRange", new JSONObject()
//                                .put("minThresholdCelsius", 10)
//                                .put("maxThresholdCelsius", 80))
//                        .put("availableThermostatModes", new JSONArray()
//                                .put("off")    // 전원 꺼짐
//                                .put("heat")); // 난방 모드
//
//                device.put("traits", new JSONArray()
//                        .put("action.devices.traits.OnOff")
//                        .put("action.devices.traits.TemperatureSetting"));
//            }
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
//
//            // 공통으로 device에 추가할 값들
//            device.put("id", deviceId);
//            device.put("type", deviceType);
//            device.put("attributes", attributes);
//
//            // GoogleDTO에서 기기 닉네임 가져오기
//            GoogleDTO params = new GoogleDTO();
//            params.setUserId(userId);
//            params.setDeviceId(deviceId);
//            GoogleDTO deviceNick = googleMapper.getNicknameByDeviceId(params);
//
//            device.put("name", new JSONObject().put("name", "대성" + "-" + deviceNick.getDeviceNickname()));
//            devices.put(device);
//        }
//
//        payload.put("devices", devices);
//        response.put("payload", payload);
//
//        log.info("handleSync response: " + response);
//        return response;
//    }
//
//    // 보일러 모드 설정
//    private JSONObject createModeBoiler(String[][] settings) {
//        JSONObject modeBoiler = new JSONObject();
//        modeBoiler.put("name", "mode_boiler");
//
//        JSONArray nameValuesArray = new JSONArray();
//        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("보일러")).put("lang", "ko"));
//        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("boiler")).put("lang", "en"));
//        modeBoiler.put("name_values", nameValuesArray);
//
//        JSONArray settingsArray = new JSONArray();
//
//        // settings 배열에 맞는 모드 설정 추가
//        for (String[] setting : settings) {
//            JSONObject settingObject = new JSONObject();
//            settingObject.put("setting_name", setting[0]);
//
//            JSONArray settingValues = new JSONArray();
//            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[1])).put("lang", "ko"));
//            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[2])).put("lang", "en"));
//
//            settingObject.put("setting_values", settingValues);
//            settingsArray.put(settingObject);
//        }
//
//        modeBoiler.put("settings", settingsArray);
//        modeBoiler.put("ordered", false); // ordered 필드 추가
//        return modeBoiler;
//    }
//
//    // 환기 모드 설정
//    private JSONObject createModeFan(String[][] settings) {
//        JSONObject modeFan = new JSONObject();
//        modeFan.put("name", "mode_fan");
//
//        JSONArray nameValuesArray = new JSONArray();
//        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("환기")).put("lang", "ko"));
//        nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("fan")).put("lang", "en"));
//        modeFan.put("name_values", nameValuesArray);
//
//        JSONArray settingsArray = new JSONArray();
//
//        // settings 배열에 맞는 모드 설정 추가
//        for (String[] setting : settings) {
//            JSONObject settingObject = new JSONObject();
//            settingObject.put("setting_name", setting[0]);
//
//            JSONArray settingValues = new JSONArray();
//            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[1])).put("lang", "ko"));
//            settingValues.put(new JSONObject().put("setting_synonym", new JSONArray().put(setting[2])).put("lang", "en"));
//
//            settingObject.put("setting_values", settingValues);
//            settingsArray.put(settingObject);
//        }
//
//        modeFan.put("settings", settingsArray);
//        modeFan.put("ordered", false);
//        return modeFan;
//    }
//
//    public JSONObject handleExecute(JSONObject requestBody, List<String> deviceIds, String userId) {
//        log.info("handleExecute CALLED");
//        log.info("requestBody: " + requestBody);
//
//        // states 객체 선언
//        JSONObject states = new JSONObject();
//
//        JSONObject response = new JSONObject();
//        response.put("requestId", requestBody.getString("requestId"));
//
//        JSONArray inputs = requestBody.getJSONArray("inputs");
//        JSONArray commandsArray = new JSONArray();  // 실제 실행 결과를 저장할 배열
//
//        inputs.forEach(inputObj -> {
//            JSONObject input = (JSONObject) inputObj;
//            JSONArray execution = input.getJSONObject("payload").getJSONArray("commands");
//
//            execution.forEach(execObj -> {
//                JSONObject command = (JSONObject) execObj;
//                JSONArray devices = command.getJSONArray("devices");
//
//                devices.forEach(deviceObj -> {
//                    JSONObject device = (JSONObject) deviceObj;
//                    String deviceId = device.getString("id");
//                    System.out.println("deviceId: " + deviceId);
//                    JSONArray execCommands = command.getJSONArray("execution");
//
//                    execCommands.forEach(execCommandObj -> {
//                        JSONObject execCommand = (JSONObject) execCommandObj;
//                        String commandName = execCommand.getString("command");
//                        boolean isSuccess = true;
//                        String errorString = "";
//
//                        System.out.println("commandName: " + commandName);
//
//                        deviceStatus.setDeviceId(deviceId);
//                        try {
//                            switch (commandName) {
//                                case "action.devices.commands.OnOff":
//                                    boolean on = execCommand.getJSONObject("params").getBoolean("on");
//                                    if (on) deviceStatus.setPowrStatus("on");
//                                    else deviceStatus.setPowrStatus("of");
//                                    log.info("=================================================================");
//                                    log.info("Turning " + (on ? "on" : "off") + " device " + deviceId);
//                                    log.info("=================================================================");
//                                    System.out.println(deviceStatus);
//                                    googleMapper.updateDeviceStatus(deviceStatus);
//                                    handleDevice(userId, deviceId, (on ? "on" : "of"), "powr");
//                                    break;
//                                case "action.devices.commands.ThermostatTemperatureSetpoint":
//                                    double temp = execCommand.getJSONObject("params").getDouble("thermostatTemperatureSetpoint");
//                                    log.info("Setting temperature of device " + deviceId + " to " + temp);
//
//                                    deviceStatus.setTempStatus(String.valueOf(temp));
//                                    googleMapper.updateDeviceStatus(deviceStatus);
//                                    handleDevice(userId, deviceId, String.valueOf(temp), "htTp");
//                                    break;
//                                case "action.devices.commands.ThermostatSetMode":
//                                    String mode = execCommand.getJSONObject("params").getString("thermostatMode");
//                                    log.info("Setting mode of device " + deviceId + " to " + mode);
//                                    if (mode.equals("off")) deviceStatus.setPowrStatus("of");
//                                    else if (mode.equals("heat")) deviceStatus.setPowrStatus("on");
//                                    googleMapper.updateDeviceStatus(deviceStatus);
//                                    handleDevice(userId, deviceId, mode, "powr");
//                                    break;
//                                case "action.devices.commands.SetModes":
//                                    JSONObject params = execCommand.getJSONObject("params").getJSONObject("updateModeSettings");
//                                    for (String modeName : params.keySet()) {
//                                        String modeValue = params.getString(modeName);
//                                        log.info("설정 mode of device " + deviceId + " to " + modeName + ": " + modeValue);
//                                        switch (modeValue) {
//                                            case "061":
//                                                deviceStatus.setModeValue("06");
//                                                deviceStatus.setSleepCode("01");
//                                                break;
//                                            case "062":
//                                                deviceStatus.setModeValue("06");
//                                                deviceStatus.setSleepCode("02");
//                                                break;
//                                            case "063":
//                                                deviceStatus.setModeValue("06");
//                                                deviceStatus.setSleepCode("03");
//                                                break;
//                                        }
//
//                                        handleSetModes(userId, deviceId, modeName, modeValue);
//                                        googleMapper.updateDeviceStatus(deviceStatus);
//                                    }
//                                    break;
//                                case "action.devices.commands.SetTemperature":
//                                    double temperature = execCommand.getJSONObject("params").getDouble("temperature");
//                                    log.info("Setting temperature of device " + deviceId + " to " + temperature);
//                                    deviceStatus.setTempStatus(String.valueOf(temperature));
//                                    googleMapper.updateDeviceStatus(deviceStatus);
//                                    handleDevice(userId, deviceId, String.valueOf(temperature), "htTp");
//                                    break;
//                                default:
//                                    isSuccess = false;
//                                    errorString = "Unsupported command: " + commandName;
//                                    log.error("Unsupported command: " + commandName);
//                            }
//                        } catch (Exception e) {
//                            isSuccess = false;
//                            errorString = e.getMessage();
//                            log.error("Error handling command: " + commandName, e);
//                        }
//
//                        // 각 장치 및 명령에 대한 실행 결과를 commands 배열에 추가
//                        JSONObject commandResult = new JSONObject();
//                        commandResult.put("ids", new JSONArray().put(deviceId));
//                        commandResult.put("status", isSuccess ? "SUCCESS" : "ERROR");
//                        commandResult.put("states", states); // states 결과 추가
//                        if (!isSuccess) {
//                            commandResult.put("errorCode", "deviceTurnOnOffFailed");
//                            commandResult.put("errorDetail", errorString);
//                        }
//                        commandsArray.put(commandResult);
//                    });
//                });
//            });
//        });
//
//        JSONObject payload = new JSONObject();
//        payload.put("commands", commandsArray);  // 실제 실행 결과를 포함한 commands 배열
//        response.put("payload", payload);
//
//        log.info("handleExecute response: " + response);
//        return response;
//    }
//
//    // 전원 제어 명령
//    private void handleOnOffCommand(String deviceId, boolean on) throws Exception {
//        log.info("Setting device " + deviceId + " power to " + (on ? "ON" : "OFF"));
//        deviceStatus.setDeviceId(deviceId);
//        deviceStatus.setPowrStatus(on ? "on" : "of");
//        googleMapper.updateDeviceStatus(deviceStatus);
//        handleDevice(deviceStatus.getUserId(), deviceId, on ? "on" : "of", "powr");
//    }
//
//    // 온도 설정 명령
//    private void handleTemperatureSetpoint(String deviceId, double temperature) throws Exception {
//        log.info("Setting temperature for device " + deviceId + " to " + temperature);
//        if (temperature < 10 || temperature > 80) {
//            throw new IllegalArgumentException("Temperature out of range");
//        }
//        deviceStatus.setDeviceId(deviceId);
//        deviceStatus.setTempStatus(String.valueOf(temperature));
//        googleMapper.updateDeviceStatus(deviceStatus);
//        handleDevice(deviceStatus.getUserId(), deviceId, String.valueOf(temperature), "htTp");
//    }
//
//    // 모드 설정 명령
//    private void handleSetModes(String deviceId, JSONObject modeSettings) throws Exception {
//        for (String mode : modeSettings.keySet()) {
//            String value = modeSettings.getString(mode);
//            log.info("Setting mode for device " + deviceId + ": " + mode + " = " + value);
//            deviceStatus.setModeValue(value);
//            googleMapper.updateDeviceStatus(deviceStatus);
//            handleDeviceWithMode(deviceStatus.getUserId(), deviceId, value, "opMd");
//        }
//    }
//
//    public JSONObject handleQuery(JSONObject requestBody, List<String> deviceIds) {
//        log.info("handleQuery CALLED");
//        log.info("requestBody: " + requestBody);
//
//        JSONObject response = new JSONObject();
//        response.put("requestId", requestBody.getString("requestId"));
//
//        JSONObject payload = new JSONObject();
//        JSONObject devices = new JSONObject();
//
//        for (String deviceId : deviceIds) {
//            deviceStatus = googleMapper.getInfoByDeviceId(deviceId);
//
//            System.out.println("handleQuery++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
//            System.out.println("deviceStatus: " + deviceStatus);
//            System.out.println("handleQuery++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
//
//            boolean deviceOnOff = false;
//
//            JSONObject deviceState = new JSONObject();
//            Map<String, Object> currentModeSettings = new HashMap<>();
//            currentModeSettings.put("mode_boiler", deviceStatus.getModeValue());
//
//            if (deviceStatus.getPowrStatus().equals("on")) {
//                deviceOnOff = true;
//            }
//            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
//            System.out.println("deviceOnOff: " + deviceOnOff);
//            System.out.println("deviceId: " + deviceId);
//            System.out.println("deviceStatus.getModeValue(): " + deviceStatus.getModeValue());
//            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
//            deviceState.put("on", deviceOnOff); // The device is ON
//            deviceState.put("online", true);
//            deviceState.put("thermostatMode", deviceOnOff ? "heat" : "off"); // 현재 모드 상태
//            deviceState.put("temperatureSetpointCelsius", Double.parseDouble(deviceStatus.getTempStatus())); // 목표 온도
//            deviceState.put("temperatureAmbientCelsius", 25.0); // 현재 온도
//            devices.put(deviceId, deviceState);
//        }
//
//        payload.put("devices", devices);
//        response.put("payload", payload);
//
//        log.info("handleQuery response: " + response);
//
//        return response;
//    }
//
//    // 보일러 설정에 따라 settings 배열 생성
//    private String[][] getBoilerSettings(String modelCode) {
//        if (modelCode.equals("ESCeco13S")) {
//            return new String[][]  {
//                    {"01", "실내온도", "Heating_Indoor_Temperature"},
//                    {"02", "온돌난방", "Heating_Water_Temperature"},
//                    {"03", "외출", "Away"},
//                    {"05", "절약난방", "Economy_Heating"},
//                    {"061", "취침", "Sleep1"},
//                    {"07", "온수전용", "Hot_Water_Only"}
//            };
//        } else {
//            return new String[][]  {
//                    {"01", "실내온도", "Heating_Indoor_Temperature"},
//                    {"02", "난방수온도", "Heating_Water_Temperature"},
//                    {"03", "외출", "Away"}, // 외출/온수전용 같음
//                    {"08", "빠른온수", "FAST_WATER"}
//            };
//        }
//    }
//
//    // 환기 설정에 따라 settings 배열 생성
//    private String[][] getVentSettings(String modelCode) {
//        if (modelCode.equals("DCR-47/WF")) {
//            return new String[][] {
//                    {"00", "Auto", "Ventilation_Auto"},
//                    {"01", "환기-1단", "Ventilation_Level_1"},
//                    {"02", "환기-2단", "Ventilation_Level_2"},
//                    {"03", "환기-3단", "Ventilation_Level_3"}
//            };
//        } else {
//            return new String[][] {
//                    {"01", "환기-중속", "Ventilation_Medium"},
//                    {"02", "환기-터보", "Ventilation_Turbo"}
//            };
//        }
//    }
//
//    // 모드를 설정하는 새로운 메서드
//    private void handleSetModes(String userId, String deviceId, String modeName, String modeValue) {
//        log.info("Setting mode for device " + deviceId + ": " + modeName + " = " + modeValue);
//        try {
//            handleDeviceWithMode(userId, deviceId, modeValue, "opMd");
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private String handleDevice(String userId, String deviceId, String value, String functionId) throws Exception {
//        MobiusResponse mobiusResponse;
//        ConcurrentHashMap<String, String> conMap = new ConcurrentHashMap<>();
//
//        conMap.put("userId", userId);
//        conMap.put("deviceId", deviceId);
//        conMap.put("value", value);
//        conMap.put("functionId", functionId);
//        System.out.println(JSON.toJson(conMap));
//        mobiusResponse = mobiusService.createCin("googleAE", "googleCNT", JSON.toJson(conMap));
//
//        return mobiusResponse.getResponseCode();
//    }
//
//    private String handleDeviceWithMode(String userId, String deviceId, String value, String functionId) throws Exception {
//        // 모드를 설정하는 로직을 여기에 구현합니다.
//        MobiusResponse mobiusResponse;
//        ConcurrentHashMap<String, String> conMap = new ConcurrentHashMap<>();
//
//        conMap.put("userId", userId);
//        conMap.put("deviceId", deviceId);
//        conMap.put("value", value);
//        conMap.put("functionId", functionId);
//        System.out.println(JSON.toJson(conMap));
//        mobiusResponse = mobiusService.createCin("googleAE", "googleCNT", JSON.toJson(conMap));
//
//        return mobiusResponse.getResponseCode();
//    }
//}

package com.google.smarthome.service;

import com.google.smarthome.contant.MobiusResponse;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FulfillmentService {

    @Autowired
    private GoogleMapper googleMapper;
    @Autowired
    private MobiusService mobiusService;

    GoogleDTO deviceStatus;

    public JSONObject handleSync(JSONObject requestBody, Map<String, String> deviceInfoMap, String userId) {
        log.info("handleSync CALLED");
        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        payload.put("agentUserId", userId);
        JSONArray devices = new JSONArray();

        for (Map.Entry<String, String> entry : deviceInfoMap.entrySet()) {
            String deviceId = entry.getKey();
            String modelCode = entry.getValue();
            log.info("Processing device with ID: " + deviceId + " and Model Code: " + modelCode);

            JSONObject device = new JSONObject();
            JSONObject attributes = new JSONObject();
            JSONArray availableModes = new JSONArray();
            String deviceType = "";

            if (modelCode.equals("ESCeco13S") || modelCode.equals("DCR-91/WF")) {
                deviceType = "action.devices.types.BOILER";
                attributes.put("temperatureUnitForUX", "C")
                        .put("temperatureStepCelsius", 1)
                        .put("temperatureRange", new JSONObject()
                                .put("minThresholdCelsius", 10)
                                .put("maxThresholdCelsius", 80))
                        .put("availableThermostatModes", new JSONArray().put("off").put("heat"));
                String[][] settings = getBoilerSettings(modelCode);
                availableModes.put(createMode("mode_boiler", "보일러", "boiler", settings));

                device.put("traits", new JSONArray()
                        .put("action.devices.traits.OnOff")
                        .put("action.devices.traits.TemperatureSetting")
                        .put("action.devices.traits.Modes"));
            } else if (modelCode.equals("DCR-47/WF")) {
                deviceType = "action.devices.types.FAN";
                String[][] settings = getVentSettings(modelCode);
                availableModes.put(createMode("mode_fan", "환기", "fan", settings));
                device.put("traits", new JSONArray()
                        .put("action.devices.traits.OnOff")
                        .put("action.devices.traits.Modes"));
            }

            attributes.put("availableModes", availableModes);
            device.put("id", deviceId);
            device.put("type", deviceType);
            device.put("attributes", attributes);

            GoogleDTO params = new GoogleDTO();
            params.setUserId(userId);
            params.setDeviceId(deviceId);
            GoogleDTO deviceNick = googleMapper.getNicknameByDeviceId(params);
            device.put("name", new JSONObject().put("name", "대성" + "-" + deviceNick.getDeviceNickname()));

            devices.put(device);
        }

        payload.put("devices", devices);
        response.put("payload", payload);
        log.info("handleSync response: " + response);
        return response;
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

    private JSONObject createMode(String modeName, String synonymKo, String synonymEn, String[][] settings) {
        JSONObject mode = new JSONObject();
        mode.put("name", modeName);
        JSONArray nameValues = new JSONArray()
                .put(new JSONObject().put("name_synonym", new JSONArray().put(synonymKo)).put("lang", "ko"))
                .put(new JSONObject().put("name_synonym", new JSONArray().put(synonymEn)).put("lang", "en"));
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

    public JSONObject handleQuery(JSONObject requestBody, List<String> deviceIds) {
        log.info("handleQuery CALLED");
        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        JSONObject devices = new JSONObject();

        for (String deviceId : deviceIds) {
            deviceStatus = googleMapper.getInfoByDeviceId(deviceId);

            JSONObject deviceState = new JSONObject();
            Map<String, Object> currentModeSettings = new HashMap<>();
            currentModeSettings.put("mode_boiler", deviceStatus.getModeValue());

            System.out.println(Double.parseDouble(deviceStatus.getTempStatus()));

            boolean deviceOnOff = "on".equals(deviceStatus.getPowrStatus());
            deviceState.put("on", deviceOnOff);
            deviceState.put("online", true);
            deviceState.put("currentModeSettings", currentModeSettings);
            deviceState.put("thermostatMode", deviceOnOff ? "heat" : "off");
            deviceState.put("temperatureSetpointCelsius", String.format("%.1f", Double.parseDouble(deviceStatus.getTempStatus())));
            deviceState.put("temperatureAmbientCelsius",  String.format("%.1f", 25.0));

            devices.put(deviceId, deviceState);
        }

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
                    {"03", "외출모드", "Away"},
                    {"05", "절약난방", "Economy_Heating"},
                    {"061", "취침모드", "Sleep1"},
                    {"07", "온수전용", "Hot_Water_Only"}
            };
        } else {
            return new String[][]  {
                    {"01", "실내난방", "Heating_Indoor_Temperature"},
                    {"02", "난방수모드", "Heating_Water_Temperature"},
                    {"03", "외출모드", "Away"}, // 외출/온수전용 같음
                    {"08", "빠른온수모드", "FAST_WATER"}
            };
        }
    }

    private String[][] getVentSettings(String modelCode) {
        return new String[][]{
                {"00", "Auto", "Ventilation_Auto"},
                {"01", "환기-1단", "Ventilation_Level_1"},
                {"02", "환기-2단", "Ventilation_Level_2"},
                {"03", "환기-3단", "Ventilation_Level_3"}
        };
    }

    private void handleOnOffCommand(String deviceId, boolean on) throws Exception {
        log.info("Setting device " + deviceId + " power to " + (on ? "ON" : "OFF"));

        // GoogleDTO를 통해 기기 상태를 설정
        deviceStatus = googleMapper.getInfoByDeviceId(deviceId);
        if (deviceStatus == null) {
            throw new RuntimeException("Device not found for ID: " + deviceId);
        }

        deviceStatus.setPowrStatus(on ? "on" : "of");
        googleMapper.updateDeviceStatus(deviceStatus);

        // 외부 시스템 호출 (Mobius와 같은 시스템에 명령 전달)
        String functionId = "powr";  // 전원 제어 기능 ID
        String value = on ? "on" : "of";
        handleDevice(deviceStatus.getUserId(), deviceId, value, functionId);

        log.info("Device " + deviceId + " power state updated to " + (on ? "ON" : "OFF"));
    }

    private void handleTemperatureSetpoint(String deviceId, double temperature) throws Exception {
        log.info("Setting temperature for device " + deviceId + " to " + temperature);

        // 온도 범위 유효성 검사
        if (temperature < 10 || temperature > 80) {
            throw new IllegalArgumentException("Temperature out of range");
        }

        // 기기 상태 업데이트
        deviceStatus.setDeviceId(deviceId);
        deviceStatus.setTempStatus(String.valueOf(temperature));
        googleMapper.updateDeviceStatus(deviceStatus);

        // 외부 시스템 호출 (예: Mobius)
        handleDevice(deviceStatus.getUserId(), deviceId, String.valueOf(temperature), "htTp");
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

    private void handleSetModes(String deviceId, JSONObject modeSettings) throws Exception {
        for (String mode : modeSettings.keySet()) {
            String value = modeSettings.getString(mode);
            log.info("Setting mode for device " + deviceId + ": " + mode + " = " + value);

            // 기기 상태 업데이트
            deviceStatus.setDeviceId(deviceId);
            deviceStatus.setModeValue(value);
            googleMapper.updateDeviceStatus(deviceStatus);

            // 외부 시스템 호출 (모드 설정)
            handleDeviceWithMode(deviceStatus.getUserId(), deviceId, value, "opMd");
        }
    }

    private String handleDeviceWithMode(String userId, String deviceId, String value, String functionId) throws Exception {
        // 외부 시스템에 모드 설정 요청
        MobiusResponse mobiusResponse;
        ConcurrentHashMap<String, String> conMap = new ConcurrentHashMap<>();

        // 요청에 필요한 데이터 구성
        conMap.put("userId", userId);
        conMap.put("deviceId", deviceId);
        conMap.put("value", value);         // 설정할 모드 값
        conMap.put("functionId", functionId); // 모드 설정을 구분하는 ID
        System.out.println(JSON.toJson(conMap));

        // Mobius 또는 외부 시스템에 데이터 전송
        mobiusResponse = mobiusService.createCin("googleAE", "googleCNT", JSON.toJson(conMap));

        // 응답 코드 반환
        return mobiusResponse.getResponseCode();
    }

    private void handleDevice(String userId, String deviceId, String value, String functionId) throws Exception {
        MobiusResponse mobiusResponse;
        ConcurrentHashMap<String, String> conMap = new ConcurrentHashMap<>();

        conMap.put("userId", userId);
        conMap.put("deviceId", deviceId);
        conMap.put("value", value);
        conMap.put("functionId", functionId);

        System.out.println(JSON.toJson(conMap));
        mobiusResponse = mobiusService.createCin("googleAE", "googleCNT", JSON.toJson(conMap));
    }

}
