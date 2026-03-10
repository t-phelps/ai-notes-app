package com.tphelps.backend.controller;

import com.tphelps.backend.dtos.mail.EmailRequest;
import com.tphelps.backend.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Post endpoint for sending a reset password link to the users email
     * @param emailRequest - user email requesting password reset
     * @return
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody EmailRequest emailRequest){

        try{
            emailService.sendPasswordResetEmail(emailRequest.email());
            return  ResponseEntity.ok().build();
        }catch(Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }
}
