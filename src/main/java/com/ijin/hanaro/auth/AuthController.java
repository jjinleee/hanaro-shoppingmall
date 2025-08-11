// auth/AuthController.java
package com.ijin.hanaro.auth;

import com.ijin.hanaro.auth.dto.LoginRequest;
import com.ijin.hanaro.auth.dto.LoginResponse;
import com.ijin.hanaro.user.User;
import com.ijin.hanaro.user.UserRepository;
import com.ijin.hanaro.user.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입/로그인/JWT 발급 API")
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Operation(summary = "로그인", description = "아이디/비밀번호로 인증 후 JWT를 발급합니다. 응답 토큰을 Swagger의 Authorize(Bearer)로 설정하여 이후 API를 호출하세요.")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );

            UserDetails principal = (UserDetails) auth.getPrincipal();
            String role = principal.getAuthorities().iterator().next().getAuthority();
            String token = jwtUtil.generateToken(principal.getUsername(), role);
            long expiresInSec = 30 * 60L; // 30분
            return new LoginResponse(token, "Bearer", expiresInSec);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다");
        }
    }

    /**
     * 회원가입 - ROLE_USER 기본 부여
     */
    @Operation(summary = "회원가입",   description = "기본 권한 ROLE_USER가 부여됩니다.")
    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다");
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setNickname(req.nickname());
        u.setPhone(req.phone());
        u.setRole(User.Role.ROLE_USER);
        u.setEnabled(true);
        Long id = userRepository.save(u).getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }
}