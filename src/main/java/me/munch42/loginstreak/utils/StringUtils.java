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
}
