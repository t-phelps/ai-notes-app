package com.tphelps.backend.repository;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import static test.generated.tables.PasswordResetTokens.PASSWORD_RESET_TOKENS;
import static test.generated.tables.Users.USERS;

@Repository
public class EmailRepository {

    private final DSLContext dsl;

    public EmailRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Store the password reset token in the database using the USER_ID from the users table
     * @param email - email used for USER_ID FK to users table
     * @param hashed_token - the UUID hashed to be stored in db
     */
    public void storePasswordResetToken(String email, String hashed_token){
        dsl.insertInto(PASSWORD_RESET_TOKENS, PASSWORD_RESET_TOKENS.USER_ID, PASSWORD_RESET_TOKENS.HASHED_TOKEN)
                .select(
                        dsl.select(USERS.ID, DSL.val(hashed_token))
                                .from(USERS)
                                .where(USERS.EMAIL.eq(email))
                )
                .execute();
    }
}
