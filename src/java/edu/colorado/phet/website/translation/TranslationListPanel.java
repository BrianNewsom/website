/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.translation;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.hibernate.Session;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.common.phetcommon.util.PhetLocales;
import edu.colorado.phet.website.DistributionHandler;
import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.authentication.PhetSession;
import edu.colorado.phet.website.components.InvisibleComponent;
import edu.colorado.phet.website.components.RawLabel;
import edu.colorado.phet.website.content.IndexPage;
import edu.colorado.phet.website.data.PhetUser;
import edu.colorado.phet.website.data.Translation;
import edu.colorado.phet.website.notification.NotificationHandler;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.attributes.ClassAppender;
import edu.colorado.phet.website.util.HtmlUtils;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;
import edu.colorado.phet.website.util.TranslationUtils;
import edu.colorado.phet.website.util.hibernate.HibernateTask;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;
import edu.colorado.phet.website.util.hibernate.Result;
import edu.colorado.phet.website.util.hibernate.Task;
import edu.colorado.phet.website.util.wicket.ConfirmLinkBehavior;
import edu.colorado.phet.website.util.wicket.IComponentFactory;
import edu.colorado.phet.website.util.wicket.WicketUtils;

/**
 * Shows a list of website translations to the user, along with the ability to perform actions on them
 * <p/>
 * Has the following fields visible (many togglable if you are admin):
 * id
 * locale
 * number of strings translated
 * visibility: (visible, hidden, inactive), with possible option to toggle visibility (visible/hidden) or reactivate (if inactive)
 * preview/view
 * edit
 * locked: toggle available
 * submit
 * delete / permanently delete: perm delete only works for inactive (IE "deleted") translations
 * request to collaborate
 */
public class TranslationListPanel extends PhetPanel {

    private PhetLocales phetLocales;

    private static final Logger logger = Logger.getLogger( TranslationListPanel.class.getName() );
    private List<Translation> translations;

    public TranslationListPanel( String id, PageContext context, final List<Translation> translations ) {
        super( id, context );
        this.translations = translations;

        final Map<Translation, Integer> sizes = new HashMap<Translation, Integer>();

        phetLocales = ( (PhetWicketApplication) getApplication() ).getSupportedLocales();

        HibernateUtils.wrapTransaction( getHibernateSession(), new HibernateTask() {
            public boolean run( Session session ) {
                for ( Translation translation : translations ) {
                    // TODO: we're taking 100ms hit on this, likely to get much bigger
                    sizes.put( translation, ( (Long) session.createQuery( "select count(*) from TranslatedString as ts where ts.translation = :translation" )
                            .setEntity( "translation", translation ).iterate().next() ).intValue() );

                    // load this into memory
                    translation.getAuthorizedUsers();
                }
                return true;
            }
        } );

        if ( translations.isEmpty() ) {
            setVisible( false );
        }

        add( new TranslationListView( "translation-list", sizes ) );

        if ( PhetSession.get().getUser().isTeamMember() ) {
            add( new Link( "sort-by-id" ) {
                @Override
                public void onClick() {
                    Collections.sort( translations, new Comparator<Translation>() {
                        public int compare( Translation a, Translation b ) {
                            return new Integer( a.getId() ).compareTo( b.getId() );
                        }
                    } );
                }
            } );
            add( new Link( "sort-by-locale" ) {
                @Override
                public void onClick() {
                    Collections.sort( translations, new Comparator<Translation>() {
                        public int compare( Translation a, Translation b ) {
                            return LocaleUtils.localeToString( a.getLocale() ).compareTo( LocaleUtils.localeToString( b.getLocale() ) );
                        }
                    } );
                }
            } );
        }
        else {
            add( new WebMarkupContainer( "sort-by-id" ) );
            add( new WebMarkupContainer( "sort-by-locale" ) );
        }

    }

    public List<Translation> getTranslations() {
        return translations;
    }

    private class TranslationListView extends ListView<Translation> {
        private final Map<Translation, Integer> sizes;

        public TranslationListView( String id, Map<Translation, Integer> sizes ) {
            super( id, translations );
            this.sizes = sizes;
        }

        protected void populateItem( ListItem<Translation> item ) {
            final Translation translation = item.getModelObject();
            final PhetUser user = PhetSession.get().getUser();

            if ( translation.isPublished( getHibernateSession() ) ) {
                // this is a parent of a visible translation
                item.add( new ClassAppender( "translation-preferred" ) );
            }

            final boolean visibleToggleShown = translation.allowToggleVisibility( user );
            boolean reactivateShown = translation.allowReactivate( user );
            boolean editShown = translation.allowEdit( user );
            boolean submitShown = translation.allowSubmit( user );
            boolean deleteShown = translation.allowDeactivate( user );
            boolean permanentDeleteShown = translation.allowPermanentDelete( user );
            boolean requestShown = translation.allowRequestToCollaborate( user );
            boolean emailsShown = user.isTeamMember();

            item.add( new Label( "id", String.valueOf( translation.getId() ) ) );
            item.add( new Label( "locale", translation.getPrettyEnglishLocaleString() ) );
            item.add( new Label( "num-strings", String.valueOf( sizes.get( translation ) ) ) );
            Label visibleLabel = new Label( "visible-label", getTranslationVisibilityString( translation ) );
            if ( translation.isVisible() ) {
                visibleLabel.add( new ClassAppender( "translation-visible" ) );
            }
            item.add( visibleLabel );

            item.add( WicketUtils.componentIf( visibleToggleShown, "visible-toggle", new IComponentFactory<Component>() {
                public Component create( String id ) {
                    return new Link( id ) {
                        @Override
                        public void onClick() {
                            toggleVisibility( translation );
                        }
                    };
                }
            } ) );

            if ( reactivateShown ) {
                item.add( new Link( "reactivate" ) {
                    @Override
                    public void onClick() {
                        reactivate( translation );
                    }
                } );
            }
            else {
                item.add( new InvisibleComponent( "reactivate" ) );
            }

            PageContext newContext;
            String type;
            if ( translation.isVisible() ) {
                type = "view";
                newContext = PageContext.getNewLocaleContext( translation.getLocale() );
            }
            else {
                type = "preview";
                newContext = PageContext.getNewIdLocaleContext( translation.getId(), translation.getLocale() );
            }

            // make a popuplink class
            Link popupLink = IndexPage.getLinker().getLink( "preview", newContext, getPhetCycle() );
            WicketUtils.makePopupLink( popupLink );
            popupLink.add( new Label( "type", type ) );
            item.add( popupLink );

            if ( editShown ) {
                item.add( new Link( "edit" ) {
                    public void onClick() {
                        PageParameters params = new PageParameters();
                        params.put( TranslationEditPage.TRANSLATION_ID, translation.getId() );
                        params.put( TranslationEditPage.TRANSLATION_LOCALE, LocaleUtils.localeToString( translation.getLocale() ) );
                        setResponsePage( TranslationEditPage.class, params );
                    }
                } );
            }
            else {
                item.add( new InvisibleComponent( "edit" ) );
            }

            if ( user.isTeamMember() || translation.isUserAuthorized( user ) ) {
                Label lockLabel = new Label( "locked-label", String.valueOf( translation.isLocked() ? "locked" : "editable" ) );
                if ( translation.isLocked() ) {
                    lockLabel.add( new ClassAppender( "translation-locked" ) );
                }
                item.add( lockLabel );
            }
            else {
                item.add( new InvisibleComponent( "locked-label" ) );
            }

            if ( translation.allowToggleLocking( user ) ) {
                item.add( new Link( "lock-toggle" ) {
                    @Override
                    public void onClick() {
                        toggleLock( translation );
                    }
                } );
            }
            else {
                item.add( new InvisibleComponent( "lock-toggle" ) );
            }

            if ( !submitShown ) {
                item.add( new InvisibleComponent( "submit-link" ) );
            }
            else {
                item.add( new Link( "submit-link" ) {
                    @Override
                    public void onClick() {
                        boolean success = HibernateUtils.wrapTransaction( getHibernateSession(), new HibernateTask() {
                            public boolean run( Session session ) {
                                Translation t = (Translation) session.load( Translation.class, translation.getId() );

                                List<PhetUser> users = new LinkedList<PhetUser>();
                                for ( Object u : t.getAuthorizedUsers() ) {
                                    users.add( (PhetUser) u );
                                }

                                if ( !DistributionHandler.allowNotificationEmails( PhetRequestCycle.get() ) ) {
                                    return false; // on dev server, ignore this
                                }

                                return NotificationHandler.sendTranslationSubmittedNotification( translation.getId(), t.getLocale(), users );
                            }
                        } );

                        if ( success ) {
                            setResponsePage( TranslationSubmittedPage.class );
                        }
                    }
                } );
            }

            if ( deleteShown ) {
                item.add( new Link( "delete" ) {
                    {
                        add( new ConfirmLinkBehavior( "Are you sure you want to permanently delete this website translation?" ) );
                    }

                    public void onClick() {
                        deactivate( translation );
                    }
                } );
            }
            else {
                item.add( new InvisibleComponent( "delete" ) );
            }

            if ( permanentDeleteShown ) {
                item.add( new Link( "permanent-delete" ) {
                    public void onClick() {
                        permanentlyDelete( translation );
                    }
                } );
            }
            else {
                item.add( new InvisibleComponent( "permanent-delete" ) );
            }

            if ( requestShown ) {
                item.add( new Link( "request-to-collaborate" ) {
                    @Override
                    public void onClick() {
                        PageParameters params = new PageParameters();
                        params.add( CollaborationRequestPage.TRANSLATION_ID, String.valueOf( translation.getId() ) );
                        setResponsePage( CollaborationRequestPage.class, params );
                    }
                } );
            }
            else {
                item.add( new InvisibleComponent( "request-to-collaborate" ) );
            }

            if ( emailsShown ) {
                String emails = "";
                for ( Object o : translation.getAuthorizedUsers() ) {
                    PhetUser translator = (PhetUser) o;
                    if ( emails.length() > 0 ) {
                        emails += "<br/>";
                    }
                    emails += HtmlUtils.encode( translator.getEmail() );
                }
                item.add( new RawLabel( "emails", emails ) );
            }
            else {
                item.add( new InvisibleComponent( "emails" ) );
            }

            WicketUtils.highlightListItem( item );
        }

        public void toggleLock( final Translation translation ) {
            final boolean[] ret = new boolean[1];
            boolean success = HibernateUtils.wrapTransaction( getHibernateSession(), new HibernateTask() {
                public boolean run( Session session ) {
                    Translation tr = (Translation) session.load( Translation.class, translation.getId() );

                    tr.setLocked( !tr.isLocked() );
                    ret[0] = tr.isLocked();
                    session.update( tr );

                    return true;
                }
            } );

            if ( success ) {
                translation.setLocked( ret[0] );
            }
        }

        public void toggleVisibility( final Translation translation ) {
            TranslationUtils.setTranslationVisibility( getHibernateSession(), translation, !translation.isVisible() );
        }

        public void reactivate( final Translation translation ) {
            Result<Translation> result = HibernateUtils.resultCatchTransaction( getHibernateSession(), new Task<Translation>() {
                public Translation run( Session session ) {
                    Translation tr = (Translation) session.load( Translation.class, translation.getId() );
                    tr.setActive( true );
                    session.update( tr );
                    return tr;
                }
            } );

            // TODO: better way to push changes back to in-memory object?
            if ( result.success ) {
                translation.setActive( true );
            }
        }

        public void deactivate( final Translation translation ) {
            Result<Translation> result = HibernateUtils.resultCatchTransaction( getHibernateSession(), new Task<Translation>() {
                public Translation run( Session session ) {
                    Translation tr = (Translation) session.load( Translation.class, translation.getId() );
                    tr.setActive( false );
                    session.update( tr );
                    return tr;
                }
            } );

            // TODO: better way to push changes back to in-memory object?
            if ( result.success ) {
                translation.setActive( false );
            }
        }

        public void permanentlyDelete( final Translation translation ) {
            final List<PhetUser> users = new LinkedList<PhetUser>();
            final Locale locale = translation.getLocale();
            final int id = translation.getId();
            final boolean success = HibernateUtils.wrapTransaction( getHibernateSession(), new HibernateTask() {
                public boolean run( Session session ) {
                    session.load( translation, translation.getId() );

                    for ( Translation childTranslation : translation.getChildren( session ) ) {
                        childTranslation.setParent( null );
                        session.update( childTranslation );
                    }

                    logger.info( "attempting to delete translation #" + translation.getId() + " from user " + PhetSession.get().getUser().getEmail() + " and IP " + PhetRequestCycle.get().getRemoteHost() );

                    translations.remove( translation );
                    for ( Object o : translation.getTranslatedStrings() ) {
                        session.delete( o );
                    }
                    for ( Object o : translation.getAuthorizedUsers() ) {
                        PhetUser user = (PhetUser) o;
                        users.add( user );
                        user.getTranslations().remove( translation );
                        session.update( user );
                    }
                    session.delete( translation );
                    return true;
                }
            } );
            final PhetUser user = PhetSession.get().getUser();
            if ( success ) {
                if ( DistributionHandler.allowNotificationEmails( PhetRequestCycle.get() ) ) {
                    ( new Thread() {
                        @Override
                        public void run() {
                            Session session = HibernateUtils.getInstance().openSession();
                            try {
                                NotificationHandler.sendTranslationDeletedNotification( id, locale, users, user );
                            }
                            finally {
                                session.close();
                            }
                        }
                    } ).start();
                }
            }
        }
    }

    public String getTranslationVisibilityString( Translation translation ) {
        if ( translation.isVisible() ) {
            if ( !translation.isActive() ) {
                logger.warn( "Translation visible and inactive: #" + translation.getId() );
                return "visible(inactive)";
            }
            return "visible";
        }
        else if ( translation.isPublished( getHibernateSession() ) ) {
            if ( !translation.isActive() ) {
                return "published(inactive)";
            }
            return "published";
        }
        else if ( !translation.isActive() ) {
            return "inactive";
        }
        else {
            return "hidden";
        }
    }

}
