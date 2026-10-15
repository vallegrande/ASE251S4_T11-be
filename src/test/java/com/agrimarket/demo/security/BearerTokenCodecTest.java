package com.agrimarket.demo.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BearerTokenCodecTest {

    @Test
    void generatesUnpredictableOpaqueTokensAndStoresOnlyTheirHash() {
        String firstToken = BearerTokenCodec.generate();
        String secondToken = BearerTokenCodec.generate();

        assertEquals(43, firstToken.length());
        assertNotEquals(firstToken, secondToken);
        assertNotEquals(firstToken, BearerTokenCodec.hash(firstToken));
        assertEquals(64, BearerTokenCodec.hash(firstToken).length());
        assertTrue(firstToken.matches("[A-Za-z0-9_-]+"));
    }
}
