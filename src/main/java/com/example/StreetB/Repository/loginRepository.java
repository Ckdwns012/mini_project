package com.example.StreetB.Repository;

import com.example.StreetB.dto.loginDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class loginRepository {
    @Autowired
    loginMapper loginMapper;

    public String login(loginDTO loginDTO){
        return loginMapper.findById(loginDTO).getPassword();
    }
    public int signIn(loginDTO loginDTO){
        return loginMapper.signIn(loginDTO);
    }
    public int checkId(String id){
        return loginMapper.checkId(id);
    }
}
