package edu.colorado.phet.website.translation.entities;

/**
 * Should contain strings that only need to be translated into English, but not other languages
 */
public class EnglishEntity extends TranslationEntity {
    public EnglishEntity() {
        addString( "admin.keyword.create.duplicate" );
        addString( "admin.keyword.create.exists" );
    }

    public String getDisplayName() {
        return "English";
    }
}
