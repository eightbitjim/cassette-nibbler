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

import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

public class BBCFileStateMachine {
    private static final int MINIMUM_LEADER_LENGTH = 32;
    enum State {
        WAITING_FOR_HEADER_LEADER,
        WAITING_FOR_END_OF_HEADER_LEADER,
        WAITING_FOR_HEADER_SYNC_BYTE,
        GETTING_HEADER,
        GETTING_DATA,
        FINISHED }


    private static final int LAST_BLOCK = 128;
    private State state;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private long currentTimeIndex;
    private char currentPulse;
    private int currentByte;
    private int numberOfShortPulsesInARow;
    private boolean currentLeaderIsValid;
    private AcornByte acornByte;
    private BBCFileBlock block;

    private BBCTapeFile currentFile;
    private boolean currentFileIsReadyToReturn;
    private FileBuilder fileBuilder;

    private static final int SYNC_BYTE_VALUE = 0x2a;

    public BBCFileStateMachine(boolean is1200BaudNot300) {
	    currentFileIsReadyToReturn = false;
        acornByte = new AcornByte(is1200BaudNot300 ? AcornByte.Baud.BAUD_1200 : AcornByte.Baud.BAUD_300);
        fileBuilder = new FileBuilder();
        reset();
    }

    private void reset() {
        state = State.WAITING_FOR_HEADER_LEADER;
        acornByte.reset();
        block = new BBCFileBlock();
        numberOfShortPulsesInARow = 0;
        currentLeaderIsValid = false;
        if (currentFile != null)
            currentFile.notInError();
    }

    private void changeStateTo(State state) {
        logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, "Changed state to " + state);
        this.state = state;

        switch (state) {
            case WAITING_FOR_HEADER_LEADER:
                reset();
                break;
        }
    }

    private void log(String message) {
        logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, message);
    }

    private void logError(String message) {
        logging.writeDataError(currentTimeIndex, message);
    }

    public TapeFile addPulse(char pulse, long timeIndex) {
        if (currentPulse == PulseStreamConsumer.END_OF_STREAM)
            attemptToRecoverFile();

        currentPulse = pulse;
        currentTimeIndex = timeIndex;
        checkForLeader();

        switch (state) {
            case WAITING_FOR_HEADER_LEADER:
                checkForHeaderLeader();
                break;

            case WAITING_FOR_END_OF_HEADER_LEADER:
                checkForEndOfHeaderLeader();
                break;

            case WAITING_FOR_HEADER_SYNC_BYTE:
            case GETTING_HEADER:
                addPulseToHeader();
                break;

            case GETTING_DATA:
                addByteToData();
                break;

            case FINISHED:
                break;
        }

        if (currentFileIsReadyToReturn) {
            currentFileIsReadyToReturn = false;
            return currentFile;
        } else
            return null;
    }

    private void checkForLeader() {
        if (currentPulse != PulseStreamConsumer.SHORT_PULSE)
            numberOfShortPulsesInARow = 0;
        else
            numberOfShortPulsesInARow++;

        currentLeaderIsValid = numberOfShortPulsesInARow >= MINIMUM_LEADER_LENGTH;
        if ((currentLeaderIsValid) && (
                (state == State.GETTING_HEADER) || (state == State.GETTING_DATA) || (state == State.WAITING_FOR_HEADER_SYNC_BYTE))) {
            logError("Leader detected unexpectedly. Resetting state machine.");
            if (state == State.GETTING_DATA)
                addDataToFile();

            reset();
        }
    }

    private void checkForHeaderLeader() {
        if (currentLeaderIsValid) {
            pushStringToPulseStream("LEADER DETECTED\n");
            changeStateTo(State.WAITING_FOR_END_OF_HEADER_LEADER);
        }
    }

    private void checkForEndOfHeaderLeader() {
        if (!currentLeaderIsValid) {
            changeStateTo(State.WAITING_FOR_HEADER_SYNC_BYTE);
            acornByte.reset();
            addPulseToHeader();

        }
    }

    private void addPulseToHeader() {
        int value = acornByte.addPulse(currentPulse, currentTimeIndex);
        if (value == AcornByte.ERROR) {
            dealWithErrorInHeader();
        }

        if (value == AcornByte.MORE_BITS_NEEDED)
            return;

        currentByte = value;
        processByteInHeader();
    }

    private void dealWithErrorInHeader() {
        logError("Error in block header. Resetting state machine.");
        reset();
        if (!options.getAttemptToRecoverCorruptedFiles())
            abandonFile();
    }

    private void processByteInHeader() {
        int result = BBCFileBlock.MORE_BYTES_NEEDED;
        switch (state) {
            case WAITING_FOR_HEADER_SYNC_BYTE:
                checkSyncByte();
                break;
            case GETTING_HEADER:
                result = block.addByte(currentByte);
                break;
        }

        switch (result) {
            case BBCFileBlock.HEADER_COMPLETE:
                pushStringToPulseStream("HEADER COMPLETE\n");
                pushStringToPulseStream(block.toString().toUpperCase() + "\n");
                changeStateTo(State.GETTING_DATA);
                log("Got header: " + block.toString());
                break;
            case BBCFileBlock.DATA_COMPLETE:
                pushStringToPulseStream("DATA COMPLETE\n");
                addDataToFile();
                changeStateTo(State.WAITING_FOR_HEADER_LEADER);
                log("Got empty header: " + block.toString());
                break;
            case BBCFileBlock.ERROR:
                dealWithErrorInHeader();
                break;
        }
    }

    private void checkSyncByte() {
        if (currentByte != SYNC_BYTE_VALUE) {
            logError("Invalid block sync byte value: " + currentByte);
            pushStringToPulseStream("INVALID SYNC BYTE\n");
            dealWithErrorInHeader();
        } else {
            pushStringToPulseStream("SYNC BYTE RECEIVED\n");
            changeStateTo(State.GETTING_HEADER);
        }
    }

    private void dealWithErrorInData() {
        logError("Error in block data. Resetting state machine.");
        currentFile.isInError();
        fileBuilder.padWithZeroBytes(block.getDataBlockLength());
        reset();
        if (!options.getAttemptToRecoverCorruptedFiles())
            abandonFile();
        else
            currentFileIsReadyToReturn = true;
    }

    private void abandonFile() {
        fileBuilder.reset();
    }

    private void addByteToData() {
        int value = acornByte.addPulse(currentPulse, currentTimeIndex);
        if (value == AcornByte.ERROR) {
            dealWithErrorInData();
        }

        if (value == AcornByte.MORE_BITS_NEEDED)
            return;

        currentByte = value;
        processByteInData();
    }

    private void processByteInData() {
        int result = block.addByte(currentByte);
        switch (result) {
            case BBCFileBlock.DATA_COMPLETE:
                log("Got data");
                addDataToFile();
                changeStateTo(State.WAITING_FOR_HEADER_LEADER);
                break;
            case BBCFileBlock.ERROR:
                dealWithErrorInData();
                break;
        }
    }

    private void addDataToFile() {
        if (!fileBuilder.isNameSameAs(block.getFilenameBytes())) {
            log("Change of filename. Starting new file.");
            attemptToRecoverFile();
            fileBuilder.reset();
            fileBuilder.setName(block.getFilenameBytes());
            log("New filename " + fileBuilder.getFilename());
        }

        log("Adding data bytes to file. Flag " + block.getBlockFlag());
        boolean success = fileBuilder.addBytesAndReturnTrueIfSuccessful(block);
        if (!success)
                dealWithErrorInData();

        if ((block.getBlockFlag() & LAST_BLOCK) != 0) {
            log("End of file. Saving.");
            finiliseFile();
            reset();
        }
    }

    private void finiliseFile() {
        currentFile = fileBuilder.getFile();
        currentFileIsReadyToReturn = true;
        fileBuilder.reset();
    }

    private void attemptToRecoverFile() {
        if (!options.getAttemptToRecoverCorruptedFiles())
            return;

        if (fileBuilder.getData().length > 0) {
            logError("Recovering partial file: " + block.toString());
            currentFile.isInError();
            finiliseFile();
        }

        fileBuilder.reset();
    }

    private void pushPulseToStream(char pulse) {
        logging.writePulse(pulse);
        if (pulse == PulseStreamConsumer.LONG_PULSE)
            logging.writePulse(' ');
    }

    private void pushStringToPulseStream(String s) {
        int strLen = s.length();
        for (int i = 0; i < strLen; i++)
            pushPulseToStream(s.charAt(i));
    }
}
