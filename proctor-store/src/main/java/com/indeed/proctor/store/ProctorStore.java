package com.indeed.proctor.store;

import java.io.Closeable;

public interface ProctorStore extends Closeable, ProctorReader, ProctorWriter {
    /** This method returns a name of the ProctorStore instance for logging purpose. */
    String getName();
}
