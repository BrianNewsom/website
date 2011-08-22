/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.translation;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RestartResponseAtInterceptPageException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.hibernate.Session;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.website.DistributionHandler;
import edu.colorado.phet.website.authentication.PhetSession;
import edu.colorado.phet.website.authentication.SignInPage;
import edu.colorado.phet.website.components.InvisibleComponent;
import edu.colorado.phet.website.constants.WebsiteConstants;
import edu.colorado.phet.website.data.PhetUser;
import edu.colorado.phet.website.data.TranslatedString;
import edu.colorado.phet.website.data.Translation;
import edu.colorado.phet.website.notification.NotificationHandler;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.PhetUrlMapper;
import edu.colorado.phet.website.util.StringUtils;
import edu.colorado.phet.website.util.hibernate.*;
import edu.colorado.phet.website.util.links.AbstractLinker;

public class TranslateLanguagePage extends TranslationPage {

    private Locale locale;

    public static final String TRANSLATION_LOCALE = "translationLocale";
    private static final Logger logger = Logger.getLogger( TranslateLanguagePage.class.getName() );

    public TranslateLanguagePage( PageParameters parameters ) {
        super( parameters );

        if ( !PhetSession.get().isSignedIn() ) {
            throw new RestartResponseAtInterceptPageException( SignInPage.class );
        }

        locale = LocaleUtils.stringToLocale( parameters.getString( TRANSLATION_LOCALE ) );

        String localeString = StringUtils.getLocaleTitle( locale, WebsiteConstants.ENGLISH, getPhetLocalizer() );

        add( new Label( "header", localeString ) );

        final List<Translation> translations = new LinkedList<Translation>();
        final PhetUser currentUser = PhetSession.get().getUser();
        HibernateUtils.wrapTransaction( getHibernateSession(), new VoidTask() {
            public Void run( Session session ) {
                List trans = session.createQuery( "select t from Translation as t where t.locale = :locale order by t.id" )
                        .setLocale( "locale", locale ).list();
                for ( Object o : trans ) {
                    Translation translation = (Translation) o;
                    if ( !currentUser.isTeamMember() ) {
                        // skip translations where team-members are the only ones with access (unless you are a team member)
                        if ( translation.isVisible() ) {
                            boolean nonAdminUser = false;
                            for ( Object user : translation.getAuthorizedUsers() ) {
                                if ( !( (PhetUser) user ).isTeamMember() ) {
                                    nonAdminUser = true;
                                }
                            }
                            if ( !nonAdminUser ) {
                                continue;
                            }
                        }

                        // don't show inactive ("deleted") translations to non-team-members
                        if ( !translation.isActive() ) {
                            continue;
                        }
                    }
                    translations.add( translation );
                }
                return null;
            }
        } );
        // sort the translations so that "preferred" simulations are at the top
        HibernateUtils.resultCatchTransaction( getHibernateSession(), new VoidTask() {
            public Void run( final Session session ) {
                Collections.sort( translations, new Comparator<Translation>() {
                    public int compare( Translation a, Translation b ) {
                        boolean ba = a.isPublished( session );
                        boolean bb = b.isPublished( session );
                        if ( ba == bb ) { return 0; }
                        return ( ba ? -1 : 1 );
                    }
                } );
                return null;
            }
        } );
        TranslationListPanel translationList = new TranslationListPanel( "translation-list-panel", getPageContext(), translations );

        add( translationList );

        add( new CreateTranslationForm( "create-new-translation-form" ) );

        add( new CopyTranslationForm( "create-version-translation-form", translationList.getTranslations() ) );

        if ( translations.isEmpty() ) {
            add( new InvisibleComponent( "no-strings-translated" ) );
        }
        else {
            add( new Label( "no-strings-translated", "This will create a translation with no strings translated." ) );
        }

    }

    private class CreateTranslationForm extends Form {

        public CreateTranslationForm( String id ) {
            super( id );
        }

        @Override
        protected void onSubmit() {
            Result<Translation> result = HibernateUtils.resultTransaction( getHibernateSession(), new Task<Translation>() {
                public Translation run( Session session ) {
                    PhetUser user = (PhetUser) session.load( PhetUser.class, PhetSession.get().getUser().getId() );

                    Translation translation = new Translation( locale, user, null );

                    session.save( translation );
                    session.save( user );
                    return translation;
                }
            } );

            if ( result.success ) {
                final Translation translation = result.value;

                logger.info( "Created translation: " + translation );

                PageParameters params = new PageParameters();
                params.put( TranslationEditPage.TRANSLATION_ID, translation.getId() );
                params.put( TranslationEditPage.TRANSLATION_LOCALE, LocaleUtils.localeToString( locale ) );

                final PhetUser user = PhetSession.get().getUser();
                final PhetRequestCycle cycle = PhetRequestCycle.get();

                if ( DistributionHandler.allowNotificationEmails( PhetRequestCycle.get() ) ) {
                    ( new Thread() {
                        @Override
                        public void run() {
                            Session session = HibernateUtils.getInstance().openSession();
                            try {
                                NotificationHandler.sendTranslationCreatedNotification( translation.getId(), locale, user );
                                NotificationHandler.sendCreationNotificationToTranslators( session, translation, user, cycle );
                            }
                            finally {
                                session.close();
                            }
                        }
                    } ).start();
                }

                setResponsePage( TranslationEditPage.class, params );
            }
        }
    }

    private class CopyTranslationForm extends Form {

        private Translation translation;

        public CopyTranslationForm( String id, final List<Translation> translations ) {
            super( id );

            add( new DropDownChoice<Translation>( "translations", new PropertyModel<Translation>( this, "translation" ), translations ) );

            if ( translations.isEmpty() ) {
                setVisible( false );
            }
        }

        @Override
        protected void onSubmit() {

            if ( translation == null ) {
                return;
            }

            final Translation ret[] = new Translation[1];

            boolean success = HibernateUtils.wrapTransaction( getHibernateSession(), new HibernateTask() {
                public boolean run( Session session ) {
                    PhetUser user = (PhetUser) session.load( PhetUser.class, PhetSession.get().getUser().getId() );
                    Translation newTranslation = new Translation( translation.getLocale(), user, translation );

                    session.save( newTranslation );
                    session.save( user ); // TODO this looks suspicious. update instead?
                    ret[0] = newTranslation;

                    Translation oldTranslation = (Translation) session.load( Translation.class, translation.getId() );

                    for ( Object o : oldTranslation.getTranslatedStrings() ) {
                        TranslatedString oldString = (TranslatedString) o;

                        TranslatedString newString = new TranslatedString();
                        newString.setCreatedAt( oldString.getCreatedAt() );
                        newString.setUpdatedAt( oldString.getUpdatedAt() );
                        newString.setKey( oldString.getKey() );
                        newString.setValue( oldString.getValue() );
                        newTranslation.addString( newString );

                        session.save( newString );
                    }

                    return true;
                }
            } );

            if ( success ) {

                logger.info( "Created translation: " + ret[0] + " based on " + translation );

                PageParameters params = new PageParameters();
                params.put( TranslationEditPage.TRANSLATION_ID, ret[0].getId() );
                params.put( TranslationEditPage.TRANSLATION_LOCALE, LocaleUtils.localeToString( ret[0].getLocale() ) );

                final PhetUser user = PhetSession.get().getUser();
                final PhetRequestCycle cycle = PhetRequestCycle.get();

                if ( DistributionHandler.allowNotificationEmails( PhetRequestCycle.get() ) ) {
                    ( new Thread() {
                        @Override
                        public void run() {
                            Session session = HibernateUtils.getInstance().openSession();
                            try {
                                NotificationHandler.sendTranslationCreatedBasedOnNotification( ret[0].getId(), ret[0].getLocale(), user, translation.getId() );
                                NotificationHandler.sendCreationNotificationToTranslators( session, ret[0], user, cycle );
                            }
                            finally {
                                session.close();
                            }
                        }
                    } ).start();
                }

                setResponsePage( TranslationEditPage.class, params );
            }
        }
    }

    public static void addToMapper( PhetUrlMapper mapper ) {
        mapper.addMap( "for-translators/website", TranslateLanguagePage.class, new String[]{} );
    }

    public static AbstractLinker getLinker() {
        return new AbstractLinker() {
            @Override
            public String getSubUrl( PageContext context ) {
                return "for-translators/website";
            }
        };
    }

}
