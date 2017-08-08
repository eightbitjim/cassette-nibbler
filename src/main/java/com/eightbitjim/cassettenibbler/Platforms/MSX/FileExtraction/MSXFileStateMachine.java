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

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;

import java.util.LinkedList;
import java.util.List;

public class MSXFileStateMachine implements FileStreamProvider, PulseStreamConsumer {
    private transient List<FileStreamConsumer> consumerList = new LinkedList<>();
    private transient long currentTimeIndex;
    private transient TapeExtractionLogging logging;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient char currentPulse;

    private enum State { WAITING_FOR_LEADER, RECEIVING_LEADER, RECEIVING_DATA }
    private transient State state;

    private transient int leaderPulsesFoundInARow;
    private transient static final int pulsesToCountAsLeader = 128;

    private transient MSXTapeBlock currentBlock;
    private transient MSXByteFrame currentByte;
    private transient MSXTapeFile currentFile;
    private transient String channelName;

    private int erroneousBytesInARow;
    private static final int maximumErroreousBytesInARowInFile = 3;
    private static final int minimumSizeOfOrphanHeader = 64;

    public MSXFileStateMachine(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.channelName = channelName;
        currentByte = new MSXByteFrame(channelName);
        reset();
    }

    private void reset() {
        state = State.WAITING_FOR_LEADER;
        leaderPulsesFoundInARow = 0;
        erroneousBytesInARow = 0;
        currentByte.reset();
        currentFile = new MSXTapeFile();
    }

    private void setState(State stateToSwitchTo) {

        switch (stateToSwitchTo) {
            case WAITING_FOR_LEADER:
                currentByte.reset();
                leaderPulsesFoundInARow = 0;
                break;

        }

        logging.writeFileParsingInformation("Switching state to " + stateToSwitchTo.toString());
        state = stateToSwitchTo;
    }

    private void pushFileToConsumers(TapeFile file) {
        for (FileStreamConsumer consumer : consumerList)
            consumer.pushFile(file, currentTimeIndex);
    }

    @Override
    public void registerFileStreamConsumer(FileStreamConsumer consumer) {
        if (!consumerList.contains(consumer))
            consumerList.add(consumer);
    }

    @Override
    public void deregisterFileStreamConsumer(FileStreamConsumer consumer) {
        consumerList.remove(consumer);
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        this.currentTimeIndex = currentTimeIndex;
        this.currentPulse = pulseType;
        processPulse();
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {
        // Ignore
    }

    private void processPulse() {
        if (currentPulse == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("End of stream. Completing current file.");
            completeFile();
            return;
        }

        pushPulseToStream(currentPulse);
        switch (state) {
            case WAITING_FOR_LEADER:
                waitingForLeader();
                break;
            case RECEIVING_LEADER:
                receivingLeader();
                break;
            case RECEIVING_DATA:
                receivingData();
                break;
        }
    }

    private void pushPulseToStream(char pulse) {
        logging.writePulse(pulse);
        if (pulse == PulseStreamConsumer.MEDIUM_PULSE)
            logging.writePulse(' ');
    }

    private void waitingForLeader() {
        switch (currentPulse) {
            case PulseStreamConsumer.SHORT_PULSE:
                foundLeaderPulse();
                break;
            default:
                notLeaderPulse();
                break;
        }
    }

    private void foundLeaderPulse() {
        leaderPulsesFoundInARow++;
        if (leaderPulsesFoundInARow > pulsesToCountAsLeader)
            setState(State.RECEIVING_LEADER);
    }

    private void notLeaderPulse() {
        leaderPulsesFoundInARow = 0;
    }

    private void receivingLeader() {
        switch (currentPulse) {
            case PulseStreamConsumer.SHORT_PULSE:
                break;
            case MEDIUM_PULSE:
                endOfLeaderFound();
                break;
            default:
                invalidPulseInLeader();
                break;
        }
    }

    private void invalidPulseInLeader() {
        logging.writeFileParsingInformation("Invalid pulse in leader. Leader lost.");
        setState(State.WAITING_FOR_LEADER);
    }

    private void endOfLeaderFound() {
        logging.writeFileParsingInformation("End of leader found.");

        createNewBlock();
        processPulseInBlock();
        setState(State.RECEIVING_DATA);
    }

    private void createNewBlock() {
        currentBlock = new MSXTapeBlock(currentFile.nextBlockType(), channelName);
    }

    private void processPulseInBlock() {
        int result = currentByte.addPulse(currentPulse, currentTimeIndex);
        switch (result) {
            case MSXByteFrame.MORE_BITS_NEEDED:
                break;
            case MSXByteFrame.ERROR:
                dealWithByteErrorInBlock();
                break;
            default:
                erroneousBytesInARow = 0;
                addByteToBlock(result);
        }
    }

    private void addByteToBlock(int value) {
        currentByte.reset();
        currentBlock.addByte((byte)value);
        if (!currentBlock.moreBytesNeeded()) {
            addBlockToFile();
        }
    }

    private void dealWithByteErrorInBlock() {
        logging.writeFileParsingInformation("Invalid pulse in byte frame");
        currentFile.hasAnError();

        if (options.getAllowIncorrectFrameChecksums() && !currentByte.tooManyErrors()) {
            spoofByteValueOrGiveUp();
        } else {
            giveUpOnBlock();
        }

        currentByte.reset();
    }

    private void spoofByteValueOrGiveUp() {
        erroneousBytesInARow++;
        if (erroneousBytesInARow < maximumErroreousBytesInARowInFile) {
            logging.writeFileParsingInformation("Spoofing 0 byte");
            addByteToBlock(0);
        } else {
            logging.writeFileParsingInformation("Exceeded maximum (" + maximumErroreousBytesInARowInFile + ") erroneous bytes tolerated in a row. Giving up on block.");
            giveUpOnBlock();
        }
    }

    private void giveUpOnBlock() {
        logging.writeFileParsingInformation("Giving up on current block and file");
        if (options.getAttemptToRecoverCorruptedFiles())
            currentFile.addBlock(currentBlock);

        completeFile();
        erroneousBytesInARow = 0;
        setState(State.WAITING_FOR_LEADER);
    }

    private void addBlockToFile() {
        if (currentBlock.blockHasErrors() && !options.getAttemptToRecoverCorruptedFiles())
            return;

        currentFile.addBlock(currentBlock);
        outputBlockInformationToLog();
        if (!currentFile.moreBlocksNeeded())
             completeFile();
        else
            getReadyForNextHeader();
    }

    private void outputBlockInformationToLog() {
        switch (currentFile.nextBlockType()) {
            case BASIC_PROGRAM:
                logging.writeFileParsingInformation("Basic program: " + currentFile.getFilename());
                break;
            case MACHINE_CODE:
                logging.writeFileParsingInformation("Machine code: " + currentFile.getFilename());
                break;
            case ASCII:
                logging.writeFileParsingInformation("ASCII file:" + currentFile.getFilename());
                break;
            default:
                logging.writeFileParsingInformation("Not a header block");
                break;
        }
    }

    private void getReadyForNextHeader() {
        setState(State.WAITING_FOR_LEADER);
    }

    private void completeFile() {
        logging.writeFileParsingInformation("Completing file");
        if (fileIsZeroLength()) {
            logging.writeFileParsingInformation("Not saving zero length file");
        } else if (fileIsOrphanHeaderBelowMinimumSize()) {
            logging.writeFileParsingInformation("Not saving orphan header of less than " + minimumSizeOfOrphanHeader + " bytes");
        } else if (!currentFile.containsErrors() || options.getAttemptToRecoverCorruptedFiles())
            pushFileToConsumers(currentFile);

        reset();
    }

    private boolean fileIsZeroLength() {
        return currentFile.length() < 1;
    }

    private boolean fileIsOrphanHeaderBelowMinimumSize() {
        return currentFile.isOrphanHeader() && currentFile.length() < minimumSizeOfOrphanHeader;
    }

    private void receivingData() {
        switch (currentPulse) {
            case MEDIUM_PULSE:
            case SHORT_PULSE:
                processPulseInBlock();
                break;
            default:
                dealWithByteErrorInBlock();
                break;
        }
    }
}
