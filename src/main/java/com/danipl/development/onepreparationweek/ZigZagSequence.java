package com.danipl.development.onepreparationweek;

import java.util.Arrays;

/**
 * @see https://www.hackerrank.com/challenges/one-week-preparation-kit-zig-zag-sequence/problem?isFullScreen=true&h_l=interview&playlist_slugs%5B%5D=preparation-kits&playlist_slugs%5B%5D=one-week-preparation-kit&playlist_slugs%5B%5D=one-week-day-three
 */
public class ZigZagSequence {

    public static void findZigZagSequence(int[] a, int n) {
        Arrays.sort(a);
        int mid = ((n + 1)/2) - 1; //
        int temp = a[mid];
        a[mid] = a[n - 1];
        a[n - 1] = temp;

        int st = mid + 1;
        int ed = n - 2; //
        while(st <= ed){
            temp = a[st];
            a[st] = a[ed];
            a[ed] = temp;
            st = st + 1;
            ed = ed - 1; //
        }
        for(int i = 0; i < n; i++){
            if(i > 0) System.out.print(" ");
            System.out.print(a[i]);
        }
        System.out.println();
    }

}
