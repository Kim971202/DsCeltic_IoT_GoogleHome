package com.google.smarthome.utils;

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

}
