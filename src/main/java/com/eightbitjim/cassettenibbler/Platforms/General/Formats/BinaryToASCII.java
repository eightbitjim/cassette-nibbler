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

package com.eightbitjim.cassettenibbler.Platforms.General.Formats;

public class BinaryToASCII {
    public static byte [] convert(byte [] binaryData) {
        byte [] outputData = new byte[binaryData.length];
        int position = 0;
        for (byte b : binaryData) {
            byte valueToStore;
            valueToStore = b;
            if (!Character.isLetterOrDigit((char)b))
                valueToStore = '~';

            outputData[position] = valueToStore;
            position++;
        }

        return outputData;
    }
}
