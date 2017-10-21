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

package com.eightbitjim.cassettenibbler.Platforms.MSX.FileExtraction;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.ArrayList;
import java.util.List;

public class MSXTapeBlock {

    public enum BlockType { HEADER, BASIC_PROGRAM, ASCII, MACHINE_CODE, UNKNOWN }
    private BlockType type;
    private List<Byte> data;
    private transient int bytesReceived;
    private transient int zerosInARow;
    private transient byte lastByte;
    private int machineCodeProgramSize;
    private boolean EOFwasInData;
    private boolean errors;

    private static final int HEADER_LENGTH = 16;
    private static final int NUMBEER_OF_ZEROS_AT_END_OF_BASIC_PROGRAM = 7;
    private static final int ASCII_BLOCK_SIZE = 256;
    private static final int FILENAME_START_POSITION = 10;

    private TapeExtractionLogging logging;
    private TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    public MSXTapeBlock(BlockType type, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        data = new ArrayList<>();
        this.type = type;
        bytesReceived = 0;
        zerosInARow = 0;
        lastByte = -1;
        machineCodeProgramSize = Integer.MAX_VALUE;
        EOFwasInData = false;
        errors = false;
    }

    public void addByte(byte b) {
        data.add(b);
        lastByte = b;
        bytesReceived++;
        checkForMachineCodeProgramSize();
        checkforEOF();
        if (b == 0)
            zerosInARow++;
        else
            zerosInARow = 0;

        checkForMalformedHeader();
    }

    public boolean blockHasErrors() {
        return errors;
    }

    private void checkForMalformedHeader() {
        if (type != BlockType.HEADER)
            return;

        if (bytesReceived < HEADER_LENGTH)
            return;

        BlockType currentHeaderIsOfType = headerType();
        if (currentHeaderIsOfType != BlockType.UNKNOWN)
            return;

        dealWithMalformedHeader();
    }

    private void dealWithMalformedHeader() {
        logging.writeFileParsingInformation("Invalid header block.");
        errors = true;

        if (!options.getAttemptToRecoverCorruptedFiles()) {
            logging.writeFileParsingInformation("Abandoning block");
            return;
        }

        logging.writeFileParsingInformation("Treating as block of unknown type. Continuing until no data found.");
        type = BlockType.UNKNOWN;
    }

    private void checkForMachineCodeProgramSize() {
        if (bytesReceived == 4) {
            int startAddress = getLoadAddressIfThisIsMachineCode();
            int endAddress = getEndAddressIfThisIsMachineCode();
            machineCodeProgramSize = endAddress - startAddress + 1 + 6;
        }
    }

    public int getLoadAddressIfThisIsMachineCode() {
        return Byte.toUnsignedInt(data.get(0)) + Byte.toUnsignedInt(data.get(1)) * 256;
    }

    public int getEndAddressIfThisIsMachineCode() {
        return Byte.toUnsignedInt(data.get(2)) + Byte.toUnsignedInt(data.get(3)) * 256;
    }

    private void checkforEOF() {
        if (Byte.toUnsignedInt(lastByte) == 0x1a)
            EOFwasInData = true;
    }

    public boolean lastBlockIfThisIsAnASCIIFile() {
        return EOFwasInData;
    }

    public byte [] getDataAsArray() {
        byte [] array = new byte[data.size()];
        int position = 0;
        for (Byte b : data) {
            array[position] = b;
            position++;
        }

        return array;
    }

    public List<Byte> getDataAsList() {
        return data;
    }

    public int getLength() {
        return data.size();
    }

    public String getFilename() {
        if (headerType() != BlockType.UNKNOWN)
            return getFilenameIfThisIsAHeader();
        else
            return "headlessFileBlock";
    }

    public boolean moreBytesNeeded() {
        switch (type) {
            case HEADER:
                return bytesReceived < HEADER_LENGTH;
            case UNKNOWN:
                return true;
            case BASIC_PROGRAM:
                return zerosInARow < NUMBEER_OF_ZEROS_AT_END_OF_BASIC_PROGRAM;
            case ASCII:
                return bytesReceived < ASCII_BLOCK_SIZE;
            case MACHINE_CODE:
                return bytesReceived < machineCodeProgramSize;
        }

        return true;
    }


    public BlockType getTypeOfNextBlockIfThisIsAHeader() {
        return headerType();
    }

    private BlockType headerType() {
        if (data.size() < HEADER_LENGTH)
            return BlockType.UNKNOWN;

        int identificationByte = Byte.toUnsignedInt(data.get(0));
        switch (identificationByte) {
            case 0xd3:
                return BlockType.BASIC_PROGRAM;
            case 0xea:
                return BlockType.ASCII;
            case 0xd0:
                return BlockType.MACHINE_CODE;
            default:
                return BlockType.UNKNOWN;
        }
    }

    private String getFilenameIfThisIsAHeader() {
        if (data.size() < HEADER_LENGTH)
            return "invalid_header";

        StringBuilder name = new StringBuilder();
        for (int pos = FILENAME_START_POSITION; pos < HEADER_LENGTH; pos++)
            name.append((char)(int)data.get(pos));

        return name.toString();
    }
}
