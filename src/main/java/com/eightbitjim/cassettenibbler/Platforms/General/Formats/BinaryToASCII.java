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
    public static byte [] removeUnprintableCharactersFrombinaryCharacterArray(byte [] binaryData) {
        byte [] outputData = new byte[binaryData.length];
        int position = 0;
        for (byte b : binaryData) {
            byte valueToStore;
            valueToStore = (byte)getPrintableCharacterForBinaryCharacterCode(b);

            outputData[position] = valueToStore;
            position++;
        }

        return outputData;
    }

    public static char getPrintableCharacterForBinaryCharacterCode(byte code) {
        char convertedValue = (char)code;
        if (Character.isLetterOrDigit(convertedValue))
            return convertedValue;

        if (convertedValue == 0x0a || convertedValue == 0x0d)
            return convertedValue;

        if (convertedValue >= 0x20 && convertedValue < 0x80)
            return convertedValue;

        return '~';
    }
}
