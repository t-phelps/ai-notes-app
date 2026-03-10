package com.tphelps.backend.service;

import com.tphelps.backend.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailRepository emailRepository;

    @Value("${spring.mail.username}") String sender;

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
                    .format("Reset your password here: http://localhost:3000/reset?token=%s", token);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(email);
            message.setSubject("Password Reset Link");
            message.setText(formattedEmailText);
            mailSender.send(message);

        }catch(Exception e){
            System.out.println("Failed to send email for password reset to " + email);
        }
    }

    /**
     * Generate, hash, and store the UUID token inside the db to verify against once the user submits their password change request
     * @param email - email to use for storing token in db
     * @return - a string containing the token to send in the email
     */
    private String generateAndStoreToken(String email){
        String token = UUID.randomUUID().toString();
        String hashedToken = sha256Hex(token);
        emailRepository.storePasswordResetToken(email, hashedToken);
        return token;
    }
}
