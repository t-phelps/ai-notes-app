package com.tphelps.backend.dtos;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for returning custom user details with a stripe id for jOOQ auto-mapping
 */
public class MyUserDetails implements UserDetails {

    private String email;
    private String username;
    private String password;
    private String roles; // store DB column "roles" as String
    private String stripeId;

    // required for jOOQ
    public MyUserDetails() { }

    public MyUserDetails(String email, String username, String password, String roles, String stripeId) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.stripeId = stripeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    /**
     * Maps roles into a list for authorities for user details required method
     * @return list of authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        // split comma-separated roles into GrantedAuthority collection
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStripeId() {
        return stripeId;
    }

    public void setStripeId(String stripeId) {
        this.stripeId = stripeId;
    }
}
