// config/DataInit.java
package com.ijin.hanaro.config;

import com.ijin.hanaro.user.User;
import com.ijin.hanaro.user.UserRepository;
import com.ijin.hanaro.user.User.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInit {
    //관리자계정 자동 생성
    @Bean
    CommandLineRunner init(UserRepository repo, PasswordEncoder enc) {
        return args -> repo.findByUsername("hanaro").orElseGet(() -> {
            User a = new User();
            a.setUsername("hanaro");
            a.setPassword(enc.encode("12345678")); // 임시 비번
            a.setNickname("관리자");
            a.setRole(Role.ROLE_ADMIN);
            a.setEnabled(true);
            return repo.save(a);
        });
    }
}