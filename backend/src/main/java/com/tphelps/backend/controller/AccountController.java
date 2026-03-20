package com.tphelps.backend.controller;

import com.tphelps.backend.dtos.ChangePasswordRequest;
import com.tphelps.backend.dtos.DeleteAccountRequest;
import com.tphelps.backend.dtos.PasswordResetRequest;
import com.tphelps.backend.dtos.responses.PurchaseHistoryResponseDto;
import com.tphelps.backend.dtos.responses.UserDetailsResponseDto;
import com.tphelps.backend.service.CustomUserDetailsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/account")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public AccountController(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Allow a user to change their password while logged in
     *
     * @param changePasswordRequest - the change password request dto containing old and new password
     * @return - ok on success, else unauthorized
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest changePasswordRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            logger.trace("Changing password for user={} at UTC time={}",
                    userDetails.getUsername(), Instant.now());
            // this has the browser delete the cookie when sent back
            // this does NOT invalidate the cookie that is being deleted by the browser
            // it can still be used, would need to implement a blacklist in db
            ResponseCookie accessToken = customUserDetailsService.invalidateAccessTokenCookie();
            ResponseCookie refreshToken = customUserDetailsService.invalidateRefreshTokenCookie();

            customUserDetailsService.changePassword(
                    userDetails,
                    changePasswordRequest.oldPassword(),
                    changePasswordRequest.newPassword());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, accessToken.toString());
            headers.add(HttpHeaders.SET_COOKIE, refreshToken.toString());

            return ResponseEntity.ok().headers(headers).build();

        } catch (IllegalArgumentException | UsernameNotFoundException e) {
            logger.error("Password change failed for user={}", userDetails.getUsername());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endpoint for resetting a password through the password reset request email
     * @param passwordResetRequest - dto containing UUID and password
     * @return - <code> 200 on success </code>
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) {
        try{

            if(isInvalidPasswordResetRequest(passwordResetRequest)){
                return ResponseEntity.badRequest().build();
            }
            logger.trace("Password reset request for user with trace UUID={} at UTC time={}",
                    passwordResetRequest.uuid(), Instant.now());
            customUserDetailsService.resetPassword(passwordResetRequest.password(), passwordResetRequest.uuid());

            return ResponseEntity.ok().build();
        }catch(Exception e){
            logger.error("Failed password reset request for user with trace UUID={}", passwordResetRequest.uuid());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete an account using the users password entered on the front end
     *
     * @param deleteAccountRequest - delete account request dto containing the password
     * @return - <code>200 on success</code>, <code>401 if not authenticated</code>, <code>400 if bad request</code>
     */
    @PostMapping("/delete")
    public ResponseEntity<?> delete(
            @RequestBody DeleteAccountRequest deleteAccountRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            logger.trace("Account deletion request initiated for user={} at UTC time={}",
                    userDetails.getUsername(), Instant.now());

            String password = deleteAccountRequest.password();

            // this has the browser delete the cookie when sent back
            // this does NOT invalidate the cookie that is being deleted by the browser
            // it can still be used, would need to implement a blacklist in db
            ResponseCookie accessToken = customUserDetailsService.invalidateAccessTokenCookie();
            ResponseCookie refreshToken = customUserDetailsService.invalidateRefreshTokenCookie();

            customUserDetailsService.deleteAccount(
                    userDetails,
                    password
            );

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, accessToken.toString());
            headers.add(HttpHeaders.SET_COOKIE, refreshToken.toString());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body("Account deleted successfully");
        } catch (IllegalArgumentException e) {
            logger.error("Account deletion failed for user={} with exception message={}",
                    userDetails.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get the authenticated users details excluding password and stripe customer id
     *
     * @return status 200 with pertinent user details if authenticated
     */
    @GetMapping("/user-details")
    public ResponseEntity<?> getUserDetails(@AuthenticationPrincipal UserDetails userDetails) {
        UserDetailsResponseDto userDetailsResponseDto = customUserDetailsService.getUserHistory(userDetails.getUsername());
        return ResponseEntity.ok().body(userDetailsResponseDto);
    }

    /**
     * Endpoint to get the users purchase history
     *
     * @return - a {@link PurchaseHistoryResponseDto} containing the period for subscription and the status
     */
    @GetMapping("/purchase-history")
    public ResponseEntity<?> getPurchaseHistory(@AuthenticationPrincipal UserDetails userDetails) {
        List<PurchaseHistoryResponseDto> purchaseHistoryResponseList = customUserDetailsService.getUserPurchaseHistory(userDetails.getUsername());
        if (purchaseHistoryResponseList != null) {
            return ResponseEntity.ok().body(purchaseHistoryResponseList);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Validate the PasswordResetRequest
     * @param passwordResetRequest - dto
     * @return - true if request meets minimum requirements
     */
    private boolean isInvalidPasswordResetRequest(PasswordResetRequest passwordResetRequest) {
        return passwordResetRequest.password() == null
                || passwordResetRequest.password().isEmpty()
                || passwordResetRequest.uuid() == null
                || passwordResetRequest.uuid().toString().isEmpty();
    }
}
