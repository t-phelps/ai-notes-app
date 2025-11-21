package com.tphelps.backend.config;

import com.tphelps.backend.jwt.JwtTokenGenerator;
import com.tphelps.backend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    /**
     * Before we get to controller, needs to check if there is a token in the header
     *
     * @param request
     * @param response
     * @param filterChain
     *
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = getJwtFromRequest(request);
        try {
            if (token != null && !token.isEmpty() && jwtTokenGenerator.validateJwt(token)) {

                String username = jwtTokenGenerator.getUsernameFromJwt(token);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken); // stores authentication in thread-local storage (Security context holder)
                // this allows for calling SecurityContextHolder in protected endpoint to get an Authentication object
            }
        }catch(Exception e){
            System.out.println("JWT filter error: " + e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Extract cookies from the servlet request
     * @param request - the {@link HttpServletRequest} request
     * @return - a String containing the cookie
     */
    public String getJwtFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("jwt")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
