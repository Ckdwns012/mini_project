package com.example.StreetB.Service;

import com.example.StreetB.Repository.loginRepository;
import com.example.StreetB.Util.jwtUtil;
import com.example.StreetB.dto.loginDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class loginService {
    @Autowired
    private loginRepository loginRepository;
    @Autowired
    private jwtUtil jwtUtil;

    public String login(loginDTO loginDTO) {
        if (loginDTO.getPassword().equals(loginRepository.login(loginDTO))) {
            String token = jwtUtil.createToken(loginDTO.getId());
            return token;
        } else {
            return "fail";
        }
    }
    public String signIn(loginDTO loginDTO){
        if(loginRepository.signIn(loginDTO)>0){
            String token = jwtUtil.createToken(loginDTO.getId());
            return token;
        }else{
            return "fail";
        }
    }
    public String checkId(String id){
        if(loginRepository.checkId(id)<1){
            return "success";
        }else{
            return "fail";
        }
    }
}
