package com.tphelps.backend.service;

import com.tphelps.backend.repository.EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailRepository emailRepository;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.username}") String sender;
    @Value("${salted.key}") String saltedKey;
    @Value("${front.end.url}") String frontEndUrl;

    public EmailService(JavaMailSender mailSender, EmailRepository emailRepository) {
        this.mailSender = mailSender;
        this.emailRepository = emailRepository;
    }

    /**
     * Send a password reset email to specified email
     * @param email - email for link
     */
    @Async("emailExecutor")
    public void sendPasswordResetEmail(String email){
        try{
            String token = generateAndStoreToken(email);
            String formattedEmailText = String
                    .format("Reset your password here: %s/reset?token=%s", frontEndUrl, token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(email);
            message.setSubject("Password Reset Link");
            message.setText(formattedEmailText);
            mailSender.send(message);
            logger.trace("Email sent for password reset request with trace UUID={} for email={}", token, email);
        }catch(Exception e){
            logger.error("Failed to send email for password reset to email={}",email);
        }
    }

    /**
     * Generate, hash, and store the UUID token inside the db to verify against once the user submits their password change request
     * @param email - email to use for storing token in db
     * @return - a string containing the token to send in the email
     */
    private String generateAndStoreToken(String email){
        String token = UUID.randomUUID().toString();
        String hashedToken = sha256Hex(token + saltedKey);
        emailRepository.storePasswordResetToken(email, hashedToken);
        return token;
    }
}
