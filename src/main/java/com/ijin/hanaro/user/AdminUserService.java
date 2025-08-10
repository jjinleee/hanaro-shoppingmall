package com.ijin.hanaro.user;

import com.ijin.hanaro.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsers(String q, Pageable pageable) {
        // 항상 ROLE_USER만
        Page<User> page = userRepository.searchUsers(q, User.Role.ROLE_USER, pageable);

        // DTO 매핑: id, nickname, phone, username, enabled (요청하신 순서)
        return page.map(u -> new UserSummaryResponse(
                u.getId(),
                u.getNickname(),
                u.getPhone(),
                u.getUsername(),
                u.isEnabled()
        ));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("회원이 존재하지 않습니다");
        }
        userRepository.deleteById(id);
    }
}