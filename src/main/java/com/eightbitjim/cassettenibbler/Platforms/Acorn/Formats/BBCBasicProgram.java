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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BBCBasicProgram {

    boolean inQuote = false;
    private static final int START_OF_LINE = 0x0d;
    boolean firstLine;

    private static final String [] keyword = {
        "OTHERWISE",
        "AND", "DIV", "EOR", "MOD", "OR", "ERROR", "LINE", "OFF",
        "STEP", "SPC", "TAB(", "ELSE", "THEN", "<line>",
        "OPENIN", "PTR",
        "PAGE", "TIME", "LOMEM", "HIMEM", "ABS", "ACS", "ADVAL", "ASC",
        "ASN", "ATN", "BGET", "COS", "COUNT", "DEG", "ERL", "ERR",

        "EVAL", "EXP", "EXT", "FALSE", "FN", "GET", "INKEY", "INSTR(",
        "INT", "LEN", "LN", "LOG", "NOT", "OPENUP", "OPENOUT", "PI",

        "POINT(", "POS", "RAD", "RND", "SGN", "SIN", "SQR", "TAN",
        "TO", "TRUE", "USR", "VAL", "VPOS", "CHR$", "GET$", "INKEY$",

        "LEFT$(", "MID$(", "RIGHT$(", "STR$", "STRING$(", "EOF",
        "<ESCFN>", "<ESCCOM>", "<ESCSTMT>",
        "WHEN", "OF", "ENDCASE", "ELSE",
        "ENDIF", "ENDWHILE", "PTR",

        "PAGE", "TIME", "LOMEM", "HIMEM", "SOUND", "BPUT", "CALL", "CHAIN",
        "CLEAR", "CLOSE", "CLG", "CLS", "DATA", "DEF", "DIM", "DRAW",

        "END", "ENDPROC", "ENVELOPE", "FOR", "GOSUB", "GOTO", "GCOL", "IF",
        "INPUT", "LET", "LOCAL", "MODE", "MOVE", "NEXT", "ON", "VDU",

        "PLOT", "PRINT", "PROC", "READ", "REM", "REPEAT", "REPORT", "RESTORE",
        "RETURN", "RUN", "STOP", "COLOUR", "TRACE", "UNTIL", "WIDTH", "OSCLI"
    };

    private InputStream inputStream;
    public BBCBasicProgram(InputStream source) {
        inputStream = source;
        firstLine = true;
    }

    public BBCBasicProgram(byte [] source) {
        inputStream = new ByteArrayInputStream(source);
        firstLine = true;
    }

    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String currentLine;
        do {
            currentLine = readLine();
            if (currentLine != null)
                builder.append(currentLine + "\n");
        } while (currentLine != null);
        return builder.toString();
    }


    public String readLine() {
        StringBuffer line = new StringBuffer();
        inQuote = false;
        try {
            if (firstLine) {
                firstLine = false;
            }

            int lineNumber = getTwoByteValue();
            int lineLength = getNextByte() - 3;

            line.append(lineNumber);
            line.append(" ");

            int currentByte;
            do {
                currentByte = getNextByte();
                if (currentByte != START_OF_LINE)
                    line.append(getBasicStringFor(currentByte));
            } while (currentByte != START_OF_LINE);
        } catch (Throwable t) {
            return null;
        }

        return line.toString();
    }

    private int getNextByte() throws IOException {
        if (inputStream.available() < 1)
            throw new IOException("End of stream reached");

        int value = inputStream.read();
        if (value < 0)
            throw new IOException("End of stream reached");

        return value;
    }

    private int getTwoByteValue() throws IOException {
        int address = getNextByte() * 256 + getNextByte();
        return address;
    }

    private String getBasicStringFor(int b) {
        if (b >= 0x7f)
            return keyword[b - 0x7f];
        else
            return Character.toString((char) b);
    }
}
