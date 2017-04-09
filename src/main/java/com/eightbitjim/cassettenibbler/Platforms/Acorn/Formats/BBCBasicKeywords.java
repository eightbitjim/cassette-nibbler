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

package com.eightbitjim.cassettenibbler.Platforms.Acorn.Formats;

class BBCBasicKeywords {
    private static final int firstKeywordTokenValue = 0x7f;
    private static final String [] keyword = {
            "OTHERWISE",
            "AND", "DIV", "EOR", "MOD", "OR", "ERROR", "LINE", "OFF",
            "STEP", "SPC", "TAB(", "ELSE", "THEN", "<line>", "OPENIN", "PTR",
            "PAGE", "TIME", "LOMEM", "HIMEM", "ABS", "ACS", "ADVAL", "ASC",
            "ASN", "ATN", "BGET", "COS", "COUNT", "DEG", "ERL", "ERR",

            "EVAL", "EXP", "EXT", "FALSE", "FN", "GET", "INKEY", "INSTR(",
            "INT", "LEN", "LN", "LOG", "NOT", "OPENUP", "OPENOUT", "PI",

            "POINT(", "POS", "RAD", "RND", "SGN", "SIN", "SQR", "TAN",
            "TO", "TRUE", "USR", "VAL", "VPOS", "CHR$", "GET$", "INKEY$",

            "LEFT$(", "MID$(", "RIGHT$(", "STR$", "STRING$(", "EOF",
            "<ESCFN>", "<ESCCOM>", "<ESCSTMT>",
            "WHEN", "OF", "ENDCASE", "ELSE"
            , "ENDIF", "ENDWHILE", "PTR",

            "PAGE", "TIME", "LOMEM", "HIMEM", "SOUND", "BPUT", "CALL", "CHAIN",
            "CLEAR", "CLOSE", "CLG", "CLS", "DATA", "DEF", "DIM", "DRAW",

            "END", "ENDPROC", "ENVELOPE", "FOR", "GOSUB", "GOTO", "GCOL", "IF",
            "INPUT", "LET", "LOCAL", "MODE", "MOVE", "NEXT", "ON", "VDU",

            "PLOT", "PRINT", "PROC", "READ", "REM", "REPEAT", "REPORT", "RESTORE",
            "RETURN", "RUN", "STOP", "COLOUR", "TRACE", "UNTIL", "WIDTH", "OSCLI"
    };

    public String getKeywordForToken(int token) {
        token -= firstKeywordTokenValue;
        if (token < 0 || token >= keyword.length)
            return "INVALID_KEYWORD_TOKEN";
        else
            return keyword[token];
    }
}
