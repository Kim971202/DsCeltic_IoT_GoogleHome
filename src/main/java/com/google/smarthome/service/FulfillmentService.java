package com.google.smarthome.service;

import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FulfillmentService {

    @Autowired
    private GoogleMapper googleMapper;
    GoogleDTO deviceStatus;

    public JSONObject handleSync(JSONObject requestBody) {
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
    public JSONObject handleExecute(JSONObject requestBody) {
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
    public JSONObject handleQuery(JSONObject requestBody) {
        log.info("handleQuery CALLED");
        log.info("requestBody: " + requestBody);

        deviceStatus = googleMapper.getInfoByDeviceId("0.2.481.1.1.2045534365636f313353.20202020303833413844434146353435");

        JSONObject response = new JSONObject();
        response.put("requestId", requestBody.getString("requestId"));

        JSONObject payload = new JSONObject();
        JSONObject devices = new JSONObject();

        JSONObject deviceState = new JSONObject();

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
