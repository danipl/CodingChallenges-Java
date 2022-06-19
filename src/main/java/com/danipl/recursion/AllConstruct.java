package com.danipl.recursion;

import java.util.*;

/**
 * Returns all possible combinations of sequences constructing the target.
 */
public class AllConstruct {

    public static Collection<List<String>> normal(String text, String[] seqs) {
        if (text.isEmpty()) return new ArrayList<>() {{
            this.add(Collections.emptyList());
        }};

        final Collection<List<String>> combs = new ArrayList<>();

        for (final String seq : seqs) {
            if (!text.startsWith(seq)) continue;
            final String subText = text.substring(seq.length());
            final Collection<List<String>> newCombs = normal(subText, seqs);
            if (!newCombs.isEmpty()) {
                for (final List<String> subComb : newCombs) {
                    final List<String> newComb = new ArrayList<>() {{
                        super.addAll(subComb);
                        super.add(seq);
                    }};
                    combs.add(newComb);
                }
            }
        }

        return combs;
    }

    public static Collection<List<String>> memo(String text, String[] seqs) {
        return memo(text, seqs, new HashMap<>());
    }

    public static Collection<List<String>> memo(String text, String[] seqs, Map<String, Collection<List<String>>> memo) {
        if (text.isEmpty()) return new ArrayList<>() {{
            this.add(Collections.emptyList());
        }};

        if (memo.containsKey(text)) {
            return memo.get(text);
        }

        final Collection<List<String>> combs = new ArrayList<>();

        for (final String seq : seqs) {
            if (!text.startsWith(seq)) continue;
            final String subText = text.substring(seq.length());
            final Collection<List<String>> newCombs = memo(subText, seqs, memo);
            if (!newCombs.isEmpty()) {
                for (final List<String> subComb : newCombs) {
                    final List<String> newComb = new ArrayList<>() {{
                        super.addAll(subComb);
                        super.add(seq);
                    }};
                    combs.add(newComb);
                }
            }
        }

        memo.put(text, combs);

        return combs;
    }

}
