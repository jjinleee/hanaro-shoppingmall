package com.ijin.hanaro.user;

import com.ijin.hanaro.user.dto.UserSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "관리자용 회원 목록/삭제 API")
public class AdminUserController {

    private final AdminUserService adminUserService;

    // 관리자 회원 목록 (검색/페이징)
    @Operation(summary = "회원 목록 조회(관리자)", description = "ROLE_USER만 조회, q(username/nickname/phone) 검색, page/size 페이징")
    @GetMapping("/admin/users")
    public Page<UserSummaryResponse> list(@RequestParam(required = false) String q,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return adminUserService.getUsers(q, PageRequest.of(page, size, Sort.by("id").descending()));
    }

    // 관리자 회원 삭제
    @Operation(summary = "회원 삭제(관리자)", description = "회원 단건 삭제")
    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}