package com.authservice.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {

    private static final Logger log =
            LoggerFactory.getLogger(OtpUtil.class);

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private static final int OTP_LENGTH = 6;

    public String generateOtp() {

        int bound =
                (int) Math.pow(10, OTP_LENGTH);

        int otp =
                SECURE_RANDOM.nextInt(bound);

        String formatted =
                String.format(
                        "%0" + OTP_LENGTH + "d",
                        otp
                );

        log.debug(
                "Generated OTP (masked): {}****",
                formatted.substring(0, 2)
        );

        return formatted;
    }

    public String buildOtpKey(
            String email) {

        return "otp:" + email;
    }
}