package com.example.StreetB.Controller;

import com.example.StreetB.dto.loginDTO;
import com.example.StreetB.Service.loginService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class loginController {
    @Autowired
    private loginService loginService;

//    로그인
    @RequestMapping("loginPage")
    public String loginPage(Model model){
        model.addAttribute("loginDTO", new loginDTO());
        return "loginPage";
    }
    @RequestMapping("login")
    public String login(@ModelAttribute loginDTO loginDTO, Model model, HttpServletResponse response){
        String token = loginService.login(loginDTO);

        if (token != null) {
            Cookie cookie = new Cookie("accessToken", token);
            cookie.setHttpOnly(true); // XSS 방지
            cookie.setPath("/");
            response.addCookie(cookie);
            model.addAttribute("message","sign in success");
            return "redirect:/aiChatPage";
        }else{
            model.addAttribute("message","login fail");
            return "loginPage";
        }
    }
//    회원가입
    @RequestMapping("signUp")
    public String signUp(){
        return "signUpPage";
    }
    @RequestMapping("signIn")
    public String signIn(@ModelAttribute loginDTO loginDTO, Model model, HttpServletResponse response){

        String token = loginService.signIn(loginDTO);

        if (token != null) {
            Cookie cookie = new Cookie("accessToken", token);
            cookie.setHttpOnly(true); // XSS 방지
            cookie.setPath("/");
            response.addCookie(cookie);
            model.addAttribute("message","sign in success");
            return "redirect:/aiChatPage";
        }else{
            model.addAttribute("message","sign in fail");
            return "signUpPage";
        }
    }
    @RequestMapping("checkId")
    @ResponseBody
    public String checkId(@RequestParam("id") String id){
        return loginService.checkId(id);
    }

    @RequestMapping("/aiChatPage")
    public String aiChatPage() {
        return "aiChatPage";
    }
}
