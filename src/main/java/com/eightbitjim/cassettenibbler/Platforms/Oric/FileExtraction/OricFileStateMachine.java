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

import com.eightbitjim.cassettenibbler.Platforms.Oric.ByteExtraction.OricByteFrame;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

public class OricFileStateMachine {
    public enum State { WAITING_FOR_SYNC, SYNC1_RECEIVED, SYNC2_RECEIVED, SYNC3_RECEIVED,
        WAITING_FOR_START_BYTE, RECEIVING_FILE}

    private static final int SYNC_BYTE_VALUE = 0x16;
    private static final int START_BYTE_VALUE = 0x24;
    private static final int MAXIMUM_ERRORS_IN_A_ROW = 5;
    private int errorsInARow;

    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    private OricByteFrame byteFrame;
    private OricLeaderRecogniser leaderRecogniser;

    private State state;
    private int currentByte;
    private char currentPulse;
    private long currentTimeIndex;
    private long erroneousPulsesBeforeThisByte;
    private boolean silenceBeforeThisByte;
    private OricTapeFile currentFile;
    private boolean currentFileReadyToReturn;
    private OricTapeFile.FileType fileType;

    public OricFileStateMachine(OricTapeFile.FileType fileType) {
        byteFrame = new OricByteFrame();
        leaderRecogniser = new OricLeaderRecogniser();
        switchToState(State.WAITING_FOR_SYNC);
        currentFileReadyToReturn = false;
        this.fileType = fileType;
        reset();
    }

    private void switchToState(State state) {
        this.state = state;
        switch (state) {
            case SYNC3_RECEIVED:
            case WAITING_FOR_START_BYTE:
                errorsInARow = 0;
                erroneousPulsesBeforeThisByte = 0;
                break;
        }
    }

    private void prepareFile() {
        currentFile = new OricTapeFile(fileType);
    }

    public TapeFile pushPulse(char pulse, long currentTimeIndex) {
        if (pulse == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("End of stream encountered.");
            abandonFile();
            if (currentFileReadyToReturn) {
                currentFileReadyToReturn = false;
                return currentFile;
            }  else
                return null;
        }

        this.currentTimeIndex = currentTimeIndex;
        this.currentPulse = pulse;

        checkForErroneousPulsesOrSilence();

        switch (state) {
            case WAITING_FOR_SYNC:
                matchSyncStream();
                break;
            case SYNC1_RECEIVED:
            case SYNC2_RECEIVED:
            case SYNC3_RECEIVED:
                putPulseIntoByte();
                checkForSnyc();
                break;
            case WAITING_FOR_START_BYTE:
                putPulseIntoByte();
                checkForStartByte();
                break;
            case RECEIVING_FILE:
                putPulseIntoByte();
                addByteToFile();
                break;
        }

        if (currentFileReadyToReturn) {
            currentFileReadyToReturn = false;
            return currentFile;
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
            pushStringToPulseStream("POSSIBLE SYNC START RECEIVED");
            processSyncValue();
        }
    }

    private void putPulseIntoByte() {
        currentByte = byteFrame.processPulseAndReturnByteOrErrorCode(currentPulse);
        switch (currentByte) {
            case OricByteFrame.END_OF_STREAM:
                logging.writeFileParsingInformation("END OF PULSE STREAM");
                attemptToRecoverPartialFileIfAllowed();
                break;
            case OricByteFrame.MORE_PULSES_NEEDED:
                break;
            case OricByteFrame.PARITY_ERROR:
                erroneousPulsesBeforeThisByte++;
                break;
            default:
                if (this.erroneousPulsesBeforeThisByte == 0)
                    errorsInARow = 0;

                break;
        }
    }

    private void checkForSnyc() {
        if (currentByte == OricByteFrame.MORE_PULSES_NEEDED)
            return;

        if (currentByte != SYNC_BYTE_VALUE)
            processNonSyncValue();
        else
            processSyncValue();

        resetPulseErrorCounters();
    }

    private void resetPulseErrorCounters() {
        erroneousPulsesBeforeThisByte = 0;
        silenceBeforeThisByte = false;
    }

    private void processNonSyncValue() {
        switch (state) {
            case SYNC3_RECEIVED:
                checkForStartByte();
                break;
            case WAITING_FOR_SYNC:
            case SYNC1_RECEIVED:
            case SYNC2_RECEIVED:
                switchToState(State.WAITING_FOR_SYNC);
                pushStringToPulseStream("NOT RECOGNISED SYNC SEQUENCE\n");
                break;
        }
    }

    private void processSyncValue() {
        switch (state) {
            case WAITING_FOR_SYNC:
                switchToState(State.SYNC1_RECEIVED);
                break;
            case SYNC1_RECEIVED:
                switchToState(State.SYNC2_RECEIVED);
                break;
            case SYNC2_RECEIVED:
                pushStringToPulseStream("SYNC\n");
                // Deliberately fall through
            case SYNC3_RECEIVED:
                switchToState(State.SYNC3_RECEIVED);
                break;
        }
    }

    private void checkForStartByte() {
        if (currentByte == OricByteFrame.MORE_PULSES_NEEDED)
            return;

        if (currentByte == START_BYTE_VALUE) {
            pushStringToPulseStream("START BYTE\n");
            prepareFile();
            switchToState(State.RECEIVING_FILE);
        } else {
            pushStringToPulseStream("INVALID START BYTE\n");
            switchToState(State.WAITING_FOR_SYNC);
        }

        resetPulseErrorCounters();
    }

    private void addByteToFile() {
        if (currentByte == OricByteFrame.MORE_PULSES_NEEDED)
            return;

        if (silenceBeforeThisByte) {
            logging.writeDataError(currentTimeIndex, "SILENCE DETECTED.");
            abandonFile();
            resetPulseErrorCounters();
            return;
        }

        if (erroneousPulsesBeforeThisByte > 0) {
            currentFile.byteErrorDetected();
            errorsInARow++;
            if (errorsInARow > MAXIMUM_ERRORS_IN_A_ROW) {
                logging.writeDataError(currentTimeIndex, "TOO MANY ERRORS IN A ROW.");
                abandonFile();
                return;
            }
        }

        currentFile.addByteToFile(currentByte);
        OricTapeFile.State fileState = currentFile.getState();
        switch (fileState) {
            case DATA_ERROR:
            case HEADER_ERROR:
                errorInFile();
                break;
            case COMPLETE:
                processReceivedFile();
                reset();
                break;
        }

        resetPulseErrorCounters();
    }

    private void abandonFile() {
        logging.writeDataError(currentTimeIndex, "ABANDONNG FILE.");
        if (currentFile != null)
            currentFile.byteErrorDetected();

        attemptToRecoverPartialFileIfAllowed();
        resetPulseErrorCounters();
        reset();
    }

    private void attemptToRecoverPartialFileIfAllowed() {
        if (options.getAttemptToRecoverCorruptedFiles()) {
            logging.writeDataError(0, "ATTEMPTING TO RECOVER PARTIAL FILE.");
            if (currentFile != null)
                currentFile.byteErrorDetected();
            processReceivedFile();
        }
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
        errorsInARow = 0;
    }

    private void errorInFile() {
        logging.writeDataError(currentTimeIndex, "ERROR RECEIVING FILE");
        reset();
    }

    private void pushStringToPulseStream(String s) {
        logging.writeFileParsingInformation(s);
    }
}
