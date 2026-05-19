package com.sabarno.hireflux.service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendInviteEmail(String to, String inviteLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(
                "HireFlux Invitation"
        );

        message.setText(
            "Dear User,\n\n"
            + "You have been invited to collaborate on HireFlux.\n\n"
            + "To accept your invitation and get started, please click the link below:\n\n"
            + inviteLink
            + "\n\n"
            + "This invitation link may expire after a certain period.\n\n"
            + "If you believe this email was sent in error, please disregard it.\n\n"
            + "Regards,\n"
            + "HireFlux Team"
        );

        mailSender.send(message);
    }

}
