package com.tphelps.backend.controller;

import com.tphelps.backend.dtos.ChangePasswordRequest;
import com.tphelps.backend.dtos.DeleteAccountRequest;
import com.tphelps.backend.dtos.responses.PurchaseHistoryResponseDto;
import com.tphelps.backend.dtos.responses.UserDetailsResponseDto;
import com.tphelps.backend.service.CustomUserDetailsService;
import static com.tphelps.backend.controller.authentication.AuthenticationValidator.validateUserAuthentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public AccountController(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Allow a user to change their password while logged in
     * @param changePasswordRequest - the change password request dto containing old and new password
     * @return - ok on success, else unauthorized
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated()){
            try {

                // this has the browser delete the cookie when sent back
                // this does NOT invalidate the cookie that is being deleted by the browser
                // it can still be used, would need to implement a blacklist in db
                ResponseCookie cookie = customUserDetailsService.invalidateUserCookie();

                customUserDetailsService.changePassword(
                        (UserDetails) authentication.getPrincipal(),
                        changePasswordRequest.oldPassword(),
                        changePasswordRequest.newPassword());

                return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();

            }catch(IllegalArgumentException e){
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Delete an account using the users password entered on the front end
     * @param deleteAccountRequest - delete account request dto containing the password
     * @return - <code>200 on success</code>, <code>401 if not authenticated</code>, <code>400 if bad request</code>
     */
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody DeleteAccountRequest deleteAccountRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated()){
            try{

                String password = deleteAccountRequest.password();

                // this has the browser delete the cookie when sent back
                // this does NOT invalidate the cookie that is being deleted by the browser
                // it can still be used, would need to implement a blacklist in db
                ResponseCookie cookie = customUserDetailsService.invalidateUserCookie();

                customUserDetailsService.deleteAccount(
                        (UserDetails) authentication.getPrincipal(),
                        password
                );

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body("Account deleted successfully");
            }catch(IllegalArgumentException e){

                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Get the authenticated users details excluding password and stripe customer id
     * @return status 200 with pertinent user details if authenticated
     */
    @GetMapping("/user-details")
    public ResponseEntity<?> getUserDetails() {

        Authentication authentication = validateUserAuthentication();
        if(authentication != null){
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            UserDetailsResponseDto userDetailsResponseDto = customUserDetailsService.getUserHistory(userDetails.getUsername());

            return ResponseEntity.ok(userDetailsResponseDto);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Endpoint to get the users purchase history
     * @return - a {@link PurchaseHistoryResponseDto} containing the period for subscription and the status
     */
    @GetMapping("/purchase-history")
    public ResponseEntity<?> getPurchaseHistory() {
        Authentication authentication = validateUserAuthentication();
        if(authentication != null){
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<PurchaseHistoryResponseDto> responseDto = customUserDetailsService.getUserPurchaseHistory(userDetails.getUsername());

            if(responseDto != null){
                return ResponseEntity.ok(responseDto);
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
