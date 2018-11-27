/*
 * Copyright (c) 2018. James Lean
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

package com.eightbitjim.cassettenibbler.Platforms.Other.MPFI.FileExtraction;

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;

import java.util.LinkedList;
import java.util.List;

public class MPFIFileStateMachine implements PulseStreamConsumer, FileStreamProvider, PulseStreamProvider {
    enum State {
        RECEIVING_LEAD_SYNC,
        RECEIVING_HEADER,
        RECEIVING_MID_SYNC,
        RECEIVING_DATA,
        RECEIVING_TAIL_SYNC
    }

    private static final int HEADER_SIZE = 7;

    private State state;
    private List<FileStreamConsumer> fileStreamConsumers;
    private List<PulseStreamConsumer> pulseStreamConsumers;
    private char currentPulse;
    private boolean fileHasError;

    private List<Integer> data;
    MPFIByteFrame byteFrame;

    private long currentTimeIndex;
    private transient TapeExtractionLogging logging;
    private transient String channelName;

    private int lengthOfLeadSync;
    private int lengthOfTailSync;
    private static final int SYNC_LENGTH = 10;

    public MPFIFileStateMachine(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        byteFrame = new MPFIByteFrame();
        this.channelName = channelName;
        fileStreamConsumers = new LinkedList<>();
        pulseStreamConsumers = new LinkedList<>();
        data = new LinkedList<>();
        state = State.RECEIVING_HEADER;
        lengthOfLeadSync = 0;
        lengthOfTailSync = 0;
        fileHasError = false;
    }

    public void addPulse(char pulseType) {
        if (pulseType == PulseStreamConsumer.END_OF_STREAM) {
            logging.writeFileParsingInformation("END OF STREAM Interpreting as silence");
            currentPulse = PulseStreamConsumer.SILENCE;
        }

        logging.writePulse(pulseType);
        pushPulseToStream(pulseType);
        checkForSyncs(pulseType);

        switch (state) {
            case RECEIVING_DATA:
            case RECEIVING_HEADER:
            case RECEIVING_LEAD_SYNC:
            case RECEIVING_MID_SYNC:
            case RECEIVING_TAIL_SYNC:
                addPulseToByte();
                break;
            default:
                logging.writeFileParsingInformation("Unknown state reached");
                break;
        }
    }

    private void checkForSyncs(char currentPulse) {
        if (currentPulse == PulseStreamConsumer.MEDIUM_PULSE)
            lengthOfLeadSync++;
        else
            lengthOfLeadSync = 0;

        if (currentPulse == PulseStreamConsumer.SHORT_PULSE)
            lengthOfTailSync++;
        else
            lengthOfTailSync = 0;

        if (lengthOfLeadSync >= SYNC_LENGTH) {
            if (state != State.RECEIVING_LEAD_SYNC) {
                logging.writeFileParsingInformation("Detected lead sync");
                resetStateMachine();
                state = State.RECEIVING_LEAD_SYNC;
            }

            byteFrame.reset();
        }

        if (lengthOfTailSync >= SYNC_LENGTH) {
            if (state == State.RECEIVING_HEADER) {
                logging.writeFileParsingInformation("Detected mid sync");
                state = State.RECEIVING_MID_SYNC;
            } else if (state == State.RECEIVING_DATA) {
                logging.writeFileParsingInformation("Detected tail sync");
                state = State.RECEIVING_TAIL_SYNC;
                storeFile();
            }

            byteFrame.reset();
        }
    }

    private void storeFile() {
        if (data.size() == 0)
            return; // Don't bother storing or telling us about files with no bytes, as it probably wasn't a file at all

        if (data.size() <= HEADER_SIZE) {
            logging.writeFileParsingInformation("File is not longer than a header so not storing");
            return;
        }

        MPFTapeFile file = new MPFTapeFile();
        file.data = new int[data.size()];
        int count = 0;
        for (Integer value: data) {
            file.data[count] = value;
            count++;
        }

        // If there is enough data for a file header, use this
        if (data.size() >= 7) {
            file.filename = String.format("%02x", file.data[1]) + String.format("%02x", file.data[0]) + '.' + (file.data[2] + file.data[3] * 256);
            file.type = "binary";
        }

        // Was there an error?
        if (fileHasError)
            file.isInError();

        logging.writeFileParsingInformation("Storing file");
        pushFileToConsumers(file);
        data.clear();
    }

    private void addPulseToByte() {
        if ((currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_LONG ||
                currentPulse == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT ||
                currentPulse == PulseStreamConsumer.SILENCE)) {
            if (state == State.RECEIVING_DATA || state == State.RECEIVING_HEADER) {
                logging.writeDataError(currentTimeIndex, "Invalid pulse found in data");
                fileHasError = true;
            }

            return;
        }

        int value = byteFrame.addPulseAndReturnByteOrStatus(currentPulse);
        if (value == MPFIByteFrame.ERROR) {
            if (state == State.RECEIVING_DATA || state == State.RECEIVING_HEADER) {
                logging.writeDataError(currentTimeIndex, "Error in byte frame");
                fileHasError = true;
            }

            return;
        }

        if (value != MPFIByteFrame.MORE_BITS_NEEDED) {
            if (state == State.RECEIVING_LEAD_SYNC) {
                state = State.RECEIVING_HEADER;
                fileHasError = false;
                logging.writeFileParsingInformation("Receiving header");
            }
            else if (state == State.RECEIVING_MID_SYNC) {
                state = State.RECEIVING_DATA;
                logging.writeFileParsingInformation("Receiving data");
            }

            logging.writeFileParsingInformation(": " + value);

            // Process the data byte as header or data
            addByteToData(value);
        }
    }

    private void addByteToData(int value) {
        data.add(value);
    }

    private void resetStateMachine() {
        storeFile();
        state = State.RECEIVING_LEAD_SYNC;
        data.clear();
    }

    public void reset() {
        byteFrame.reset();
        resetStateMachine();
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

    private void pushFileToConsumers(TapeFile file) {
        for (FileStreamConsumer consumer : fileStreamConsumers)
            consumer.pushFile(file, currentTimeIndex);
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

