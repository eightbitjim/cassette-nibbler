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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SpectrumBasicProgram {
    boolean inQuote = false;
    private int END_OF_STREAM = -1;

    private InputStream inputStream;
    public SpectrumBasicProgram(InputStream source) {
        inputStream = source;
    }

    public SpectrumBasicProgram(byte [] data) {
        inputStream = new ByteArrayInputStream(data);
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
            if (inputStream.available() < 1)
                return null;
        } catch (IOException e) {
            return null;
        }

        try {
            int lineNumber = getNextLineAddress();
            line.append(lineNumber);
            line.append(" ");

            int currentByte;
            do {
                currentByte = getNextByte();
                if (currentByte < 0)
                    break;

                if (currentByte != 0x0d)
                    line.append(getBasicStringFor(currentByte));
            } while (currentByte != 0x0d);

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

    private int getNextLineAddress() throws IOException {
        int address = getNextByte() * 256 + getNextByte();
        return address;
    }

    private String getBasicStringFor(int b) throws IOException {
        if (b == 0x0e) {
            skipFLoatValue();
            return "";
        } else
            return SpectrumASCII.printableStringForZXCode(b);
    }

    private void skipFLoatValue() throws IOException {
        for (int i = 0; i < 5; i++)
            inputStream.read();
    }

}
