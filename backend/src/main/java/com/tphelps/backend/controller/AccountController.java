package com.tphelps.backend.controller;

import com.tphelps.backend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * @param newPassword - the updated password
     * @return - ok on success, else unauthorized
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestParam("newPassword") String newPassword,
                                            @RequestParam("oldPassword") String oldPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated()){
            try {
                customUserDetailsService.changePassword(
                        (UserDetails) authentication.getPrincipal(),
                        oldPassword,
                        newPassword);

                return ResponseEntity.ok("Password Changed Successfully");

            }catch(IllegalArgumentException e){
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Delete an account using the users password entered on the front end
     * @param password - password to match
     * @return - <code>200 on success</code>, <code>401 if not authenticated</code>, <code>400 if bad request</code>
     */
    @PostMapping("delete")
    public ResponseEntity<?> delete(@RequestParam("password") String password) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated()){
            try{
                customUserDetailsService.deleteAccount(
                        (UserDetails) authentication.getPrincipal(),
                        password
                );

                return ResponseEntity.ok().build();
            }catch(IllegalArgumentException e){
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
