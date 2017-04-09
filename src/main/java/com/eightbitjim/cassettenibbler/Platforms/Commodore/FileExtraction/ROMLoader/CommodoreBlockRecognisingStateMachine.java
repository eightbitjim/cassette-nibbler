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

import com.eightbitjim.cassettenibbler.Platforms.Commodore.ByteExtraction.CommodoreByteFrame;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.LinkedList;
import java.util.Queue;

import static com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreBlockRecognisingStateMachine.RepeatedStatus.NOT_REPEATED;
import static com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.ROMLoader.CommodoreBlockRecognisingStateMachine.RepeatedStatus.REPEATED;
import static com.eightbitjim.cassettenibbler.PulseStreamConsumer.INVALID_PULSE_TOO_LONG;
import static com.eightbitjim.cassettenibbler.PulseStreamConsumer.LONG_PULSE;
import static com.eightbitjim.cassettenibbler.PulseStreamConsumer.SHORT_PULSE;

public class CommodoreBlockRecognisingStateMachine {
    protected enum State {
        WAITING_FOR_LEADER,
        RECEIVING_LEADER,
        RECEIVING_SYNC_CHAIN,
        RECEIVING_CONTENT,
        RECEIVING_END_OF_BLOCK
    }

    protected enum RepeatedStatus { REPEATED, NOT_REPEATED, UNDETERMINED }

    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private CommodoreFileBuilder fileBuilder;
    private State state;
    private CommodoreFileBlock block;
    private CommodoreByteFrame frame;
    private char currentPulse;
    private long currentTimeIndex;
    private long lastTimeIndex;
    private long currentPulseLengthInNanoseconds;
    private boolean firstPulse;
    private boolean leaderOrTrailerCurrentlyValid;

    private int shortPulsesInARow;
    private static final int MINIMUM_LEADER_LENGTH = 16;
    private RepeatedStatus currentBlockIsRepeated;
    private int syncChainBytesReceived;

    private Queue<TapeFile> filesToReturn = new LinkedList<>();

    public CommodoreBlockRecognisingStateMachine(String defaultFileExtension) {
        frame = new CommodoreByteFrame();
        switchStateTo(State.WAITING_FOR_LEADER);
        firstPulse = true;
        leaderOrTrailerCurrentlyValid = false;
        fileBuilder = new CommodoreFileBuilder(defaultFileExtension);
    }

    private void switchStateTo(State destinationState) {
        logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, "Switching state to " + destinationState.toString());
        switch (destinationState) {
            case WAITING_FOR_LEADER:
                shortPulsesInARow = 0;
                break;

            case RECEIVING_LEADER:
                break;

            case RECEIVING_SYNC_CHAIN:
                syncChainBytesReceived = 0;
                currentBlockIsRepeated = RepeatedStatus.UNDETERMINED;
                frame.reset();
                frame.resetFrameLengthMeasurements();
                frame.addPulseAndReturnStatus(currentPulse, currentPulseLengthInNanoseconds);
                block = new CommodoreFileBlock();
                break;

            case RECEIVING_CONTENT:
                break;

            case RECEIVING_END_OF_BLOCK:
                break;
        }

        state = destinationState;
    }

    public void processPulse(char pulseType, long nanoseconds) {
        if (pulseType == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("End of stream encountered.");
            getPartialFileOrNull();
            return;
        }

        currentPulse = pulseType;
        currentTimeIndex = nanoseconds;
        if (firstPulse) {
            lastTimeIndex = currentTimeIndex;
            firstPulse = false;
        }

        currentPulseLengthInNanoseconds = currentTimeIndex - lastTimeIndex;
        logging.writePulse(pulseType);
        checkForSilence();
        checkForLeaderOrTrailer();

        switch (state) {
            case WAITING_FOR_LEADER:
                stateWaitingForLeader();
                break;
            case RECEIVING_LEADER:
                stateReceivingHeaderLeader();
                break;
            case RECEIVING_SYNC_CHAIN:
                stateReceivingHeaderSyncChain();
                break;
            case RECEIVING_CONTENT:
                stateReceivingBlockContent();
                break;
            case RECEIVING_END_OF_BLOCK:
                stateReceivingEndOfBlock();
                break;
            default:
                break;

        }

        lastTimeIndex = currentTimeIndex;
    }

    public TapeFile getFile() {
        if (filesToReturn.isEmpty())
            return null;
        else {
            TapeFile fileToReturn = filesToReturn.remove();
            return fileToReturn;
        }
    }

    private void reset() {
        block = null;
        leaderOrTrailerCurrentlyValid = false;
        switchStateTo(State.WAITING_FOR_LEADER);
    }

    private void checkForSilence() {
        if (currentPulse == PulseStreamConsumer.SILENCE) {
            logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, "Silence found. Resetting file state machine.");
            reset();
        }
    }

    public void getPartialFileOrNull() {
        logging.writeFileParsingInformation("Checking for any files that have been partially received");
        fileBuilder.completeAnyCurrentFiles();
        addFileToReturnQueue(fileBuilder.getFile());
    }

    private void addFileToReturnQueue(TapeFile file) {
        if (file == null)
            return;

        filesToReturn.add(file);
    }

    private void checkForLeaderOrTrailer() {
        if (currentPulse == SHORT_PULSE)
            shortPulsesInARow++;
        else
            shortPulsesInARow = 0;

        if (shortPulsesInARow >= MINIMUM_LEADER_LENGTH) {
            if (!leaderOrTrailerCurrentlyValid)
                logging.writeFileParsingInformation("LEADER OR TRAILER DETECTED");

            leaderOrTrailerCurrentlyValid = true;
        } else
            leaderOrTrailerCurrentlyValid = false;
    }

    private void stateWaitingForLeader() {
        if (leaderOrTrailerCurrentlyValid)
            switchStateTo(State.RECEIVING_LEADER);
    }

    private void stateReceivingHeaderLeader() {
        if (leaderOrTrailerCurrentlyValid)
            return;

        if ((currentPulse == LONG_PULSE) || (currentPulse == INVALID_PULSE_TOO_LONG)) {
            switchStateTo(State.RECEIVING_SYNC_CHAIN);
            return;
        } else {
            shortPulsesInARow = 0;
            switchStateTo(State.WAITING_FOR_LEADER);
        }
    }

    private void stateReceivingHeaderSyncChain() {
        if (leaderOrTrailerCurrentlyValid) {
            logging.writeFileParsingInformation("Leader or trailer resets state machine.");
            reset();
            return;
        }

        int status = frame.addPulseAndReturnStatus(currentPulse, currentPulseLengthInNanoseconds);
        switch (status) {
            case CommodoreByteFrame.FRAME_COMPLETE_WITH_ERROR:
                logging.writeDataError(currentTimeIndex, "Frame error");
                if (options.getAllowIncorrectFrameChecksums()) {
                    logging.writeDataError(currentTimeIndex, "Options specify to allow checkbit errors, so will treat frame as valid");
                    addByteToHeaderSyncChain(frame.getByteToleratingError());
                } else
                    errorWhileReceivingBlock();
                break;
            case CommodoreByteFrame.INVALID_FRAME_START:
                logging.writeDataError(currentTimeIndex, "Unexpected pulse type received: " + currentPulse);
                errorWhileReceivingBlock();
                break;
            case CommodoreByteFrame.FRAME_INCOMPLETE: // Ok, continue
                break;
            case CommodoreByteFrame.FRAME_COMPLETE:
                addByteToHeaderSyncChain(frame.getByte());
                break;
        }
    }

    private void addByteToHeaderSyncChain(int receivedSyncByte) {
        boolean validSyncByteReceived = false;
        switch (currentBlockIsRepeated) {
            case REPEATED:
                if (receivedSyncByte == 0x09 - syncChainBytesReceived)
                    validSyncByteReceived = true;
                break;
            case NOT_REPEATED:
                if (receivedSyncByte == 0x89 - syncChainBytesReceived)
                    validSyncByteReceived = true;
                break;
            case UNDETERMINED:
                if (receivedSyncByte == 0x09) {
                    validSyncByteReceived = true;
                    currentBlockIsRepeated = REPEATED;
                    block.setRepeated(true);
                    break;
                }

                if (receivedSyncByte == 0x89) {
                    validSyncByteReceived = true;
                    currentBlockIsRepeated = NOT_REPEATED;
                    block.setRepeated(false);
                    break;
                }

                validSyncByteReceived = false;
                break;
        }

        if (!validSyncByteReceived) {
            logging.writeDataError(currentTimeIndex, "Invalid sync byte");
            errorWhileReceivingBlock();
            return;
        }

        syncChainBytesReceived++;
        if (syncChainBytesReceived == 9)
            switchStateTo(State.RECEIVING_CONTENT);
    }

    private void errorWhileReceivingBlock() {
        if (options.getAttemptToRecoverCorruptedFiles()) {
            logging.writeDataError(currentTimeIndex, "Options specified to attempt to recover files. Attempting to recover this block.");
            blockReceived();
        }

        reset();
    }

    private void stateReceivingBlockContent() {
        if (leaderOrTrailerCurrentlyValid) {
            endOfBlockFound();
            return;
        }

        int status = frame.addPulseAndReturnStatus(currentPulse, currentPulseLengthInNanoseconds);
        switch (status) {
            case CommodoreByteFrame.FRAME_COMPLETE_WITH_ERROR:
                logging.writeDataError(currentTimeIndex, "Frame error");
                if (options.getAllowIncorrectFrameChecksums()) {
                    logging.writeDataError(currentTimeIndex, "Options specify to allow checkbit errors, so will treat frame as valid");
                    addInvalidByteToBlockContents(frame.getByteToleratingError());
                } else
                    errorWhileReceivingBlock();
                break;

            case CommodoreByteFrame.INVALID_FRAME_START:
                logging.writeDataError(currentTimeIndex, "Invalid frame start received");
                if (options.getAllowIncorrectFrameChecksums()) {
                    logging.writeDataError(currentTimeIndex, "Options specify to allow checkbit errors, so will scan for the next frame start.");
                    frame.scanForNextFrameStart();
                } else
                    errorWhileReceivingBlock();

                break;
            case CommodoreByteFrame.FRAME_INCOMPLETE: // Ok, continue
                break;
            case CommodoreByteFrame.FRAME_COMPLETE:
                addValidByteToBlockContents(frame.getByte());
                break;
        }
    }

    private void addValidByteToBlockContents(int receivedHeaderByte) {
        block.addValidByte(receivedHeaderByte, currentBlockIsRepeated == REPEATED);
        dealWithBlockStatus();
    }

    private void addInvalidByteToBlockContents(int receivedHeaderByte) {
        block.invalidByteReceived(receivedHeaderByte);
        dealWithBlockStatus();
    }

    private void dealWithBlockStatus() {
        switch (block.getStatus()) {
            case IN_PROGRESS:
                break;
            case ERROR:
                errorWhileReceivingBlock();
                break;
            case SUCCESS:
                switchStateTo(State.RECEIVING_END_OF_BLOCK);
                break;
        }
    }

    private void endOfBlockFound() {
        logging.writeFileParsingInformation("END OF BLOCK DETECTED");
        blockReceived();
    }

    private void stateReceivingEndOfBlock() {
        if (currentPulse == PulseStreamConsumer.SHORT_PULSE)
            return;

        logging.writeFileParsingInformation("END OF TRAILER FOUND");
        blockReceived();
    }

    private void blockReceived() {
        if (block == null) {
            logging.writeFileParsingInformation("No block received to store.");
        } else {
            logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, "Received block type " + block.getFileType() + " length " + block.getBlockLength() + " Repeated " + block.isARepeat());

            if (block.getBlockLength() > 0) {
                fileBuilder.addBlock(block);
                addFileToReturnQueue(fileBuilder.getFile());
            }  else
                logging.writeFileParsingInformation("Zero data bytes in block, so not storing.");
        }

        switchStateTo(State.WAITING_FOR_LEADER);
    }
}
