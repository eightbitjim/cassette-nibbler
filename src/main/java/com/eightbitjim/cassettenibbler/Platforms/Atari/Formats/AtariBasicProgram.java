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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class AtariBasicProgram {
    private int END_OF_STREAM = -1;

    private static final int NUMBER_OF_MEMORY_POINTERS = 7;
    private int [] memoryPointers;
    private int nextMemoryPosition;
    private VariableSet variables;

    private boolean errorFound;
    private boolean programStartBytesValid;
    private boolean endOfFileFound;

    private StringBuilder builder;
    private InputStream inputStream;

    public AtariBasicProgram(InputStream source) {
        inputStream = source;
        parseFile();
    }

    public AtariBasicProgram(byte [] data) {
        if (data != null && data.length > 0) {
            inputStream = new ByteArrayInputStream(data);
            parseFile();
        }
    }

    private void parseFile() {
        builder = new StringBuilder();
        try {
            readMemoryPointers();
            if (programStartBytesValid) {
                readVariableTable();
                readTokenFile();
            }
        } catch (IOException e) {
            errorFound = true;
        }
    }

    @Override
    public String toString() {
        if (errorFound)
            return "invalid Basic file";

        if (inputStream == null || builder == null)
            return "empty file";
        else
            return builder.toString();
    }

    public void close() throws IOException {
        inputStream.close();
    }

    public boolean isValid() {
        return programStartBytesValid;
    }

    private int getNextByte() throws IOException {
        if (inputStream.available() < 1) {
            endOfFileFound = true;
            return END_OF_STREAM;
        }

        int value = inputStream.read();
        nextMemoryPosition++;

        return value;
    }

    private int getTwoByteValue() throws IOException {
        int address = getNextByte() + getNextByte() * 256;
        return address;
    }

    private void readMemoryPointers() throws IOException {
        memoryPointers = new int[NUMBER_OF_MEMORY_POINTERS];
        for (int i = 0; i < NUMBER_OF_MEMORY_POINTERS; i++)
            memoryPointers[i] = getTwoByteValue();

        checkForBasicProgram();
        validateMemoryPointers();
    }

    private void validateMemoryPointers() {
        errorFound |= !(getStartOfVariableNameTable() <= getVariableNameTableDummyEnd());
        errorFound |= !(getVariableNameTableDummyEnd() <= getStartOfVariableValueTable());
        errorFound |= !(getStartOfVariableValueTable() <= getStartOfStatementTable());
    }

    private void checkForBasicProgram() {
        // Will assume this is a basic program if the first 2 pointers are: 0x0000 and 0x0100
        programStartBytesValid = (memoryPointers[0] == 0x00) && (memoryPointers[1] == 0x0100);
    }

    private void readVariableTable() throws IOException {
        if (errorFound)
            return;

        readVariableNameTable();
        readVariableValueTable();
    }

    private void readVariableNameTable() throws IOException {
        if (errorFound)
            return;

        nextMemoryPosition = getStartOfVariableNameTable();
        variables = new VariableSet();
        StringBuilder nameBuilder = new StringBuilder();
        while (nextMemoryPosition <= getVariableNameTableDummyEnd()) {
            int value = getNextByte();
            if (value == 0 && nextMemoryPosition >= getVariableNameTableDummyEnd())
                break;

            if (value == END_OF_STREAM) {
                errorFound = true;
                return;
            }

            boolean lastCharacterInVariableName = false;
            if ((value & 0x80) != 0) {
                lastCharacterInVariableName = true;
                value ^= 0x80;
            }

            nameBuilder.append(AtariBasicTokens.asciiCharacterforATASCIICode(value));
            if (lastCharacterInVariableName) {
                variables.addVariable(nameBuilder.toString());
                nameBuilder = new StringBuilder();
            }
        }
    }

    private int getStartOfVariableNameTable() {
        return memoryPointers[1];
    }

    private int getVariableNameTableDummyEnd() {
        return memoryPointers[2];
    }

    private int getStartOfVariableValueTable() {
        return memoryPointers[3];
    }

    private int getStartOfStatementTable() {
        return memoryPointers[4];
    }

    private void readVariableValueTable() throws IOException {
        if (errorFound)
            return;

        while (nextMemoryPosition < getStartOfStatementTable() - 3) {
            int value = getNextByte();
            if (value == END_OF_STREAM) {
                errorFound = true;
                return;
            }
        }
    }

    private void readTokenFile() throws IOException {
        if (errorFound)
            return;

        String currentLine;
        do {
            currentLine = readLine();
            if (currentLine != null)
                builder.append(currentLine);
        } while (currentLine != null && !endOfFileFound);
    }

    public String readLine() throws IOException {
        AtariBasicLine line = new AtariBasicLine(inputStream, variables);
        endOfFileFound = inputStream.available() < 1;
        return line.toString();
    }
}
