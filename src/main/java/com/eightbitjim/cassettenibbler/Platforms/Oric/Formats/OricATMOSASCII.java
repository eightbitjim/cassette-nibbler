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

package com.eightbitjim.cassettenibbler.Platforms.Oric.Formats;

public class OricATMOSASCII {
    static String [] basicKeyword = {
            "END", "EDIT",
            "STORE", "RECALL",
            "TRON", "TROFF",
            "POP", "PLOT",

            "PULL", "LORES",
            "DOKE", "REPEAT",
            "UNTIL", "FOR",
            "LLIST", "LPRINT",

            "NEXT", "DATA",
            "INPUT", "DIM",
            "CLS", "READ",
            "LET", "GOTO",

            "RUN", "IF",
            "RESTORE", "GOSUB",
            "RETURN", "REM",
            "HIMEM", "GRAB",

            "RELEASE", "TEXT",
            "HIRES", "SHOOT",
            "EXPLODE", "ZAP",
            "PING", "SOUND",

            "MUSIC", "PLAY",
            "CURSET", "CURMOV",
            "DRAW", "CIRCLE",
            "PATERN", "FILL",

            "CHAR", "PAPER",
            "INK", "STOP",
            "ON", "WAIT",
            "CLOAD", "CSAVE",

            "DEF", "POKE",
            "PRINT", "CONT",
            "LIST", "CLEAR",
            "GET", "CALL",

            "!", "NEW",
            "TAB(", "TO",
            "FN", "SPC(",
            "@", "AUTO",

            "ELSE", "THEN",
            "NOT", "STEP",
            "+", "-",
            "*", "/",

            "^", "AND",
            "OR", ">",
            "=", "<",
            "SGN", "INT",

            "ABS", "USR",
            "FRE", "POS",
            "HEX$", "&",
            "SQR", "RND",

            "LN", "EXP",
            "COS", "SIN",
            "TAN", "ATN",
            "PEEK", "DEEK",

            "LOG", "LEN",
            "STR$", "VAL",
            "ASC", "CHR$",
            "PI", "TRUE",

            "FALSE", "KEY$",
            "SCRN", "POINT",
            "LEFT$", "RIGHT$",
            "MID$", "(unused)"
    };

    public static String printableStringForByteCode(int code) {
        if (code < 128) return Character.toString((char)code);
        return basicKeyword[code - 128] + " ";
    }
}

