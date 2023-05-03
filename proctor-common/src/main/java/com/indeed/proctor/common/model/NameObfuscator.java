package com.indeed.proctor.common.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class to ensure that all obfuscations of proctor test name use the same hashing mechanism.
 */
public class NameObfuscator {

    /**
     * wraps the function by which testnames are obfuscated
     */
    public String obfuscateTestName(final String stringToHash) {
        return sha256Hash(stringToHash);
    }

    /**
     * perform sha256 hashing
     */
    private String sha256Hash(final String stringToHash) {
        // must create new MessageDigest, because not thread-safe
        final byte[] encodedHash = getSha256MessageDigest().digest(stringToHash.getBytes(StandardCharsets.UTF_8));
        // Convert the byte array to a string as the encoded obfuscateTestName is not a string by default
        // Common recipe that deals with inserting leading zeros
        final StringBuilder hexString = new StringBuilder(64);
        for (final byte b : encodedHash) {
            if ((0xff & b) < 0x10) {
                hexString.append('0').append(Integer.toHexString((0xFF & b)));
            } else {
                hexString.append(Integer.toHexString(0xFF & b));
            }
        }
        return hexString.toString();
    }

    /**
     * @return (non-thread-safe) MessageDigest.
     */
    private static MessageDigest getSha256MessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot initialize SHA-256 hasher", e);
        }
    }

}
