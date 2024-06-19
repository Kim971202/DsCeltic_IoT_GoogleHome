package com.google.smarthome.controller;

import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.service.FulfillmentService;
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
    @Autowired
    private FulfillmentService fulfillmentService;
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
                response = fulfillmentService.handleSync(requestBody);
                break;
            case "action.devices.EXECUTE":
                response = fulfillmentService.handleExecute(requestBody);
                break;
            case "action.devices.QUERY":
                response = fulfillmentService.handleQuery(requestBody);
                break;
            default:
                response.put("error", "Unknown intent");
        }
        return response.toString();
    }

}