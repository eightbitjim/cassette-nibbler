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

import java.io.IOException;
import java.io.InputStream;

public class Line {
    private InputStream inputStream;
    private StringBuilder builder;

    private static final int END_OF_LINE_MARKER = 0;
    private static final int EXTENDED_TOKEN_PREFIX = 0xff;
    private static final int RSX_TOKEN_MARKER = 0x7c;
    
    public Line(InputStream inputStream) throws IOException {
        this.inputStream = inputStream;
        builder = new StringBuilder();
        constructLine();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    private void constructLine() throws IOException {
        int lineLength = getTwoByteValue();
        int lineNumber = getTwoByteValue();

        if (lineLength == 0)
            return;

        builder.append(Integer.toString(lineNumber)).append(" ");
        while (inputStream.available() > 0) {
            int value = getByteValue();
            processValue(value);
            if (value == END_OF_LINE_MARKER)
                break;
        }

        Token.lineEnded();
    }

    private void processValue(int tokenValue) throws IOException {
        switch (tokenValue) {
            case 2:
            case 3:
            case 4:
                processVariableWithTokenAndSuffix(tokenValue);
                return;
            case 11:
            case 12:
            case 13:
                processVariableWithTokenNoSuffix();
                return;
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
                processIntegerNoValueByte(tokenValue);
                return;
            case 25:
                processIntegerOneValueByte();
                return;
            case 26:
                processIntegerTwoValueBytes();
                return;
            case 27:
                processBinaryIntegerValue();
                return;
            case 28:
                processHexIntegerValue();
                return;
            case 29:
                processLineMemoryAddressPointer();
                return;
            case 30:
                processLineNumber();
                return;
            case 31:
                processFloatingPoint();
                return;
            case RSX_TOKEN_MARKER:
                processRSXCommand();
                return;
        }

        if (tokenValue == EXTENDED_TOKEN_PREFIX)
            tokenValue = getByteValue() + 0x100;

        builder.append(Token.getStringForTokenValue(tokenValue));
    }

    private void processBinaryIntegerValue() throws IOException {
        int valueRepresented = getTwoByteValue();
        builder.append("%").append(Integer.toBinaryString(valueRepresented));
    }

    private void processHexIntegerValue() throws IOException {
        int valueRepresented = getTwoByteValue();
        builder.append("&").append(Integer.toHexString(valueRepresented));
    }

    private void processLineMemoryAddressPointer() {
        builder.append("(memory address values not implemented)");
    }

    private int getByteValue() throws IOException {
        if (inputStream.available() < 1)
            throw new IOException("End of file reached");

        int value = inputStream.read();
      //  builder.append("(" + value + ")");
        return value;
    }

    private int getTwoByteValue() throws IOException {
        return getByteValue() + getByteValue() * 0x100;
    }

    private void processLineNumber() throws IOException {
        int lineNumber = getTwoByteValue();
        builder.append(lineNumber);
    }

    private void processIntegerNoValueByte(int value) {
        int valueRepresented = value - 14;
        builder.append(valueRepresented);
    }

    private void processIntegerOneValueByte() throws IOException {
        int valueRepresented = getByteValue();
        builder.append(valueRepresented);
    }

    private void processIntegerTwoValueBytes() throws IOException {
        int valueRepresented = getTwoByteValue();
        builder.append(valueRepresented);
    }

    private void processFloatingPoint() throws IOException {
        double [] fpData = new double[5];
        for (int i = 0; i < fpData.length; i++)
            fpData[i] = getByteValue();

        double valueRepresented = (2.0 * (fpData[4] - 145.0) * (65536.0 + (fpData[1] / 128.0 ) + (fpData[2]) + (fpData[3] * 512.0 ) + (fpData[0] / 32800.0)));
        builder.append(valueRepresented);
    }

    private void processRSXCommand() throws IOException {
        int valueOffsetToIgnore = getByteValue();
        String commandName = getStringFromInput();
        builder.append("|").append(commandName);
    }

    private void processVariableWithTokenNoSuffix() throws IOException {
        int offsetToIgnore = getTwoByteValue();
        String variableName = getStringFromInput();
        builder.append(variableName);
    }


    private void processVariableWithTokenAndSuffix(int token) throws IOException {
        processVariableWithTokenNoSuffix();
        switch (token) {
            case 2:
                builder.append("%");
                break;
            case 3:
                builder.append("$");
                break;
            case 4:
                builder.append("!");
                break;
        }
    }

    private String getStringFromInput() throws IOException {
        StringBuilder localString = new StringBuilder();
        boolean valueHadTopBitSet = false;
        while (!valueHadTopBitSet) {
            char value = (char)getByteValue();
            if ((value & 128) != 0) {
                value -= 128;
                valueHadTopBitSet = true;
            }

            localString.append(value);
        }

        return localString.toString();
    }
}