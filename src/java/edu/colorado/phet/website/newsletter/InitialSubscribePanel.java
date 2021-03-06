/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.newsletter;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;

import edu.colorado.phet.website.components.LocalizedText;
import edu.colorado.phet.website.content.ErrorPage;
import edu.colorado.phet.website.data.PhetUser;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.hibernate.Result;

public class InitialSubscribePanel extends PhetPanel {

    private FeedbackPanel feedback;

    private static Logger logger = Logger.getLogger( InitialSubscribePanel.class );

    private static Map<String, Integer> ipAttempts = new HashMap<String, Integer>();
    private static Map<String, Integer> emailAttempts = new HashMap<String, Integer>();

    private static final int MAX_IP_ATTEMPTS = 20; // max number of times an IP can try subscribing in an hour
    private static final int MAX_EMAIL_ATTEMPTS = 2; // max number of times an exact email address can be subscribed to in an hour

    private final PageContext context;

    // TODO: add security measures like limiting # of times per IP / user

    public InitialSubscribePanel( String id, PageContext context ) {
        super( id, context );
        this.context = context; // TODO: evaluate whether context should be part of PhetPanel, since it is serializable

        add( new SubscribeForm( "subscribe-form" ) );

        feedback = new FeedbackPanel( "feedback" );
        feedback.setVisible( false );
        add( feedback );

        add( new LocalizedText( "pleaseSignUp", "newsletter.pleaseSignUp" ) );
    }

    private class SubscribeForm extends Form {
        private final Model<String> emailModel = new Model<String>( "" );
        private RequiredTextField<String> emailTextField;

        public SubscribeForm( String id ) {
            super( id );

            emailTextField = new RequiredTextField<String>( "esnail", emailModel );
            add( emailTextField ); // called esnail instead of email because maybe it won't be picked up as such by spam?

            // validating the emails should hopefully block most things
            add( new AbstractFormValidator() {
                public FormComponent<?>[] getDependentFormComponents() {
                    return new FormComponent<?>[] { emailTextField };
                }

                public void validate( Form<?> form ) {
                    String emailAddress = emailTextField.getInput();
                    String ip = PhetRequestCycle.get().getWebRequest().getHttpServletRequest().getRemoteAddr();
                    if ( !PhetUser.isValidEmail( emailAddress ) ) {
                        error( emailTextField, "newsletter.validation.email" );
                        logger.info( "bad email entered: " + emailAddress + ", with IP: " + ip );
                    }
                    Integer i;
                    Integer j;
                    synchronized ( this ) {
                        i = ipAttempts.get( ip );
                        j = emailAttempts.get( emailAddress );
                    }
                    if ( i == null ) {
                        i = 1;
                    }
                    else {
                        i += 1;
                    }
                    if ( j == null ) {
                        j = 1;
                    }
                    else {
                        j += 1;
                    }
                    synchronized ( this ) {
                        ipAttempts.put( ip, i );
                        emailAttempts.put( emailAddress, j );
                    }
                    if ( i > MAX_IP_ATTEMPTS || j > MAX_EMAIL_ATTEMPTS ) {
                        error( emailTextField, "newsletter.validation.attempts" );
                        logger.info( "too many subscription attempts made. ip: " + ip + ", email: " + emailAddress + ", ip attempts: " + i + ", email attempts: " + j );
                    }
                }
            } );
        }

        @Override
        protected void onValidate() {
            super.onValidate();
            feedback.setVisible( feedback.anyMessage() );
        }

        @Override
        protected void onSubmit() {
            final String emailAddress = emailTextField.getInput();

            final Result<PhetUser> userResult = NewsletterUtils.subscribeUserAndSendEmail( context, getHibernateSession(), emailAddress, false );

            if ( userResult.success ) {
                PhetUser user = userResult.value;
                setResponsePage( ConfirmEmailSentPage.class, ConfirmEmailSentPage.getParameters( user ) );
            }
            else {
                setResponsePage( ErrorPage.class );
            }
        }
    }

    /**
     * Called from SchedulerService on a regular time interval. As of creation, this will be an hour
     */
    public static synchronized void resetSecurity() {
        logger.info( "resetting subscription form anti-spam cache" );
        ipAttempts = new HashMap<String, Integer>();
        emailAttempts = new HashMap<String, Integer>();
    }

    public static synchronized int getIPCount() {
        return ipAttempts.size();
    }

    public static synchronized int getEmailCount() {
        return emailAttempts.size();
    }
}
