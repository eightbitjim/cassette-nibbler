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

package com.eightbitjim.cassettenibbler.Platforms.Apple.Formats;

import com.eightbitjim.cassettenibbler.Platforms.General.Formats.BinaryToASCII;

public class ApplesoftBasicTokens {
    private static final int TOKEN_VALUE = 0x80;

    static String [] token = {
            "END", "FOR", "NEXT", "DATA", "INPUT", "DEL", "DIM", "READ",
            "GR", "TEXT", "PR #", "IN #", "CALL", "PLOT", "HLIN", "VLIN ",
            "HGR2", "HGR", "HCOLOR=", "HPLOT", "DRAW", "XDRAW", "HTAB", "HOME",
            "ROT=", "SCALE=", "SHLOAD", "TRACE", "NOTRACE", "NORMAL", "INVERSE",
            "FLASH", "COLOR=", "POP", "VTAB", "HIMEM:", "LOMEM:", "ONERR", "RESUME",
            "RECALL", "STORE", "SPEED=", "LET", "GOTO", "RUN", "IF", "RESTORE", "&",
            "GOSUB", "RETURN", "REM", "STOP", "ON", "WAIT", "LOAD", "SAVE", "DEF FN",
            "POKE", "PRINT", "CONT", "LIST", "CLEAR", "GET", "NEW", "TAB", "TO", "FN",
            "SPC(", "THEN", "AT", "NOT", "STEP", "+", "-", "*", "/", ";", "AND", "OR",
            ">", "=", "<", "SGN", "INT", "ABS", "USR", "FRE", "SCRN (", "PDL", "POS",
            "SQR", "RND", "LOG", "EXP", "COS", "SIN", "TAN", "ATN", "PEEK", "LEN", "STR$",
            "VAL", "ASC", "CHR$", "LEFT$", "RIGHT$", "MID$"
    };

    public static String printableStringForByteCode(int code) {
        if (code < TOKEN_VALUE)
            return Character.toString(BinaryToASCII.getPrintableCharacterForBinaryCharacterCode((byte)code));
        else {
            int tokenOffset = code - TOKEN_VALUE;
            if (tokenOffset > token.length - 1)
                return "(invalid token)";
            else
                return token[tokenOffset] + " ";
        }
    }
}

