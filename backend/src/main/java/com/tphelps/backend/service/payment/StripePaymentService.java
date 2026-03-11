package com.tphelps.backend.service.payment;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.PriceListParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.tphelps.backend.dtos.payment.SubscriptionCreationDto;
import com.tphelps.backend.dtos.payment.SubscriptionUpdateDto;
import com.tphelps.backend.repository.AccountRepository;
import com.tphelps.backend.repository.payment.StripePaymentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class StripePaymentService {

    private static Gson gson = new Gson();

    // value does NOT inject into static fields
    // Caching issues and i have no idea why its still caching old test keys
//    @Value("${stripe.api.key}")
//    private String STRIPE_API_KEY;

    @Value("${front.end.url}")
    private String MY_DOMAIN;


    private final StripePaymentRepository stripePaymentRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public StripePaymentService(StripePaymentRepository stripePaymentRepository, StringHttpMessageConverter stringHttpMessageConverter, ListableBeanFactory listableBeanFactory, AccountRepository accountRepository) {
        this.stripePaymentRepository = stripePaymentRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Service method for constructing a url redirect for a stripe checkout session
     * @param key - the key for the object the user is buying
     * @return a map containing the redirect url
     * @throws StripeException - if any stripe api error occurs
     */
    public Map<String, String> getCreateCheckoutSessionRedirectUrl(String key, String username) throws StripeException {
        String stripeKey = System.getenv("STRIPE_TEST_KEY");
        if(stripeKey == null || stripeKey.isEmpty()) {
            throw new IllegalStateException("Environment variable STRIPE_TEST_KEY is not set");
        }
        Stripe.apiKey = stripeKey;

        PriceListParams priceListParams = PriceListParams.builder().addLookupKey(key).build();
        PriceCollection prices = Price.list(priceListParams);// requires a product within stripe dashboard to have a lookup_key assigned to the product
        if(prices.getData().isEmpty()) {
            throw new IllegalStateException("No price list found");
        }

        java.lang.String  stripeCustomerId= stripePaymentRepository.getUserStripeCustomerId(username);

        // create checkout session, controls what customer sees on stripe-hosted payment page
        SessionCreateParams sessionCreateParams = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder().setPrice(prices.getData().get(0).getId()).setQuantity(1L).build())
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(stripeCustomerId) // pass in the customer id generated when user created account
                .setSuccessUrl(MY_DOMAIN + "/landing")
                .build();
        Session session = Session.create(sessionCreateParams);
        return Map.of("url", session.getUrl());
    }

    /**
     * Service method for constructing a redirect url for stripe customer portal
     * @param username - the username for the authenticated Principal
     * @return - map containing the portal session redirect url
     * @throws StripeException - if any stripe api error occurs
     */
    public Map<String, String> getPortalSessionRedirectUrl(String username) throws StripeException {
        String stripeKey = System.getenv("STRIPE_TEST_KEY");
        if(stripeKey == null || stripeKey.isEmpty()) {
            throw new IllegalStateException("Environment variable STRIPE_TEST_KEY is not set");
        }
        Stripe.apiKey = stripeKey;
        java.lang.String  stripeCustomerId= stripePaymentRepository.getUserStripeCustomerId(username);

        com.stripe.param.billingportal.SessionCreateParams params = new com.stripe.param.billingportal.SessionCreateParams.Builder()
                .setCustomer(stripeCustomerId) // stripe customer id from the db to not create a new customer ID on checkout
                .setReturnUrl(MY_DOMAIN + "/landing").build();

        com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);
        return Map.of("url", portalSession.getUrl());
    }

    /**
     * Handle the subscription deleted event (cancelled) and update the subscription status for customer id within the db
     * @param subscription - a subscription object from Stripe
     */
    public void handleSubscriptionDeleted(Subscription subscription){
        updateUserSubscription(subscription);
    }

    /**
     * Handles a subscription created event from the webhook for stripe events
     * Adds {@link SubscriptionCreationDto} into the db
     * @param subscription - a subscription object from Stripe
     */
    public void handleSubscriptionCreated(Subscription subscription){
        SubscriptionItem dataList = subscription.getItems().getData().get(0);
        SubscriptionCreationDto subscriptionCreationDto = new SubscriptionCreationDto(
                subscription.getCustomer(),
                subscription.getId(),
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(subscription.getStartDate()), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(dataList.getCreated()), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(dataList.getCurrentPeriodStart()), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(dataList.getCurrentPeriodEnd()), ZoneOffset.UTC),
                dataList.getPrice().getId(),
                subscription.getLatestInvoice());

        stripePaymentRepository.insertUserSubscription(subscriptionCreationDto);
    }


    /**
     * Handle a subscription updated event from Stripe
     * @param subscription - subscription object from stripe
     */
    public void handleSubscriptionUpdated(Subscription subscription){
        // update the database table SUBSCRIPTIONS with the updated info
        updateUserSubscription(subscription);
    }

    /**
     * Handle an invoice paid webhook from stripe, update their status and end time for subscription in db
     * @param subscription - subscription object from stripe
     */
    public void handleInvoicePaid(Invoice invoice){
//       InvoiceLineItem dataList = invoice.getLines().getData().get(0);
        stripePaymentRepository.insertInvoicePaid(
                invoice.getCustomer(),
                // get subscription ID or more likely invoice ID here since subscription object provides an invoice_id when received as event
                invoice.getId(),
                "ACTIVE");
    }
    /**
     * Extracted method for updating a user subscription in the db
     * @param subscription - the subscription object from stripe
     */
    private void updateUserSubscription(Subscription subscription){
        SubscriptionUpdateDto subscriptionUpdateDto = createSubscriptionUpdateDto(subscription);

        stripePaymentRepository.updateUserSubscription(subscriptionUpdateDto);
    }

    /**
     * Extracted method for creating a {@link SubscriptionUpdateDto}
     * @param subscription - the subscription object from stripe
     * @return a populated subscription update dto
     */
    private SubscriptionUpdateDto createSubscriptionUpdateDto(Subscription subscription){
        return new SubscriptionUpdateDto(
                subscription.getCustomer(),
                subscription.getId(),
                subscription.getStatus());
    }
}
