package com.tphelps.backend.controller.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthenticationValidator {

    public static Authentication validateUserAuthentication(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated()){
            return authentication;
        }
        return null;
    }
}
