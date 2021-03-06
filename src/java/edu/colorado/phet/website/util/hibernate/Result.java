/*
 * Copyright 2010, University of Colorado
 */

package edu.colorado.phet.website.util.hibernate;

public class Result<T> {
    public final boolean success;
    public final T value;
    public final Exception exception;

    public Result( boolean success, T value, Exception exception ) {
        this.success = success;
        this.value = value;
        this.exception = exception;
    }
}
