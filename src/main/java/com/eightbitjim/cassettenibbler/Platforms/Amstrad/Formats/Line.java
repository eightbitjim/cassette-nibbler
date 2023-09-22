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
    private boolean endOfLine = false;
    private boolean insideRemStatement = false;
    private boolean insideQuote = false;

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
        endOfLine = false;
        while ((inputStream.available() > 0) && (!endOfLine)) {
            int value = getByteValue();
            if (value == END_OF_LINE_MARKER)
                break;
            processValue(value);
        }
    }

    private void processValue(int tokenValue) throws IOException {
        // Treat a colon (1) as special: if we see a colon, move on to the next
        // token, and only add the colon if the next token isn't an ELSE
        if (tokenValue == 1) {
            tokenValue = getByteValue();
            if (tokenValue != 151)
                builder.append(":");            
        }

        if (insideQuote) {
            // Only respond to ends of lines and closig quotes, otherwise print the character if it's
            // in the 32-127 range. If outside this range, replace with a '#', as we don't have the
            // Amstrad character set
            if (tokenValue == 0) {
                endOfLine = true;
                return;
            }
            if (tokenValue > 31 && tokenValue < 128)
                builder.append((char)tokenValue);
            else
                builder.append("#");
            if (tokenValue == 34)
                insideQuote = false;
            return;
        }

        switch (tokenValue) {
            case 0:
                // End of line marker, should have already been picked up
                endOfLine = true;
                return;
            case 1:
                // Don't output a colon until we see the next token, as we don't want
                // to display colons in front of an ELSE
                // But this will terminate a REM statement
                insideRemStatement = false;
                return;
            case 2:
            case 3:
            case 4:
                processVariableWithTokenAndSuffix(tokenValue);
                return;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
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
            case 34:
                insideQuote = !insideQuote;
                break;
            case RSX_TOKEN_MARKER:
                processRSXCommand();
                return;
            case 192: // '
                processApostrophe();
                return;
            case 197: // REM statement
                insideRemStatement = true;
                break;
            case 245:
                processMinus();
                return;
            case EXTENDED_TOKEN_PREFIX:
                tokenValue = getByteValue() + 0x100;
                break;
        }

        builder.append(Token.getStringForTokenValue(tokenValue));
    }

    private void processApostrophe() {
        builder.append("'");
    }

    private void processMinus() {
        builder.append("-");
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
        builder.append("(memory address values not yet implemented)");
    }

    private int getByteValue() throws IOException {
        if (inputStream.available() < 1)
            throw new IOException("End of file reached");

        int value = inputStream.read();
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
        // Skip over the next byte
        getByteValue();
        String commandName = getStringFromInput();
        builder.append("|").append(commandName);
    }

    private void processVariableWithTokenNoSuffix() throws IOException {
        // Skip over the next byte
        getTwoByteValue();
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
        boolean insideString = false;
        StringBuilder tempOriginal = new StringBuilder();
        while (!valueHadTopBitSet) {
            char value = (char)getByteValue();
            if (value == 34) { /* Quotes */
                if (!insideString)
                    insideString = true;
                else
                    insideString = false;
                    break;
            }

            if (value == 0) {
                endOfLine = true;
                break;
            }

            if (!insideString) {
                if ((value & 128) != 0) {
                    value -= 128;
                    valueHadTopBitSet = true;
                }
            }

            tempOriginal.append((int)value);
            tempOriginal.append(" ");
            localString.append(value);
        }

        return localString.toString();
    }
}