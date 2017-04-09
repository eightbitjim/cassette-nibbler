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

public class SpectrumASCII {
    static String [] basicKeyword = {
        "RND", "INKEY$", "PI", "FN", "POINT", "SCREEN$", "ATTR",
            "AT", "TAB", "VAL$", "CODE", "VAL", "LEN", "SIN", "COS",
            "TAN", "ASN", "ACS", "ATN", "LN", "EXP", "INT", "SOR",
            "SGN", "ABS", "PEEK", "IN", "USR", "STR$", "CHR$", "NOT",
            "BIN", "OR", "AND", "<=" , ">=", "<>", "LINE", "THEN",
            "TO", "STEP", "DEF FN", "CAT", "FORMAT", "MOVE", "ERASE",
            "OPEN #", "CLOSE #", "MERGE", "VERIFY", "BEEP", "CIRCLE",
            "INK", "PAPER", "FLASH", "BRIGHT", "INVERSE", "OVER",
            "OUT", "LPRINT", "LLIST", "STOP", "READ", "DATA", "RESTORE",
            "NEW", "BORDER", "CONTINUE", "DIM", "REM", "FOR", "GO TO",
            "GO SUB", "INPUT", "LOAD", "LIST", "LET", "PAUSE", "NEXT",
            "POKE", "PRINT", "PLOT", "RUN", "SAVE", "RANDOMIZE", "IF",
            "CLS", "DRAW", "CLEAR", "RETURN", "COPY" };

    public static String printableStringForZXCode(int code) {
        if (code < 32) return "";
        if (code < 128) return Character.toString((char)code);
        if (code < 144) return "#";
        if (code < 165) return "(uknown)";
        return basicKeyword[code - 165] + " ";
    }
}

