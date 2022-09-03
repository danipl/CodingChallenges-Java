package com.danipl.leetcode;

import java.util.ArrayList;
import java.util.List;

public class FizzBuzz {

    public List<String> fizzBuzz(int n) {
        final List<String> list = new ArrayList();
        for (int pos = 1; pos < (n + 1); pos++) {
            final StringBuffer sb = new StringBuffer();
            if (pos % 3 == 0) {
                sb.append("Fizz");
            }
            if (pos % 5 == 0) {
                sb.append("Buzz");
            }
            list.add((sb.length() == 0) ? String.valueOf(pos) : sb.toString());
        }
        return list;
    }

}
