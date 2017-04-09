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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats;

public class PETSCII {
    public static char printableCharacterForPetsciiCode(int code) {
        if (code <13) return '~';
        if (code == 13) return '\n';
        if (code < 32) return '~';
        if (code < 65) return (char)code;
        if (code < 91) return (char)(97 + (code - 65));
        if (code < 97) return (char)code;
        if (code < 123) return (char)(65 + (code - 97));
        if (code < 193) return '~';
        if (code < 218) return (char)(65 + (code - 193));
        return '~';
    }

    public static String printableStringFromPetsciiBytes(byte [] data) {
        StringBuilder builder = new StringBuilder();
        for (byte b : data) {
            int code = Byte.toUnsignedInt(b);
            builder.append(printableCharacterForPetsciiCode(code));
        }

        return builder.toString();
    }
}
