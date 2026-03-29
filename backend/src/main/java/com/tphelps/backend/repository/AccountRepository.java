package com.tphelps.backend.repository;


import com.tphelps.backend.controller.pojos.SubscriptionData;
import com.tphelps.backend.dtos.notes.UserNoteDto;
import com.tphelps.backend.dtos.responses.PurchaseHistoryResponseDto;
import com.tphelps.backend.dtos.responses.UserDetailsResponseDto;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import static test.generated.tables.Users.USERS;
import static test.generated.tables.PasswordResetTokens.PASSWORD_RESET_TOKENS;

import java.time.OffsetDateTime;
import java.util.List;

import static test.generated.tables.UserNoteHistory.USER_NOTE_HISTORY;
import static test.generated.tables.Subscriptions.SUBSCRIPTIONS;

@Repository
public class AccountRepository {

    private final DSLContext dslContext;

    public AccountRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /**
     * Change an existing users password
     * @param username - user to match
     * @param hashedPassword - the new password
     * @throws IllegalArgumentException - if user not found
     */
    public void changePassword(String username, String hashedPassword) throws IllegalArgumentException {
        int rowsAffected = dslContext.update(USERS)
                .set(USERS.PASSWORD, hashedPassword)
                .where(USERS.USERNAME.eq(username))
                .execute();

        if (rowsAffected == 0) {
            throw new IllegalArgumentException("User not found");
        }
    }

    /**
     * Overloaded method for changing the password based off the userId
     * @param userId - userId to change pwd for
     * @param hashedPassword - hashed password
     * @throws IllegalArgumentException - if user not found
     */
    public void changePassword(DSLContext ctx, int userId, String hashedPassword) throws IllegalArgumentException {
        int rowsAffected = ctx.update(USERS)
                .set(USERS.PASSWORD, hashedPassword)
                .where(USERS.ID.eq(userId))
                .execute();

        if(rowsAffected == 0) {
            throw new IllegalArgumentException("User not found");
        }
    }

    /**
     * Delete a user from the db
     * @param username - user to delete
     * @throws IllegalArgumentException - if user not found
     */
    public void deleteAccount(String username) throws IllegalArgumentException {
        int rowsAffected = dslContext.deleteFrom(USERS)
                .where(USERS.USERNAME.eq(username))
                .execute();

        if (rowsAffected == 0) {
            throw new IllegalArgumentException("User not found");
        }
    }

    /**
     * Fetch all pertinent user info to store in the front end to reduce repository calls when switching from page to page
     * Get link to note and saved at and store into a list of {@link UserNoteDto} which is stored in a single {@link UserDetailsResponseDto} object
     * @param username - username to match on
     * @return - a single {@link UserDetailsResponseDto}
     */
    public UserDetailsResponseDto getUserInfo(String username){

        List<UserNoteDto> userNotesDto = dslContext.select(USER_NOTE_HISTORY.LINK_TO_NOTE, USER_NOTE_HISTORY.SAVED_AT)
                .from(USER_NOTE_HISTORY)
                .where(USER_NOTE_HISTORY.USERNAME.eq(username))
                .fetch(row -> new UserNoteDto(
                        row.get(USER_NOTE_HISTORY.LINK_TO_NOTE),
                        row.get(USER_NOTE_HISTORY.SAVED_AT)));

        var row = dslContext
                .select(USERS.USERNAME, USERS.EMAIL)
                .from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOne();

        return new  UserDetailsResponseDto(
                row.get(USERS.EMAIL),
                row.get(USERS.USERNAME),
                userNotesDto);
    }

    /**
     * Get the users purchase history based on their username
     * @param username - username to match on
     * @return - a list of {@link PurchaseHistoryResponseDto}
     */
    public List<PurchaseHistoryResponseDto> getPurchaseHistory(String username){
        return dslContext.select(
                        SUBSCRIPTIONS.CURRENT_PERIOD_START.as("current_period_start"),
                        SUBSCRIPTIONS.CURRENT_PERIOD_END.as("current_period_end"),
                        SUBSCRIPTIONS.STATUS.as("status"))
                .from(USERS)
                .join(SUBSCRIPTIONS)
                .on(USERS.STRIPE_CUSTOMER_ID.eq(SUBSCRIPTIONS.CUSTOMER_ID))
                .where(USERS.USERNAME.eq(username))
                .fetchInto(PurchaseHistoryResponseDto.class);
    }

    /**
     * Fetch the user id for the password reset request that meets the requirements set
     * (request must be fulfilled within 1 hour from email sent, same UUID, token valid for 1 time use)
     * and then set the USED field to TRUE only IF there was a match
     * @param hashedUUID - hashed UUID to match on
     */
    public Integer consumePasswordResetToken(DSLContext ctx, String hashedUUID){
        return ctx.update(PASSWORD_RESET_TOKENS)
                .set(PASSWORD_RESET_TOKENS.USED, true)
                .where(PASSWORD_RESET_TOKENS.HASHED_TOKEN.eq(hashedUUID))
                .and(PASSWORD_RESET_TOKENS.EXPIRES_AT.greaterThan(OffsetDateTime.now()))
                .and(PASSWORD_RESET_TOKENS.USED.eq(false))
                .returning(PASSWORD_RESET_TOKENS.USER_ID)
                .fetchOne(PASSWORD_RESET_TOKENS.USER_ID);
    }


    /**
     * Get the subscription status for that user, will either be the farthest one in the future OR
     * closest one in the pass (a user can have a subscription that expired, and then a new active one)
     * @param username - user from authenticated obj
     * @return - the subscription status
     */
    public SubscriptionData getSubscriptionStatus(String username){
        return dslContext.select(SUBSCRIPTIONS.STATUS, SUBSCRIPTIONS.GENERATIONS_LEFT)
                .from(SUBSCRIPTIONS)
                .join(USERS).on(USERS.STRIPE_CUSTOMER_ID.eq(SUBSCRIPTIONS.CUSTOMER_ID))
                .where(USERS.USERNAME.eq(username))
                .orderBy(
                        DSL.when(SUBSCRIPTIONS.CURRENT_PERIOD_END.ge(OffsetDateTime.now()), 0)
                                .otherwise(1),                       // prioritize future subscriptions first
                        SUBSCRIPTIONS.CURRENT_PERIOD_END.desc()  // then pick the one ending farthest in future / closest in past
                )
                .limit(1)
                .fetchOneInto(SubscriptionData.class);
    }


    /**
     * Decrement generations left after a successful study guide generation and stream
     * @param username - user to deduct
     * @param deduction - constant 1 deduction
     */
    public void decrementUserGenerationsLeft(String username, int deduction){
        int rowsAffected = dslContext.update(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.GENERATIONS_LEFT,
                        SUBSCRIPTIONS.GENERATIONS_LEFT.subtract(deduction))
                .from(USERS)
                .where(USERS.USERNAME.eq(username))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(USERS.STRIPE_CUSTOMER_ID))
                .and(SUBSCRIPTIONS.GENERATIONS_LEFT.ge(deduction))
                .execute();

        if(rowsAffected == 0){
            throw new EmptyResultDataAccessException(1);
        }
    }
}
