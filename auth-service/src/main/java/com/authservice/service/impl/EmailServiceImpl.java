package com.authservice.service.impl;

import com.authservice.exception.AuthExceptions;
import com.authservice.service.EmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

        private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

        private final JavaMailSender mailSender;

        public EmailServiceImpl(
                        JavaMailSender mailSender) {

                this.mailSender = mailSender;
        }

        @Value("${app.email.from}")
        private String fromEmail;

        @Async
        @Override
        public void sendWelcomeEmail(
                        String toEmail,
                        String username) {

                log.info(
                                "Sending welcome email to: {}",
                                toEmail);

                String subject = "Welcome to Radius — Account Created";

                String body = buildWelcomeEmailBody(username);

                try {

                        sendHtmlEmail(
                                        toEmail,
                                        subject,
                                        body);

                        log.info(
                                        "Welcome email sent successfully to: {}",
                                        toEmail);

                } catch (Exception e) {

                        log.error(
                                        "Failed to send welcome email to {}: {}",
                                        toEmail,
                                        e.getMessage());

                        throw new AuthExceptions.EmailSendException(
                                        "Failed to send welcome email",
                                        e);
                }
        }

        @Async
        @Override
        public void sendOtpEmail(
                        String toEmail,
                        String username,
                        String otp) {

                log.info(
                                "Sending OTP email to: {}",
                                toEmail);

                String subject = "Your Login OTP — Radius";

                String body = buildOtpEmailBody(
                                username,
                                otp);

                try {

                        sendHtmlEmail(
                                        toEmail,
                                        subject,
                                        body);

                        log.info(
                                        "OTP email sent successfully to: {}",
                                        toEmail);

                } catch (Exception e) {

                        log.error(
                                        "Failed to send OTP email to {}: {}",
                                        toEmail,
                                        e.getMessage());

                        throw new AuthExceptions.EmailSendException(
                                        "Failed to send OTP email",
                                        e);
                }
        }

        private void sendHtmlEmail(
                        String to,
                        String subject,
                        String htmlBody)
                        throws MessagingException {

                MimeMessage message = mailSender.createMimeMessage();

                MimeMessageHelper helper = new MimeMessageHelper(
                                message,
                                true,
                                "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                mailSender.send(message);
        }

        private String buildWelcomeEmailBody(
                        String username) {

                return """
                                <html>
                                  <body style="font-family: Arial, sans-serif; color: #333;">
                                    <div style="max-width: 600px; margin: 0 auto; padding: 24px;">
                                      <h2 style="color: #4A90E2;">Welcome to Radius!</h2>
                                      <p>Hi <strong>%s</strong>,</p>
                                      <p>Your account has been successfully created. You can now log in using your email and password.</p>
                                      <p>For your security, you will receive a One-Time Password (OTP) each time you log in.</p>
                                    </div>
                                  </body>
                                </html>
                                """
                                .formatted(username);
        }

        private String buildOtpEmailBody(
                        String username,
                        String otp) {

                return """
                                <html>
                                  <body style="font-family: Arial, sans-serif; color: #333;">
                                    <div style="max-width: 600px; margin: 0 auto; padding: 24px;">
                                      <h2 style="color: #4A90E2;">Your Login OTP</h2>
                                      <p>Hi <strong>%s</strong>,</p>
                                      <p>Your OTP is:</p>
                                      <h1>%s</h1>
                                    </div>
                                  </body>
                                </html>
                                """.formatted(username, otp);
        }
}
