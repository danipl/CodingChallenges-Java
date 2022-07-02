package com.danipl.hackerrank.onepreparationweek;

import static java.time.LocalTime.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Locale.US;

public class TimeConversion {

    public static String timeConversion(final String dateString) {
        return parse(dateString, ofPattern("hh:mm:ssa", US)).format(ofPattern("HH:mm:ss"));
    }

}
