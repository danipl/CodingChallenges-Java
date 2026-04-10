package com.danipl.development.leetcode;

import java.util.HashMap;
import java.util.Map;

/**
 * https://leetcode.com/problems/ransom-note/
 */
public class RansomNote {

    public boolean canConstruct(String ransomNote, String magazine) {
        final Map<Character, Integer> map = new HashMap<>();
        for (int pos = 0; pos < magazine.length(); pos++) {
            final char character = magazine.charAt(pos);
            final int counter = map.getOrDefault(character, 0);
            map.put(character, counter + 1);
        }
        for (int pos = 0; pos < ransomNote.length(); pos++) {
            final char character = ransomNote.charAt(pos);
            final int counter = map.getOrDefault(character, 0);
            if (counter == 0) {
                return false;
            }
            map.put(character, counter - 1);
        }
        return true;
    }

}
