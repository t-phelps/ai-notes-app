package com.tphelps.backend.repository.payment;

import com.tphelps.backend.dtos.payment.SubscriptionInfoDto;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import static test.generated.tables.Users.USERS;
import static test.generated.tables.Subscriptions.SUBSCRIPTIONS;

import test.generated.tables.records.SubscriptionsRecord;
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
    public Users getUserStripeCustomerId(String username){
        return dsl
                .select(USERS.STRIPE_CUSTOMER_ID).from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOneInto(Users.class);
    }

    /**
     * Insert subscription info into the database for a user
     * @param subscriptionInfoDto - dto containing pertinent information about the user
     *                            including the FK for the stripe customer id from USERS table
     */
    public void insertUserSubscription(SubscriptionInfoDto subscriptionInfoDto){
        int rowsAffected = dsl.insertInto(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.CUSTOMER_ID, subscriptionInfoDto.customerId())
                .set(SUBSCRIPTIONS.SUBSCRIPTION_ID, subscriptionInfoDto.subscriptionId())
                .set(SUBSCRIPTIONS.STATUS, subscriptionInfoDto.status())
                .set(SUBSCRIPTIONS.START_DATE, subscriptionInfoDto.startDate())
                .set(SUBSCRIPTIONS.CREATED, subscriptionInfoDto.created())
                .set(SUBSCRIPTIONS.CURRENT_PERIOD_START, subscriptionInfoDto.currentPeriodStart())
                .set(SUBSCRIPTIONS.CURRENT_PERIOD_END, subscriptionInfoDto.currentPeriodEnd())
                .set(SUBSCRIPTIONS.PRICE_ID, subscriptionInfoDto.priceId())
                .execute();
        if(rowsAffected == 0){
            throw new EmptyResultDataAccessException(1);
        }
    }
}
