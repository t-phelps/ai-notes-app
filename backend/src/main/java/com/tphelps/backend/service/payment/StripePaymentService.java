package com.tphelps.backend.service.payment;

import com.tphelps.backend.repository.payment.StripePaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentService {

    StripePaymentRepository stripePaymentRepository;

    @Autowired
    public StripePaymentService(StripePaymentRepository stripePaymentRepository) {
        this.stripePaymentRepository = stripePaymentRepository;
    }


}
