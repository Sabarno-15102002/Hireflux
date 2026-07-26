package com.sabarno.hireflux.service.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
 
    @Mock
    private JavaMailSender mailSender;
 
    @InjectMocks
    private EmailService emailService;
 
    @Test
    void testSendInviteEmail_shouldSetCorrectRecipient() {
        emailService.sendInviteEmail("jane@example.com", "https://hireflux.com/invite/accept?token=abc123");
 
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
 
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage.getTo());
        assertEquals(1, sentMessage.getTo().length);
        assertEquals("jane@example.com", sentMessage.getTo()[0]);
    }
 
    @Test
    void testSendInviteEmail_shouldSetCorrectSubject() {
        emailService.sendInviteEmail("jane@example.com", "https://hireflux.com/invite/accept?token=abc123");
 
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
 
        assertEquals("HireFlux Invitation", messageCaptor.getValue().getSubject());
    }
 
    @Test
    void testSendInviteEmail_shouldIncludeInviteLinkInBody() {
        String inviteLink = "https://hireflux.com/invite/accept?token=abc123";
 
        emailService.sendInviteEmail("jane@example.com", inviteLink);
 
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
 
        String body = messageCaptor.getValue().getText();
        assertNotNull(body);
        assertTrue(body.contains(inviteLink));
    }
 
    @Test
    void testSendInviteEmail_shouldIncludeExpectedBoilerplateContent() {
        emailService.sendInviteEmail("jane@example.com", "https://hireflux.com/invite/accept?token=abc123");
 
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
 
        String body = messageCaptor.getValue().getText();
        assertTrue(body.contains("You have been invited to collaborate on HireFlux."));
        assertTrue(body.contains("This invitation link may expire after a certain period."));
        assertTrue(body.contains("HireFlux Team"));
    }
 
    @Test
    void testSendInviteEmail_shouldCallMailSenderExactlyOnce() {
        emailService.sendInviteEmail("jane@example.com", "https://hireflux.com/invite/accept?token=abc123");
 
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verifyNoMoreInteractions(mailSender);
    }
 
    @Test
    void testSendInviteEmail_shouldProduceDifferentBodies_forDifferentInviteLinks() {
        emailService.sendInviteEmail("jane@example.com", "https://hireflux.com/invite/accept?token=aaa");
        emailService.sendInviteEmail("jane@example.com", "https://hireflux.com/invite/accept?token=bbb");
 
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messageCaptor.capture());
 
        String firstBody = messageCaptor.getAllValues().get(0).getText();
        String secondBody = messageCaptor.getAllValues().get(1).getText();
 
        assertTrue(firstBody.contains("token=aaa"));
        assertTrue(secondBody.contains("token=bbb"));
        assertNotEquals(firstBody, secondBody);
    }
 
    @Test
    void testSendInviteEmail_shouldPropagateException_whenMailSenderFails() {
        MailSendException mailFailure = new MailSendException("SMTP server unreachable");
        doThrow(mailFailure).when(mailSender).send(any(SimpleMailMessage.class));
 
        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> emailService.sendInviteEmail("jane@example.com", "https://hireflux.com/invite/accept?token=abc123")
        );
 
        assertSame(mailFailure, exception);
    }
 
    @Test
    void testSendInviteEmail_shouldHandleNullInviteLink_withoutThrowing() {
        assertDoesNotThrow(() ->
                emailService.sendInviteEmail("jane@example.com", null));
 
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertTrue(messageCaptor.getValue().getText().contains("null"));
    }
}