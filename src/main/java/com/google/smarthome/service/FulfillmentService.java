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

    public JSONObject handleSync(JSONObject requestBody, List<String> deviceIds, String userId) {
        log.info("handleSync CALLED");
        log.info("requestBody: " + requestBody);

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        payload.put("agentUserId", userId);

        JSONArray devices = new JSONArray();

        for (String deviceId : deviceIds) {
            log.info("Processing device with ID: " + deviceId);
            if (deviceId == null) {
                log.error("Null device ID found");
                continue;
            }
            JSONObject boiler = new JSONObject();

            // 보일러 온도 최소/최대 값
            JSONObject temperatureRange = new JSONObject();
            temperatureRange.put("minThresholdCelsius", 10);
            temperatureRange.put("maxThresholdCelsius", 80);

            // 모드 관련 속성 추가
            JSONArray availableModes = new JSONArray();

            JSONObject modeBoiler = new JSONObject();
            modeBoiler.put("name", "mode_boiler");

            JSONArray nameValuesArray = new JSONArray();
            nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("보일러")).put("lang", "ko"));
            nameValuesArray.put(new JSONObject().put("name_synonym", new JSONArray().put("boiler")).put("lang", "en"));
            modeBoiler.put("name_values", nameValuesArray);

            JSONArray settingsArray = new JSONArray();

            // 설정 정보를 배열로 정의
            String[][] settings = {
                    {"01", "난방-실내온도", "Heating_Indoor_Temperature"},
                    {"02", "난방-난방수온도", "Heating_Water_Temperature"},
                    {"03", "외출", "Away"},
                    {"05", "절약난방", "Economy_Heating"},
                    {"061", "취침1", "Sleep1"},
                    {"062", "취침2", "Sleep2"},
                    {"063", "취침3", "Sleep3"},
                    {"07", "온수전용", "Hot_Water_Only"},
                    {"08", "온수-빠른온수", "Quick_Hot_Water"},
                    {"10", "24시간예약", "24_Hour_Reservation"},
                    {"11", "12시간예약", "12_Hour_Reservation"},
                    {"12", "주간예약", "Weekly_Reservation"}
            };

            // for 문을 사용하여 설정 정보를 추가
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
            availableModes.put(modeBoiler);

            boiler.put("attributes", new JSONObject()
                    .put("temperatureUnitForUX", "C")
                    .put("temperatureStepCelsius", 1)
                    .put("temperatureRange", temperatureRange)
                    .put("availableModes", availableModes));

            boiler.put("id", deviceId);
            boiler.put("type", "action.devices.types.BOILER");
            boiler.put("traits", new JSONArray()
                    .put("action.devices.traits.OnOff")
                    .put("action.devices.traits.TemperatureControl")
                    .put("action.devices.traits.Modes"));
    
            GoogleDTO params = new GoogleDTO();
            params.setUserId(userId);
            params.setDeviceId(deviceId);
            GoogleDTO deviceNick = googleMapper.getNicknameByDeviceId(params);

            boiler.put("name", new JSONObject().put("name", deviceNick.getDeviceNickname() +
                    "_" +
                    deviceNick.getAddressNickname()));

            devices.put(boiler);
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