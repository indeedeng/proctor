package com.indeed.proctor.store;

public interface GitProctorCallable<T> {
    T call() throws StoreException.TestUpdateException;
}
