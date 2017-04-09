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

package com.eightbitjim.cassettenibbler.Platforms.Oric.FileExtraction;

import com.eightbitjim.cassettenibbler.Platforms.Oric.Formats.OricOneBasicProgram;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.Arrays;

public class OricTapeFile extends TapeFile {
    public enum State { RECEIVING_HEADER, HEADER_ERROR, RECEIVING_DATA, DATA_ERROR, COMPLETE }

    private static final int MAXIMUM_HEADER_BUFFER_SIZE = 1024;

    private static final int PROGRAM_TYPE_POSITION = 2;
    private static final int END_ADDRESS_POSITION = 4;
    private static final int START_ADDRESS_POSITION = 6;
    private static final int FILENAME_START_POSITION = 9;

    public static final int PROGRAM_TYPE_BASIC = 0;
    public static final int PROGRAM_TYPE_MACHINE_CODE = 1; // Actually, the ROM loader takes any non-zero value as machine code

    private byte [] headerBuffer;
    private int bufferPointer;
    private byte [] dataBuffer;
    private String filename;
    private boolean errorInFile;

    private State state;
    private int currentByte;

    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    public OricTapeFile() {
        state = State.RECEIVING_HEADER;
        headerBuffer = new byte[MAXIMUM_HEADER_BUFFER_SIZE];
        errorInFile = false;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (headerBuffer != null)
            hash ^= Arrays.hashCode(headerBuffer);

        if (dataBuffer != null)
            hash ^= Arrays.hashCode(dataBuffer);

        hash ^= state.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof OricTapeFile))
            return false;

        OricTapeFile other = (OricTapeFile)o;
        if (!Arrays.equals(headerBuffer, other.headerBuffer))
            return false;

        if (!Arrays.equals(dataBuffer, other.dataBuffer))
            return false;

        return state.equals(other.state);
    }

    @Override
    public String toString() {
        return getFilename();
    }

    public State getState() {
        return state;
    }

    @Override
    public String getFilename() {
        StringBuilder filenameBuffer = new StringBuilder();

        if (filename != null && filename.length() > 0)
            filenameBuffer.append(filename);
        else
            filenameBuffer.append("unnamed");

        return filenameBuffer.toString();
    }

    @Override
    public byte[] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case EMULATOR:
                return getEmulatorFileData();
            case BINARY:
            default:
                return getRawData();
            case READABLE:
                return getBasicProgram();
        }
    }

    private byte[] getRawData() {
        return dataBuffer;
    }

    private byte [] getBasicProgram() {
        OricOneBasicProgram program = new OricOneBasicProgram(getRawData());
        return program.toString().getBytes();
    }

    @Override
    public String getExtension() {
        return "oric";
    }

    @Override
    public boolean containsErrors() {
        return errorInFile;
    }

    public void addByteToFile(int currentByte) {
        this.currentByte = currentByte;
        switch (state) {
            case RECEIVING_HEADER:
                addByteToHeader();
                break;
            case RECEIVING_DATA:
                addByteToData();
                break;
        }
    }

    private int getBigEndianTwoByteAt(byte [] array, int position) {
        return Byte.toUnsignedInt(array[position]) * 256 + Byte.toUnsignedInt(array[position + 1]);
    }

    public int getFileStartPosition() {
        return getBigEndianTwoByteAt(headerBuffer, START_ADDRESS_POSITION);
    }

    public int getFileEndPosition() {
        return getBigEndianTwoByteAt(headerBuffer, END_ADDRESS_POSITION);
    }

    @Override
    public int length() {
        if (dataBuffer == null)
            return 0;
        else
            return dataBuffer.length;
    }

    private void addByteToHeader() {
        headerBuffer[bufferPointer] = (byte)currentByte;
        if (endOfHeaderDetected())
            processEndOfHeader();
        else
            bufferPointer++;
    }

    private boolean endOfHeaderDetected() {
        return (bufferPointer >= FILENAME_START_POSITION && currentByte == 0);
    }

    private void processEndOfHeader() {
        truncateHeader();
        checkHeaderValidity();
        constructFilename();
        if (state != State.HEADER_ERROR)
            startReceivingData();
    }

    private void truncateHeader() {
        headerBuffer = Arrays.copyOf(headerBuffer, bufferPointer + 1);
    }

    private void checkHeaderValidity() {
        checkProgramType();
        checkfileLength();
    }

    private void constructFilename() {
        StringBuffer filenameBuffer = new StringBuffer();
        int position = FILENAME_START_POSITION;
        while (position <= MAXIMUM_HEADER_BUFFER_SIZE && headerBuffer[position] != 0) {
            filenameBuffer.append(Character.toString((char)headerBuffer[position]));
            position++;
        }

        filename = filenameBuffer.toString();
        logging.writeFileParsingInformation("FILENAME PARSED AS: " + filename);
    }

    private void checkProgramType() {
        int programType = Byte.toUnsignedInt(headerBuffer[PROGRAM_TYPE_POSITION]);
        switch (programType) {
            case PROGRAM_TYPE_BASIC:
                logging.writeFileParsingInformation("FILE TYPE 0: BASIC");
                break;
            case PROGRAM_TYPE_MACHINE_CODE:
            default:
                logging.writeFileParsingInformation("FILE TYPE NONZERO: MACHINE CODE");
                break;
        }
    }

    private void checkfileLength() {
        if (getFileEndPosition() < getFileStartPosition()) {
            logging.writeDataError(0, "INVALID FILE LENGTH. START ADDRESS IS GREATER THAN END ADDRESS.");
            recordErrorInHeader();
        } else {
            logging.writeFileParsingInformation("FILE LENGTH: " + (getFileEndPosition() - getFileStartPosition()) + " BYTES.");
        }
    }

    private void recordErrorInHeader() {
        logging.writeDataError(0, "ERROR IN DATA. STOPPING PROCESSING FILE.");
        state = State.HEADER_ERROR;
    }

    private void startReceivingData() {
        state = State.RECEIVING_DATA;
        dataBuffer = new byte[getFileEndPosition() - getFileStartPosition()];
        bufferPointer = 0;
        checkForEndOfData();
    }

    private void addByteToData() {
        dataBuffer[bufferPointer] = (byte)currentByte;
        bufferPointer++;
        checkForEndOfData();
    }

    private void checkForEndOfData() {
        if (bufferPointer >= dataBuffer.length)
            finishReceivingData();
    }

    private void finishReceivingData() {
        state = State.COMPLETE;
    }

    public void byteErrorDetected() {
        errorInFile = true;
        logging.writeDataError(0, "ERROR DETECTED IN FILE");
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "oric.bin";
            case EMULATOR:
                return "oric.tap";
            case READABLE:
                return "oric.txt";
        }
    }

    private byte [] getEmulatorFileData() {
        OricEmulatorFileData emulatorFileData = new OricEmulatorFileData(headerBuffer, dataBuffer);
        return emulatorFileData.getOricTAPFileData();
    }
}
