package com.tphelps.backend.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jooq.ExceptionTranslatorExecuteListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthEntryPoint jwtAuthEntryPoint;

    @Autowired
    public SecurityConfig(JwtAuthEntryPoint jwtAuthEntryPoint) {
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
    }

    @Bean
    public AuthenticationManager authenticationManager(UsernamePwdAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    /**
     * SecurityFilterChain method to define which api endpoints we should require authorization on
     * as well as filter per JWT authentication and define our own auth provider
     * @param http - is this the http request coming in from the authorization manager?
     * @return - the built http response
     * @throws Exception - on error
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                  UsernamePwdAuthenticationProvider usernamePwdAuthenticationProvider) throws Exception {
        http
                // disable CSRF since we are using JWT
                .csrf(AbstractHttpConfigurer::disable)
                // configure CORS to allow frontend requests
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.setAllowedOrigins(List.of("http://localhost:3000"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    return config;
                }))
                // handle unauthorized requests
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthEntryPoint))
                // define which endpoints require authentication
                .authorizeHttpRequests(requests -> {
                    requests
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers("/auth/login", "/auth/create", "/stripe/create-checkout-session").permitAll()
                            .requestMatchers("/OTHER______ENDPOINTS").authenticated()
                            .anyRequest().authenticated();
                })
                // disable default login forms
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // make session stateless since JWT is used
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // use custom authentication provider
        http.authenticationProvider(usernamePwdAuthenticationProvider);
        // add our JWT filter before Spring Security's UsernamePasswordAuthenticationFilter
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean for utilizing bcrypt functionalities
     * @return - a Bcrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean for jwt authentication functionalities
     * @return our Jwt auth filter
     * @throws Exception
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        return new JwtAuthenticationFilter();
    }
}

