/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.panels;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainerWithAssociatedMarkup;
import org.apache.wicket.model.Model;

import edu.colorado.phet.common.phetcommon.util.LocaleUtils;
import edu.colorado.phet.website.components.RawLabel;
import edu.colorado.phet.website.translation.PhetLocalizer;
import edu.colorado.phet.website.util.HtmlUtils;
import edu.colorado.phet.website.util.PageContext;

public class SearchPanel extends PhetPanel {
    public SearchPanel( String id, PageContext context ) {
        super( id, context );

        // NOTE: Search JS will be loaded asynchronously

        //add( JavascriptPackageResource.getHeaderContribution( JS.JQUERY_AUTOCOMPLETE ) );

        // store the locale so we can pick it up easily from JavaScript
        String jsPhetLocale = "var phetLocale = \"" + LocaleUtils.localeToString( getMyLocale() ) + "\";";
        String jsSimulationString = "var phetSimulationString = \"" + HtmlUtils.encodeForAttribute( PhetLocalizer.get().getString( "search.autocomplete.simulation", this ) ) + "\";";
        add( new RawLabel( "javascript-search", jsPhetLocale + jsSimulationString ) );

        WebMarkupContainerWithAssociatedMarkup form = new WebMarkupContainerWithAssociatedMarkup( "search-form" );
        form.add( new AttributeAppender( "action", true, new Model<String>( context.getPrefix() + "search" ), " " ) );
        add( form );
    }
}