package com.google.smarthome.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Common {

    @Autowired
    private GoogleMapper googleMapper;
    @Autowired
    private PasswordEncoder encoder;

    public boolean checkLogin(String userId, String password) throws Exception{

        GoogleDTO account = googleMapper.getAccountByUserId(userId);

        if (account == null) {
            System.out.println("계정이 존재하지 않습니다.");
        } else {
            if(!encoder.matches(password, account.getUserPassword())){
                System.out.println("PW 에러");
            }
            System.out.println("PW 확인 성공!!");
            return true;
        }

        return false;
    }

    public String readCon(String jsonString, String value) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);

        JsonNode serviceNode = jsonNode.get("md");
        JsonNode baseNode = jsonNode.path("m2m:sgn").path("nev").path("rep").path("m2m:cin");
        JsonNode conNode = baseNode.path("con");
        JsonNode surNode = jsonNode.path("m2m:sgn").path("sur");

        switch (value) {
            case "functionId":
                return serializeAndClean(conNode.path("functionId"), objectMapper, "functionId");
            case "value":
                return serializeAndClean(conNode.path("value"), objectMapper, "value");
            case "deviceId":
                return serializeAndClean(conNode.path("deviceId"), objectMapper, "deviceId");
            case "userId":
                return serializeAndClean(conNode.path("userId"), objectMapper, "userId");
            case "wkTm":
                return serializeAndClean(conNode.path("wkTm"), objectMapper, "wkTm");
            case "msDt":
                return serializeAndClean(conNode.path("msDt"), objectMapper, "msDt");
            case "con":
                return serializeAndClean(baseNode, objectMapper, "con");
            case "sur":
                return serializeAndClean(surNode, objectMapper, "sur");
            case "md":
                return serializeAndClean(conNode.path("rsCf").path("24h").path("md"), objectMapper, "md");
            case "serviceMd":
                return serializeAndClean(serviceNode, objectMapper, "serviceMd");
            case "24h":
                return serializeAndClean(conNode.path("rsCf").path("24h"), objectMapper, "24h");
            case "12h":
                return serializeAndClean(conNode.path("rsCf").path("12h"), objectMapper, "12h");
            case "7wk":
                return serializeAndClean(conNode.path("rsCf").path("7wk"), objectMapper, "7wk");
            case "fwh":
                return serializeAndClean(conNode.path("rsCf").path("fwh"), objectMapper, "fwh");
            case "rsSl":
                return serializeAndClean(conNode.path("rsCf").path("rsSl"), objectMapper, "rsSl");
            case "rsPw":
                return serializeAndClean(conNode.path("rsCf").path("rsPw"), objectMapper, "rsPw");
            case "24h_old":
                return serializeAndClean(jsonNode.path("24h"), objectMapper, "24h_old");
            case "12h_old":
                return serializeAndClean(jsonNode.path("12h"), objectMapper, "12h_old");
            case "7wk_old":
                return serializeAndClean(jsonNode.path("7wk"), objectMapper, "7wk_old");
            default:
                return serializeAndClean(conNode.path(value), objectMapper, "DEFAULT");
        }
    }

    private String serializeAndClean(JsonNode node, ObjectMapper mapper, String key) throws Exception {
        if (node == null || node.isMissingNode()) {
            return null;
        }

        String serializedValue = mapper.writeValueAsString(node);

        // fwh, rsSl, rsPw 인 경우 그대로 반환
        if ("fwh".equals(key) || "rsSl".equals(key) || "rsPw".equals(key)) {
            return serializedValue;  // 변형 없이 그대로 반환
        }

        return serializedValue.replace("\"", "");
    }

}
