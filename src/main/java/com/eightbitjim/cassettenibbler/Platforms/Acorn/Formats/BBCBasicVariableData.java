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
import java.text.DecimalFormat;
import java.util.Stack;

public class BBCBasicVariableData {
    private static final int REAL_TYPE_VALUE = 0xff;
    private static final int STRING_TYPE_VALUE = 0x00;
    private static final int INTEGER_TYPE_VALUE = 0x40;

    enum Type { REAL, INTEGER, STRING, UNKNOWN }
    private InputStream inputStream;
    private int linesOutput;
    private Type expressionType;
    private Type lastType;
    private StringBuilder builder;

    public BBCBasicVariableData(InputStream source) {
        inputStream = source;
        processInputStreamIntoString();
    }

    public BBCBasicVariableData(byte [] source) {
        inputStream = new ByteArrayInputStream(source);
        processInputStreamIntoString();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    private void processInputStreamIntoString() {
        builder = new StringBuilder();
        linesOutput = 0;
        try {
            while (inputStream.available() > 0) {
                processLineFromInputStream();
                linesOutput++;
            }
        } catch (Throwable t) {
            // Problem reading stream. Just stop here.
        }

        if (linesOutput > 0)
            builder.append("\n");
    }

    private void processLineFromInputStream() throws IOException {
        lastType = expressionType;
        String nextLine = getStringRepresentationOfNextVariable();
        if (needToInsertLineBreakBeforeNextLine())
                builder.append("\n");

        builder.append(nextLine);
    }

    private boolean needToInsertLineBreakBeforeNextLine() {
        return (linesOutput > 0)
            && (expressionType != Type.UNKNOWN || (expressionType == Type.UNKNOWN && lastType != Type.UNKNOWN));
    }

    private String getStringRepresentationOfNextVariable() throws IOException {
        int typeValue = getNextByte();
        expressionType = getNextVariableType(typeValue);
        switch (expressionType) {
            case INTEGER:
                return getIntegerVariable();
            case REAL:
                return getFloatingPointVariable();
            case STRING:
                return getStringVariable();
            default:
            case UNKNOWN:
                return tryToGetASCIITextCharacter(typeValue);
        }
    }

    private Type getNextVariableType(int typeMarker) {
        switch (typeMarker) {
            case STRING_TYPE_VALUE:
                return Type.STRING;
            case REAL_TYPE_VALUE:
                return Type.REAL;
            case INTEGER_TYPE_VALUE:
                return Type.INTEGER;
            default:
                return Type.UNKNOWN;
        }
    }

    private int getNextByte() throws IOException {
        if (inputStream.available() < 1)
            throw new IOException("End of stream reached");

        int value = inputStream.read();
        if (value < 0)
            throw new IOException("End of stream reached");

        return value;
    }

    private String getStringVariable() throws IOException {
        int length = getNextByte();
        Stack<Character> stringReverser = new Stack<>();
        for (int i = 0; i < length; i++) {
            stringReverser.add((char)getNextByte());
        }

        StringBuilder rightWayAroundString = new StringBuilder();
        while (!stringReverser.isEmpty())
            rightWayAroundString.append(stringReverser.pop().charValue());

        return rightWayAroundString.toString();
    }

    private String getIntegerVariable() throws IOException {
        int value = 0;
        for (int i = 3; i >= 0; i--) {
            value |= getNextByte() << (8 * i);
        }

        return Integer.toString(value);
    }

    private String tryToGetASCIITextCharacter(int value) throws IOException {
        return Character.toString((char)value);
    }

    private String getFloatingPointVariable() throws IOException {
        final int exponentBias = 0x80;
        long mantissa4 = getNextByte();
        long mantissa3 = getNextByte();
        long mantissa2 = getNextByte();
        long mantissa1 = getNextByte();

        int exponent = getNextByte();
        if (exponent == 0)
            return "0";
        else
            exponent -= exponentBias;

        int sign = (mantissa1 & 0x80) != 0 ? -1 : 1;
        mantissa1 |= 0x80;

        long assembledMantissa = (mantissa1 << 24) | (mantissa2 << 16) | (mantissa3 << 8) | (mantissa4);
        double value = (double)assembledMantissa * Math.pow(2.0, (double)exponent - 32.0);
        value *= (double)sign;

        DecimalFormat formatter = new DecimalFormat("##########.#####");
        return formatter.format(value);
    }
}
