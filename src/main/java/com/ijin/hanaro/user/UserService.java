// 예: user/UserService.java
package com.ijin.hanaro.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public Long register(String username, String rawPassword, String nickname) {
        if (userRepository.findByUsername(username).isPresent())
            throw new IllegalArgumentException("userId already exists");
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(rawPassword));
        u.setNickname(nickname);
        u.setRole(User.Role.ROLE_USER);
        u.setEnabled(true);
        return userRepository.save(u).getId();
    }
}