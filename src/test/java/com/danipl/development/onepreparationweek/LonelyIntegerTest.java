package com.danipl.development.onepreparationweek;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.danipl.development.onepreparationweek.LonelyInteger.lonelyinteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LonelyIntegerTest {

    @Test
    public void testSimple() {
        assertEquals(4, lonelyinteger(List.of(1, 2, 3, 4, 3, 2, 1)));
    }

}
