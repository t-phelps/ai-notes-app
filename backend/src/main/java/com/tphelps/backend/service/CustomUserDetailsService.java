package com.tphelps.backend.service;

import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.tphelps.backend.dtos.MyUserDetails;
import com.tphelps.backend.dtos.responses.PurchaseHistoryResponseDto;
import com.tphelps.backend.dtos.responses.UserDetailsResponseDto;
import com.tphelps.backend.repository.AccountRepository;
import com.tphelps.backend.jwt.JwtTokenGenerator;
import com.tphelps.backend.repository.AuthenticationRepository;
import com.tphelps.backend.dtos.CreateAccountRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import test.generated.tables.pojos.Users;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthenticationRepository authenticationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final JwtTokenGenerator jwtTokenGenerator;

    @Autowired
    public CustomUserDetailsService(AuthenticationRepository authenticationRepository,
                                    PasswordEncoder passwordEncoder,
                                    AccountRepository accountRepository, JwtTokenGenerator jwtTokenGenerator) {
        this.authenticationRepository = authenticationRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
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
    public void changePassword(UserDetails principal, String oldPassword, String newPassword) {

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
    public ResponseCookie createUser(CreateAccountRequest request) {
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

        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }

        return generateUserCookie(username);
    }

    /**
     * UserDetails service allowance method for fetching user info
     * @param username - the user to fetch info for
     * @return - a populated {@link UserDetailsResponseDto} object
     */
    public UserDetailsResponseDto getUserHistory(String username) {
       return accountRepository.getUserInfo(username);
    }



    public List<PurchaseHistoryResponseDto> getUserPurchaseHistory(String username) {
        return accountRepository.getPurchaseHistory(username);
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
     * Authenticate user against the db
     * @param principal - the username
     * @return - a response cookie for user, else null
     */
    public ResponseCookie generateUserCookie(Object principal) {
        String username = principal.toString();

        String jws = jwtTokenGenerator.getJwt(username);

        // return a response cookie to be stored in the front end
        return ResponseCookie.from("jwt", jws)
                .httpOnly(true)
                .path("/")
                .maxAge(3600)
                .build();
    }
}
