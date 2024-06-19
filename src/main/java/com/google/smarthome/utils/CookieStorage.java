package com.google.smarthome.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import lombok.NonNull;

@Component
public class CookieStorage {

    //5분
    private final int maxAge = 60 * 5;

    /**
     * 쿠키 조회
     * https://androi.tistory.com/356
     */
    public String getCookie(@NonNull HttpServletRequest request, String key) {
        if (key != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    if (key.equalsIgnoreCase(name)) {
                        String value = cookie.getValue();
    					/*
    					try {
							value = URLDecoder.decode(value, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}*/
                        return value;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 쿠키 저장
     */
    public void setCookie(HttpServletRequest request, @NonNull String key, String value, HttpServletResponse response) {
        if (key != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    String name = cookie.getName();
                    if (key.equalsIgnoreCase(name)) {
                        cookie.setValue(value);
                        cookie.setPath("/");
                        cookie.setMaxAge(maxAge);
                        response.addCookie(cookie);
                        return;
                    }
                }
            }
            // 엑세스 토큰 쿠키 저장
            Cookie cookie = new Cookie(key, value);
            cookie.setPath("/");
            cookie.setMaxAge(maxAge);
            response.addCookie(cookie);
        }
    }

    /**
     * 쿠키 삭제
     */
    public void removeCookie(@NonNull String key, @NonNull HttpServletResponse response) {
        if (key != null) {
            Cookie cookie = new Cookie(key, null);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }
    }

    /**
     * 모든 쿠키 삭제
     */
    public void clear(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String key = cookie.getName();
                removeCookie(key, response);
            }
        }
    }

}
