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

import com.eightbitjim.cassettenibbler.DataSource.IncorrectFileFormatException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class ZX81PFileExtract {
    private InputStream input;
    private boolean validFileFormat;

    private int currentMemoryAddress;
    private int endMemoryAddress;
    private boolean finished;
    private String fileSummary;
    private int [] data;

    private static final int DISPLAYING_SYSTEM_VARIABLES = 0;
    private static final int DISPLAYING_DISPLAY_FILE = 1;
    private static final int DISPLAYING_BASIC_PROGRAM = 2;
    private int displayingWhat;

    private static final int FILE_START_MEMORY_ADDRESS = 16393;

    private int currentLine;
    private int displayFileLine;

    public ZX81PFileExtract(byte [] data) {
        this.input = new ByteArrayInputStream(data);
        validFileFormat = false;
        parseFile();
        currentLine = 0;
        finished = false;
        displayingWhat = DISPLAYING_SYSTEM_VARIABLES;
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

    private void parseFile() {
        LinkedList<Integer> fileDataBuffer = new LinkedList<>();
        int currentByte;
        do {
            try {
                currentByte = input.read();
            } catch (IOException e) {
                currentByte = -1;
            }

            if (currentByte != -1) {
                fileDataBuffer.add(currentByte);
            }
        } while (currentByte != -1);

        if (!fileDataBuffer.isEmpty()) {
            data = new int[fileDataBuffer.size()];
            for (int i = 0; i < data.length; i++)
                data[i] = fileDataBuffer.get(i);
        }

        checkIfFileIsValidFormat();
    }

    private void checkIfFileIsValidFormat() {
        if (data[0] != 0) {
            fileSummary = "Invalid file version. Should be 0.";
        }

        fileSummary = "File data length: " + data.length;
        validFileFormat = true;
    }

    private int getByteAt(int address) throws IncorrectFileFormatException {
        if (address - FILE_START_MEMORY_ADDRESS < 0)
            throw new IncorrectFileFormatException("Requested address is before start of file: " + address);

        if (address - FILE_START_MEMORY_ADDRESS > data.length - 1)
            throw new IncorrectFileFormatException("Requested address is past the end of the file: " + address);

        return data[address - FILE_START_MEMORY_ADDRESS];
    }

    private int getLittleEndianWordAt(int address) throws IncorrectFileFormatException {
        return getByteAt(address) + 256 * getByteAt(address + 1);
    }

    private int getBigEndianWordAt(int address) throws IncorrectFileFormatException {
        return getByteAt(address) * 256 + getByteAt(address + 1);
    }

    public String readLine() {
        if (finished)
            return null;

        try {
            switch (displayingWhat) {
                case DISPLAYING_SYSTEM_VARIABLES:
                    return displaySystemVariables();

                case DISPLAYING_DISPLAY_FILE:
                    return displayDisplayFile();

                case DISPLAYING_BASIC_PROGRAM:
                    return displayBasicProgram();

                default:
                    return null;
            }
        } catch (IncorrectFileFormatException e) {
            return advanceToNextdisplayState();
        }
    }

    private String displaySystemVariables() {
        try {
            switch (currentLine++) {
                case 0:
                    return fileSummary;

                case 1:
                    return "VERSN: " + getByteAt(16393);

                case 2:
                    return "D_FILE: " + getLittleEndianWordAt(16396);

                case 3:
                    return "STKEND: " + getLittleEndianWordAt(16410);

                case 4:
                    return "FLAGX: " + getByteAt(16429);
                default:
                    return advanceToNextdisplayState();
            }
        } catch (IncorrectFileFormatException e) {
            finished = true;
            return "END OF FILE";
        }
    }

    private String displayDisplayFile() throws IncorrectFileFormatException {
        currentLine++;
        if (currentLine == 1) {
            currentMemoryAddress = getLittleEndianWordAt(16396);
            endMemoryAddress = getLittleEndianWordAt(16400);
            displayFileLine = 0;
            return "";
        }

        if (currentLine == 2) return "Display file contents:";
        if (currentLine == 3) return "------- ---- --------";

        displayFileLine++;
        if (displayFileLine > 24)
            return advanceToNextdisplayState();

        String line = getNextdisplayLine();
        if (line == null)
            return advanceToNextdisplayState();
        else
            return line;
    }

    private String getNextdisplayLine() throws IncorrectFileFormatException {
        if (currentMemoryAddress > endMemoryAddress)
            return null;

        StringBuffer line = new StringBuffer();
        boolean endOfLine = false;
        int column = 0;
        while (!endOfLine && currentMemoryAddress <= endMemoryAddress && column < 32) {
            int nextCharacter = getByteAt(currentMemoryAddress);
            if (nextCharacter != 118)
                line.append(ZX81Characters.printableStringForZXCode(nextCharacter));
            else
                endOfLine = true;

            column++;
            currentMemoryAddress++;
        }

        return line.toString();
    }

    private String displayBasicProgram() throws IncorrectFileFormatException {
        currentLine++;
        if (currentLine == 1) {
            currentMemoryAddress = 16509; // BASIC PROGRAM FIXED ADDRESS
            endMemoryAddress = getLittleEndianWordAt(16396); // DFILE
            return "";
        }

        if (currentLine == 2) return "Basic program:";
        if (currentLine == 3) return "----- -------";

        String line = getNextBasicLine();
        if (line == null)
            return advanceToNextdisplayState();
        else
            return line;
    }

    private String getNextBasicLine() throws IncorrectFileFormatException {
        if (currentMemoryAddress > endMemoryAddress)
            return null;

        StringBuffer line = new StringBuffer();
        boolean finished = false;
        boolean gotLineNumber = false;
        while (!finished && currentMemoryAddress <= endMemoryAddress) {
            if (getByteAt(currentMemoryAddress) == 118) {
                finished = true;
            } else
            if (!gotLineNumber) {
                line.append(getBigEndianWordAt(currentMemoryAddress)).append(" ");
                currentMemoryAddress+=3;
                gotLineNumber = true;
            } else
            if (getByteAt(currentMemoryAddress) == 0x7e) {
                currentMemoryAddress += 5; // Skip floating point
            } else {
                line.append(ZX81Characters.printableStringForZXCode(getByteAt(currentMemoryAddress)));
            }

            currentMemoryAddress++;
        }

        return line.toString();
    }

    private String advanceToNextdisplayState() {
        currentLine = 0;
        switch (displayingWhat) {
            case DISPLAYING_SYSTEM_VARIABLES:
                displayingWhat = DISPLAYING_DISPLAY_FILE;
                return readLine();

            case DISPLAYING_DISPLAY_FILE:
                displayingWhat = DISPLAYING_BASIC_PROGRAM;
                return readLine();

            case DISPLAYING_BASIC_PROGRAM:
            default:
                finished = true;
                return null;
        }
    }

}
