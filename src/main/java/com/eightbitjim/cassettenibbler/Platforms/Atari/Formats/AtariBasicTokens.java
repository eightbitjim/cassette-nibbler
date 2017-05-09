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

package com.eightbitjim.cassettenibbler.Platforms.Atari.Formats;

public class AtariBasicTokens {
    static String [] command = {
            "REM", "DATA", "INPUT", "COLOR", "LIST", "ENTER", "LET", "IF",
            "FOR", "NEXT", "GOTO", "GO TO", "GOSUB", "TRAP", "BYE", "CONT", "COM",
            "CLOSE", "CLR", "DEG", "DIM", "END", "NEW", "OPEN", "LOAD",
            "SAVE", "STATUS", "NOTE", "POINT", "XIO", "ON", "POKE", "PRINT",
            "RAD", "READ", "RESTORE", "RETURN", "RUN", "STOP", "POP", "PRINT",
            "GET", "PUT", "GRAPHICS", "PLOT", "POSITION", "DOS", "DRAWTO", "SETCOLOR",
            "LOCATE", "SOUND", "LPRINT", "CSAVE", "CLOAD", "LET", "ERROR"
    };

    static String [] operatorOrFunction = {
            // Operators
            ",", "$", ":", ";", "", "GOTO", "GOSUB", "TO",
            "STEP", "THEN", "#", "<=", "<>", ">=", "<", ">",
            "=", "", "*", "+", "-", "/", "NOT", "OR",
            "AND", "(", ")", "=", "=", "<=", "<>", ">=",
            "<", ">", "=", "+", "-", "(", "(", "(",
            "(", "(", ",",

            // Functions
            "STR$", "CHR$", "USR", "ASC", "VAL", "LEN", "ADR", "ATN",
            "COS", "PEEK", "SIN", "RND", "FRE", "EXP", "LOG", "CLOG",
            "SQR", "SGN", "ABS", "INT", "PADDLE", "STICK", "PTRIG", "STRIG"
    };

    public static String printableStringForCommandByteCode(int code) {
            int tokenOffset = code;
            if (tokenOffset > command.length - 1 || tokenOffset < 0)
                return "(invalid command)";
            else
                return command[tokenOffset] + " ";
    }

    public static String printableStringForOperatorOrTokenByteCode(int code) {
        final int MINIMUM_OPERATOR_VALUE = 18;
        int tokenOffset = code - MINIMUM_OPERATOR_VALUE;
        if (tokenOffset > operatorOrFunction.length - 1 || tokenOffset < 0)
            return Character.toString(asciiCharacterforATASCIICode(code));
        else
            return operatorOrFunction[tokenOffset];
    }

    public static char asciiCharacterforATASCIICode(int code) {
        if (code < 32)
            return '?';

        switch (code) {
            case 155:
                return '\n';
            case 125:
                return '?';
            case 253:
                return '~';
        }

        if (code >= 128)
            code -= 128;

        return (char)code;
    }
}

