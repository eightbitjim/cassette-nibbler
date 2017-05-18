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

package com.eightbitjim.cassettenibbler.Platforms.Acorn.FileExtraction;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.Arrays;

public class BBCFileBlock {
    public static final int HEADER_COMPLETE = 0;
    public static final int MORE_BYTES_NEEDED = 1;
    public static final int ERROR = 2;
    public static final int DATA_COMPLETE = 3;

    private int [] headerBuffer;
    private int headerBufferPointer;

    private int [] dataBuffer;
    private int dataBufferPointer;

    private int startOfFieldsPointer;
    private boolean filenameReceived;
    private boolean headerReceived;
    private static final int MAX_FILENAME_SIZE = 10;
    private static final int FIELDS_SIZE = 19;
    private static final int CRC_SIZE = 2;
    private static final int MAX_HEADER_SIZE = MAX_FILENAME_SIZE + 1 + FIELDS_SIZE;

    private static final int HEADER_CRC_POSITION = 17;
    private boolean errorInHeader;

    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    public BBCFileBlock() {
        reset();
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (headerBuffer != null)
            hash ^= Arrays.hashCode(headerBuffer);

        if (dataBuffer != null)
            hash ^= Arrays.hashCode(dataBuffer);

        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof BBCFileBlock))
            return false;

        BBCFileBlock other = (BBCFileBlock)o;

        if (!Arrays.equals(headerBuffer, other.headerBuffer))
            return false;

        if (!Arrays.equals(dataBuffer, other.dataBuffer))
            return false;

        return true;
    }

    private void log(String message) {
        logging.writeFileParsingInformation(message);
    }

    public boolean hasAnErorr() {
        return errorInHeader;
    }

    private void logError(String message) {
        logging.writeDataError(0, message);
    }

    public void reset() {
        headerBuffer = new int[MAX_HEADER_SIZE];
        headerBufferPointer = 0;
        startOfFieldsPointer = 0;
        dataBufferPointer = 0;
        filenameReceived = false;
        headerReceived = false;
    }

    public int addByte(int value) {
        if (headerReceived)
            return addByteToData(value);
        else
            return addByteToHeader(value);
    }

    private int addByteToHeader(int value) {
        headerBuffer[headerBufferPointer++]  = value;

        if (!filenameReceived && value == 0) {
            filenameReceived = true;
            startOfFieldsPointer = headerBufferPointer;
        }

        if (!filenameReceived && headerBufferPointer > MAX_FILENAME_SIZE) {
            logError("Filename is too long. Probably a data error.");
            return ERROR;
        }

        if (headerBufferPointer == startOfFieldsPointer + FIELDS_SIZE) {
            if (headerCrcIsValid()) {
                headerReceived = true;
                prepareDataBuffer();
                if (getDataBlockLength() > 0)
                    return HEADER_COMPLETE;
                else
                    return DATA_COMPLETE;
            } else {
                logError("CRC error in header");
                return ERROR;
            }
        }

        return MORE_BYTES_NEEDED;
    }

    public int [] getFilenameBytes() {
        if (startOfFieldsPointer == 0)
            return new int[0];

        return Arrays.copyOf(headerBuffer, startOfFieldsPointer - 1);
    }

    public String getFilename() {
        StringBuilder builder = new StringBuilder();
        if (startOfFieldsPointer == 1)
            return "UNNAMED";

        for (int i = 0; i < startOfFieldsPointer - 1; i++) {
            builder.append((char) headerBuffer[i]);
        }

        return builder.toString();
    }

    private int getTwoByteValue(int start) {
        return headerBuffer[start] + headerBuffer[start + 1] * 256;
    }

    public int getLoadAddress() {
        return getTwoByteValue(startOfFieldsPointer);
    }

    public int getExecutionAddress() {
        return getTwoByteValue(startOfFieldsPointer + 4);
    }

    public int getBlockNumber() {
        return getTwoByteValue(startOfFieldsPointer + 8);
    }

    public int getDataBlockLength() {
        return getTwoByteValue(startOfFieldsPointer + 10);
    }

    public int getBlockFlag() {
        return headerBuffer[startOfFieldsPointer + 12];
    }

    public int getAddressOfNextFile() {
        return getTwoByteValue(startOfFieldsPointer + 13);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(getFilename());
        builder.append(": ");
        builder.append("Load address " + getLoadAddress());
        builder.append(" Execution address " + getExecutionAddress());
        builder.append(" Block " + getBlockNumber());
        builder.append(" Data length " + getDataBlockLength());
        builder.append(" Flags " + getBlockFlag());
        builder.append(" Address of next file " + getAddressOfNextFile());
        return builder.toString();
    }

    private void prepareDataBuffer() {
        dataBuffer = new int[getDataBlockLength() + CRC_SIZE];
        dataBufferPointer = 0;
    }

    private int addByteToData(int value) {
        dataBuffer[dataBufferPointer++] = value;
        if (dataBufferPointer == dataBuffer.length) {
            if (dataCrcIsValid())
                return DATA_COMPLETE;
            else {
                logError("Invalid data CRC");
                return ERROR;
            }
        }

        return MORE_BYTES_NEEDED;
    }

    public boolean headerCrcIsValid() {
        AcornDataCRC crc = new AcornDataCRC();
        for (int i = 0; i < startOfFieldsPointer + HEADER_CRC_POSITION; i++)
            crc.addByte(headerBuffer[i]);

        if (crc.getCRCLow() != headerBuffer[startOfFieldsPointer + HEADER_CRC_POSITION + 1]) {
            logError("Low byte of CRC incorrect in block header");
            if (!options.getAllowIncorrectFileChecksums())
                return false;
            else
                logError("Options set to ignore checksum errors, so continuing.");
        }

        if (crc.getCRCHigh() != headerBuffer[startOfFieldsPointer + HEADER_CRC_POSITION]) {
            logError("High byte of CRC incorrect in block header");
            if (!options.getAllowIncorrectFileChecksums())
                return false;
            else
                logError("Options set to ignore checksum errors, so continuing.");
        }

        return true;
    }

    public boolean dataCrcIsValid() {
        AcornDataCRC crc = new AcornDataCRC();
        for (int i = 0; i < dataBuffer.length - CRC_SIZE; i++)
            crc.addByte(dataBuffer[i]);

        if (crc.getCRCLow() != dataBuffer[dataBuffer.length - 1]) {
            logError("Low byte of CRC incorrect in block data");
            errorInHeader = true;
            if (!options.getAllowIncorrectFileChecksums())
                return false;
            else
                logError("Options set to ignore checksum errors, so continuing.");
        }

        if (crc.getCRCHigh() != dataBuffer[dataBuffer.length - 2]) {
            logError("High byte of CRC incorrect in block data");
            errorInHeader = true;
            if (!options.getAllowIncorrectFileChecksums())
                return false;
            else
                logError("Options set to ignore checksum errors, so continuing.");
        }

        return true;
    }

    public int getNumberOfDataBytesReceived() {
        int numberOfBytes = dataBufferPointer;
        if (dataBufferPointer > getDataBlockLength())
            numberOfBytes = getDataBlockLength();

        return numberOfBytes;
    }

    public int [] getData() {
        return Arrays.copyOf(dataBuffer, getDataBlockLength());
    }
}
