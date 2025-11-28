package com.tphelps.backend.service.payment;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.tphelps.backend.dtos.payment.SubscriptionInfoDto;
import com.tphelps.backend.repository.payment.StripePaymentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import test.generated.tables.pojos.Users;

import java.util.Map;

@Service
public class StripePaymentService {

    private static Gson gson = new Gson();

    @Value("${stripe.api.key}")
    private static String STRIPE_API_KEY;

    @Value("${stripe.endpoint.secret}")
    public static String ENDPOINT_SECRET;

    @Value("${front.end.url}")
    private static String MY_DOMAIN;

    @PostConstruct
    public void init() {
        Stripe.apiKey = STRIPE_API_KEY;
    }

    StripePaymentRepository stripePaymentRepository;

    @Autowired
    public StripePaymentService(StripePaymentRepository stripePaymentRepository, StringHttpMessageConverter stringHttpMessageConverter, ListableBeanFactory listableBeanFactory) {
        this.stripePaymentRepository = stripePaymentRepository;
    }

    /**
     * Service method for constructing a url redirect for a stripe checkout session
     * @param key - the key for the object the user is buying
     * @return a map containing the redirect url
     * @throws StripeException - if any stripe api error occurs
     */
    public Map<String, String> getCreateCheckoutSessionRedirectUrl(String key) throws StripeException {
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
                .setSuccessUrl(MY_DOMAIN + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                .build();
        Session session = Session.create(sessionCreateParams);
        return Map.of("url", session.getUrl());
    }

    /**
     * Service method for constructing a redirect url for stripe customer portal
     * @param authentication - an authentication object containing an already authenticated user
     * @return - map containing the portal session redirect url
     * @throws StripeException - if any stripe api error occurs
     */
    public Map<String, String> getPortalSessionRedirectUrl(Authentication authentication) throws StripeException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        Users user = stripePaymentRepository.getUserStripeCustomerId(username);

        com.stripe.param.billingportal.SessionCreateParams params = new com.stripe.param.billingportal.SessionCreateParams.Builder()
                .setCustomer(user.getUsername())
                .setReturnUrl(MY_DOMAIN).build();

        com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
        return Map.of("url", portalSession.getUrl());
    }


    public void handleTrialSubscriptionEnding(Subscription subscription) {
        // TODO could use this to send a reminder email to user saying their subscription will be over soon and when
        // as well as price details etc
    }

    public void handleSubscriptionDeleted(Subscription Subscription){

    }

    /**
     * Handles a subscription created event from the webhook for stripe events
     * Adds {@link SubscriptionInfoDto} into the db
     * @param subscription - a subscription object
     */
    public void handleSubscriptionCreated(Subscription subscription){
        SubscriptionItem dataList = subscription.getItems().getData().get(0);
        SubscriptionInfoDto subscriptionInfoDto = new SubscriptionInfoDto(
                subscription.getCustomer(),
                subscription.getId(),
                subscription.getStatus(),
                subscription.getStartDate(),
                dataList.getCreated(),
                dataList.getCurrentPeriodStart(),
                dataList.getCurrentPeriodEnd(),
                dataList.getPrice().getId());

        stripePaymentRepository.insertUserSubscription(subscriptionInfoDto);
    }

    public void handleSubscriptionUpdated(Subscription Subscription){
        // update the database table SUBSCRIPTIONS with the updated info
    }

    public void handleEntitlementUpdated(Subscription Subscription){

    }
}
