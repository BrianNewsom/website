/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.translation.entities;

public class CommonEntity extends TranslationEntity {
    public CommonEntity() {
        addString( "language.dir", "This should be either 'ltr' for left-to-right languages, or 'rtl' for right-to-left languages (both without the quotes)." );
        addString( "language.name", "This will be the language name displayed to the user (translated into the native language)." );
        addString( "footer.someRightsReserved", "Copyright notice in the footer" );
        addString( "error.pageNotFound", "Displayed when a page is not found" );
        addString( "translation.credits", "Please fill this in with whatever credits you want to show. This will be displayed above the language selections when your translation is being used." );
    }

    public String getDisplayName() {
        return "Common";
    }
}
