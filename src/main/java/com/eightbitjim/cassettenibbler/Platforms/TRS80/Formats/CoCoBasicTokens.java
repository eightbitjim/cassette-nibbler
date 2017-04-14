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

package com.eightbitjim.cassettenibbler.Platforms.TRS80.Formats;

public class CoCoBasicTokens {
    private static final int MINIMUM_TOKEN_VALUE = 0x80;

    static String [] tokenNoEscape = {
            "FOR", "GO", "REM", "'", "ELSE", "IF", "DATA", "PRINT",
            "ON", "INPUT", "END", "NEXT", "DIM", "READ", "RUN",
            "RESTORE", "RETURN", "STOP", "POKE", "CONT", "LIST", "CLEAR", "NEW",
            "CLOAD", "CSAVE", "OPEN", "CLOSE", "LLIST", "SET", "RESET",
            "CLS", "MOTOR", "SOUND", "AUDIO", "EXEC", "SKIPF", "TAB(", "TO", "SUB", "THEN",
            "NOT", "STEP", "OFF", "+", "-", "*", "/", "^",
            "AND", "OR", ">", "=", "<", "DEL", "EDIT", "TRON", "TROFF",
            "DEF", "LET", "LINE", "PCLS", "PSET", "PRESET", "SCREEN", "PCLEAR",
            "COLOR", "CIRCLE", "PAINT", "GET", "PUT", "DRAW", "PCOPY", "PMODE", "PLAY",
            "DLOAD", "RENUM", "FN", "USING"
    };

    static String[] tokenWithEscape = {
            "SGN" ,"INT" ,"ABS" ,"USR", "RND",
            "SIN" ,"PEEK" ,"LEN" ,"STR$" ,"VAL",
            "ASC" ,"CHR$" ,"EOF" ,"JOYSTK" ,"LEFT$" ,"RIGHT$",
            "MID$" ,"POINT" ,"INKEY$" ,"MEM", "ATN", "COS", "TAN", "EXP", "FIX",
            "LOG", "POS", "SQR", "HEX$", "VARPTR" ,"INSTR" ,"TIMER" ,"PPOINT",
            "STRING$"
    };

    public static String printableStringForByteCode(int code, boolean functionEscape) {
        if (code < MINIMUM_TOKEN_VALUE)
            return Character.toString((char)code);

        code -= MINIMUM_TOKEN_VALUE;
        String [] tokenArray = functionEscape ? tokenWithEscape : tokenNoEscape;
        if (tokenArray.length < code + 1)
            return "(invalid token)";

        String tokenString = tokenArray[code];
        return tokenString + " ";
    }
}

