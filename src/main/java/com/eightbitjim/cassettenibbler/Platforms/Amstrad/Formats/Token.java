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

package com.eightbitjim.cassettenibbler.Platforms.Amstrad.Formats;

public class Token {

    private static String [] lowTokenArray = {
      "AFTER", "AUTO", "BORDER", "CALL", "CAT", "CHAIN", "CLEAR", "CLG", "CLOSEIN", "CLOSEOUT",
            "CLS", "CONT", "DATA", "DEF", "DEFINT", "DEFREAL", "DEFSTR", "DEG", "DELETE", "DIM", "DRAW",
            "DRAWR", "EDIT", "ELSE", "END", "ENT", "ENV", "ERASE", "ERROR", "EVERY", "FOR", "GOSUB", "GOTO",
            "IF", "INK", "INPUT", "KEY", "LET", "LINE", "LIST", "LOAD", "LOCATE", "MEMORY", "MERGE", "MID$",
            "MODE", "MOVE", "MOVER", "NEXT", "NEW", "ON", "ON BREAK", "ON ERROR GOTO", "SQ", "OPENIN", "OPENOUT",
            "ORIGIN", "OUT", "PAPER", "PEN", "PLOT", "PLOTR", "POKE", "PRINT", "'", "RAD", "RANDOMIZE", "READ",
            "RELEASE", "REM", "RENUM", "RESOTRE", "RESUME", "RETURN", "RUN", "SAVE", "SOUND", "SPEED", "STOP",
            "SYMBOL", "TAG", "TAGOFF", "TROFF", "TRON", "WAIT", "WEND", "WHILE", "WIDTH", "WINDOW", "WRITE",
            "ZONE", "DI", "EI", "FILL", "GRAPHICS", "MASK", "FRAME", "CURSOR", "", "ERL", "FN", "SPC",
            "STEP", "SWAP", "", "", "TAB", "THEN", "TO", "USING", ">", "=", ">=", "<", "<=",
            "+", "-", "*", "/", "^", "\\", "AND", "MOD", "OR", "XOR", "NOT"
    };

    private static String [] highTokenArray = {
            "ABS", "ASC", "ATN", "CHR$", "CINT", "COS", "CREAL", "EXP", "FIX", "FRE", "INKEY", "INP", "INT",
            "JOY", "LEN", "LOG", "LOG10", "LOWER$", "PEEK", "REMAIN", "SGN", "SIN", "SPACE$", "SQ", "SQR",
            "STR$", "TAN", "UNT", "UPPER$", "VAL",
            // $1e to $3f not used
            "(unused token)", "(unused token)", // $1e, $1f
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)",
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", // $20 to 2f
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)",
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", // $30 to 3f
            "EOF", "ERR", "HIMEM", "INKEY$", "PI", "RND", "TIME", "XPOS",
            "YPOS", "DERR",
            // $4a to $70 not used
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", // $4a to 4f
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)",
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", // $50 to 5f
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)",
            "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", "(unused token)", // $60 to 6f
            "(unused token)", // $70
            "BIN$", "DEC$", "HEX$", "INSTR", "LEFT$", "MAX", "MIN", "POS", "RIGHT$",
            "ROUND", "STRING$", "TEST", "TESTR", "COPYCHR$", "VPOS"
    };

    private static boolean insideRemStatement;
    private static boolean insideQuote;

    public static void lineEnded() {
        insideRemStatement = false;
        insideQuote = false;
    }

    public static String getStringForTokenValue(int value) {
        boolean foundRemStatement = false;
        switch (value) {
            case 0: // End of line
                insideRemStatement = false;
                return "";
            case 1: // :
                insideRemStatement = false;
                return ":";
            case 32:
                return " ";
            case 33:
                return "!";
            case 34:
                insideQuote = !insideQuote;
                return "\"";
            case 245:
                return "-";
            case 0xc0: // '
            case 0xc5: // REM statement
                foundRemStatement = true;
        }

        if (insideRemStatement || insideQuote)
            return "" + (char)value;

        if (foundRemStatement)
            insideRemStatement = true;

        if (value < 32)
            return "";

        if (value < 0x7c)
            return "" + (char)value;

        if (value < 255) {
            if (value - 128 >= 0 && value - 128 < lowTokenArray.length)
                return lowTokenArray[value - 128];
            else
                return "*INVALIDTOKEN" + value + "*";
        }

        if (value - 256 >= 0 && value - 256 < highTokenArray.length)
            return highTokenArray[value - 256];

        return "*INVALIDTOKEN" + value +"*";
    }
}
