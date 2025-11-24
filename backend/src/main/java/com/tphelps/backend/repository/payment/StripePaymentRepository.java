package com.tphelps.backend.repository.payment;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import static test.generated.tables.Users.USERS;
import test.generated.tables.pojos.Users;

@Repository
public class StripePaymentRepository {

    DSLContext dsl;

    @Autowired
    public StripePaymentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Users getUserStripeCustomerId(String username){
        return dsl
                .select(USERS.STRIPE_CUSTOMER_ID).from(USERS)
                .where(USERS.USERNAME.eq(username))
                .fetchOneInto(Users.class);
    }
}
