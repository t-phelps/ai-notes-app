package com.tphelps.backend.config;

import com.tphelps.backend.jwt.JwtTokenGenerator;
import com.tphelps.backend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.StringJoiner;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;

    @Autowired
    private RequestAttributeSecurityContextRepository requestAttributeSecurityContextRepository;

    private static final Logger logger =  LoggerFactory.getLogger(JwtAuthenticationFilter.class);

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
                // saves context to be restored within async or other types of threads
                requestAttributeSecurityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
            }
        }catch(Exception e){
            SecurityContextHolder.clearContext();
            logger.error("JWT filter error with message={}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Extract cookies from the servlet request
     * @param request - the {@link HttpServletRequest} request
     * @return - a String containing the cookie
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // tries authorization header first
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        // fall back to cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
