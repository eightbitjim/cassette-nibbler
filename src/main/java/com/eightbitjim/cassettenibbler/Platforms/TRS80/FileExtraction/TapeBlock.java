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

package com.eightbitjim.cassettenibbler.Platforms.TRS80.FileExtraction;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.ArrayList;
import java.util.List;

public class TapeBlock {

    public enum BlockType { DATA, END_OF_FILE, NAMEFILE, UNKNOWN }
    public enum FileDataType { DATA, BASIC, MACHINE_CODE, UNKNOWN }

    private List<Byte> rawBlockBytes;
    private transient int bytesReceived;
    private boolean errors;
    private int checksum;

    private static final int MAXIMUM_BLOCK_LENGTH = 0xff;
    private static final int BLOCK_TYPE_BYTE_POSITION = 0;

    private static final int NAMEFILE_BLOCK_VALUE = 0x00;
    private static final int DATA_BLOCK_VALUE = 0x01;
    private static final int END_OF_FILE_BLOCK_VALUE = 0xff;

    private static final int FILENAME_START_POSITION = 0x00;
    private static final int FILENAME_LENGTH = 0x08;
    private static final int DATA_TYPE_POSITION = 0x0a;
    private static final int ASCII_FLAG_POSITION = 0x0b;
    private static final int GAP_FLAG_POSITION = 0x0c;
    private static final int MACHINE_CODE_START_ADDRESS_POSITION = 0x0d;
    private static final int MACHINE_CODE_LOAD_ADDRESS_POSITION = 0x0f;

    private static final int BASIC_TYPE_VALUE = 0x00;
    private static final int DATA_TYPE_VALUE = 0x01;
    private static final int MACHINE_CODE_TYPE_VALUE = 0x02;

    private static final int BLOCK_LENGTH_BYTE_POSITION = 1;
    private TapeExtractionLogging logging;
    private TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private String channelName;

    public TapeBlock(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.channelName = channelName;
        rawBlockBytes = new ArrayList<>();
        bytesReceived = 0;
        errors = false;
        checksum = 0;
        logging.writeFileParsingInformation("New tape block started");
    }

    public void addByte(byte b) {
        rawBlockBytes.add(b);
        bytesReceived++;
        addByteToChecksum(b);
        checkForMalformedNamefileBlock();
    }

    public void addByteToChecksum(byte b) {
        if (moreBytesNeeded())
            checksum = (checksum + Byte.toUnsignedInt(b)) % 0x100;
    }

    public boolean blockHasErrors() {
        if (!blockIsComplete())
            errors = true;

        if (!checksumIsCorrect())
            errors = true;

        return errors;
    }

    private int getBlockLength() {
        if (bytesReceived < BLOCK_LENGTH_BYTE_POSITION + 1)
            return MAXIMUM_BLOCK_LENGTH;
        else
            return oneByteValueAt(BLOCK_LENGTH_BYTE_POSITION);
    }

    public BlockType getType() {
        if (bytesReceived < BLOCK_TYPE_BYTE_POSITION + 1)
            return BlockType.UNKNOWN;

        switch (oneByteValueAt(BLOCK_TYPE_BYTE_POSITION)) {
            case NAMEFILE_BLOCK_VALUE:
                return BlockType.NAMEFILE;
            case DATA_BLOCK_VALUE:
                return BlockType.DATA;
            case END_OF_FILE_BLOCK_VALUE:
                return BlockType.END_OF_FILE;
            default:
                return BlockType.UNKNOWN;
        }
    }

    private int oneByteValueAt(int position) {
        return Byte.toUnsignedInt(rawBlockBytes.get(position));
    }

    private int twoByteValueAt(int position) {
        return oneByteValueAt(position) + oneByteValueAt(position + 1) * 0x100;
    }

    private void checkForMalformedNamefileBlock() {
        if (getType() != BlockType.NAMEFILE)
            return;

        if (moreBytesNeeded())
            return;

        // TODO
    }

    private void dealWithMalformedHeader() {
        logging.writeFileParsingInformation("Invalid namefile block.");
        errors = true;

        if (!options.getAttemptToRecoverCorruptedFiles()) {
            logging.writeFileParsingInformation("Abandoning block");
            return;
        }

        logging.writeFileParsingInformation("Handling malformed headers is not yet implemented");
        // TODO
    }

    public byte [] getDataAsArray() {
        byte [] array = new byte[rawBlockBytes.size()];
        int position = 0;
        for (Byte b : rawBlockBytes) {
            array[position] = b;
            position++;
        }

        return array;
    }

    public List<Byte> getPayloadDataAsList() {
        List<Byte> payloadData = new ArrayList<>();
        if (rawBlockBytes.size() > 3) {
            int position = 0;
            int payloadDataStartPosition = 2;
            int checksumPosition = rawBlockBytes.size() - 1;
            for (Byte value : rawBlockBytes) {
                if (position >= payloadDataStartPosition && position < checksumPosition)
                    payloadData.add(value);

                position++;
            }
        }

        return payloadData;
    }

    public List<Byte> getBlockDataAsList() {
        return rawBlockBytes;
    }

    public List<Byte> getEmulatorDataAsList() {
        List<Byte> data = new ArrayList<>();
        addLeaderSequenceTo(data);
        addBlockStarTo(data);
        data.addAll(getBlockDataAsList());
        addTrailerSequenceTo(data);
        return data;
    }

    private void addLeaderSequenceTo(List<Byte> data) {
        int numberOfLeaderBytes = 0x80;
        final byte syncByteValue = 0x55;

        for (;numberOfLeaderBytes > 0; numberOfLeaderBytes--) {
            data.add(syncByteValue);
        }
    }

    private void addTrailerSequenceTo(List<Byte> data) {
        final byte syncByteValue = 0x55;
        data.add(syncByteValue);
    }

    private void addBlockStarTo(List<Byte> data) {
        final byte blockStartMarker = 0x55;
        final byte syncByte = 0x3c;

        data.add(blockStartMarker);
        data.add(syncByte);
    }

    public int getLength() {
        if (rawBlockBytes == null || rawBlockBytes.size() < 3)
            return 0;

        return rawBlockBytes.size() - 3;
    }

    public String getFilename() {
        if (getType() != BlockType.UNKNOWN)
            return getFilenameIfThisIsNameFile();
        else
            return "headlessFileBlock";
    }

    public boolean moreBytesNeeded() {
        return bytesReceived < getBlockLength() + 3;
    }

    private boolean blockIsComplete() {
        if (moreBytesNeeded())
            return false;

        boolean checksumIsCorrect = checksumIsCorrect();
        if (!checksumIsCorrect)
            logging.writeFileParsingInformation("Block checksum is incorrect");

        if (!checksumIsCorrect() && options.getAllowIncorrectFileChecksums()) {
            logging.writeFileParsingInformation("Options set to allow incorrect file checksums, so continuing.");
            return true;
        }

        return checksumIsCorrect();
    }

    public boolean checksumIsCorrect() {
        if (moreBytesNeeded()) {
            logging.writeFileParsingInformation("Checking checksum, but not enough bytes received in block.");
            return false;
        }

        if (options.getAllowIncorrectFileChecksums())
            return true;

        boolean checksumIsValid = checksum == oneByteValueAt(bytesReceived - 1);
        return checksumIsValid;
    }

    private String getFilenameIfThisIsNameFile() {
        if (!blockIsComplete() || (bytesReceived < FILENAME_LENGTH + 1))
            return "invalid_header";

        StringBuilder name = new StringBuilder();
        for (int pos = FILENAME_START_POSITION + 2; pos < FILENAME_LENGTH + FILENAME_START_POSITION + 2; pos++)
            name.append((char)(int) rawBlockBytes.get(pos));

        return name.toString();
    }

    public FileDataType getFileDataTypeIfthisIsNameFile() {
        if (bytesReceived < DATA_TYPE_POSITION + 1)
            return FileDataType.UNKNOWN;

        switch (oneByteValueAt(DATA_TYPE_POSITION)) {
            case BASIC_TYPE_VALUE:
                return FileDataType.BASIC;
            case DATA_TYPE_VALUE:
                return FileDataType.DATA;
            case MACHINE_CODE_TYPE_VALUE:
                return FileDataType.MACHINE_CODE;
            default:
                return FileDataType.UNKNOWN;
        }
    }

    public boolean ASCIIFlagIsSet() {
        if (bytesReceived < ASCII_FLAG_POSITION + 1)
            return false;

        return oneByteValueAt(ASCII_FLAG_POSITION) != 0;
    }

    public static TapeBlock createDummyNamefile(String channelName) {
        TapeBlock namefile = new TapeBlock(channelName);
        namefile.addByte((byte)DATA_BLOCK_VALUE);
        namefile.addByte((byte)15);
        namefile.addByte((byte)'h');
        namefile.addByte((byte)'e');
        namefile.addByte((byte)'a');
        namefile.addByte((byte)'d');
        namefile.addByte((byte)'l');
        namefile.addByte((byte)'e');
        namefile.addByte((byte)'s');
        namefile.addByte((byte)'s');
        namefile.addByte((byte)DATA_TYPE_VALUE);
        namefile.addByte((byte)0);
        namefile.addByte((byte)0);
        namefile.addByte((byte)0);
        namefile.addByte((byte)0);
        namefile.addByte((byte)0);
        namefile.addByte((byte)0);
        namefile.addByte((byte)(namefile.checksum % 0x100));
        return namefile;
    }

    public int getLoadAddressIfThisIsMachineCode() {
        if (getType() != BlockType.NAMEFILE) {
            logging.writeProgramOrEnvironmentError(0, "Attempted to get machine code load address for a non-namefile block");
            return 0;
        }

        if (!blockIsComplete()) {
            logging.writeProgramOrEnvironmentError(0, "Attempted to get machine code load address for incomplete namefile block");
            return 0;
        }

        return twoByteValueAt(MACHINE_CODE_LOAD_ADDRESS_POSITION);
    }
}
