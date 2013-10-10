package com.indeed.proctor.store;

import java.io.Closeable;

public interface ProctorStore extends Closeable, ProctorReader, ProctorWriter {
}
