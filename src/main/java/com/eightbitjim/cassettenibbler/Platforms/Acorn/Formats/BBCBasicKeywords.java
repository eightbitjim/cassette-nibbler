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
    private static final int EXTENDED_TOKEN_1 = 0xc6;
    private static final int EXTENDED_TOKEN_2 = 0xc7;
    private static final int EXTENDED_TOKEN_3 = 0xc8;

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

    private static final String [] extendedTokens1 = {
            "SUM", "BEAT"
    };

    private static final String [] extendedTokens2 = {
            "EXTENDED TOKENS NOT YET IMPLEMENTED"
    };

    private static final String [] extendedTokens3 = {
            "EXTENDED TOKENS NOT YET IMPLEMENTED"
    };

    public static String getKeywordForToken(int token) {
        token -= firstKeywordTokenValue;
        if (token < 0 || token >= keyword.length)
            return "INVALID_KEYWORD_TOKEN";
        else
            return keyword[token];
    }

    public static String getKayboardForExtendedToken(int firstByte, int secondByte) {
        switch (firstByte) {
            case EXTENDED_TOKEN_1:
                return decodeExtendedToken1(secondByte);
            case EXTENDED_TOKEN_2:
                return decodeExtendedToken2(secondByte);
            case EXTENDED_TOKEN_3:
                return decodeExtendedToken3(secondByte);
            default:
                return "INVALID_EXTENDED_TOKEN";
        }
    }

    public static String decodeExtendedToken1(int token) {
        token -= 0x8e;
        if (token < 0 || token >= extendedTokens1.length)
            return "INVALID_EXTENDED_TOKEN";
        else
            return extendedTokens1[token];
    }

    public static String decodeExtendedToken2(int token) {
        token -= 0x8e;
        if (token < 0 || token >= extendedTokens2.length)
            return "INVALID_EXTENDED_TOKEN";
        else
            return extendedTokens1[token];
    }

    public static String decodeExtendedToken3(int token) {
        token -= 0x8e;
        if (token < 0 || token >= extendedTokens3.length)
            return "INVALID_EXTENDED_TOKEN";
        else
            return extendedTokens1[token];
    }
}
