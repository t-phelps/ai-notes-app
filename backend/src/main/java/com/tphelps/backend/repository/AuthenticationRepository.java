package com.tphelps.backend.repository;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static test.generated.tables.Users.USERS;
import test.generated.tables.pojos.Users;

@Repository
public class AuthenticationRepository {

    private final DSLContext dsl;

    public AuthenticationRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Users getUser(String username) {
        return dsl.selectFrom(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOneInto(Users.class);
    }

    public void createUser(Users user){
        dsl.insertInto(USERS)
                .set(USERS.USERNAME, user.getUsername())
                .set(USERS.EMAIL, user.getEmail())
                .set(USERS.PASSWORD, user.getPassword())
                .set(USERS.ROLE, user.getRole())
                .execute();
    }
}
