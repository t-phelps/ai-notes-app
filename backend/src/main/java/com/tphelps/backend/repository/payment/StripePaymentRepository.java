package com.tphelps.backend.repository.payment;

import com.tphelps.backend.dtos.payment.SubscriptionCreationDto;
import com.tphelps.backend.dtos.payment.SubscriptionUpdateDto;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import static test.generated.tables.Users.USERS;
import static test.generated.tables.Subscriptions.SUBSCRIPTIONS;

import test.generated.tables.pojos.Users;

@Repository
public class StripePaymentRepository {

    DSLContext dsl;

    @Autowired
    public StripePaymentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Get the customer id from the users table
     * @param username - the username for which we want the customer id
     * @return - a {@link Users} object
     */
    public String getUserStripeCustomerId(String username){
        return dsl
                .select(USERS.STRIPE_CUSTOMER_ID).from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOneInto(String.class);
    }

    /**
     * Upsert for handling async events, utilizes onConflict() for the unique column latest invoice
     * to detect if the invoice_payment.aid event has come through before customer.subscription.created
     * and deals with the data accordingly
     * @param subscriptionCreationDto - dto containing necessary info for a subscription created insert
     */
    public void upsertUserSubscription(SubscriptionCreationDto subscriptionCreationDto){
        int rowsAffected = dsl.insertInto(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.CUSTOMER_ID, subscriptionCreationDto.customerId())
                .set(SUBSCRIPTIONS.SUBSCRIPTION_ID, subscriptionCreationDto.subscriptionId())
                .set(SUBSCRIPTIONS.START_DATE, subscriptionCreationDto.startDate())
                .set(SUBSCRIPTIONS.CREATED, subscriptionCreationDto.created())
                .set(SUBSCRIPTIONS.CURRENT_PERIOD_START, subscriptionCreationDto.currentPeriodStart())
                .set(SUBSCRIPTIONS.CURRENT_PERIOD_END, subscriptionCreationDto.currentPeriodEnd())
                .set(SUBSCRIPTIONS.PRICE_ID, subscriptionCreationDto.priceId())
                .set(SUBSCRIPTIONS.LATEST_INVOICE, subscriptionCreationDto.latestInvoice())
                .onConflict(SUBSCRIPTIONS.LATEST_INVOICE)
                .doUpdate()
                .set(SUBSCRIPTIONS.SUBSCRIPTION_ID, subscriptionCreationDto.subscriptionId())
                .set(SUBSCRIPTIONS.START_DATE, subscriptionCreationDto.startDate())
                .set(SUBSCRIPTIONS.CREATED, subscriptionCreationDto.created())
                .set(SUBSCRIPTIONS.CURRENT_PERIOD_START, subscriptionCreationDto.currentPeriodStart())
                .set(SUBSCRIPTIONS.CURRENT_PERIOD_END, subscriptionCreationDto.currentPeriodEnd())
                .set(SUBSCRIPTIONS.PRICE_ID, subscriptionCreationDto.priceId())
                .execute();

        if(rowsAffected == 0){
            throw new EmptyResultDataAccessException(1);
        }
    }

    /**
     * Update the users current matching subscription
     * @param subscriptionUpdateDto - the dto containing pertinent update info
     */
    public void updateUserSubscription(SubscriptionUpdateDto subscriptionUpdateDto){
        int rowsAffected = dsl.update(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.STATUS, subscriptionUpdateDto.status())
                .where(SUBSCRIPTIONS.CUSTOMER_ID.eq(subscriptionUpdateDto.customerId()))
                .and(SUBSCRIPTIONS.SUBSCRIPTION_ID.eq(subscriptionUpdateDto.subscriptionId()))
                .execute();

        if(rowsAffected == 0){
            throw new EmptyResultDataAccessException(1);
        }
    }

    /**
     * Upsert users status based on event received from stripe (either payment success or failure)
     * @param customerId - the customer_id from stripe
     * @param invoiceId - invoiceId from stripe invoice obj
     * @param status - "active" the status from stripe
     */
    public void upsertInvoiceEvent(String customerId, String invoiceId, String status){
        int rowsAffected = dsl.insertInto(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.STATUS, status)
                .set(SUBSCRIPTIONS.CUSTOMER_ID, customerId)
                .set(SUBSCRIPTIONS.LATEST_INVOICE, invoiceId)
                .onConflict(SUBSCRIPTIONS.LATEST_INVOICE)
                .doUpdate()
                .set(SUBSCRIPTIONS.STATUS, status)
                .execute();

        if(rowsAffected == 0){
            throw new EmptyResultDataAccessException(1);
        }
    }
}
