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

package com.eightbitjim.cassettenibbler.Platforms.Amstrad.FileExtraction;

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class FileStateMachine implements PulseStreamConsumer, FileStreamProvider, PulseStreamProvider {
    enum State {
        WAITING_FOR_LEADER,
        LEADER_FOUND,
        RECEIVING_BLOCK_CONTENTS
    }

    private char currentPulse;

    private static final double maxGapBetweenBytesInSecondsBeforeReset = 0.5;
    private static final double NANOSECOND = 1.0 / 1000000000.0;
    private static final int maxByteErrorsInARow = 8;
    private static final int minimumFileSizeInBytes = 1;

    private long lastCompleteByteReceivedAtTimeIndex;
    private boolean currentByteContainedErrors;
    private boolean receivedByteSinceLeader;
    private List<FileStreamConsumer> fileStreamConsumers;
    private List<PulseStreamConsumer> pulseStreamConsumers;
    private State state;
    private char [] pulseBuffer;
    private static final int SIZE_OF_PULSE_BUFFER = 64;
    private int bufferPointer;
    private int bitPointer;
    private int constructingByte;
    private int syncPulses;
    private FileBlock currentBlock;
    private int numberOfByteErrorsInARow;
    Stack<TapeFile> fileStack;
    AmstradTapeFile currentFile;

    private long currentTimeIndex;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging;
    private transient String channelName;

    private boolean leaderIsCurrentlyValid;

    public FileStateMachine(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.channelName = channelName;
        fileStreamConsumers = new LinkedList<>();
        pulseStreamConsumers = new LinkedList<>();
        state = State.WAITING_FOR_LEADER;
        pulseBuffer = new char[SIZE_OF_PULSE_BUFFER];
        bufferPointer = SIZE_OF_PULSE_BUFFER - 1;
        fileStack = new Stack<>();
        resetByte();
        leaderIsCurrentlyValid = false;
    }

    public void addPulse(char pulseType) {
        if (pulseType == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("END OF STREAM\n");
            outputFileFragmentIfAllowedTo();
            pushFileToConsumers();
            state = State.WAITING_FOR_LEADER;
            return;
        }

        logging.writePulse(pulseType);
        updateLeaderDetection(pulseType);
        pushPulseToStream(pulseType);

        switch (state) {
            case WAITING_FOR_LEADER:
                checkForValidLeader();
                break;
            case LEADER_FOUND:
                checkForEndOfLeader();
                break;
            case RECEIVING_BLOCK_CONTENTS:
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

            if (pulseBuffer[positionToCheck] != PulseStreamConsumer.MEDIUM_PULSE) {
                leaderIsCurrentlyValid = false;
                return;
            }
        }

        leaderIsCurrentlyValid = true;
    }

    private void checkForValidLeader() {
        if (!leaderIsCurrentlyValid)
            return;

        state = State.LEADER_FOUND;
        logging.writeFileParsingInformation("LEADER DETECTED");
    }

    private void outputFileFragmentIfAllowedTo() {
        if (!options.getAttemptToRecoverCorruptedFiles())
            return;

        logging.writeFileParsingInformation("Completing current file.");
        pushFileAndStartNewOne();
    }

    private void checkForEndOfLeader() {
        receivedByteSinceLeader = false;
        if (currentPulse() == PulseStreamConsumer.MEDIUM_PULSE)
            return;

        if (currentPulse() != PulseStreamConsumer.SHORT_PULSE) {
            state = State.WAITING_FOR_LEADER;
            logging.writeFileParsingInformation("INVALID PULSE DETECTED. RESETTING.");
            return;
        }

        syncPulses = 1;
        resetBlockBuffer();
        state = State.RECEIVING_BLOCK_CONTENTS;
        logging.writeFileParsingInformation("END OF HEADER LEADER. WAITING FOR SYNC PULSES.");

        resetByte();
        addPulseToByte();
    }

    private void resetBlockBuffer() {
        createNewFileBlock();
        numberOfByteErrorsInARow = 0;
    }

    private void createNewFileBlock() {
        createNewFileIfNeeded();
        currentBlock = new FileBlock(currentFile.getNumberOfSubsectionsInNextBlock(), channelName);
    }

    private void createNewFileIfNeeded() {
        if (currentFile == null)
            currentFile = new AmstradTapeFile(channelName);
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

        if (currentPulse() == PulseStreamConsumer.INVALID_PULSE_TOO_LONG ||
                currentPulse() == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT) {
            logging.writeDataError(currentTimeIndex, "Invalid pulse found in data block");
            pushStringToPulseStream("INVALID PULSE.\n");
            if (!options.getAllowIncorrectFrameChecksums()) {
                outputFileFragmentIfAllowedTo();
                state = State.WAITING_FOR_LEADER;
                return;
            } else {
                // Spoof two 1 bits if too long. If too short ignore.
                if (currentPulse() == PulseStreamConsumer.INVALID_PULSE_TOO_LONG) {
                    logging.writeDataError(currentTimeIndex, "Options ask us to continue, so spoofing two 1 bits");
                    changeCurrentPulseTo(PulseStreamConsumer.MEDIUM_PULSE);
                    addPulseToByte();
                    currentByteContainedErrors = true;
                } else {
                    logging.writeFileParsingInformation("Ignoring.");
                }
            }
        }

        constructingByte <<=1;
        if (currentPulse() == PulseStreamConsumer.MEDIUM_PULSE || currentPulse() == PulseStreamConsumer.LONG_PULSE)
            constructingByte |= 1;

        bitPointer++;

        if (bitPointer == 8)
            processCompletedByte();
    }

    private void processCompletedByte() {
        bitPointer = 0;
        logging.writeFileParsingInformation(": " + constructingByte);

        addByteToBlock();

        if (!currentByteContainedErrors) {
            numberOfByteErrorsInARow = 0;
            lastCompleteByteReceivedAtTimeIndex = currentTimeIndex;
        } else {
            numberOfByteErrorsInARow++;
            if (numberOfByteErrorsInARow > maxByteErrorsInARow) {
                logging.writeDataError(currentTimeIndex, "Exceeded maximum number of byte errors in a row. Abandoning file.");
                outputFileFragmentIfAllowedTo();
                state = State.WAITING_FOR_LEADER;
                return;
            }
        }

        currentByteContainedErrors = false;
        receivedByteSinceLeader = true;
        constructingByte = 0;
    }

    private void changeCurrentPulseTo(char pulse) {
        pulseBuffer[bufferPointer] = pulse;
    }

    private void resetStateMachine() {
        state = State.WAITING_FOR_LEADER;
    }

    private boolean delayTooLongToContinue() {
        return lastCompleteByteReceivedAtTimeIndex != 0 &&
                currentTimeIndex - lastCompleteByteReceivedAtTimeIndex > (long)(maxGapBetweenBytesInSecondsBeforeReset / NANOSECOND);
    }

    private void addByteToBlock() {
        if (receivedByteSinceLeader && delayTooLongToContinue()) {
            logging.writeDataError(currentTimeIndex, "Too long a pause since last byte. Resetting file state machine");
            outputFileFragmentIfAllowedTo();
            resetStateMachine();
            return;
        }

        currentBlock.addByte((byte)constructingByte);
        if (!currentBlock.moreBytesNeeded()) {
            blockReceived();
        }
    }

    private void blockReceived() {
        logging.writeFileParsingInformation("Complete block received");
        addBlockToCurrentFile();
        completeFileIfAllBytesReceived();
        state = State.WAITING_FOR_LEADER;
    }

    private void addBlockToCurrentFile() {
        currentFile.addBlock(currentBlock);
    }

    private void completeFileIfAllBytesReceived() {
        if (!currentFile.moreBytesNeeded()) {
            if (currentFile.containsErrors() && !options.getAttemptToRecoverCorruptedFiles())
                logging.writeFileParsingInformation("File contains errors and options are set not to recover corrupted files. Discarding.");
            else {
                logging.writeFileParsingInformation("Completing current file.");
                pushFileAndStartNewOne();
            }
        }
    }

    private void pushFileAndStartNewOne() {
        if (currentFile != null) {
            if (currentFile.length() < minimumFileSizeInBytes) {
                logging.writeFileParsingInformation("File is too short so not storing.");
                return;
            }

            fileStack.add(currentFile);
        }

        currentFile = new AmstradTapeFile(channelName);
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

