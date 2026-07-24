package com.dtvi.Pet.Ecommerce.payment;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnPaySignerTests {

    private final VnPaySigner signer = new VnPaySigner();

    @Test
    void signsSortedParametersWithHmacSha512() {
        Map<String, String> parameters = Map.of(
                "vnp_TxnRef", "PET-20260724-ABC12345",
                "vnp_TmnCode", "TEST0001",
                "vnp_Command", "pay",
                "vnp_Amount", "12500000"
        );

        String signature = signer.sign(parameters, "demo-secret");

        assertTrue(signature.equals(
                "9e19936e5df951bdc4c614c762e4dc27c6ff02e9e267ca6c4c378c191c017a72"
                        + "cc795e85666b57ad0fd2076ec6d04f60a645dc58980ee748231d07d22d294b61"
        ));
    }

    @Test
    void rejectsModifiedCallback() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("vnp_Amount", "1000000");
        parameters.put("vnp_TxnRef", "ORDER-1");
        parameters.put("vnp_SecureHash", signer.sign(parameters, "secret"));

        assertTrue(signer.verify(parameters, "secret"));
        parameters.put("vnp_Amount", "99900000");
        assertFalse(signer.verify(parameters, "secret"));
    }
}
