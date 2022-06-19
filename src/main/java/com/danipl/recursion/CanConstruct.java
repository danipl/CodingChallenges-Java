package com.danipl.recursion;

import java.util.HashMap;
import java.util.Map;

/**
 * Determine if the text might be constructed by the sequences provided.
 */
public class CanConstruct {

    public static boolean normal(String text, String[] seqs) {
        if (text.isEmpty()) return true;

        for (final String seq : seqs) {
            if (!text.startsWith(seq)) {
                continue;
            }

            final String subText = text.substring(seq.length());

            if (normal(subText, seqs)) {
                return true;
            }
        }

        return false;
    }

    public static boolean memo(String text, String[] seqs) {
        return memo(text, seqs, new HashMap<>());
    }

    public static boolean memo(String text, String[] seqs, Map<String, Boolean> memo) {
        if (text.isEmpty()) return true;

        if (memo.containsKey(text)) {
            return memo.get(text);
        }

        for (final String seq : seqs) {
            if (!text.startsWith(seq)) {
                continue;
            }

            final String subText = text.substring(seq.length());

            if (memo(subText, seqs, memo)) {
                return true;
            }
            memo.put(text, false);
        }

        return false;
    }

}
