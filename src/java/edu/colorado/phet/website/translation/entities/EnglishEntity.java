/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.translation.entities;

/**
 * Should contain strings that only need to be translated into English, but not other languages
 */
public class EnglishEntity extends TranslationEntity {
    public EnglishEntity() {

        addString( "emergencyMessage", "If non-blank, will be shown as an emergency banner at the top of the page" );

        addString( "admin.keyword.create.duplicate" );
        addString( "admin.keyword.create.exists" );

        addString( "sponsors.sim.supportedByThe" );

        addString( "home.meta", "Home (Index) page meta description" );
    }

    public String getDisplayName() {
        return "English";
    }
}
