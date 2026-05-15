package com.authservice.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.springframework.stereotype.Component;

@Component
public class PhoneValidator {

    private static final PhoneNumberUtil UTIL =
            PhoneNumberUtil.getInstance();

    public boolean isValid(
            String phoneNumber) {

        try {

            Phonenumber.PhoneNumber parsed =
                    UTIL.parse(
                            phoneNumber,
                            null
                    );

            return UTIL.isValidNumber(
                    parsed
            );

        } catch (NumberParseException e) {

            return false;
        }
    }


    public String toE164(
            String phoneNumber) {

        try {

            Phonenumber.PhoneNumber parsed =
                    UTIL.parse(
                            phoneNumber,
                            null
                    );

            return UTIL.format(
                    parsed,
                    PhoneNumberUtil.PhoneNumberFormat.E164
            );

        } catch (Exception e) {

            return phoneNumber;
        }
    }
}