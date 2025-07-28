package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.config.jwt.TokenProvider;
import com.CBTServer.WebCSAT.domain.User;
import com.CBTServer.WebCSAT.dto.QuestionDTO;
import com.CBTServer.WebCSAT.dto.UserDTO;
import com.CBTServer.WebCSAT.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserApiController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/login";
    }

    @GetMapping("/api/user/{email}")
    public ResponseEntity<?> apiUserInfo(@PathVariable String email) {
        try {
            UserDTO dto = userService.apiUserInfo(email);
            return ResponseEntity.ok().body(dto) ;
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "존재하지 않는 유저입니다."));
        }
    }

    @GetMapping("/api/user")
    public ResponseEntity<List<UserDTO>> apiUserInfo() {
        List<UserDTO> dtoList = userService.apiAllUserInfo();
        return ResponseEntity.ok().body(dtoList) ;
    }

    @GetMapping("/api/user/me")
    public ResponseEntity<UserDTO> getMyInfo(HttpServletRequest request) {
        String token = resolveToken(request);
        String email = tokenProvider.getEmail(token); // 토큰에서 이메일 추출

        User user = userService.findByEmail(email);
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        throw new RuntimeException("토큰이 필요합니다.");
    }

    @DeleteMapping("/api/user/me")
    public ResponseEntity<?> withdrawMe(HttpServletRequest request) {
        String token = resolveToken(request);
        String email = tokenProvider.getEmail(token);

        userService.withdrawUserByEmail(email);
        return ResponseEntity.ok("회원탈퇴가 완료되었습니다.");
    }

    @PutMapping("/api/user/{userId}/role")
    public ResponseEntity<?> changeUserRole(@PathVariable Long userId,
                                            @RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        String token = resolveToken(request);
        String requesterEmail = tokenProvider.getEmail(token);

        if (!userService.isAdmin(requesterEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("권한이 없습니다.");
        }

        String newRole = body.get("role");

        if (!List.of("USER", "ADMIN").contains(newRole)) {
            return ResponseEntity.badRequest().body("유효하지 않은 권한입니다.");
        }

        userService.changeRole(userId, newRole);
        return ResponseEntity.ok().body("권한이 변경되었습니다.");
    }
}
