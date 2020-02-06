package com.indeed.proctor.common.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestNameObfuscator {

    @Test
    public void obfuscateName() {
        final NameObfuscator hasher = new NameObfuscator();
        assertThat(hasher.obfuscateTestName("payloaded_excluded"))
                .isEqualTo("7f5e9c05a1d834b63e33e21e0071f872c4f8eb013de38f6813e6636dbd5fa1c9");
        assertThat(hasher.obfuscateTestName("string1"))
                .isEqualTo("93fedde43203e0a76172135221b8636313635d7afff96a490ae9066330505d47");
        assertThat(hasher.obfuscateTestName("asdf"))
                .isEqualTo("f0e4c2f76c58916ec258f246851bea091d14d4247a2fc3e18694461b1816e13b");
        assertThat(hasher.obfuscateTestName("ec2bda07342460b4699a7a8c5ff6a0f393fcebd78bbb082f4ea1a4d57db78ac1"))
                .isEqualTo("6bd2f60033424528cd54f3e4faecae5670efd01e453becdc0f9a7150977777d9");
        assertThat(hasher.obfuscateTestName("][l!#@$%$&%$#&^}%{$#}%$L:>DF:G<SREG"))
                .isEqualTo("540390f88e74eb9fecdabec883142b9bf8a12ee5355adee159703c8a7b5f2c8f");

    }
}
