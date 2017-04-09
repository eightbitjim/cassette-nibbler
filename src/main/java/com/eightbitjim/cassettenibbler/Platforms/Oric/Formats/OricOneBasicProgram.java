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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OricOneBasicProgram {
    // Basic line format:
    // 2 bytes: next line address. 0x0000 is end of file
    // 2 bytes: little endian, line number
    // line contents
    // 1 byte 0: end of line
    boolean inQuote = false;
    private int END_OF_STREAM = -1;

    private InputStream inputStream;
    public OricOneBasicProgram(InputStream source) {
        inputStream = source;
    }

    public OricOneBasicProgram(byte [] data) {
        if (data != null && data.length > 0)
            inputStream = new ByteArrayInputStream(data);
    }

    @Override
    public String toString() {
        if (inputStream == null)
            return "empty file";

        StringBuilder builder = new StringBuilder();
        String currentLine;
        do {
            currentLine = readLine();
            if (currentLine != null)
                builder.append(currentLine + "\n");
        } while (currentLine != null);
        return builder.toString();
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public String readLine() {
        StringBuffer line = new StringBuffer();
        inQuote = false;
        try {
            if (inputStream.available() < 1)
                return null;
        } catch (IOException e) {
            return null;
        }

        try {
            int nextLineAddress = getTwoByteValue();
            if (nextLineAddress == 0) {
                return null;
            }

            int lineNumber = getTwoByteValue();
            line.append(lineNumber);
            line.append(" ");

            int currentByte;
            do {
                currentByte = getNextByte();
                if (currentByte < 0)
                    break;

                if (currentByte != 0x00)
                    line.append(getBasicStringFor(currentByte));
            } while (currentByte != 0x00);

        } catch (Throwable t) {

        }

        return line.toString();
    }

    private int getNextByte() throws IOException {
        if (inputStream.available() < 1)
            return END_OF_STREAM;

        int value = inputStream.read();
        return value;
    }

    private int getTwoByteValue() throws IOException {
        int address = getNextByte() + getNextByte() * 256;
        return address;
    }

    protected String getBasicStringFor(int b) throws IOException {
        if (b == 0x0e) {
            skipFLoatValue();
            return "";
        } else
            return OricOneASCII.printableStringForByteCode(b);
    }

    protected void skipFLoatValue() throws IOException {
        for (int i = 0; i < 5; i++)
            inputStream.read();
    }

}
