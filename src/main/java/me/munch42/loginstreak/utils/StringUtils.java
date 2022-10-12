package me.munch42.loginstreak.utils;

import java.util.ArrayList;

public class StringUtils {
    public static String[] trimSpacesFromStringArray(String[] array){
        ArrayList<String> trimmedStringsList = new ArrayList<String>();

        for(String str : array){
            trimmedStringsList.add(str.trim());
        }

        String[] newArray = new String[trimmedStringsList.size()];
        trimmedStringsList.toArray(newArray);

        return newArray;
    }

    public static void printArray(String[] array, boolean numberItems){
        int i = 0;

        for (String x: array) {
            if (numberItems) {
                System.out.println(i + ": " + x);
                i++;
            } else {
                System.out.println(x);
            }
        }
    }
}
