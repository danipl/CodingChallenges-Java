package com.danipl.development.onepreparationweek;

import org.junit.jupiter.api.Test;

import static com.danipl.development.onepreparationweek.TimeConversion.timeConversion;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeConversionTest {

    @Test
    public void testSimple(){
        assertEquals("12:01:00", timeConversion("12:01:00PM"));
    }

    @Test
    public void testSimple02(){
        assertEquals("00:01:00", timeConversion("12:01:00AM"));
    }

}
