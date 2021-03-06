/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.admin.deploy;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.hibernate.Session;

import edu.colorado.phet.website.PhetWicketApplication;
import edu.colorado.phet.website.components.RawLabel;
import edu.colorado.phet.website.components.RawLink;
import edu.colorado.phet.website.data.Project;
import edu.colorado.phet.website.panels.ComponentThread;
import edu.colorado.phet.website.panels.ComponentThreadStatusPanel;
import edu.colorado.phet.website.panels.LoggerComponentThread;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.hibernate.HibernateUtils;

public class WebsiteTranslationFinishedPanel extends PhetPanel {

    private Component publisher;

    public WebsiteTranslationFinishedPanel( String id, final PageContext context, String finalText, final File translationDir, final File docRoot ) {
        super( id, context );

        add( new RawLabel( "text", finalText ) );

        List<File> fileList = Arrays.asList( translationDir.listFiles() );

        Collections.sort( fileList, new Comparator<File>() {
            public int compare( File a, File b ) {
                return a.getName().compareToIgnoreCase( b.getName() );
            }
        } );

        add( new ListView<File>( "file", fileList ) {
            @Override
            protected void populateItem( ListItem<File> item ) {
                try {
                    File file = item.getModelObject();
                    String path = file.getCanonicalPath().substring( file.getCanonicalPath().indexOf( "/staging/translations" ) );

                    RawLink link = new RawLink( "link", path );
                    item.add( link );

                    link.add( new Label( "name", file.getName() ) );
                }
                catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        } );

        AjaxLink publishLink = new AjaxLink( "publish-link" ) {
            @Override
            public void onClick( AjaxRequestTarget target ) {
                target.addComponent( this );
                setVisible( false );

                final File simsDir = PhetWicketApplication.get().getSimulationsRoot();
                final File docRoot = PhetWicketApplication.get().getWebsiteProperties().getPhetDocumentRoot();

                ComponentThread thread = new LoggerComponentThread( WebsiteTranslationDeployPublisher.getLogger() ) {
                    @Override
                    public boolean process() throws IOException, InterruptedException {
                        WebsiteTranslationDeployPublisher runner = new WebsiteTranslationDeployPublisher( simsDir, docRoot );

                        runner.publishTranslations( translationDir );

                        // open a custom session
                        Session session = HibernateUtils.getInstance().openSession();

                        for ( String projectName : runner.getDeployedProjectNames() ) {
                            Project.synchronizeProject(
                                    docRoot,
                                    session,
                                    projectName,
                                    WebsiteTranslationDeployPublisher.getLogger()
                            );
                        }

                        session.close();

                        return true;
                    }
                };

                Component newPublisher = new ComponentThreadStatusPanel( "publisher", context, thread, 1000 );
                newPublisher.setOutputMarkupId( true );
                newPublisher.setMarkupId( "deploy-publisher" );
                publisher.replaceWith( newPublisher );
                target.addComponent( newPublisher );

                thread.start();
            }
        };
        publishLink.setOutputMarkupId( true );
        add( publishLink );

        publisher = new Label( "publisher", "" );
        publisher.setOutputMarkupId( true );
        publisher.setMarkupId( "deploy-publisher" );
        add( publisher );
    }
}