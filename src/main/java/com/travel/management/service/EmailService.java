package com.travel.management.service;

import com.travel.management.exception.EmailFailedToSendException;
import com.travel.management.model.Booking;
import com.travel.management.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new EmailFailedToSendException(e.getMessage());
        }
    }

    public void sendVerificationCode(String email, String code) {
        String subject = "Email Verification - Travel Nest";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .email-container {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f8f9fa;
                    }
                    .header {
                        background-color: #2c3e50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }
                    .content {
                        background-color: white;
                        padding: 20px;
                        border-radius: 0 0 5px 5px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #2c3e50;
                        text-align: center;
                        padding: 20px;
                        margin: 20px 0;
                        background-color: #f8f9fa;
                        border-radius: 5px;
                        letter-spacing: 5px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>Travel Nest</h1>
                    </div>
                    <div class="content">
                        <h2>Email Verification</h2>
                        <p>Your verification code is:</p>
                        <div class="code">%s</div>
                        <p>Use this code to verify your email address. This code will expire in 1 hour.</p>
                        <p>If you didn't request this code, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Travel Nest. All rights reserved.</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);

        sendEmail(email, subject, htmlContent);
    }

    public void sendPasswordResetCode(String email, String code) {
        String subject = "Password Reset Request - Travel Nest";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .email-container {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #f8f9fa;
                    }
                    .header {
                        background-color: #2c3e50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }
                    .content {
                        background-color: white;
                        padding: 20px;
                        border-radius: 0 0 5px 5px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        color: #2c3e50;
                        text-align: center;
                        padding: 20px;
                        margin: 20px 0;
                        background-color: #f8f9fa;
                        border-radius: 5px;
                        letter-spacing: 5px;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>Travel Nest</h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>Your password reset code is:</p>
                        <div class="code">%s</div>
                        <p>Use this code to reset your password. This code will expire in 15 minutes.</p>
                        <p>If you didn't request this code, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Travel Nest. All rights reserved.</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);

        sendEmail(email, subject, htmlContent);
    }
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public String generateBookingConfirmationEmail(Booking booking) {
        String itemType = booking.getTrip() != null ? "Trip" : "Package";
        String itemName = getItemName(booking);
        Double price = booking.getPayment().getAmount();

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    .email-container {
                        font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                        background-color: #ffffff;
                    }
                    .header {
                        background-color: #2c3e50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }
                    .content {
                        padding: 20px;
                        background-color: #ffffff;
                        border: 1px solid #f0c17a;
                        border-radius: 0 0 5px 5px;
                    }
                    .booking-details {
                        background-color: #f8f9fa;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 5px;
                    }
                    .highlight {
                        color: #f0c17a;
                        font-weight: bold;
                    }
                    .footer {
                        text-align: center;
                        margin-top: 20px;
                        padding: 20px;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>Travel Nest - Booking Confirmation</h1>
                    </div>
                    <div class="content">
                        <h2>Thank you for your booking!</h2>
                        <div class="booking-details">
                            <h3>Booking Details:</h3>
                            <p><strong>%s:</strong> %s</p>
                            <p><strong>Date & Time:</strong> %s</p>
                            <p><strong>Price:</strong> $%.2f</p>
                            <p><strong>Status:</strong> %s</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2024 Travel Nest. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                itemType,
                itemName,
                booking.getBookingDateTime().format(DATE_FORMATTER),
                price,
                booking.getStatus()
        );
    }

    public String generateAdminNotificationEmail(Booking booking) {
        String itemType = booking.getTrip() != null ? "Trip" : "Package";
        String itemName = getItemName(booking);
        User user = booking.getUser();

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    /* Same styles as above */
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>New Booking Notification</h1>
                    </div>
                    <div class="content">
                        <div class="booking-details">
                            <h3>Customer Details:</h3>
                            <p><strong>Name:</strong> %s %s</p>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Phone:</strong> %s</p>
                            
                            <h3>Booking Details:</h3>
                            <p><strong>%s:</strong> %s</p>
                            <p><strong>Date & Time:</strong> %s</p>
                            <p><strong>Price:</strong> $%.2f</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2024 Travel Nest. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                itemType,
                itemName,
                booking.getBookingDateTime().format(DATE_FORMATTER),
                booking.getPayment().getAmount()
        );
    }

    public String generateStatusUpdateEmail(Booking booking) {
        String itemType = booking.getTrip() != null ? "Trip" : "Package";
        String itemName = getItemName(booking);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    /* Same styles as above */
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>Booking Status Update</h1>
                    </div>
                    <div class="content">
                        <h2>Your booking status has been updated</h2>
                        <div class="booking-details">
                            <p><strong>%s:</strong> %s</p>
                            <p><strong>New Status:</strong> %s</p>
                            <p><strong>Updated At:</strong> %s</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2024 Travel Nest. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                itemType,
                itemName,
                booking.getStatus(),
                LocalDateTime.now().format(DATE_FORMATTER)
        );
    }

    private String getItemName(Booking booking) {
        return booking.getTrip() != null ?
                booking.getTrip().getTitle() :
                booking.getBookedPackage().getName();
    }
}
