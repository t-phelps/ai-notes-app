package com.tphelps.backend.controller;

import com.tphelps.backend.dtos.mail.EmailRequest;
import com.tphelps.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
public class EmailController {

    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);

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
            logger.info("Initiating password reset request for email={}", emailRequest.email());
            emailService.sendPasswordResetEmail(emailRequest.email());
            return  ResponseEntity.ok().build();
        }catch(Exception e){
            logger.error("Error occurred during password reset request for email={} with exception messsage={}",
                    emailRequest.email(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
