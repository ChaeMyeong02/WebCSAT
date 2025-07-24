package com.CBTServer.WebCSAT.controller;

import com.CBTServer.WebCSAT.service.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class MailApiController {
    private final MailService mailService;

    @PostMapping("/api/mail")
    public ResponseEntity<String> sendEmail(@RequestParam String email) throws MessagingException {
        mailService.sendMail(email);
        return ResponseEntity.status(HttpStatus.OK).body("인증 코드가 전송되었습니다.");
    }
}