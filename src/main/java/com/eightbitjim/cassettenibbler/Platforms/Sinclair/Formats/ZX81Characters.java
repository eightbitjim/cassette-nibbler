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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.Formats;

public class ZX81Characters {
    static String [] printableCharacter = {
            " ", " ", "#", "#", "#", "#", "#", "#", "#", "#", "#", "\"", "£", "$", ":", "?",
            "(",  ")", ">", "<", "=", "+", "-", "*", "/", ";", ",", ".", "0", "1", "2", "3",
            "4",  "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K",  "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",

            "RND ",  "INKEY$ ", "PI ", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".",
            ".",  ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".",
            ".",  ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".", ".",
            ".",  ".", ".", ".", "@", ".", "\n", ".", ".", ".", ".", ".", ".", ".", ".", ".",

            // Inverse
            " ", " ", "#", "#", "#", "#", "#", "#", "#", "#", "#", "\"", "£", "$", ":", "?",
            "(",  ")", ">", "<", "=", "+", "-", "*", "/", ";", ",", ".", "0", "1", "2", "3",
            "4",  "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K",  "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",

            "\"\"",  "AT ", "TAB ", ".", "CODE ", "VAL ", "LEN ", "SIN ", "COS ", "TAN ", "ASN ", "ACS ", "ATN ", "LN ", "EXP ", "INT ",
            "SQR ",  "SGN ", "ABS ", "PEEK ", "USR ", "STR$ ", "CHR$ ", "NOT ", "**", "OR " , "AND ", "<=", ">=", "<>", "THEN ", "TO ",
            "STEP ", "LPRINT ", "LLIST ", "STOP ", "SLOW ", "FAST ", "NEW ", "SCROLL ", "CONT ", "DIM ", "REM ", "FOR ", "GOTO ", "GOSUB ", "INPUT ", "LOAD ",
            "LIST ", "LET ", "PAUSE ", "NEXT ", "POKE ", "PRINT ", "PLOT ", "RUN ", "SAVE ", "RAND ", "IF ", "CLS ", "UNPLOT ", "CLEAR ", "RETURN ", "COPY "
    };

    public static String printableStringForZXCode(int code) {
        return printableCharacter[code];
    }
}

