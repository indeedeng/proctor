package com.indeed.proctor;

/**
 * @author matts
 */
public class Config {
    public static final class Account {
        public final int id;

        @SuppressWarnings("UnusedDeclaration")
        public Account() {
            this(0);
        }

        public Account(final int id) {
            this.id = id;
        }
    }
}
