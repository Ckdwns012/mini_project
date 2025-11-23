// src/main/java/com/example/StreetB/Controller/aiPageController.java
package com.example.StreetB.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/lm/page") // HTML 페이지 요청 경로는 /lm/page/...
public class aiPageController {

    @GetMapping("/aiChat")
    public String chatPage() {
        // templates/aiChat.html 렌더링
        return "aiChat";
    }

    // loginSuccess도 필요하다면 여기에 추가
    @GetMapping("/loginSuccess")
    public String loginSuccessPage() {
        // templates/loginSuccess.html 렌더링
        return "loginSuccess";
    }
}
