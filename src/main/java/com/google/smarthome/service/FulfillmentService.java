package com.google.smarthome.service;

import com.google.smarthome.contant.MobiusResponse;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.utils.JSON;
import com.google.smarthome.utils.RedisCommand;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FulfillmentService {

    @Autowired
    private GoogleMapper googleMapper;
    @Autowired
    private MobiusService mobiusService;
    GoogleDTO deviceStatus;

    public JSONObject handleSync(JSONObject requestBody, String deviceId, String userId) {
        log.info("handleSync CALLED");
        log.info("requestBody: " + requestBody);

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        payload.put("agentUserId", userId);

        JSONArray devices = new JSONArray();
        JSONObject boiler = new JSONObject();
        boiler.put("id", deviceId);
//        boiler.put("type", "action.devices.types.THERMOSTAT");
        boiler.put("type", "action.devices.types.BOILER");
        boiler.put("traits", new JSONArray()
                .put("action.devices.traits.OnOff")
                .put("action.devices.traits.TemperatureSetting")
                .put("action.devices.traits.Modes"));

        boiler.put("name", new JSONObject().put("name", "대성IoT 보일러 + modelCode"));

        boiler.put("attributes", new JSONObject()
                .put("availableThermostatModes", new JSONArray().put("off").put("heat"))
                .put("thermostatTemperatureUnit", "C"));

        devices.put(boiler);
        payload.put("devices", devices);

        response.put("payload", payload);
        log.info("handleSync response: " + response);
        return response;
    }
    public JSONObject handleExecute(JSONObject requestBody, String userId) {
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
                    JSONArray execCommands = command.getJSONArray("execution");

                    execCommands.forEach(execCommandObj -> {
                        JSONObject execCommand = (JSONObject) execCommandObj;
                        String commandName = execCommand.getString("command");
                        boolean isSuccess = true;
                        String errorString = "";

                        try {
                            switch (commandName) {
                                case "action.devices.commands.OnOff":
                                    boolean on = execCommand.getJSONObject("params").getBoolean("on");
                                    if (on) deviceStatus.setPowrStatus("on");
                                    else deviceStatus.setPowrStatus("of");
                                    log.info("=================================================================");
                                    log.info("Turning " + (on ? "on" : "off") + " device " + deviceId);
                                    log.info("=================================================================");
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
    public JSONObject handleQuery(JSONObject requestBody, String devceId) {
        log.info("handleQuery CALLED");
        log.info("requestBody: " + requestBody);

        deviceStatus = googleMapper.getInfoByDeviceId(devceId);

        boolean deviceOnOff = false;

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        JSONObject devices = new JSONObject();

        JSONObject deviceState = new JSONObject();

        if(deviceStatus.getPowrStatus().equals("on")) deviceOnOff = true;
        deviceState.put("on", deviceOnOff); // The device is ON

//        if(!deviceStatus.getPowrStatus().equals("of")) {
//            deviceState.put("thermostatMode", "heat"); // Current mode
//            log.info("Double.parseDouble(deviceStatus.getTempStatus()): " + Double.parseDouble(deviceStatus.getTempStatus()));
//            deviceState.put("thermostatTemperatureSetpoint", Double.parseDouble(deviceStatus.getTempStatus())); // default temp
//        } else {
//            deviceStatus.setPowrStatus("off");
//            log.info("deviceStatus.getPowrStatus(): " + deviceStatus.getPowrStatus());
//            deviceState.put("thermostatMode", deviceStatus.getPowrStatus());
//        }

        devices.put(devceId, deviceState);
        payload.put("devices", devices);
        response.put("payload", payload);

        log.info("handleQuery response: " + response);

        return response;
    }

    private String handleDevice(String userId, String deviceId, String value, String functionId) throws Exception {
        MobiusResponse mobiusResponse;
        ConcurrentHashMap<String, String> conMap = new ConcurrentHashMap<>();

        conMap.put("userId", userId);
        conMap.put("deviceId",deviceId);
        conMap.put("value", value);
        conMap.put("functionId", functionId);
        System.out.println(JSON.toJson(conMap));
        mobiusResponse = mobiusService.createCin("googleAE", "googleCNT", JSON.toJson(conMap));

        return mobiusResponse.getResponseCode();
    }

}
