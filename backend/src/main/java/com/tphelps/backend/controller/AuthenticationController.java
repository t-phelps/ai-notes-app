package com.tphelps.backend.controller;

import com.tphelps.backend.service.CustomUserDetailsService;
import com.tphelps.backend.dtos.CreateAccountRequest;
import com.tphelps.backend.dtos.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationManager authenticationManager;


    @Autowired
    public AuthenticationController(CustomUserDetailsService customUserDetailsService,
                                    AuthenticationManager authenticationManager) {
        this.customUserDetailsService = customUserDetailsService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Endpoint for logging a user in
     * @param loginRequest - login request containing credentials of the user
     * @return - response containing success or failure and their signed jwt if passed
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if(loginRequest.username().isEmpty() || loginRequest.password().isEmpty()) {
            return ResponseEntity.badRequest().body("Failed Login: A field within the request is empty");
        }

        try{
            Authentication  authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

            ResponseCookie cookie = customUserDetailsService.generateUserCookie(authentication.getPrincipal());
            return  ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body("User login successful");
        }catch(Exception e){
            return ResponseEntity.badRequest().build();
        }
    }


    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request){
        if(request.email().isEmpty() || request.username().isEmpty() || request.password().isEmpty()) {
            return ResponseEntity.badRequest().body("Failed To Create Account: A field within the request is empty");
        }

        try{
            ResponseCookie cookie = customUserDetailsService.createUser(request);
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body("Account created successfully");
        }catch(Exception e){
            return ResponseEntity.badRequest().body("Failed To Create Account");
        }
    }
}