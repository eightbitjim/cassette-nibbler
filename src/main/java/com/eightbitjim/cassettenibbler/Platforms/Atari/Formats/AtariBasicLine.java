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

package com.eightbitjim.cassettenibbler.Platforms.Atari.Formats;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class AtariBasicLine {
    private InputStream inputStream;
    private StringBuilder builder;
    private boolean endOfLineFound;
    private VariableSet variableSet;
    private boolean inRemStatement;

    private static final int END_OF_FILE = -1;
    private static final int END_OF_STATEMENT_VALUE = 0x14;
    private static final int END_OF_LINE_VALUE = 0x16;
    private static final int THEN_TOKEN_VALUE = 0x1b;
    private static final int SCALAR_CONSTANT_TOKEN = 0x0e;
    private static final int STRING_CONSTANT_TOKEN = 0x0f;

    public AtariBasicLine(InputStream inputSteam, VariableSet variableSet) throws IOException {
        this.inputStream = inputSteam;
        builder = new StringBuilder();
        endOfLineFound = false;
        this.variableSet = variableSet;
        parseLine();
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    private void parseLine() throws IOException {
        processLineNumber();
        skipOffsetByte();

        while (!endOfLineFound) {
            processStatement();
        }
    }

    private int getNextByte() throws IOException {
        if (inputStream.available() < 1) {
            endOfLineFound = true;
            return END_OF_FILE;
        }

        int value = inputStream.read();
        return value;
    }

    private int getTwoByteValue() throws IOException {
        int address = getNextByte() + getNextByte() * 256;
        return address;
    }

    private void processLineNumber() throws IOException {
        int lineNumber = getTwoByteValue();
        builder.append(lineNumber).append(" ");
    }

    private void skipOffsetByte() throws IOException {
        getNextByte();
    }

    private void processStatement() throws IOException {
        skipOffsetByte();
        resetStatement();
        processCommandByte();

        boolean endOfStatementFound;
        do {
            endOfStatementFound = processFunctionByte();
        } while (!endOfStatementFound && !endOfLineFound);
    }

    private void resetStatement() {
        inRemStatement = false;
    }

    private void processCommandByte() throws IOException {
        int value = getNextByte();
        if (value < 0)
            return;

        if (value == 0)
            inRemStatement = true;

        if (value >= 0x80) {
            processOnGotoOrGosub(value);
        } else
            builder.append(AtariBasicTokens.printableStringForCommandByteCode(value));
    }

    private void processOnGotoOrGosub(int commandValue) throws IOException {
        builder.append("ON ");
        processVariableReference(commandValue);
    }

    private boolean processFunctionByte() throws IOException {
        int value = getNextByte();
        if (inRemStatement) {
            if (value == END_OF_STATEMENT_VALUE) {
                builder.append(":");
                return true;
            } else {
                builder.append(AtariBasicTokens.asciiCharacterforATASCIICode(value));
                return false;
            }
        }

        switch (value) {
            case SCALAR_CONSTANT_TOKEN:
                processBCDConstant();
                return false;
            case STRING_CONSTANT_TOKEN:
                processStringConstant();
                return false;
            case END_OF_STATEMENT_VALUE:
                builder.append(":");
                return true;
            case THEN_TOKEN_VALUE:
                processSingleToken(value);
                return true;
            case END_OF_LINE_VALUE:
                endOfLineFound = true;
                builder.append("\n");
                return true;
        }

        if (value >= 0x80)
            processVariableReference(value);
        else
            processSingleToken(value);

        return false;
    }

    private void processStringConstant() throws IOException {
        builder.append("\"");

        int stringLength = getNextByte();
        for (int i = 0; i < stringLength; i++)
            builder.append(AtariBasicTokens.asciiCharacterforATASCIICode(getNextByte()));

        builder.append("\"");
    }

    private void processBCDConstant() throws IOException {
        int exponentAndSignByte = getNextByte();

        int exponent = getExponent(exponentAndSignByte);
        int sign = getSign(exponentAndSignByte);
        double mantissa = getMantissa();

        double result = mantissa * Math.pow(100.0, (double)exponent) * (double)sign;
        DecimalFormat formatter = new DecimalFormat("#########.######");
        builder.append(formatter.format(result));
    }

    private int getExponent(int exponentAndSignByte) {
        int exponent = exponentAndSignByte;
        exponent &= 0x7f;
        exponent -= 0x40;
        return exponent;
    }

    private int getSign(int exponentAndSignByte) {
        int sign = (exponentAndSignByte & 0x80) != 0 ? -1 : 1;
        return sign;
    }

    private double getMantissa() throws IOException {
        double mantissa = 0;
        for (int i = 0; i < 5; i++) {
            int bcdByte = getNextByte();
            int firstNumber = (bcdByte & 0xf0) >> 4;
            int secondNumber = (bcdByte & 0x0f);

            mantissa += firstNumber * Math.pow(10.0, 1.0 - (double)i * 2.0);
            mantissa += secondNumber * Math.pow(10.0, 1.0 - (double)i * 2.0 - 1.0);
        }

        return mantissa;
    }

    private void processVariableReference(int variableNumber) {
        if (variableNumber < 0x80) {
            builder.append("(INVALID VARIABLE NUMBER)");
            return;
        } else {
            variableNumber -= 0x80;
            builder.append(variableSet.getVariableNumber(variableNumber));
        }
    }

    private void processSingleToken(int value) throws IOException {
        builder.append(AtariBasicTokens.printableStringForOperatorOrTokenByteCode(value));
    }
}
