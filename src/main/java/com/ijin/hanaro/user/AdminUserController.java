package com.ijin.hanaro.user;

import com.ijin.hanaro.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // 관리자 회원 목록 (검색/페이징)
    @GetMapping("/admin/users")
    public Page<UserSummaryResponse> list(@RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return adminUserService.getUsers(q, PageRequest.of(page, size, Sort.by("id").descending()));
    }

    // 관리자 회원 삭제
    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}