package tr.name.sualp.merter.evernote.converter.mail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MailSender {

  private final String to;
  private final String subject;
  private final String messageBody;

  private static final Logger logger = LogManager.getLogger(MailSender.class);

  private MailSender(String to, String subject, String messageBody) {
    this.to = to;
    this.subject = subject;
    this.messageBody = messageBody;
  }

  public static MailSender mailSenderFactory(String to, String subject, String messageBody) {
    return new MailSender(to, subject, messageBody);
  }

  public void sendThis() throws MessagingException {
    final String from = "your.mail@gmail.com";
    final String password = "password";

    Properties props = new Properties();

    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.socketFactory.port", "465");
    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.port", "465");

    Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(from, password);
      }
    });

    // Create a default MimeMessage object.
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
    message.setSubject(subject);
    message.setText(messageBody);

    Transport.send(message);
    logger.info("Sent message successfully....");
  }

}
