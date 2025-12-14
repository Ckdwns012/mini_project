package com.example.StreetB;

//import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@MapperScan("com.example.StreetB.Repository") // loginMapper 패키지
@SpringBootApplication
public class StreetBApplication {
	public static void main(String[] args) {
		SpringApplication.run(StreetBApplication.class, args);
	}
}
