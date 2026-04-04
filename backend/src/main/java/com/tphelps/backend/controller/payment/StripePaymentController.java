package com.tphelps.backend.controller.payment;

import com.google.gson.JsonSyntaxException;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;

import com.tphelps.backend.service.payment.StripePaymentService;
import com.tphelps.backend.service.payment.enums.StripeEventEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/stripe")
public class StripePaymentController {

    private static final Logger logger =  LoggerFactory.getLogger(StripePaymentController.class);

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
    public ResponseEntity<?> createCheckoutSession(
            @RequestParam("lookup_key") String key,
            @AuthenticationPrincipal UserDetails userDetails){
        try {
            if (key == null || key.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            logger.info("Generating a checkout session in stripe for user={} with lookup_key={}",
                    userDetails.getUsername(), key);

            Map<String, String> urlMap = stripePaymentService
                    .getCreateCheckoutSessionRedirectUrl(key, userDetails.getUsername());

            return ResponseEntity.ok(urlMap);
        } catch (StripeException e) {
            logger.error("Error occurred while generating a stripe checkout session for user={}", userDetails.getUsername());
            Map<String, String> errorMap = Map.of("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMap);
        }
    }

    /**
     * Initiate a customer portal session that lets customers manage subscriptions and user details
     * @return - the redirect url for stripes customer portal
     */
    @PostMapping("/create-portal-session")
    public ResponseEntity<?> createPortalSession(@AuthenticationPrincipal UserDetails userDetails){
        try{
            logger.info("Initiating a stripe customer portal session for user={}", userDetails.getUsername());
            Map<String, String> urlMap = stripePaymentService.getPortalSessionRedirectUrl(userDetails.getUsername());
            return ResponseEntity.ok(urlMap);
        }catch(Exception e){
            logger.error("Error occurred while generating a stripe customer portal session for user={} with exception={}",
                    userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
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
            logger.error("Webhook error while parsing basic request json with exception message={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }catch(IllegalArgumentException e){
            logger.error("Stripe Webhook Payload is null or empty with exception message={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        if(sigHeader != null){
            try{
                event = Webhook.constructEvent(payload, sigHeader, ENDPOINT_SECRET);
            }catch(com.stripe.exception.SignatureVerificationException e){
                logger.error("Webhook error while validating signature");
                return ResponseEntity.badRequest().build();
            }
        }else{
            return ResponseEntity.badRequest().build();
        }

        // idempotency
        if(stripePaymentService.hasProcessedEvent(event.getId())){
            return ResponseEntity.ok().build();
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
        Invoice invoice = null;
        try {
            if (eventType != null) {
                logger.info("Received event type={} with ID={}", eventType, event.getId());
                switch (eventType) {
                    case CUSTOMER_SUBSCRIPTION_DELETED -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
                        stripePaymentService.handleSubscriptionDeleted(subscription);
                    }
                    case CUSTOMER_SUBSCRIPTION_CREATED -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
                        stripePaymentService.handleSubscriptionCreated(subscription);
                    }
                    case CUSTOMER_SUBSCRIPTION_UPDATED -> {
                        subscription = (Subscription) stripeObject;
                        // define and call a function to handle event
                        stripePaymentService.handleSubscriptionUpdated(subscription);
                    }
                    case INVOICE_PAYMENT_SUCCEEDED -> {
                        // this is when we give access to the user: ACTIVE and give a current_period_end
                        invoice =  (Invoice) stripeObject;

                        stripePaymentService.handleInvoicePaid(invoice);
                    }
                    case INVOICE_PAYMENT_FAILURE -> {
                        invoice = (Invoice) stripeObject;

                        stripePaymentService.handleInvoicePaymentFailure(invoice);
                    }
                    case REFUND_CREATED -> {
                        // handle refund created, what type of object will i get here
                    }
                    case CHARGE_REFUNDED -> {
                        // handle charge refunded, what type of object will i get here
                    }
                    case  ISSUING_DISPUTE_FUNDS_RESCINDED -> {
                        // handle funds rescinded, what type of object will i get here
                    }
//                    case ENTITLEMENT_SUMMARY_UPDATED -> {
//                        subscription = (Subscription) stripeObject;
//                        // define and call a function to handle event
//                        // handleEntitlementUpdated
//                    }
                }
            }else{
                logger.info("Unhandled event type={} ", event.getType());
            }
        }catch(EmptyResultDataAccessException | IllegalArgumentException e){
            // do something
            logger.error("Error occurred during stripe webhook processing with exception message={}",
                    e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
        stripePaymentService.insertSuccessfulEventId(event, payload);
        return ResponseEntity.ok().build();
    }
}
