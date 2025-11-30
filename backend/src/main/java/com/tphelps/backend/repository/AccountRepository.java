package com.tphelps.backend.repository;


import com.tphelps.backend.dtos.notes.UserNoteDto;
import com.tphelps.backend.dtos.responses.UserDetailsResponseDto;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static test.generated.tables.Users.USERS;
import test.generated.tables.pojos.Users;

import java.util.List;

import static test.generated.tables.UserNoteHistory.USER_NOTE_HISTORY;

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
                row.get(USERS.USERNAME),
                row.get(USERS.EMAIL),
                userNotesDto);
    }

}
