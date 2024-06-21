package com.google.smarthome.controller;

import com.google.smarthome.mapper.GoogleMapper;
import com.google.smarthome.service.MobiusService;
import com.google.smarthome.utils.Common;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
public class MobiusController {

    @Autowired
    private MobiusService mobiusService;
    @Autowired
    private Common common;
    @Autowired
    private GoogleMapper googleMapper;

    @GetMapping({"/mobius_code"})
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public String authorizationCode(HttpSession session, HttpServletRequest request,
                                    @RequestParam(name = "username", required = false) String username,
                                    @RequestParam(name = "password", required = false) String password,
                                    Model model, HttpServletResponse response) throws Exception {

        // username과 password 출력 (테스트용)
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        common.checkLogin(username, password);

        mobiusService.createAe("googleAE");
        mobiusService.createCnt("googleAE", "googleCNT");
        mobiusService.createSub("googleAE", "googleCNT");

        mobiusService.createCin("googleAE", "googleCNT", "THIS IS CON");
        return "OK";
    }

}
