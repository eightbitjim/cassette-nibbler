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

package com.eightbitjim.cassettenibbler.Platforms.Apple.FileExtraction;

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;

import java.util.*;

public class AppleFileStateMachine implements PulseStreamConsumer, FileStreamProvider, PulseStreamProvider {
    enum State {
        WAITING_FOR_HEADER_LEADER,
        HEADER_LEADER_FOUND,
        DATA_LEADER_FOUND,
        RECEIVING_HEADER,
        RECEIVING_DATA,
        WAITING_FOR_DATA_LEADER,
        ERROR
    }

    private State state;
    private static final double maxGapBetweenBytesInSecondsBeforeReset = 0.5;
    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final int maxByteErrorsInARow = 8;

    private long lastCompleteByteReceivedAtTimeIndex;
    private boolean currentByteContainedErrors;
    private boolean receivedByteSinceLeader;

    private List<FileStreamConsumer> fileStreamConsumers;
    private List<PulseStreamConsumer> pulseStreamConsumers;
    private char [] pulseBuffer;
    private static final int SIZE_OF_PULSE_BUFFER = 64;
    private static final int MINIMUM_FILE_LENGTH = 4;
    private int bufferPointer;
    private int bitPointer;
    private int constructingByte;
    private char currentPulse;
    private AppleFileBlock header;
    private AppleFileBlock data;
    private int syncPulses;
    private int numberOfByteErrorsInARow;

    Stack<TapeFile> fileStack;
    AppleTapeFile currentFile;

    private long currentTimeIndex;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging;
    private transient String channelName;

    private boolean leaderIsCurrentlyValid;

    public AppleFileStateMachine(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.channelName = channelName;
        fileStreamConsumers = new LinkedList<>();
        pulseStreamConsumers = new LinkedList<>();
        state = State.WAITING_FOR_HEADER_LEADER;
        pulseBuffer = new char[SIZE_OF_PULSE_BUFFER];
        bufferPointer = SIZE_OF_PULSE_BUFFER - 1;
        fileStack = new Stack<>();
        resetByte();
        leaderIsCurrentlyValid = false;
    }

    public void addPulse(char pulseType) {
        if (pulseType == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("END OF STREAM. Interpreting as silence");
            currentPulse = PulseStreamConsumer.SILENCE;
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

        if (state == State.WAITING_FOR_HEADER_LEADER) {
            state = State.HEADER_LEADER_FOUND;
            logging.writeFileParsingInformation("HEADER LEADER DETECTED");
        }
        else
            state = State.DATA_LEADER_FOUND;
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
        if (currentFile != null) {
            currentFile.isInError();
        }

        checkDataChecksum();
    }

    private void outputPartialDataFragment() {
        if (currentFile != null) {
            currentFile.isInError();
        }

        checkDataChecksum();
    }

    private void checkForEndOfLeader() {
        receivedByteSinceLeader = false;
        if (currentPulse == PulseStreamConsumer.LONG_PULSE)
            return;

        if (currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_LONG ||
                currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT ||
                currentPulse == PulseStreamConsumer.SILENCE) {
            outputFileFragmentIfAllowedTo();
            state = State.WAITING_FOR_HEADER_LEADER;
            logging.writeFileParsingInformation("INVALID PULSE DETECTED. RESETTING.");
            return;
        }

        syncPulses = 1;
        if (state == State.HEADER_LEADER_FOUND) {
            resetHeaderBuffer();
            state = State.RECEIVING_HEADER;
            logging.writeFileParsingInformation("END OF HEADER LEADER. WAITING FOR SYNC PULSES.");
        }
        else {
            resetDataBuffer();
            state = State.RECEIVING_DATA;
            logging.writeFileParsingInformation("END OF DATA LEADER. RECEIVING DATA.");
        }

        resetByte();
        addPulseToByte();
    }

    private void resetHeaderBuffer() {
        header = new AppleFileBlock(channelName);
        numberOfByteErrorsInARow = 0;
    }

    private void resetDataBuffer() {
        data = new AppleFileBlock(channelName);
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
            state = State.WAITING_FOR_HEADER_LEADER;
            logging.writeFileParsingInformation("Leader detected where not expected. Resetting state machine.");
            outputFileFragmentIfAllowedTo();
            return;
        }

        if (currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_LONG ||
                currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT ||
                currentPulse == PulseStreamConsumer.SILENCE) {
            logging.writeDataError(currentTimeIndex, "Invalid pulse found in data block. Signifies end of the block");
            checkBlockChecksum();
            return;
        }

        constructingByte <<=1;
        if (currentPulse == PulseStreamConsumer.MEDIUM_PULSE || currentPulse == PulseStreamConsumer.LONG_PULSE)
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
                    state = State.WAITING_FOR_HEADER_LEADER;
                    return;
                }
            }

            receivedByteSinceLeader = true;
            constructingByte = 0;
        }
    }

    private void checkBlockChecksum() {
        switch (state) {
            case RECEIVING_DATA:
                checkDataChecksum();
                break;

            case RECEIVING_HEADER:
                checkHeaderChecksum();
                break;
        }
    }

    private void checkHeaderChecksum() {
        boolean validChecksum = header.checksumIsCorrect();
        if (!validChecksum && !options.getAllowIncorrectFileChecksums()) {
            logging.writeFileParsingInformation("Invalid header checksum, and options set to not allow invalid checksums. Abandoning block and file.");
            giveUpOnFile();
            return;
        }

        currentFile = new AppleTapeFile(channelName);
        if (!validChecksum) {
            logging.writeFileParsingInformation("Invalid header checksum, but options set to allow.");
            currentFile.isInError();
        }

        state = State.WAITING_FOR_DATA_LEADER;
    }

    private void giveUpOnFile() {
        logging.writeFileParsingInformation("Giving up on current file.");
        state = State.WAITING_FOR_HEADER_LEADER;
    }

    private void resetStateMachine() {
        state = State.WAITING_FOR_HEADER_LEADER;
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
        header.addByte((byte)b);
    }
    private boolean isDataChecksumValid() {
        return data.checksumIsCorrect();
    }

    private void addByteToData(int b) {
        data.addByte((byte)b);
    }

    private void checkDataChecksum() {
        if (!isDataChecksumValid()) {
            logging.writeDataError(currentTimeIndex, "Data checksum invalid");
            if (!options.getAllowIncorrectFileChecksums()) {
                logging.writeDataError(currentTimeIndex, "Use options to specify allowing bad checksum if you wish to ignore this checksum");
                outputFileFragmentIfAllowedTo();
                state = State.WAITING_FOR_HEADER_LEADER;
                return;
            } else {
                logging.writeDataError(currentTimeIndex, "Options specify to ignore bad checksums, so continuing.");
            }
        }

        currentFile.addBlock(header);
        currentFile.addBlock(data);
        logging.writeFileParsingInformation("Storing file. No filename, data length " + currentFile.length());

        fileStack.add(currentFile);
        state = State.WAITING_FOR_HEADER_LEADER;
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
            if (currentFile == null) {
                logging.writeProgramOrEnvironmentError(currentTimeIndex, "Attempt to push null tape file ignored.");
                continue;
            }

            if (currentFile.length() < MINIMUM_FILE_LENGTH) {
                logging.writeFileParsingInformation("Received file is below minimum length of " + MINIMUM_FILE_LENGTH + " bytes so ignoring.");
                continue;
            }

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
}

