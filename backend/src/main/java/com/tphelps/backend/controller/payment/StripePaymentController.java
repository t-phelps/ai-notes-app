package com.tphelps.backend.controller.payment;

import com.google.gson.JsonSyntaxException;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;

import com.tphelps.backend.service.payment.StripePaymentService;
import com.tphelps.backend.service.payment.enums.StripeEventEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.tphelps.backend.controller.authentication.AuthenticationValidator.validateUserAuthentication;
@Controller
@RequestMapping("/stripe")
public class StripePaymentController {

    StripePaymentService stripePaymentService;

    @Autowired
    public StripePaymentController(StripePaymentService  stripePaymentService) {
        this.stripePaymentService = stripePaymentService;
    }

    @Value("${stripe.endpoint.secret}")
    public String ENDPOINT_SECRET;

    /**
     * Create a checkout session for the user which redirects to stripes payment processing page
     * @param key - the request param for the lookup key
     * @return - a map containing the url for the redirect
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestParam("lookup_key") String key){

        Authentication authentication = validateUserAuthentication();
        if(authentication != null) {
            try {
                if (key == null || key.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                Map<String, String> urlMap = stripePaymentService.getCreateCheckoutSessionRedirectUrl(key, userDetails.getUsername());
                return ResponseEntity.ok(urlMap);
            } catch (StripeException e) {
                Map<String, String> errorMap = Map.of("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
            }
        }else{
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You are not authorized to perform this operation"));
        }
    }

    /**
     * Initiate a customer portal session that lets customers manage subscriptions and user details
     * @return - the redirect url for stripes customer portal
     */
    @PostMapping("/create-portal-session")
    public ResponseEntity<?> createPortalSession(){
        Authentication authentication = validateUserAuthentication();
        if(authentication != null){
            try{
                Map<String, String> urlMap = stripePaymentService.getPortalSessionRedirectUrl(authentication);
                return ResponseEntity.ok(urlMap);
            }catch(Exception e){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
            }
        }else{
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "You are not authorized to perform this operation"));
        }
    }

    /**
     * The redirect on a successful payment from the /create-checkout-session redirect
     *
     * This works with a user being redirected to stripe hosted checkout session, once a user pays this endpoint is hit,
     * and we return details we may want to display to the user on the front end
     * @param sessionId - the session id from the checkout session
     * @return response 200 with customer name on success
     */
    @GetMapping("/order/success")
    public ResponseEntity<?> getOrderSuccess(@RequestParam("session_id") String sessionId){
        try {
            Session session = Session.retrieve(sessionId);
            Customer customer = Customer.retrieve(session.getCustomer());
            return ResponseEntity.ok("<html><body><h1>Thanks for your order, " + customer.getName() + "!</h1></body></html>");
        }catch(StripeException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Webhook endpoint so stripe can send events, and we can process them on the server
     * @param payload
     * @param sigHeader - contains the stripe signature which we need to verify
     * @return
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String sigHeader){
        Event event = null;
        try{
            if(payload == null || payload.isEmpty()){
                throw new IllegalArgumentException("payload is null or empty");
            }
            event = ApiResource.GSON.fromJson(payload, Event.class);
        }catch(JsonSyntaxException e){
            System.out.println("Webhook error while parsing basic request");
            return ResponseEntity.badRequest().build();
        }catch(IllegalArgumentException e){
            System.out.println("Payload is null or empty");
            return ResponseEntity.badRequest().build();
        }

        if(sigHeader != null){
            try{
                event = Webhook.constructEvent(payload, sigHeader, ENDPOINT_SECRET);
            }catch(com.stripe.exception.SignatureVerificationException e){
                System.out.println("Webhook error while validating signature");
                return ResponseEntity.badRequest().build();
            }
        }else{
            return ResponseEntity.badRequest().build();
        }

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = null;
        if(dataObjectDeserializer.getObject().isPresent()){
            stripeObject = dataObjectDeserializer.getObject().get();
        }else{
            throw new IllegalStateException("Unable to deserialize event data object for: " + event);
        }

        StripeEventEnum eventType = null;
        for(StripeEventEnum eventEnum : StripeEventEnum.values()){
            if(eventEnum.getValue().equals(event.getType())){
                eventType = eventEnum;
                break;
            }
        }

        // handle event
        Subscription subscription = null;
        try {
            if (eventType != null) {
                switch (eventType) {
                    case DELETED -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
                        stripePaymentService.handleSubscriptionDeleted(subscription);
                    }
                    case TRIAL_WILL_END -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
//                        stripePaymentService.handleTrialSubscriptionEnding(subscription);
                    }
                    case CREATED -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
                        stripePaymentService.handleSubscriptionCreated(subscription);
                    }
                    case UPDATED -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
                        stripePaymentService.handleSubscriptionUpdated(subscription);
                    }
                    case ENTITLEMENT_SUMMARY_UPDATED -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
                        // handleEntitlementUpdated
                    }
                }
            } else {
                throw new IllegalArgumentException("Event type unhandled: " + eventType);
            }
        }catch(EmptyResultDataAccessException | IllegalArgumentException e){
            // do something
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}
