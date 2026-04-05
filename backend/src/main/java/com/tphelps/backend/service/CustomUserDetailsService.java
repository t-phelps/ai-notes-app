package com.tphelps.backend.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.tphelps.backend.controller.exceptions.IllegalRefreshTokenException;
import com.tphelps.backend.controller.pojos.SubscriptionData;
import com.tphelps.backend.dtos.MyUserDetails;
import com.tphelps.backend.dtos.responses.PurchaseHistoryResponseDto;
import com.tphelps.backend.dtos.responses.UserDetailsResponseDto;
import com.tphelps.backend.repository.AccountRepository;
import com.tphelps.backend.jwt.JwtTokenGenerator;
import com.tphelps.backend.repository.AuthenticationRepository;
import com.tphelps.backend.dtos.CreateAccountRequest;

import io.jsonwebtoken.JwtException;
import org.apache.commons.codec.digest.DigestUtils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;


import test.generated.tables.pojos.Users;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final DSLContext dslContext;

    @Value("${salted.key}")
    private String saltedKey;

    @Autowired
    public CustomUserDetailsService(AuthenticationRepository authenticationRepository,
                                    PasswordEncoder passwordEncoder,
                                    AccountRepository accountRepository,
                                    JwtTokenGenerator jwtTokenGenerator,
                                    DSLContext dslContext) {
        this.authenticationRepository = authenticationRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.dslContext = dslContext;
    }

    /**
     * Calls repository to find a user by username
     *
     * @param username the username identifying the user whose data is required.
     * @return - a {@link UserDetails} object
     * @throws UsernameNotFoundException - on user not found in database
     */
    @Override
    public MyUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MyUserDetails customer = authenticationRepository.getUser(username);
        if (customer == null) {
            throw new UsernameNotFoundException(username);
        }

        // safely handle null stripeId
        if (customer.getStripeId() == null) {
            customer.setStripeId("");
        }

        return customer;
    }


    /**
     * Service method for changing a users password
     * @param principal - the UserDetails principal object to get the username
     * @param oldPassword - users old password
     * @param newPassword - users new password
     */
    public void changePassword(UserDetails principal, String oldPassword, String newPassword)
            throws UsernameNotFoundException, EmptyResultDataAccessException{

        String username = verifyPassword(principal, oldPassword);
        String encodedPassword =  passwordEncoder.encode(newPassword);
        accountRepository.changePassword(username, encodedPassword);

        // fetch the updated userDetails
        UserDetails updatedUserDetails = loadUserByUsername(username);

        // get the new authentication object
        Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                updatedUserDetails,
                null,
                updatedUserDetails.getAuthorities());

        // reset the security context with the new authentication object
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);
    }

    /**
     * Service Method for a password reset request // cpu intensive with 2 encode calls??
     */
    public void resetPassword(String password, UUID uuid) throws IllegalArgumentException{

        dslContext.transaction(configuration -> {
            DSLContext ctx = DSL.using(configuration);

            String hashedUUID = DigestUtils.sha256Hex(uuid.toString() + saltedKey);

            Integer userId = accountRepository.consumePasswordResetToken(ctx, hashedUUID);
            if(userId == null){
                throw new IllegalArgumentException("Invalid token or expired token");
            }

            String hashedPwd = passwordEncoder.encode(password);
            accountRepository.changePassword(ctx, userId, hashedPwd);
        });
    }

    /**
     * Service method for deleting an account
     * @param principal - the UserDetails object
     * @param password - the password to verify
     */
    public void deleteAccount(UserDetails principal, String password) {
        String username = verifyPassword(principal, password);

        accountRepository.deleteAccount(username);

        // set user to unauthenticated
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    /**
     * Create a user in the database
     * @param request - a {@link CreateAccountRequest} with user details
     * @return a response cookie for user
     */
    public Optional<List<ResponseCookie>> createUser(CreateAccountRequest request) {
        String username = request.username();
        String email  = request.email();
        String password = request.password();
        String role = "USER";

        // hash the password using jBCrypt
        String hashedPassword = passwordEncoder.encode(password);

        try {
            // create the stripe customer to store a customer id in the db to be used in the events later
            // this is to create the link between the users table and the subscriptions table
            Customer customer = Customer.create(
                    CustomerCreateParams.builder()
                            .setEmail(email)
                            .build());

            // save the user to the database
            authenticationRepository.createUser(new Users(
                    null, email, username, hashedPassword, role, LocalDateTime.now(), customer.getId()));

        }catch(StripeException e) {
            logger.error("Stripe Exception occurred while creating a customer object for username={} in the CreateUser request", username);
            return Optional.empty();
        }catch(EmptyResultDataAccessException e){
            logger.error("Exception occurred while creating a user in the database for username={}", username);
            return Optional.empty();
        }

        return Optional.of(List.of(generateUserCookie(username), generateRefreshToken(username)));
    }

    /**
     * UserDetails service allowance method for fetching user info
     * @param username - the user to fetch info for
     * @return - a populated {@link UserDetailsResponseDto} object
     */
    public UserDetailsResponseDto getUserHistory(String username) throws EmptyResultDataAccessException{
       UserDetailsResponseDto userDetailsResponseDto =  accountRepository.getUserInfo(username);
       if(userDetailsResponseDto == null){
           throw new EmptyResultDataAccessException(1);
       }

       return userDetailsResponseDto;
    }


    /**
     * Fetch a user purchase history
     *
     * Will never return null, jooq will map this to an empty list
     * @param username
     * @return a {@link PurchaseHistoryResponseDto}
     */
    public List<PurchaseHistoryResponseDto> getUserPurchaseHistory(String username) {
        return accountRepository.getPurchaseHistory(username);
    }

    /**
     * Get user subscription data from the database
     * @param username
     * @return - a {@link SubscriptionData}
     * @throws EmptyResultDataAccessException - if jooq doesn't find a row
     */
    public SubscriptionData getUserSubscriptionData(String username) throws EmptyResultDataAccessException{
        SubscriptionData subscriptionData = accountRepository.getSubscriptionStatus(username);
        if(subscriptionData == null){
            throw new EmptyResultDataAccessException(1);
        }
        return subscriptionData;
    }

    /**
     * Decrement the amount of generations a user has left for AI feature
     * @param username
     * @param deduction - 1
     */
    public void decrementUserGenerationsLeft(String username, int deduction){
        accountRepository.decrementUserGenerationsLeft(username, deduction);
    }

    /**
     * Verify a users password by principal and password against the db
     * @param principal - user details object of principal
     * @param password - password to check against db
     * @return - the username
     */
    private String verifyPassword(UserDetails principal, String password) {
        String username = principal.getUsername();

        UserDetails userDetails = loadUserByUsername(username);
        if(!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid old password");
        }

        return username;
    }

    /**
     * Generate a jwt for the user
     * @param principal - the username
     * @return - a response cookie for user, else jwt exception
     */
    public ResponseCookie generateUserCookie(Object principal) throws JwtException {
        String username = principal.toString();

        String access_token = jwtTokenGenerator.getJwt(username);

        // return a response cookie to be stored in the front end
        return ResponseCookie.from("access_token", access_token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("None")
                .secure(true)
                .build();
    }

    /**
     * Refresh the access token with the long-lived refresh token provided
     * @param refreshToken - the long-lived token
     * @return - a response cookie containing the
     */
    public ResponseCookie refreshAccessToken(String refreshToken) throws IllegalRefreshTokenException{
        if(!jwtTokenGenerator.validateJwt(refreshToken)){
            throw new IllegalRefreshTokenException("Invalid refresh token");
        }
        String username = jwtTokenGenerator.getUsernameFromJwt(refreshToken);
        String access_token = jwtTokenGenerator.getJwt(username);

        return ResponseCookie.from("access_token", access_token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite("None")
                .secure(true)
                .build();
    }

    /**
     * Refresh the current refresh token
     * @param refreshToken
     * @return
     */
    public ResponseCookie refreshRefreshToken(String refreshToken){
        String username = jwtTokenGenerator.getUsernameFromJwt(refreshToken);
        String refresh_token = jwtTokenGenerator.getRefreshToken(username);

        return ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .secure(true)
                .build();
    }

    /**
     * Generate a long-lived token for the user to refresh against
     * @param principal - the principle object containing details about the authenticated user
     * @return a response cookie containing the refresh token
     */
    public ResponseCookie generateRefreshToken(Object principal) throws JwtException {
        String username = principal.toString();
        String jws = jwtTokenGenerator.getRefreshToken(username);

        return ResponseCookie.from("refresh_token", jws)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None")
                .secure(true)
                .build();
    }

    /**
     * Generate an expired access_token
     * @return - a ResponseCookie that's expired
     */
    public ResponseCookie invalidateAccessTokenCookie() {
       return ResponseCookie.from("access_token", "")
                .maxAge(0)
                .httpOnly(true)
                .path("/")
                .build();
    }

    /**
     * Generate an expired refresh_token
     * @return - a ResponseCookie that's expired
     */
    public ResponseCookie invalidateRefreshTokenCookie() {
        return ResponseCookie.from("refresh_token", "")
                .maxAge(0)
                .httpOnly(true)
                .path("/")
                .build();
    }
}
