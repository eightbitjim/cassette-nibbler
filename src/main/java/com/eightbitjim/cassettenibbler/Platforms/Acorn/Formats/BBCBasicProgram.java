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
    private static final int LINE_NUMBER_TOKEN = 0x8d;
    private static final int EXTENDED_TOKEN_1 = 0xc6;
    private static final int EXTENDED_TOKEN_2 = 0xc7;
    private static final int EXTENDED_TOKEN_3 = 0xc8;

    boolean firstLine;

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
                getNextByte(); // Discard
            }

            int lineNumber = getTwoByteValue();
            int lineLength = getNextByte() - 3;

            line.append(lineNumber);
            line.append(" ");

            int currentByte;
            do {
                currentByte = getNextByte();
                if (currentByte == '\"')
                    inQuote = !inQuote;

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

    private String getBasicStringFor(int value) throws IOException {
        if (value >= 0x7f && !inQuote)
            return getStringForToken(value);
        else
            return Character.toString((char) value);
    }

    private String getStringForToken(int token) throws IOException {
        switch (token) {
            case LINE_NUMBER_TOKEN:
                return getLineNumberAsString();

            case EXTENDED_TOKEN_1:
            case EXTENDED_TOKEN_2:
            case EXTENDED_TOKEN_3:
                return BBCBasicKeywords.getKayboardForExtendedToken(token, getNextByte());

            default:
                return BBCBasicKeywords.getKeywordForToken(token);
        }
    }

    private String getLineNumberAsString() throws IOException {
        int byte1 = getNextByte();
        int byte2 = getNextByte();
        int byte3 = getNextByte();

        int resultByte1 = 0;
        int resultByte2 = 0;

        byte1 ^= 0x54;
        resultByte1 |= (byte1 & 0x30) << 2;
        resultByte2 |= (byte1 & 0x0c) << 4;

        byte2 ^= 0x40;
        resultByte1 |= byte2;
        byte3 ^= 0x40;
        resultByte2 |= byte3;

        return Integer.toString(resultByte1 + resultByte2 * 0x100);
    }
}
