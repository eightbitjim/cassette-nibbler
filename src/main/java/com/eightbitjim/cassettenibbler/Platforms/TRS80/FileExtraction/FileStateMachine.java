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

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;

import java.util.LinkedList;
import java.util.List;

public class FileStateMachine implements FileStreamProvider, PulseStreamConsumer {
    private transient List<FileStreamConsumer> consumerList = new LinkedList<>();
    private transient long currentTimeIndex;
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient char currentPulse;

    private transient LeaderRecogniser leaderRecogniser;

    private enum State { WAITING_FOR_LEADER, RECEIVING_BLOCK }
    private transient State state;

    private transient TapeBlock currentBlock;
    private transient ByteFrame currentByte;
    private transient FileBuilder fileBuilder;

    private int erroneousBytesInARow;
    private static final int maximumErroreousBytesInARowInFile = 3;

    public FileStateMachine(TapeFile.FileType fileType) {
        currentByte = new ByteFrame();
        leaderRecogniser = new LeaderRecogniser();
        fileBuilder = new FileBuilder(fileType);
        reset();
    }

    private void reset() {
        state = State.WAITING_FOR_LEADER;
        erroneousBytesInARow = 0;
        currentByte.reset();
        currentBlock = null;
    }

    private void setState(State stateToSwitchTo) {

        switch (stateToSwitchTo) {
            case WAITING_FOR_LEADER:
                currentByte.reset();
                break;
        }

        logging.writeFileParsingInformation("Switching state to " + stateToSwitchTo.toString());
        state = stateToSwitchTo;
    }

    private void pushFileToConsumers(com.eightbitjim.cassettenibbler.TapeFile file) {
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
            fileBuilder.finishAnyPartiallyCompleteFiles();
            pushAnyCompletedFiles();
            return;
        }

        pushPulseToStream(currentPulse);
        switch (state) {
            case WAITING_FOR_LEADER:
                waitingForLeader();
                break;
            case RECEIVING_BLOCK:
                receiveBlock();
                break;
        }

        pushAnyCompletedFiles();
    }

    private void pushAnyCompletedFiles() {
        while (fileBuilder.moreFilesToReturn())
            pushFileToConsumers(fileBuilder.getNextFile());
    }

    private void pushPulseToStream(char pulse) {
        logging.writePulse(pulse);
    }

    private void waitingForLeader() {
        leaderRecogniser.addPulse(currentPulse);
        if (leaderRecogniser.leaderIsValid()) {
            logging.writeFileParsingInformation("Leader sequence found");
            createNewBlock();
            setState(State.RECEIVING_BLOCK);
        }
    }

    private void createNewBlock() {
        currentBlock = new TapeBlock();
    }

    private void receiveBlock() {
        int result = currentByte.addPulse(currentPulse, currentTimeIndex);
        switch (result) {
            case ByteFrame.MORE_BITS_NEEDED:
                break;
            case ByteFrame.ERROR:
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
            logging.writeFileParsingInformation("Block complete");
            addBlockToFile();
        }
    }

    private void dealWithByteErrorInBlock() {
        logging.writeFileParsingInformation("Invalid pulse in byte frame");
        fileBuilder.fileHasAnError();

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
            fileBuilder.addBlock(currentBlock);

        fileBuilder.finishAnyPartiallyCompleteFiles();
        erroneousBytesInARow = 0;
        reset();
    }

    private void addBlockToFile() {
        logging.writeFileParsingInformation("Complete block received. Adding to file.");
        fileBuilder.addBlock(currentBlock);
        reset();
    }
}
