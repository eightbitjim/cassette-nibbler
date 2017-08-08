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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader;

import com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats.PETSCII;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.Arrays;

import static com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreFileBlock.DataStatus.ERROR;
import static com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreFileBlock.DataStatus.IN_PROGRESS;
import static com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreFileBlock.DataStatus.SUCCESS;

public class CommodoreFileBlock {
    protected int [] data;
    protected boolean [] validDataHere;
    protected int checksum;
    protected int dataPointer;
    protected boolean checksumIsValid;
    protected int numberOfValidBytes;
    protected boolean isRepeated;

    public static final int DATA = 0;
    public static final int PROGRAM_HEADER = 1;
    public static final int SEQENTIAL_FILE_DATA = 2;
    public static final int NON_RELOCATABLE_PROGRAM_HEADER = 3;
    public static final int SEQENTIAL_FILE_HEADER = 4;
    public static final int END_OF_TAPE_MARKER = 5;

    private static final int MAXIMUM_DATA_LENGTH = 65536;
    public static final int HEADER_DATA_LENGTH = 193;
    protected String filenameOverrideIfNotNull;

    private static final String [] fileTypes = {"ORPHAN_DATA_BLOCK", "PRG", "SEQ", "NRP", "SEQ HEADER", "EOT"};

    protected CommodoreFileBlock nextBlock;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging;

    public enum DataStatus { SUCCESS, ERROR, IN_PROGRESS }

    public CommodoreFileBlock(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        data = new int[MAXIMUM_DATA_LENGTH];
        validDataHere = new boolean[MAXIMUM_DATA_LENGTH];
        prepareFields();
    }

    @Override
    public int hashCode() {
        int hash;
        hash = Arrays.hashCode(data);
        hash += isRepeated ? 1 : 2;
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof CommodoreFileBlock))
            return false;

        CommodoreFileBlock other = (CommodoreFileBlock)o;
        if (!Arrays.equals(data, other.data))
            return false;

        return true;
    }

    private void prepareFields() {
        dataPointer = 0;
        numberOfValidBytes = 0;
        checksumIsValid = false;
        nextBlock = null;
        filenameOverrideIfNotNull = null;
    }

    public int getFileType() {
        if (getBlockLength() != HEADER_DATA_LENGTH)
            return DATA;
        else {
            switch (data[0]) {
                case DATA:
                case PROGRAM_HEADER:
                case SEQENTIAL_FILE_DATA:
                case SEQENTIAL_FILE_HEADER:
                case NON_RELOCATABLE_PROGRAM_HEADER:
                case END_OF_TAPE_MARKER:
                    return data[0];
                default:
                    return DATA;
            }
        }
    }

    public void setType(int type) {
        data[0] = type;
    }

    public int getStartAddress() {
        return data[1] + data[2] * 256;
    }
    public void setStartAddress(int address) {
        data[1] = address % 256;
        data[2] = address / 256;
    }

    public void setEndAddressPlusOne(int address) {
        data[3] = address % 256;
        data[4] = address / 256;
    }

    public int [] getFilenameBuffer() {
        return Arrays.copyOfRange(data, 5, 20);
    }

    public void setFilenameBuffer(int [] buffer) {
        for (int i = 0; i < buffer.length; i++)
            data[5 + i] = buffer[i];
    }

    public String getFilenameString() {
        if (getFileType() == SEQENTIAL_FILE_DATA) {
            return "SEQ BLOCK";
        }

        if (filenameOverrideIfNotNull != null)
            return filenameOverrideIfNotNull;

        StringBuffer filename = new StringBuffer();
        for (int c : getFilenameBuffer()) {
            filename.append(PETSCII.printableCharacterForPetsciiCode(c));
        }

        return filename.toString().trim();
    }

    public int [] makeFilenameBuffer(String name) {
        int [] buffer = new int[16];
        for (int i = 0; i < buffer.length; i++) {
            if (i < name.length())
                buffer[i] = name.charAt(i);
            else
                buffer[i] = 32;
        }

        return buffer;
    }

    public void prepareDummySequentialFileHeader(int fileSequenceNumber) {
        data[0] = SEQENTIAL_FILE_HEADER;
        data[1] = 0; // Start address
        data[2] = 0;
        data[3] = 0; // End address
        data[4] = 0;

        filenameOverrideIfNotNull = "orphan seq file data " + fileSequenceNumber;
    }

    public void setRepeated(boolean isRepeated) {
        this.isRepeated = isRepeated;
    }

    public boolean isARepeat() {
        return isRepeated;
    }

    public DataStatus getStatus() {
        if (dataPointer < data.length + 1)
            return IN_PROGRESS;

        if (isValid())
            return SUCCESS;
        else
            return ERROR;
    }

    public void addValidByte(int byteToAdd, boolean isFromRepeatedBlock) {
        if (dataPointer == data.length) {
            receiveChecksum(byteToAdd);
            dataPointer++;
            return;
        }

        data[dataPointer] = byteToAdd;
        if (!validDataHere[dataPointer]) {
            validDataHere[dataPointer] = true;
            numberOfValidBytes++;
        }

        dataPointer++;
    }

    public void merge(CommodoreFileBlock other) {
        int size = other.getBlockLength();
        for (int i = 0; i < size; i++) {
            if (i >= dataPointer) {
                data[i] = other.data[i];
                dataPointer = i + 1;
            } else {
                if (!validDataHere[i] && other.validDataHere[i])
                    data[i] = other.data[i];
            }
        }
    }

    public void invalidByteReceived(int byteReceivedAs) {
        if (dataPointer == data.length) {
            receiveChecksum(byteReceivedAs);
            return;
        }

        if (!validDataHere[dataPointer])
            data[dataPointer] = mergeBytes(data[dataPointer], byteReceivedAs);

        dataPointer++;
    }

    private void receiveChecksum(int value) {
        checksum = value;
        checksumIsValid = checksum == computeChecksum();
    }

    private int computeChecksum() {
        int sum = 0;
        for (int i = 0; i < getBlockLength(); i++) {
            sum ^= data[i];
        }

        return sum;
    }

    private int mergeBytes(int a, int b) {
        return a; // TODO
    }

    public int [] getData() {
        return data;
    }

    public int getBlockLength() {
        return dataPointer;
    }

    private boolean isValid() {
        if (numberOfValidBytes < getBlockLength())
            return false;

        if (dataPointer != getBlockLength() + 1)
            return false;

        if (!checksumIsValid && options.getAllowIncorrectFileChecksums())
            logging.writeDataError(-1, "Invalid checksum received, but ignoring due to chosen options");
        else if (!checksumIsValid)
            logging.writeDataError(-1, "Invalid checksum received, and will discard data. Specify options to try to recover");

        if (!checksumIsValid && !options.getAllowIncorrectFileChecksums())
            return false;

        return true;
    }
}
