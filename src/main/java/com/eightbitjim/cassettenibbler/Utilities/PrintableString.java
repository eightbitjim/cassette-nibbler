/*
 * Copyright (c) 2017. James Lean
 * This file is part of cassette-nibbler.
 *
 * cassette-nibbler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * cassette-nibbler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with cassette-nibbler.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.eightbitjim.cassettenibbler.Utilities;

import java.awt.event.KeyEvent;

public class PrintableString {
    public static String convertToPrintable(String inputString) {
        StringBuilder builder = new StringBuilder();
        int lengthOfString = inputString.length();
        for (int i = 0; i < lengthOfString; i++) {
            char c = inputString.charAt(i);
            if (isPrintableChar(c))
                builder.append(c);
        }

        return builder.toString();
    }

    public static String convertToSuitableFilename(String nameWithoutPath) {
        String filename = convertToPrintable(nameWithoutPath);
        filename = removeReservedCharacters(filename);
        if (filename.length() == 0)
            filename = "unnamed";
        
        return filename;
    }

    private static String removeReservedCharacters(String name) {
        String nameToReturn = name.replaceAll("/", "").replaceAll("\\x2a", "").replaceAll("\\\\", "");
        while (nameToReturn.startsWith(".")) {
            if (nameToReturn.length() == 1)
                nameToReturn = "";
            else
                nameToReturn = nameToReturn.substring(1);
        }

        return nameToReturn;
    }

    private static boolean isPrintableChar( char c ) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }
}
