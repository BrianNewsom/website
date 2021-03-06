/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.WebsiteProperties;

public class EmailUtils {

    public static class MessageBuilder {
        public MimeMessage build( Session session ) throws MessagingException, UnsupportedEncodingException {
            return new MimeMessage( session );
        }
    }

    public static class GeneralEmailBuilder extends MessageBuilder {
        private String subject;
        private String fromAddress;
        private String fromName;
        private List<String> recipients = new LinkedList<String>();
        private List<InternetAddress> replyTos = new LinkedList<InternetAddress>();
        private MimeBodyPart messageBodyPart;
        private MimeBodyPart plainTextPart;
        private List<BodyPart> bodyParts = new LinkedList<BodyPart>(); // somewhat morbid!

        public GeneralEmailBuilder( String subject, String fromAddress ) {
            this.subject = subject;
            this.fromAddress = fromAddress;
        }

        @Override
        public MimeMessage build( Session session ) throws MessagingException, UnsupportedEncodingException {
            MimeMessage message = super.build( session );

            if ( fromName == null ) {
                message.setFrom( new InternetAddress( fromAddress ) );
            }
            else {
                message.setFrom( new InternetAddress( fromAddress, fromName ) );
            }
            message.setSubject( subject, "utf-8" );

            for ( String email : recipients ) {
                message.addRecipient( Message.RecipientType.TO, new InternetAddress( email ) );
            }

            if ( replyTos.size() > 0 ) {
                message.setReplyTo( replyTos.toArray( new InternetAddress[replyTos.size()] ) );
            }

            MimeMultipart multipart = new MimeMultipart();
            if ( plainTextPart != null ) {
                // we are sending a plain-text alternative in addition
                multipart.addBodyPart( plainTextPart );
                multipart.setSubType( "alternative" );
            }
            multipart.addBodyPart( messageBodyPart );

            for ( BodyPart bodyPart : bodyParts ) {
                multipart.addBodyPart( bodyPart );
            }

            message.setContent( multipart );

            return message;
        }

        /*---------------------------------------------------------------------------*
        * adders
        *----------------------------------------------------------------------------*/

        public void addRecipient( String email ) {
            recipients.add( email );
        }

        public void addRecipients( Collection<String> recipients ) {
            recipients.addAll( recipients );
        }

        public void setBody( String body ) throws MessagingException {
            messageBodyPart = new MimeBodyPart();

            // messageBodyPart.setContent( body, "text/html; charset=UTF-8" ); 
            messageBodyPart.setText( body, "UTF-8", "html" );
        }

        public void setPlainTextAlternative( String body ) throws MessagingException {
            plainTextPart = new MimeBodyPart();

            plainTextPart.setText( body );
        }

        public void addBodyPart( BodyPart bodyPart ) {
            bodyParts.add( bodyPart );
        }

        public void addReplyTo( String email ) throws AddressException {
            replyTos.add( new InternetAddress( email ) );
        }

        public void setFromName( String fromName ) {
            this.fromName = fromName;
        }

        /*---------------------------------------------------------------------------*
        * getters
        *----------------------------------------------------------------------------*/

        public String getSubject() {
            return subject;
        }

        public String getFromAddress() {
            return fromAddress;
        }

        public List<String> getRecipients() {
            return recipients;
        }
    }

    public static boolean sendMessage( MessageBuilder builder ) {
        return sendMessage( builder, PhetWicketApplication.get().getWebsiteProperties() );
    }

    public static boolean sendMessage( MessageBuilder builder, final WebsiteProperties websiteProperties ) {
        try {
            Properties props = System.getProperties();

            //props.put( "mail.debug", "true" );
            props.put( "mail.smtp.host", websiteProperties.getMailHost() );
            props.put( "mail.smtp.user", websiteProperties.getMailUser() );
            props.put( "password", websiteProperties.getMailPassword() );
            props.put( "mail.smtp.auth", "true" );
            props.put( "mail.smtp.starttls.enable", "true" ); //necessary if you use cu or google, otherwise you receive an error:

            Session session = Session.getInstance( props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication( websiteProperties.getMailUser(), websiteProperties.getMailPassword() );
                }
            } );

            Message message = builder.build( session ); // build the message

            Transport.send( message );
            return true; // success = true
        }
        catch ( MessagingException e ) {
            e.printStackTrace();
        }
        catch ( UnsupportedEncodingException e ) {
            e.printStackTrace();
        }

        return false; // success = false
    }

    public static boolean sendMessage( String mailHost, final String mailUser, final String mailPassword, List<String> emailAddresses, String body, String fromAddress, String subject, ArrayList<BodyPart> additionalParts ) {
        try {
            Properties props = System.getProperties();
            props.put( "mail.smtp.host", mailHost );

            //props.put( "mail.debug", "true" );
            props.put( "mail.smtp.starttls.enable", "true" ); //necessary if you use cu or google, otherwise you receive an error:

            props.put( "mail.smtp.auth", "true" );
            props.put( "mail.smtp.user", mailUser );
            props.put( "password", mailPassword );

            Session session = Session.getInstance( props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication( mailUser, mailPassword );
                }
            } );

            Message message = new MimeMessage( session );
            message.setFrom( new InternetAddress( fromAddress ) );
            for ( String email : emailAddresses ) {
                message.addRecipient( Message.RecipientType.TO, new InternetAddress( email ) );
            }
            message.setSubject( subject );

            MimeBodyPart messageBodyPart = new MimeBodyPart();

            messageBodyPart.setText( body, "UTF-8", "html" );

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart( messageBodyPart );

            for ( BodyPart bodyPart : additionalParts ) {
                multipart.addBodyPart( bodyPart );
            }

            // add attachments here, see example

            message.setContent( multipart );

            Transport.send( message );

            return true; // success = true
        }
        catch ( MessagingException e ) {
            e.printStackTrace();
        }

        return false; // success = false
    }

}
