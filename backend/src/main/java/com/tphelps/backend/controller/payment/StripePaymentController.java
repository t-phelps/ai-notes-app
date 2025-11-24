package com.tphelps.backend.controller.payment;

import com.google.gson.Gson;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.model.*;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import com.stripe.param.PriceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.checkout.Session;

import com.tphelps.backend.service.payment.enums.StripeEventEnum;
import com.tphelps.backend.repository.payment.StripePaymentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import test.generated.tables.pojos.Users;

import java.util.Map;

@Controller
@RequestMapping("/stripe")
public class StripePaymentController {

    StripePaymentRepository stripePaymentRepository;

    private static Gson gson = new Gson();

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.endpoint.secret}")
    private String endpointSecret;

    @Value("${front.end.url}")
    private String myDomain;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @Autowired
    public StripePaymentController(StripePaymentRepository stripePaymentRepository) {
        this.stripePaymentRepository = stripePaymentRepository;
    }

    /**
     * Create a checkout session for the user which redirects to stripes payment processing page
     * @param key - the request param for the lookup key
     * @return - a map containing the url for the redirect
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestParam("lookup_key") String key) throws Exception {
        try {
            PriceListParams priceListParams = PriceListParams.builder().addLookupKey(key).build();
            PriceCollection prices = Price.list(priceListParams);// requires a product within stripe dashboard to have a lookup_key assigned to the product
            if(prices.getData().isEmpty()) {
                throw new IllegalStateException("No price list found");
            }

            // create checkout session, controls what customer sees on stripe-hosted payment page
            SessionCreateParams sessionCreateParams = SessionCreateParams.builder()
                    .addLineItem(
                            SessionCreateParams.LineItem.builder().setPrice(prices.getData().get(0).getId()).setQuantity(1L).build())
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(myDomain + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                    .build();
            Session session = Session.create(sessionCreateParams);
            Map<String, String> urlMap = Map.of("url", session.getUrl());
            return ResponseEntity.ok(urlMap);
        }catch(Exception e) {
            Map<String, String> errorMap = Map.of("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }

    /**
     * Initiate a customer portal session that lets customers manage subscriptions and user details
     * @param request - the http servlet request object
     * @return - the redirect url for stripes customer portal
     */
    @PostMapping("/create-portal-session")
    public ResponseEntity<?> createPortalSession(HttpServletRequest request){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated()){
            try{
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();

                Users user = stripePaymentRepository.getUserStripeCustomerId(username);

                com.stripe.param.billingportal.SessionCreateParams params = new com.stripe.param.billingportal.SessionCreateParams.Builder()
                        .setCustomer(user.getUsername())
                        .setReturnUrl(myDomain).build();

                com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
                return ResponseEntity.ok(Map.of("url", portalSession.getUrl()));
            }catch(Exception e){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
            }
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(HttpServletRequest request) throws Exception {
        // TODO i know this is a body im receiving but have no idea what is supposed to be in it
        // todo read about the method ApiResource.GSON.fromJson()
        String payload = request.toString();
        Event event = null;
        try{
            event = ApiResource.GSON.fromJson(payload, Event.class);
        }catch(JsonSyntaxException e){
            System.out.println("Webhook error while parsing basic request");
            return ResponseEntity.badRequest().build();
        }

        String sigHeader = request.getHeader("Stripe-Signature");
        if(sigHeader != null){
            try{
                event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            }catch(com.stripe.exception.SignatureVerificationException e){
                System.out.println("Webhook error while validating signature");
                return ResponseEntity.badRequest().build();
            }
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
        if(eventType != null) {
            switch (eventType) {
                case DELETED -> {
                    subscription = (Subscription) stripeObject;
                    // define and call a function to handle event
                    // handleSubscriptionTrialEnding(subscription)
                }
                case TRIAL_WILL_END -> {
                    subscription = (Subscription) stripeObject;
                    // define and call a function to handle event
                    // handleSubscriptionDeleted(subscriptionDeleted)
                }
                case CREATED -> {
                    subscription = (Subscription) stripeObject;
                    // define and call a function to handle event
                    // handleSubscriptionCreated(subscription)
                }
                case UPDATED -> {
                    subscription = (Subscription) stripeObject;
                    // define and call a function to handle event
                    // handleSubscriptionUpdated(subscription)
                }
                case ENTITLEMENT_SUMMARY_UPDATED -> {
                    subscription = (Subscription) stripeObject;
                    // define and call a function to handle event
                    // handleEntitlementUpdated
                }
            }
        }else{
            System.out.println("Unhandled event type: " + eventType);
        }

        return ResponseEntity.ok().build();
    }

    private void handleTrialSubscriptionEnding(Subscription subscription) {

    }

    private void handleSubscriptionDeleted(Subscription Subscription){

    }

    private void handleSubscriptionCreated(Subscription subscription){

    }

    private void handleSubscriptionUpdated(Subscription Subscription){

    }

    private void handleEntitlementUpdated(Subscription Subscription){

    }

}
