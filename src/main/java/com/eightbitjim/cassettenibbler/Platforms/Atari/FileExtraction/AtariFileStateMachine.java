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

package com.eightbitjim.cassettenibbler.Platforms.Atari.FileExtraction;

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;

import java.util.LinkedList;
import java.util.List;

public class AtariFileStateMachine implements FileStreamProvider, PulseStreamConsumer {
    private transient List<FileStreamConsumer> consumerList = new LinkedList<>();
    private transient long currentTimeIndex;
    private transient TapeExtractionLogging logging;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient String channelName;
    private transient char currentPulse;

    private enum State {WAITING_FOR_LEADER_BYTES, RECEIVING_DATA }
    private transient State state;

    private transient AtariTapeBlock currentBlock;
    private transient AtariByteFrame currentByte;
    private transient AtariTapeFile currentFile;
    private transient AtariLeaderRecogniser leaderRecogniser;

    private int erroneousBytesInARow;
    private static final int maximumErroreousBytesInARowInFile = 3;
    private static final int minimumSizeOfOrphanHeader = 64;

    public AtariFileStateMachine(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.channelName = channelName;
        currentByte = new AtariByteFrame(channelName);
        leaderRecogniser = new AtariLeaderRecogniser();
        reset();
    }

    private void reset() {
        state = State.WAITING_FOR_LEADER_BYTES;
        erroneousBytesInARow = 0;
        currentByte.reset();
        currentFile = new AtariTapeFile();
        currentBlock = null;
        leaderRecogniser.reset();
    }

    private void setState(State stateToSwitchTo) {

        switch (stateToSwitchTo) {
            case WAITING_FOR_LEADER_BYTES:
                currentByte.reset();
                currentBlock = null;
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
            case WAITING_FOR_LEADER_BYTES:
                waitingForLeaderBytes();
                break;
            case RECEIVING_DATA:
                receivingData();
                break;
        }
    }

    private void pushPulseToStream(char pulse) {
        logging.writePulse(pulse);
    }

    private void waitingForLeaderBytes() {
        leaderRecogniser.addPulse(currentPulse);
        if (leaderRecogniser.isLeaderValid())
            setState(State.RECEIVING_DATA);
    }

    private void createNewBlock() {
        logging.writeFileParsingInformation("Creating new atari tape block");
        currentBlock = new AtariTapeBlock(channelName);
    }

    private void processPulseInBlock() {
        int result = currentByte.addPulse(currentPulse, currentTimeIndex);
        switch (result) {
            case AtariByteFrame.MORE_BITS_NEEDED:
                break;
            case AtariByteFrame.ERROR:
                dealWithByteErrorInBlock();
                break;
            default:
                erroneousBytesInARow = 0;
                addByteToBlock(result);
        }
    }

    private void addByteToBlock(int value) {
        if (currentBlock == null)
            createNewBlock();

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
        setState(State.WAITING_FOR_LEADER_BYTES);
    }

    private void addBlockToFile() {
        if (currentBlock.blockHasErrors() && !options.getAttemptToRecoverCorruptedFiles())
            return;

        currentFile.addBlock(currentBlock);
        if (!currentFile.moreBlocksNeeded())
             completeFile();
        else
            getReadyForNextHeader();
    }

    private void getReadyForNextHeader() {
        setState(State.WAITING_FOR_LEADER_BYTES);
    }

    private void completeFile() {
        logging.writeFileParsingInformation("Completing file");
        if (fileIsZeroLength()) {
            logging.writeFileParsingInformation("Not saving zero length file");
        } else if (!currentFile.containsErrors() || options.getAttemptToRecoverCorruptedFiles())
            pushFileToConsumers(currentFile);

        reset();
    }

    private boolean fileIsZeroLength() {
        return currentFile.length() < 1;
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
