package com.CBTServer.WebCSAT.service;

import com.CBTServer.WebCSAT.config.mail.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class MailService {
    private final MailProperties mailProperties;
    private final JavaMailSender javaMailSender;
    private static int number;

    public static void createNumber() {
        number = (int)(Math.random() * (90000)) + 100000;
    }

    public MimeMessage createMail(String mail) {
        createNumber();
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            message.setFrom(new InternetAddress(mailProperties.getUsername(), "임앤장 CBT", "UTF-8"));
            message.setRecipients(MimeMessage.RecipientType.TO, mail);
            message.setSubject("임앤장 CBT - 이메일 인증 문자 전송");
            String body = "";
            body += "<h3>" + "해당 인증 문자를 보안 문자에 입력해주십시오." + "</h3>";
            body += "<h1>" + number + "</h1>";
            body += "<h3>" + "감사합니다." + "</h3>";
            message.setText(body,"UTF-8", "html");
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return message;
    }

    public int sendMail(String mail) {
        MimeMessage message = createMail(mail);
        javaMailSender.send(message);
        return number;
    }
}