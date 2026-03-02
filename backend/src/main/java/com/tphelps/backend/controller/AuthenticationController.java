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

import java.util.List;
import java.util.Optional;

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

            ResponseCookie accessToken = customUserDetailsService.generateUserCookie(authentication.getPrincipal());
            ResponseCookie refreshToken = customUserDetailsService.generateRefreshToken(authentication.getPrincipal());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, accessToken.toString());
            headers.add(HttpHeaders.SET_COOKIE, refreshToken.toString());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body("User logged in successfully");
        }catch(Exception e){
            return ResponseEntity.internalServerError().body("Failed login: " + e.getMessage());
        }
    }


    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request){
        if(request.email().isEmpty() || request.username().isEmpty() || request.password().isEmpty()) {
            return ResponseEntity.badRequest().body("Failed To Create Account: A field within the request is empty");
        }

        try{
            Optional<List<ResponseCookie>> responseCookieList = customUserDetailsService.createUser(request);
            if(responseCookieList.isEmpty()){
                return ResponseEntity.internalServerError().body("Failed To Generate Cookies While Creating Account");
            }

            ResponseCookie accessToken = responseCookieList.get().get(0);
            ResponseCookie refreshToken = responseCookieList.get().get(1);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, accessToken.toString());
            headers.add(HttpHeaders.SET_COOKIE, refreshToken.toString());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body("Account created successfully");
        }catch(Exception e){
            return ResponseEntity.internalServerError().body("Failed To Create Account");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh_token") String refreshToken){

        if(refreshToken.isEmpty()){
            return ResponseEntity.badRequest().body("Refresh ");
        }

        try{
            ResponseCookie accessToken = customUserDetailsService.refreshAccessToken(refreshToken);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessToken.toString())
                    .body("Token Refreshed");
        }catch(Exception e){
            return ResponseEntity.internalServerError().body("Failed To Refresh Access Token");
        }
    }
}