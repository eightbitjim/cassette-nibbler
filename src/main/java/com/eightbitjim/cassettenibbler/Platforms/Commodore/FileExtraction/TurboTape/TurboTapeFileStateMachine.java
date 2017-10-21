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

package com.eightbitjim.cassettenibbler.Platforms.Commodore.FileExtraction.TurboTape;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;
import com.eightbitjim.cassettenibbler.TapeFile;

public class TurboTapeFileStateMachine {
    public enum State { WAITING_FOR_SYNC, SYNC_IN_PROGRESS, VALID_SYNC, START_SEQUENCE_IN_PROGRESS, RECEIVING_FILE}

    private static final int SYNC_BYTE_VALUE = 2;
    private int syncBytesInARow;

    private static final int SYNC_VALUES_IN_A_ROW_NEEDED = 10;
    private transient TapeExtractionLogging logging;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    private TurboTapeByteFrame byteFrame;
    private TurboTapeLeaderRecogniser leaderRecogniser;

    private State state;
    private int currentByte;
    private char currentPulse;
    private long currentTimeIndex;
    private long erroneousPulsesBeforeThisByte;
    private boolean silenceBeforeThisByte;
    private TurboTapeFile currentFile;
    private boolean currentFileReadyToReturn;
    private int nextStartByte;

    public TurboTapeFileStateMachine(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        byteFrame = new TurboTapeByteFrame(channelName);
        leaderRecogniser = new TurboTapeLeaderRecogniser();
        switchToState(State.WAITING_FOR_SYNC);
        currentFileReadyToReturn = false;
        reset();
    }

    private void switchToState(State state) {
        this.state = state;
        switch (state) {
            case START_SEQUENCE_IN_PROGRESS:
                nextStartByte = 0x09;
                erroneousPulsesBeforeThisByte = 0;
                break;
            case SYNC_IN_PROGRESS:
                byteFrame.reset();
                break;
            case RECEIVING_FILE:
                erroneousPulsesBeforeThisByte = 0;
                currentFile = new TurboTapeFile();
                break;
        }
    }

    private void prepareFile() {
        currentFile = new TurboTapeFile();
    }

    public TapeFile pushPulse(char pulse, long currentTimeIndex) {
        if (pulse == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("End of stream encountered.");
            abandonFile();
            if (currentFileReadyToReturn) {
                TapeFile fileToReturn = currentFile;
                currentFile = null;
                currentFileReadyToReturn = false;
                return fileToReturn;
            } else
                return null;
        }

        this.currentTimeIndex = currentTimeIndex;
        this.currentPulse = pulse;

        checkForErroneousPulsesOrSilence();

        switch (state) {
            case WAITING_FOR_SYNC:
                matchSyncStream();
                break;
            case SYNC_IN_PROGRESS:
                checkSyncStreamIsLongEnough();
                break;
            case VALID_SYNC:
                checkIfSyncFinished();
                break;
            case START_SEQUENCE_IN_PROGRESS:
                processPulseForStartSequence();
                break;
            case RECEIVING_FILE:
                putPulseIntoByte();
                addByteToFile();
                break;
        }

        if (currentFileReadyToReturn) {
            TapeFile fileToReturn = currentFile;
            currentFile = null;
            currentFileReadyToReturn = false;
            return fileToReturn;
        } else {
            return null;
        }
    }

    private void checkForErroneousPulsesOrSilence() {
        switch (currentPulse) {
            case PulseStreamConsumer.INVALID_PULSE_TOO_SHORT:
                // Ignore
                break;
            case PulseStreamConsumer.INVALID_PULSE_TOO_LONG:
                erroneousPulsesBeforeThisByte++;
                break;
            case PulseStreamConsumer.SILENCE:
                silenceBeforeThisByte = true;
                break;
        }
    }

    private void matchSyncStream() {
        leaderRecogniser.addPulse(currentPulse);
        if (leaderRecogniser.isLeaderValid()) {
            logging.writeFileParsingInformation("Possible sync start");
            switchToState(State.SYNC_IN_PROGRESS);
        }
    }

    private void checkSyncStreamIsLongEnough() {
        putPulseIntoByte();
        checkIfSyncStillValid();
    }

    private void putPulseIntoByte() {
        currentByte = byteFrame.processPulseAndReturnByteOrErrorCode(currentPulse);
        switch (currentByte) {
            case TurboTapeByteFrame.END_OF_STREAM:
                logging.writeFileParsingInformation("END OF PULSE STREAM");
                attemptToRecoverPartialFileIfAllowed();
                break;
            case TurboTapeByteFrame.MORE_PULSES_NEEDED:
                break;
            case TurboTapeByteFrame.INVALID_PULSE:
                erroneousPulsesBeforeThisByte++;
                finishFile();
                break;
            default:
                if (this.erroneousPulsesBeforeThisByte == 0)
                break;
        }
    }

    private void checkIfSyncStillValid() {
        if (currentByte == TurboTapeByteFrame.MORE_PULSES_NEEDED)
            return;

        logging.writeFileParsingInformation("Got byte value " + currentByte);
        if (currentByte != SYNC_BYTE_VALUE)
            processNonSyncValue();
        else
            processSyncValue();
    }

    private void resetPulseErrorCounters() {
        erroneousPulsesBeforeThisByte = 0;
        silenceBeforeThisByte = false;
    }

    private void processNonSyncValue() {
        syncBytesInARow = 0;
        logging.writeFileParsingInformation("Not valid sync sequence");
        switchToState(State.WAITING_FOR_SYNC);
    }

    private void processSyncValue() {
        syncBytesInARow++;
        if (syncBytesInARow == SYNC_VALUES_IN_A_ROW_NEEDED) {
            logging.writeFileParsingInformation("Sync sequence confirmed");
            switchToState(State.VALID_SYNC);
        }
    }

    private void checkIfSyncFinished() {
        currentByte = byteFrame.processPulseAndReturnByteOrErrorCode(currentPulse);
        switch (currentByte) {
            case TurboTapeByteFrame.MORE_PULSES_NEEDED:
                return;
        }

        if (currentByte == SYNC_BYTE_VALUE)
            return;

        switchToState(State.START_SEQUENCE_IN_PROGRESS);
        checkStartSequence();
    }

    private void processPulseForStartSequence() {
        currentByte = byteFrame.processPulseAndReturnByteOrErrorCode(currentPulse);
        if (currentByte == TurboTapeByteFrame.MORE_PULSES_NEEDED)
            return;

        checkStartSequence();
    }

    private void checkStartSequence() {
        if (currentByte != nextStartByte) {
            logging.writeFileParsingInformation("Invalid start sequence byte received");
            switchToState(State.WAITING_FOR_SYNC);
            return;
        }

        if (nextStartByte == 0) {
            logging.writeFileParsingInformation("Start sequence received");
            switchToState(State.RECEIVING_FILE);
            return;
        }

        nextStartByte--;
    }

    private void addByteToFile() {
        if (currentByte == TurboTapeByteFrame.MORE_PULSES_NEEDED)
            return;

        if (silenceBeforeThisByte) {
            logging.writeDataError(currentTimeIndex, "SILENCE DETECTED.");
            abandonFile();
            resetPulseErrorCounters();
            return;
        }

        currentFile.addByteToFile(currentByte);
        resetPulseErrorCounters();
    }

    private void abandonFile() {
        logging.writeDataError(currentTimeIndex, "ABANDONNG FILE.");
        attemptToRecoverPartialFileIfAllowed();
        resetPulseErrorCounters();
        reset();
    }

    private void attemptToRecoverPartialFileIfAllowed() {
        if (options.getAttemptToRecoverCorruptedFiles()) {
            logging.writeDataError(0, "ATTEMPTING TO RECOVER PARTIAL FILE.");
            processReceivedFile();
        }
    }

    private void finishFile() {
        logging.writeFileParsingInformation("File finished");
        processReceivedFile();
        switchToState(State.WAITING_FOR_SYNC);
    }

    private void processReceivedFile() {
        logging.writeFileParsingInformationWithTimestamp(currentTimeIndex, "FILE RECEIVED");
        if (currentFile != null)
            currentFileReadyToReturn = true;
    }

    private void reset() {
        switchToState(State.WAITING_FOR_SYNC);
        leaderRecogniser.reset();
        byteFrame.reset();
    }

    private void errorInFile() {
        logging.writeDataError(currentTimeIndex, "ERROR RECEIVING FILE");
        reset();
    }

    private void pushStringToPulseStream(String s) {
        logging.writeFileParsingInformation(s);
    }
}
