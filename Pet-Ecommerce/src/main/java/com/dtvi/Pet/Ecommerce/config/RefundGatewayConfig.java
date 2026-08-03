package com.dtvi.Pet.Ecommerce.config;

import com.dtvi.Pet.Ecommerce.payment.FallbackRefundGateway;
import com.dtvi.Pet.Ecommerce.payment.RefundGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RefundGatewayConfig {

    @Bean
    @ConditionalOnMissingBean(RefundGateway.class)
    public RefundGateway fallbackRefundGateway() {
        return new FallbackRefundGateway();
    }
}
