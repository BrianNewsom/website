/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.translation;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;

import edu.colorado.phet.website.components.InvisibleComponent;
import edu.colorado.phet.website.panels.PhetPanel;
import edu.colorado.phet.website.translation.entities.TranslationEntity;
import edu.colorado.phet.website.util.PageContext;
import edu.colorado.phet.website.util.PhetRequestCycle;

public class PreviewHolder extends PhetPanel {

    public PreviewHolder( String id, final PageContext context, TranslationEntity entity ) {
        super( id, context );

        setOutputMarkupId( true );

        add( new ListView<PhetPanelPreview>( "sub-panels", entity.getPreviews() ) {
            protected void populateItem( ListItem<PhetPanelPreview> item ) {
                PhetPanelPreview preview = item.getModelObject();
                item.add( preview.getNewPanel( "holder-sub-panel", context, (PhetRequestCycle) getRequestCycle() ) );
                item.add( new Label( "preview-number", String.valueOf( item.getIndex() + 1 ) ) );
                item.add( new Label( "preview-name", preview.getName() ) );
            }
        } );

        if ( entity.hasPreviews() ) {
            add( new InvisibleComponent( "previews-unavailable" ) );
        }
        else {
            add( new Label( "previews-unavailable", "Previews are unavailable for this section." ) );
        }
    }

}
