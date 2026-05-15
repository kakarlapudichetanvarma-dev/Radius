package com.authservice.service.impl;


import com.authservice.exception.AuthExceptions;
import com.authservice.service.OtpService;
import com.authservice.util.OtpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log =
            LoggerFactory.getLogger(OtpServiceImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final OtpUtil otpUtil;

    public OtpServiceImpl(
            StringRedisTemplate redisTemplate,
            OtpUtil otpUtil) {

        this.redisTemplate = redisTemplate;
        this.otpUtil = otpUtil;
    }

    @Value("${app.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;

    @Override
    public String generateAndStoreOtp(String email) {

        String otp = otpUtil.generateOtp();

        String key = otpUtil.buildOtpKey(email);

        redisTemplate.opsForValue().set(
                key,
                otp,
                otpExpirationSeconds,
                TimeUnit.SECONDS
        );

        log.info(
                "OTP generated and stored in Redis for email: {} | TTL: {}s",
                email,
                otpExpirationSeconds
        );

        return otp;
    }

    @Override
    public void validateOtp(
            String email,
            String submittedOtp) {

        String key =
                otpUtil.buildOtpKey(email);

        String storedOtp =
                redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {

            log.warn(
                    "OTP not found in Redis for email: {} — may have expired",
                    email
            );

            throw new AuthExceptions.OtpExpiredException(
                    "OTP has expired. Please request a new one."
            );
        }

        if (!storedOtp.equals(submittedOtp)) {

            log.warn(
                    "Invalid OTP submitted for email: {}",
                    email
            );

            throw new AuthExceptions.InvalidOtpException(
                    "The OTP you entered is incorrect."
            );
        }

        redisTemplate.delete(key);

        log.info(
                "OTP verified and invalidated successfully for email: {}",
                email
        );
    }
}