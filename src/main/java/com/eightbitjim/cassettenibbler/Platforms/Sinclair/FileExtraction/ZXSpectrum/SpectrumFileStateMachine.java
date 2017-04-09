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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.FileExtraction.ZXSpectrum;

import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.FileStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamProvider;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class SpectrumFileStateMachine implements PulseStreamConsumer, FileStreamProvider, PulseStreamProvider {
    public static final int WAITING_FOR_HEADER_LEADER = 0;
    public static final int HEADER_LEADER_FOUND = 1;
    public static final int DATA_LEADER_FOUND = 6;
    public static final int RECEIVING_HEADER = 2;
    public static final int RECEIVING_DATA = 3;
    public static final int WAITING_FOR_DATA_LEADER = 4;
    public static final int ERROR = 7;
    public static final int LENGTH_OF_HEADER = 19;

    private char currentPulse;

    private static final double maxGapBetweenBytesInSecondsBeforeReset = 0.5;
    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final int maxByteErrorsInARow = 8;

    private long lastCompleteByteReceivedAtTimeIndex;
    private boolean currentByteContainedErrors;
    private boolean receivedByteSinceLeader;

    private List<FileStreamConsumer> fileStreamConsumers;
    private List<PulseStreamConsumer> pulseStreamConsumers;
    private int state;
    private char [] pulseBuffer;
    private static final int SIZE_OF_PULSE_BUFFER = 64;
    private int bufferPointer;
    private int bitPointer;
    private int constructingByte;
    private int [] headerBuffer;
    private int headerBufferPointer;
    private int [] dataBuffer;
    private int dataBufferPointer;
    private int syncPulses;

    private int numberOfByteErrorsInARow;
    private int fileCount;

    Stack<TapeFile> fileStack;
    SpectrumTapeFile currentFile;

    private long currentTimeIndex;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    private boolean leaderIsCurrentlyValid;

    public SpectrumFileStateMachine() {
        fileStreamConsumers = new LinkedList<>();
        pulseStreamConsumers = new LinkedList<>();
        state = WAITING_FOR_HEADER_LEADER;
        pulseBuffer = new char[SIZE_OF_PULSE_BUFFER];
        bufferPointer = SIZE_OF_PULSE_BUFFER - 1;
        headerBuffer = new int[LENGTH_OF_HEADER];
        fileStack = new Stack<>();
        resetByte();
        fileCount = 0;
        leaderIsCurrentlyValid = false;
    }

    public void addPulse(char pulseType) {
        if (pulseType == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("END OF STREAM\n");
            outputFileFragmentIfAllowedTo();
            pushFileToConsumers();
            state = WAITING_FOR_HEADER_LEADER;
            return;
        }

        logging.writePulse(pulseType);
        updateLeaderDetection(pulseType);
        pushPulseToStream(pulseType);

        switch (state) {
            case WAITING_FOR_HEADER_LEADER:
            case WAITING_FOR_DATA_LEADER:
                checkForValidLeader();
                break;
            case HEADER_LEADER_FOUND:
            case DATA_LEADER_FOUND:
                checkForEndOfLeader();
                break;
            case RECEIVING_DATA:
            case RECEIVING_HEADER:
                addPulseToByte();
                break;
            default:
                logging.writeFileParsingInformation("Unknown state reached.");
                break;
        }

        pushFileToConsumers();
    }

    private void updateLeaderDetection(char pulseType) {
        bufferPointer = (bufferPointer + 1) % SIZE_OF_PULSE_BUFFER;
        pulseBuffer[bufferPointer] = pulseType;

        for (int i = 0; i < SIZE_OF_PULSE_BUFFER - 1; i++) {
            int positionToCheck = bufferPointer - 1 - i;
            if (positionToCheck < 0)
                positionToCheck += SIZE_OF_PULSE_BUFFER;

            if (pulseBuffer[positionToCheck] != PulseStreamConsumer.LONG_PULSE) {
                leaderIsCurrentlyValid = false;
                return;
            }
        }

        leaderIsCurrentlyValid = true;
    }

    private void checkForValidLeader() {
        if (!leaderIsCurrentlyValid)
            return;

        if (state == WAITING_FOR_HEADER_LEADER) {
            state = HEADER_LEADER_FOUND;
            logging.writeFileParsingInformation("HEADER LEADER DETECTED");
            fileCount++;
        }
        else
            state = DATA_LEADER_FOUND;
    }

    private void outputFileFragmentIfAllowedTo() {
        if (!options.getAttemptToRecoverCorruptedFiles())
            return;

        logging.writeFileParsingInformation("Attempting to recover partial file. State " + state);
        switch (state) {
            case WAITING_FOR_DATA_LEADER:
                // We must already have a header, so output this as a file
                outputHeaderFragment();
                break;

            case RECEIVING_DATA:
                // Part way through receiving data and found invalid pulse. Output what we have
                outputPartialDataFragment();
                break;
            default:
                logging.writeFileParsingInformation("No file to recover.");
        }
    }

    private void outputHeaderFragment() {
        // TODO -- implement this
    }

    private void outputPartialDataFragment() {
        currentFile.filename = currentFile.filename + ".incomplete";
        currentFile.isInError();
        if (dataBufferPointer == 0)
            currentFile.data = null;
        else {
            currentFile.data = Arrays.copyOf(dataBuffer, dataBufferPointer);
        }

        headerBuffer = new int[LENGTH_OF_HEADER];
        fileStack.add(currentFile);
        logging.writeFileParsingInformation("Added file " + currentFile.filename);
    }

    private void checkForEndOfLeader() {
        receivedByteSinceLeader = false;
        if (currentPulse() == PulseStreamConsumer.LONG_PULSE)
            return;

        if (currentPulse() == PulseStreamConsumer.INVALID_PULSE_TOO_LONG ||
                currentPulse() == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT) {
            outputFileFragmentIfAllowedTo();
            state = WAITING_FOR_HEADER_LEADER;
            logging.writeFileParsingInformation("INVALID PULSE DETECTED. RESETTING.");
            return;
        }

        syncPulses = 1;
        if (state == HEADER_LEADER_FOUND) {
            resetHeaderBuffer();
            state = RECEIVING_HEADER;
            logging.writeFileParsingInformation("END OF HEADER LEADER. WAITING FOR SYNC PULSES.");
        }
        else {
            state = RECEIVING_DATA;
            logging.writeFileParsingInformation("END OF DATA LEADER. RECEIVING DATA.");
        }

        resetByte();
        addPulseToByte();
    }

    private void resetHeaderBuffer() {
        headerBufferPointer = 0;
        numberOfByteErrorsInARow = 0;
    }

    private void resetDataBuffer(int length) {
        dataBufferPointer = 0;
        dataBuffer = new int[length];
        numberOfByteErrorsInARow = 0;
    }

    private void resetByte() {
            bitPointer = 0;
            constructingByte = 0;
            currentByteContainedErrors = false;
    }

    private void addPulseToByte() {
        if (syncPulses > 0) {
            syncPulses--;
            logging.writeFileParsingInformation("Discarding sync pulse");
            return;
        }

        if (leaderIsCurrentlyValid) {
            state = WAITING_FOR_HEADER_LEADER;
            logging.writeFileParsingInformation("Leader detected where not expected. Resetting state machine.");
            outputFileFragmentIfAllowedTo();
            return;
        }

        if (currentPulse() == PulseStreamConsumer.INVALID_PULSE_TOO_LONG ||
                currentPulse() == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT) {
            logging.writeDataError(currentTimeIndex, "Invalid pulse found in data block");
            pushStringToPulseStream("INVALID PULSE.\n");
            if (!options.getAllowIncorrectFrameChecksums()) {
                outputFileFragmentIfAllowedTo();
                state = WAITING_FOR_HEADER_LEADER;
                return;
            } else {
                // Spoof a 1 bit
                logging.writeDataError(currentTimeIndex, "Options ask us to continue, so spoofing a 1 bit");
                changeCurrentPulseTo(PulseStreamConsumer.MEDIUM_PULSE);
                currentByteContainedErrors = true;
            }
        }

        constructingByte <<=1;
        if (currentPulse() == PulseStreamConsumer.MEDIUM_PULSE || currentPulse() == PulseStreamConsumer.LONG_PULSE)
            constructingByte |= 1;

        bitPointer++;

        if (bitPointer == 8) {
            bitPointer = 0;
            logging.writeFileParsingInformation(": " + constructingByte);
            addByteToDataOrHeader(constructingByte);

            if (!currentByteContainedErrors) {
                numberOfByteErrorsInARow = 0;
                lastCompleteByteReceivedAtTimeIndex = currentTimeIndex;
            } else {
                numberOfByteErrorsInARow++;
                if (numberOfByteErrorsInARow > maxByteErrorsInARow) {
                    logging.writeDataError(currentTimeIndex, "Exceeded maximum number of byte errors in a row. Abandoning file.");
                    outputFileFragmentIfAllowedTo();
                    state = WAITING_FOR_HEADER_LEADER;
                    return;
                }
            }

            receivedByteSinceLeader = true;
            constructingByte = 0;
        }
    }

    private void changeCurrentPulseTo(char pulse) {
        pulseBuffer[bufferPointer] = pulse;
    }

    private void resetStateMachine() {
        state = WAITING_FOR_HEADER_LEADER;
    }

    private boolean delayTooLongToContinue() {
        return lastCompleteByteReceivedAtTimeIndex != 0 &&
                currentTimeIndex - lastCompleteByteReceivedAtTimeIndex > (long)(maxGapBetweenBytesInSecondsBeforeReset / NANOSECOND);
    }

    private void addByteToDataOrHeader(int b) {
        if (receivedByteSinceLeader && delayTooLongToContinue()) {
            logging.writeDataError(currentTimeIndex, "Too long a pause since last byte. Resetting file state machine");
            outputFileFragmentIfAllowedTo();
            resetStateMachine();
            return;
        }

        switch (state) {
            case RECEIVING_HEADER:
                addByteToHeader(b);
                break;
            case RECEIVING_DATA:
                addByteToData(b);
                break;
        }
    }

    private void addByteToHeader(int b) {
        headerBuffer[headerBufferPointer] = b;
        headerBufferPointer++;
        if (headerBufferPointer == LENGTH_OF_HEADER)
            checkHeaderContents();
    }

    private void checkHeaderContents() {
        if (headerBuffer[0] != 0 && options.getAttemptToRecoverCorruptedFiles()) {
            logging.writeDataError(currentTimeIndex, "Header block does not appear to be header block. Code " + headerBuffer[0]);
            logging.writeFileParsingInformation("Will assume it is a program and read as many bytes as can be found");
            startReceivingOrphanDataBlock();
            return;
        } else {
            if (!isHeaderChecksumValid()) {
                logging.writeDataError(currentTimeIndex, "Header checksum invalid");
                if (!options.getAllowIncorrectFileChecksums()) {
                    logging.writeDataError(currentTimeIndex, "Use options to specify allowing bad checksum if you wish to ignore this checksum");
                    outputFileFragmentIfAllowedTo();
                    state = WAITING_FOR_HEADER_LEADER;
                    return;
                } else {
                    logging.writeDataError(currentTimeIndex, "Options specify to ignore bad checksums, so continuing.");
                }
            }
        }

        state = WAITING_FOR_DATA_LEADER;
        currentFile = new SpectrumTapeFile();
        currentFile.setHeaderData(headerBuffer);
        switch (headerBuffer[1]) {
            case 0:
                currentFile.type = "program";
                break;
            case 1:
                currentFile.type = "numbers";
                break;
            case 2:
                currentFile.type = "characters";
                break;
            case 3:
                currentFile.type = "bytes";
                break;
            default:
                logging.writeDataError(currentTimeIndex, "Invalid file type: " + headerBuffer[1]);
                outputFileFragmentIfAllowedTo();
                state = WAITING_FOR_HEADER_LEADER;
                return;
        }

        logging.writeFileParsingInformation("FILE TYPE: " + currentFile.type.toUpperCase());
        String filename = "";
        for (int i = 0; i < 10; i++)
            filename += (char)headerBuffer[2 + i];

        currentFile.filename = filename.trim();
        logging.writeFileParsingInformation("FILE NAME: " + currentFile.filename.toUpperCase());
        int dataSize = headerBuffer[12] + 256 * headerBuffer[13];
        if (dataSize > 48 * 1024) {
            logging.writeDataError(currentTimeIndex, "Data size specified in header is too large");
            outputFileFragmentIfAllowedTo();
            state = WAITING_FOR_HEADER_LEADER;
            return;
        }

        // TODO other fields such as line number
        dataSize += 2; // Checksum and flag byte
        resetDataBuffer(dataSize);
    }

    private void startReceivingOrphanDataBlock() {
        currentFile = new SpectrumTapeFile();
        currentFile.filename = "headlessFile" + fileCount;
        currentFile.isInError();
        logging.writeFileParsingInformation("FILE NAME: " + currentFile.filename.toUpperCase());
        resetDataBuffer(65535); // Receive maximim size, and finish when there is an error
        for (int i = 1; i < LENGTH_OF_HEADER; i++)
            dataBuffer[i - 1] = headerBuffer[i];

        dataBufferPointer = LENGTH_OF_HEADER - 1;
        state = RECEIVING_DATA;
    }

    private boolean isHeaderChecksumValid() {
        int count = 0;
        for (int i = 0; i < LENGTH_OF_HEADER - 1; i++)
            count = (count ^ headerBuffer[i]) % 256;

        logging.writeFileParsingInformation("EXPECTED CHECKSUM " + count + " GOT " + headerBuffer[LENGTH_OF_HEADER - 1]);
        return count == headerBuffer[LENGTH_OF_HEADER - 1];
    }

    private boolean isDataChecksumValid() {
        int count = 0;
        for (int i = 0; i < dataBuffer.length - 1; i++)
            count = (count ^ dataBuffer[i]) % 256;

        logging.writeFileParsingInformation("EXPECTED CHECKSUM " + count + " GOT " + dataBuffer[dataBuffer.length - 1]);
        return count == dataBuffer[dataBuffer.length - 1];
    }

    private void addByteToData(int b) {
        dataBuffer[dataBufferPointer] = b;
        dataBufferPointer++;
        if (dataBufferPointer == dataBuffer.length)
            checkDataChecksum();
    }

    private void checkDataChecksum() {
        if (!isDataChecksumValid()) {
            logging.writeDataError(currentTimeIndex, "Data checksum invalid");
            if (!options.getAllowIncorrectFileChecksums()) {
                logging.writeDataError(currentTimeIndex, "Use options to specify allowing bad checksum if you wish to ignore this checksum");
                outputFileFragmentIfAllowedTo();
                state = WAITING_FOR_HEADER_LEADER;
                return;
            } else {
                logging.writeDataError(currentTimeIndex, "Options specify to ignore bad checksums, so continuing.");
            }
        }

        currentFile.data = new int[dataBuffer.length - 2];
        for (int i = 0; i < currentFile.data.length - 1; i++)
            currentFile.data[i] = dataBuffer[i + 1];

        fileStack.add(currentFile);
        state = WAITING_FOR_HEADER_LEADER;
    }

    private char currentPulse() {
        return pulseBuffer[bufferPointer];
    }

    @Override
    public void registerFileStreamConsumer(FileStreamConsumer consumer) {
        if (!fileStreamConsumers.contains(consumer))
            fileStreamConsumers.add(consumer);
    }

    @Override
    public void deregisterFileStreamConsumer(FileStreamConsumer consumer) {
        fileStreamConsumers.remove(consumer);
    }

    private void pushFileToConsumers() {
        for (TapeFile file : fileStack) {
            for (FileStreamConsumer consumer : fileStreamConsumers)
                consumer.pushFile(file, currentTimeIndex);
        }

        fileStack.clear();
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        this.currentPulse = pulseType;
        this.currentTimeIndex = currentTimeIndex;
        if (!PulseUtilities.isPulseAnnotation(pulseType))
            addPulse(pulseType);
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {

    }

    @Override
    public void registerPulseStreamConsumer(PulseStreamConsumer consumer) {
        pulseStreamConsumers.add(consumer);
    }

    @Override
    public void deregisterPulseStreamConsumer(PulseStreamConsumer consumer) {
        pulseStreamConsumers.remove(consumer);
    }

    private void pushPulseToStream(char pulse) {
        for (PulseStreamConsumer consumer : pulseStreamConsumers)
            consumer.pushPulse(pulse, currentTimeIndex);
    }

    private void pushStringToPulseStream(String s) {
        s = "<=" + s;
        int strLen = s.length();
        for (int i = 0; i < strLen; i++)
            pushPulseToStream(s.charAt(i));
    }
}

