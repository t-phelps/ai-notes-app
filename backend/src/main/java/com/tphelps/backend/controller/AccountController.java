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
import org.springframework.dao.EmptyResultDataAccessException;
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

        if(isInvalidChangePasswordRequest(changePasswordRequest)){
            logger.error("Invalid change password request for user={}", userDetails.getUsername());
            return ResponseEntity.badRequest().build();
        }
        try {
            logger.info("Changing password for user={} at UTC time={}",
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

        } catch (UsernameNotFoundException e) {
            logger.error("Password change failed, user not found in database, for user={}", userDetails.getUsername());
            return ResponseEntity.internalServerError().build(); // internal server error because this is from the account page so user should exist
        } catch(EmptyResultDataAccessException e){
            logger.error("Password change failed for user={} with exception={}", userDetails.getUsername(), e.getMessage());
            return  ResponseEntity.internalServerError().build();
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
                logger.error("Invalid password reset request object received by the controller");
                return ResponseEntity.badRequest().build();
            }
            logger.info("Password reset request for user with trace UUID={} at UTC time={}",
                    passwordResetRequest.uuid(), Instant.now());
            customUserDetailsService.resetPassword(passwordResetRequest.password(), passwordResetRequest.uuid());

            return ResponseEntity.ok().build();
        }catch(IllegalArgumentException e){
            logger.error("Failed password reset request for user with trace UUID={} and exception={}",
                    passwordResetRequest.uuid(),
                    e.getMessage());
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
        if(isInvalidDeleteAccountRequest(deleteAccountRequest)){
            logger.error("Invalid delete account request for user={}", userDetails.getUsername());
            return ResponseEntity.badRequest().build();
        }
        try {
            logger.info("Account deletion request initiated for user={} at UTC time={}",
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
        } catch (EmptyResultDataAccessException e) {
            logger.error("Account deletion failed for user={} with exception message={}",
                    userDetails.getUsername(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get the authenticated users details excluding password and stripe customer id
     *
     * @return status 200 with pertinent user details if authenticated
     */
    @GetMapping("/user-details")
    public ResponseEntity<UserDetailsResponseDto> getUserDetails(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            logger.trace("Fetching user details for user={}", userDetails.getUsername());
            UserDetailsResponseDto userDetailsResponseDto = customUserDetailsService.getUserHistory(userDetails.getUsername());
            return ResponseEntity.ok().body(userDetailsResponseDto);
        }catch(EmptyResultDataAccessException e){
            logger.error("Excpetion occurred while fetching user details for user={} with exception={}",
                    userDetails.getUsername(),
                    e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint to get the users purchase history
     *
     * @return - a {@link PurchaseHistoryResponseDto} containing the period for subscription and the status
     */
    @GetMapping("/purchase-history")
    public ResponseEntity<List<PurchaseHistoryResponseDto>> getPurchaseHistory(@AuthenticationPrincipal UserDetails userDetails) {
        logger.trace("Fetching user purchase history for user={}", userDetails.getUsername());
        List<PurchaseHistoryResponseDto> purchaseHistoryResponseList =
                customUserDetailsService.getUserPurchaseHistory(userDetails.getUsername());

        return  ResponseEntity.ok().body(purchaseHistoryResponseList);
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

    private boolean isInvalidChangePasswordRequest(ChangePasswordRequest changePasswordRequest) {
        return changePasswordRequest.newPassword() == null
                || changePasswordRequest.newPassword().isEmpty()
                ||  changePasswordRequest.oldPassword() == null
                || changePasswordRequest.oldPassword().isEmpty();
    }

    private boolean isInvalidDeleteAccountRequest(DeleteAccountRequest deleteAccountRequest) {
        return deleteAccountRequest.password() == null
                || deleteAccountRequest.password().isEmpty();
    }
}
