package com.example.gitoo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "forward:/main.html"; // 루트 접속 시 로그인 페이지로 이동
    }

    @GetMapping("/login")
    public String loginPage() {
        return "forward:/login.html"; // src/main/resources/templates/login.html 을 찾아갑니다.
    }

    @GetMapping("/signup")
    public String signup() {
        return "forward:/signup.html";
    }

    @GetMapping("/main")
    public String mainPage() {
        return "forward:/main.html";
    }
}