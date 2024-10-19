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

            // modelCode에 따라 보일러와 환기 기기를 구분
            if (modelCode.startsWith("BOILER")) {
                deviceType = "action.devices.types.BOILER";
                attributes.put("temperatureUnitForUX", "C")
                        .put("temperatureStepCelsius", 1)
                        .put("temperatureRange", new JSONObject()
                                .put("minThresholdCelsius", 10)
                                .put("maxThresholdCelsius", 80));

                // 보일러에 대한 settings 정의
                String[][] settings = getBoilerSettings(modelCode);

                JSONObject modeBoiler = createModeBoiler(settings);
                availableModes.put(modeBoiler);
                attributes.put("availableModes", availableModes);

                device.put("traits", new JSONArray()
                        .put("action.devices.traits.OnOff")
                        .put("action.devices.traits.TemperatureControl")
                        .put("action.devices.traits.Modes"));
            }
            else if (modelCode.startsWith("VENT")) {
                deviceType = "action.devices.types.FAN";

                // 환기 기기에 대한 settings 정의
                String[][] settings = getVentSettings(modelCode);

                JSONObject modeFan = createModeFan(settings);
                availableModes.put(modeFan);
                attributes.put("availableModes", availableModes);

                device.put("traits", new JSONArray()
                        .put("action.devices.traits.OnOff")
                        .put("action.devices.traits.FanSpeed"));
            }

            // 공통으로 device에 추가할 값들
            device.put("id", deviceId);
            device.put("type", deviceType);
            device.put("attributes", attributes);

            // GoogleDTO에서 기기 닉네임 가져오기
            GoogleDTO params = new GoogleDTO();
            params.setUserId(userId);
            params.setDeviceId(deviceId);
            GoogleDTO deviceNick = googleMapper.getNicknameByDeviceId(params);

            device.put("name", new JSONObject().put("name", deviceNick.getDeviceNickname() + "_" + deviceNick.getAddressNickname()));
            devices.put(device);
        }

        payload.put("devices", devices);
        response.put("payload", payload);

        log.info("handleSync response: " + response);
        return response;
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

        JSONArray inputs = requestBody.getJSONArray("inputs");
        JSONArray commandsArray = new JSONArray();  // 실제 실행 결과를 저장할 배열

        inputs.forEach(inputObj -> {
            JSONObject input = (JSONObject) inputObj;
            JSONArray execution = input.getJSONObject("payload").getJSONArray("commands");

            execution.forEach(execObj -> {
                JSONObject command = (JSONObject) execObj;
                JSONArray devices = command.getJSONArray("devices");

                devices.forEach(deviceObj -> {
                    JSONObject device = (JSONObject) deviceObj;
                    String deviceId = device.getString("id");
                    System.out.println("deviceId: " + deviceId);
                    JSONArray execCommands = command.getJSONArray("execution");

                    execCommands.forEach(execCommandObj -> {
                        JSONObject execCommand = (JSONObject) execCommandObj;
                        String commandName = execCommand.getString("command");
                        boolean isSuccess = true;
                        String errorString = "";

                        deviceStatus.setDeviceId(deviceId);
                        try {
                            switch (commandName) {
                                case "action.devices.commands.OnOff":
                                    boolean on = execCommand.getJSONObject("params").getBoolean("on");
                                    if (on) deviceStatus.setPowrStatus("on");
                                    else deviceStatus.setPowrStatus("of");
                                    log.info("=================================================================");
                                    log.info("Turning " + (on ? "on" : "off") + " device " + deviceId);
                                    log.info("=================================================================");
                                    System.out.println(deviceStatus);
                                    googleMapper.updateDeviceStatus(deviceStatus);
                                    handleDevice(userId, deviceId, (on ? "on" : "of"), "powr");
                                    break;
                                case "action.devices.commands.ThermostatTemperatureSetpoint":
                                    double temp = execCommand.getJSONObject("params").getDouble("thermostatTemperatureSetpoint");
                                    log.info("Setting temperature of device " + deviceId + " to " + temp);
                                    deviceStatus.setTempStatus(String.valueOf(temp));
                                    googleMapper.updateDeviceStatus(deviceStatus);
                                    handleDevice(userId, deviceId, String.valueOf(temp), "htTp");
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
                                        handleSetModes(userId, deviceId, modeName, modeValue);
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
                                        googleMapper.updateDeviceStatus(deviceStatus);
                                    }
                                    break;
                                default:
                                    isSuccess = false;
                                    errorString = "Unsupported command: " + commandName;
                                    log.error("Unsupported command: " + commandName);
                            }
                        } catch (Exception e) {
                            isSuccess = false;
                            errorString = e.getMessage();
                            log.error("Error handling command: " + commandName, e);
                        }

                        // 각 장치 및 명령에 대한 실행 결과를 commands 배열에 추가
                        JSONObject commandResult = new JSONObject();
                        commandResult.put("ids", new JSONArray().put(deviceId));
                        commandResult.put("status", isSuccess ? "SUCCESS" : "ERROR");
                        if (!isSuccess) {
                            commandResult.put("errorCode", "deviceTurnOnOffFailed");
                            commandResult.put("errorDetail", errorString);
                        }
                        commandsArray.put(commandResult);
                    });
                });
            });
        });

        JSONObject payload = new JSONObject();
        payload.put("commands", commandsArray);  // 실제 실행 결과를 포함한 commands 배열
        response.put("payload", payload);

        log.info("handleExecute response: " + response);
        return response;
    }

    public JSONObject handleQuery(JSONObject requestBody, List<String> deviceIds) {
        log.info("handleQuery CALLED");
        log.info("requestBody: " + requestBody);

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        JSONObject devices = new JSONObject();

        for (String deviceId : deviceIds) {
            deviceStatus = googleMapper.getInfoByDeviceId(deviceId);

            System.out.println("handleQuery++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("deviceStatus: " + deviceStatus);
            System.out.println("handleQuery++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

            boolean deviceOnOff = false;

            JSONObject deviceState = new JSONObject();
            Map<String, Object> currentModeSettings = new HashMap<>();
            currentModeSettings.put("mode_boiler", deviceStatus.getModeValue());

            if (deviceStatus.getPowrStatus().equals("on")) {
                deviceOnOff = true;
            }
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("deviceOnOff: " + deviceOnOff);
            System.out.println("deviceId: " + deviceId);
            System.out.println("deviceStatus.getModeValue(): " + deviceStatus.getModeValue());
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            deviceState.put("on", deviceOnOff); // The device is ON
            deviceState.put("online", true);
            deviceState.put("currentModeSettings", currentModeSettings);
            deviceState.put("temperatureAmbientCelsius", 55);
            deviceState.put("temperatureSetpointCelsius", 55);
            deviceState.put("status", "SUCCESS");

            devices.put(deviceId, deviceState);
        }

        payload.put("devices", devices);
        response.put("payload", payload);

        log.info("handleQuery response: " + response);

        return response;
    }

    // 보일러 설정에 따라 settings 배열 생성
    private String[][] getBoilerSettings(String modelCode) {
        if (modelCode.equals("BOILER_A")) {
            return new String[][] {
                    {"01", "난방-실내온도", "Heating_Indoor_Temperature"},
                    {"02", "난방-난방수온도", "Heating_Water_Temperature"},
                    {"03", "외출", "Away"}
            };
        } else {
            return new String[][] {
                    {"01", "난방-실내온도", "Heating_Indoor_Temperature"},
                    {"05", "절약난방", "Economy_Heating"},
                    {"07", "온수전용", "Hot_Water_Only"}
            };
        }
    }

    // 환기 설정에 따라 settings 배열 생성
    private String[][] getVentSettings(String modelCode) {
        if (modelCode.equals("VENT_A")) {
            return new String[][] {
                    {"01", "환기-저속", "Ventilation_Low"},
                    {"02", "환기-고속", "Ventilation_High"}
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
        deviceStatus.setModeValue(modeValue);
        googleMapper.updateDeviceStatus(deviceStatus);
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
}