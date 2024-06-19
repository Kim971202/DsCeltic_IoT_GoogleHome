package com.google.smarthome.controller;

import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FulfillmentController {

    @Autowired
    private GoogleMapper googleMapper;

    String powerStatus = "";

    GoogleDTO deviceStatus;

    @PostMapping("/api/fulfillment")
    public String handleFulfillment(@RequestBody String request) {
        log.info("POST /api/fulfillment CALLED");
        log.info("Request Body: " + request);

        JSONObject requestBody = new JSONObject(request);
        String intent = requestBody.getJSONArray("inputs").getJSONObject(0).getString("intent");

        JSONObject response = new JSONObject();

        switch (intent) {
            case "action.devices.SYNC":
                response = handleSync(requestBody);
                break;
            case "action.devices.EXECUTE":
                response = handleExecute(requestBody);
                break;
            case "action.devices.QUERY":
                response = handleQuery(requestBody);
                break;
            default:
                response.put("error", "Unknown intent");
        }

        return response.toString();
    }

    private JSONObject handleSync(JSONObject requestBody) {
        log.info("handleSync CALLED");
        log.info("requestBody: " + requestBody);

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        payload.put("agentUserId", "userId123");

        JSONArray devices = new JSONArray();
        JSONObject boiler = new JSONObject();
        boiler.put("id", "deviceId123");
        boiler.put("type", "action.devices.types.THERMOSTAT");
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

    private JSONObject handleExecute(JSONObject requestBody) {
        log.info("handleExecute CALLED");
        log.info("requestBody: " + requestBody);

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONArray inputs = requestBody.getJSONArray("inputs");
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

                        switch (commandName) {
                            case "action.devices.commands.OnOff":
                                boolean on = execCommand.getJSONObject("params").getBoolean("on");
                                log.info("Turning " + (on ? "on" : "off") + " device " + deviceId);
                                break;
                            case "action.devices.commands.ThermostatTemperatureSetpoint":
                                double temp = execCommand.getJSONObject("params").getDouble("thermostatTemperatureSetpoint");
                                log.info("Setting temperature of device " + deviceId + " to " + temp);
                                deviceStatus.setTempStatus(String.valueOf(temp));
                                googleMapper.updateDeviceStatus(deviceStatus);
                                break;
                            case "action.devices.commands.ThermostatSetMode":
                                String mode = execCommand.getJSONObject("params").getString("thermostatMode");
                                log.info("Setting mode of device " + deviceId + " to " + mode);
                                if(mode.equals("off")) deviceStatus.setPowrStatus("of");
                                else if(mode.equals("heat")) deviceStatus.setPowrStatus("on");
                                googleMapper.updateDeviceStatus(deviceStatus);
                                break;
                        }
                    });
                });
            });
        });

        JSONObject payload = new JSONObject();
        payload.put("commands", new JSONArray());
        response.put("payload", payload);

        log.info("handleExecute response: " + response);
        return response;
    }
    private JSONObject handleQuery(JSONObject requestBody) {
        log.info("handleQuery CALLED");
        log.info("requestBody: " + requestBody);

        deviceStatus = googleMapper.getInfoByDeviceId("0.2.481.1.1.2045534365636f313353.20202020303833413844434146353435");

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        JSONObject devices = new JSONObject();

        JSONObject deviceState = new JSONObject();
//        deviceState.put("on", true); // The device is ON

        log.info("deviceStatus.getPowrStatus(): " + deviceStatus.getPowrStatus());
        if(!deviceStatus.getPowrStatus().equals("of")) {
            deviceState.put("thermostatMode", "heat"); // Current mode
            log.info("Double.parseDouble(deviceStatus.getTempStatus()): " + Double.parseDouble(deviceStatus.getTempStatus()));
            deviceState.put("thermostatTemperatureSetpoint", Double.parseDouble(deviceStatus.getTempStatus())); // default temp
        } else {
            deviceStatus.setPowrStatus("off");
            log.info("deviceStatus.getPowrStatus(): " + deviceStatus.getPowrStatus());
            deviceState.put("thermostatMode", deviceStatus.getPowrStatus());
        }

        devices.put("deviceId123", deviceState);
        payload.put("devices", devices);
        response.put("payload", payload);

        log.info("handleQuery response: " + response);

        return response;
    }
}