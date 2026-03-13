package com.tphelps.backend.controller;

import com.tphelps.backend.controller.exceptions.IllegalAccessTokenException;
import com.tphelps.backend.controller.exceptions.IllegalRefreshTokenException;
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

            HttpHeaders headers = buildHttpHeaders(accessToken, refreshToken);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body("User logged in successfully");
        }catch(Exception e){
            return ResponseEntity.internalServerError().body("Failed login: " + e.getMessage());
        }
    }


    /**
     * Endpoint for creating a user account
     * @param request - request dto containing email, username, password
     * @return
     */
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

            HttpHeaders headers = buildHttpHeaders(responseCookieList.get().get(0), responseCookieList.get().get(1));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body("Account created successfully");
        }catch(Exception e){
            return ResponseEntity.internalServerError().body("Failed To Create Account");
        }
    }

    /**
     * Logout user, set cookies to expired
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie accessToken = customUserDetailsService.invalidateAccessTokenCookie();
        ResponseCookie refreshToken = customUserDetailsService.invalidateRefreshTokenCookie();

        HttpHeaders headers = buildHttpHeaders(accessToken, refreshToken);

        return ResponseEntity.ok()
                .headers(headers)
                .body("User cookies invalidated");
    }

    /**
     * Refresh endpoint for refreshing refresh_token with a long-lived token
     * @param refreshToken - long-lived cookie to refresh access_token
     * @return - a new access_token if refresh_cookie hasn't expired
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh_token") String refreshToken){
        try{
            validateRefreshToken(refreshToken);
            ResponseCookie accessToken = customUserDetailsService.refreshAccessToken(refreshToken);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessToken.toString())
                    .body("Token Refreshed");
        }catch(IllegalRefreshTokenException e){
            return ResponseEntity.badRequest().body("Invalid Refresh Token");
        }
        catch(Exception e){
            return ResponseEntity.internalServerError().body("Failed To Refresh Access Token");
        }
    }

    private HttpHeaders buildHttpHeaders(ResponseCookie access_token, ResponseCookie refresh_token){
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, access_token.toString());
        headers.add(HttpHeaders.SET_COOKIE, refresh_token.toString());
        return headers;
    }

    private void validateRefreshToken(String refreshToken) throws IllegalRefreshTokenException {
        if(refreshToken == null || refreshToken.isEmpty()){
            throw new IllegalRefreshTokenException("Invalid Refresh Token");
        }
    }

    private void validateAccessToken(String accessToken) throws IllegalAccessTokenException {
        if(accessToken == null || accessToken.isEmpty()){
            throw new IllegalAccessTokenException("Invalid Access Token");
        }
    }
}