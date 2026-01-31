package com.example.gitoo.controller;

import com.example.gitoo.dto.request.ChangePasswordRequest;
import com.example.gitoo.dto.request.DeleteAccountRequest;
import com.example.gitoo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    @PostMapping("/delete-account")
    public ResponseEntity<String> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DeleteAccountRequest request) {
        userService.deleteAccount(userDetails.getUsername(), request.getPassword());
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }
}
