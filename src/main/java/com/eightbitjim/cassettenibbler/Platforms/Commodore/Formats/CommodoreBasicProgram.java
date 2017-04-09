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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CommodoreBasicProgram {

    boolean inQuote = false;
    int basicVersion = 2;
    private static final int C64_PROGRAM_START_ADDRESS = 2049;
    private static final int C128_PROGRAM_START_ADDRESS = 7169;
    private static final int VIC20_PROGRAM_START_ADDRESS = 0x4097;
    private static final int VIC20_PROGRAM_START_ADDRESS2 = 0x4609;

    private static final String [] keyword = {
            "END", "FOR", "NEXT", "DATA", "INPUT#", "INPUT", "DIM", "READ", "LET", "GOTO",
            "RUN", "IF", "RESTORE", "GOSUB", "RETURN", "REM", "STOP", "ON", "WAIT",
            "LOAD", "SAVE", "VERIFY", "DEF", "POKE", "PRINT#", "PRINT", "CONT", "LIST",
            "CLR", "CMD", "SYS", "OPEN", "CLOSE", "GET", "NEW", "TAB(", "TO", "FN",
            "SPC(", "THEN", "NOT", "STEP", "+", "-", "*", "/", "^", "AND",
            "OR", ">", "=", "<", "SGN", "INT", "ABS", "USR", "FRE",
            "POS", "SQR", "RND", "LOG", "EXP", "COS", "SIN", "TAN",
            "ATN", "PEEK", "LEN", "STR$", "VAL", "ASC", "CHR$", "LEFT$",
            "RIGHT$", "MID$", "GO"
    };

    private boolean alreadyReadLoadAddress;

    private InputStream inputStream;
    public CommodoreBasicProgram(InputStream source) {
        inputStream = source;
        alreadyReadLoadAddress = false;
    }

    public CommodoreBasicProgram(byte [] data) {
        inputStream = new ByteArrayInputStream(data);
        alreadyReadLoadAddress = false;
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
            if (!alreadyReadLoadAddress) {
                alreadyReadLoadAddress = true;
                getLoadAddress();
            }

            int address = getNextLineAddress();
            if (address == 0) return null;

            int lineNumber = getNextLineAddress();
            line.append(lineNumber);
            line.append(" ");

            int currentByte;
            do {
                currentByte = getNextByte();
                if (currentByte != 0)
                    line.append(getBasicStringFor(currentByte));
            } while (currentByte != 0);

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

    private int getNextLineAddress() throws IOException {
        int address = getNextByte() + getNextByte() * 256;
        return address;
    }

    private int getLoadAddress() throws IOException {
        int address = getNextByte() + getNextByte() * 256;
        getBasicVersionForLoadAddress(address);
        return address;
    }

    private void getBasicVersionForLoadAddress(int address) {
        switch (address) {
            case C64_PROGRAM_START_ADDRESS:
            case VIC20_PROGRAM_START_ADDRESS:
            case VIC20_PROGRAM_START_ADDRESS2:
                basicVersion = 2;
                break;

            case C128_PROGRAM_START_ADDRESS:
                basicVersion = 7;
                break;

            default:
                basicVersion = 2;
                break;
        }
    }

    private String getBasicStringFor(int b) {
        if (b == 34) inQuote = !inQuote;

        if (b > 127 && b < 204 && !inQuote)
            return keyword[b - 128].toLowerCase();
        else
            return Character.toString(PETSCII.printableCharacterForPetsciiCode(b));
}
}
