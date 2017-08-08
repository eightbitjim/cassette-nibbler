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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction;

import com.eightbitjim.cassettenibbler.*;
import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;

import java.util.LinkedList;
import java.util.List;

public class EncodingSchemeFileExtractor implements PulseStreamConsumer, FileStreamProvider {

    private transient TapeExtractionLogging logging;

    List<Integer> data;
    List<FileStreamConsumer> consumers;

    enum State { INTER_BYTE_GAP, RECEIVING_BYTE }
    private State state;

    private static final int MINIMUM_FILE_SIZE = 8;
    private EncodingScheme scheme;
    private int initialPulsesToSkip;
    private int pulsesLeftToSkip;

    private char currentPulse;
    private long currentTimeIndex;

    private int interByteGapPulsesLeft;
    private int bitsReceivedInByte;

    private char [] currentBitPulses;
    private int numberOfPulsesReceivedInCurrentBit;

    private int currentByte;
    private int errors;

    public EncodingSchemeFileExtractor(EncodingScheme scheme, int initialPulsesToSkip, String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        this.scheme = scheme;
        this.initialPulsesToSkip = initialPulsesToSkip;
        this.pulsesLeftToSkip = initialPulsesToSkip;
        reset();
        data = new LinkedList<>();
        consumers = new LinkedList<>();
    }

    @Override
    public void registerFileStreamConsumer(FileStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterFileStreamConsumer(FileStreamConsumer consumer) {
        consumers.remove(consumer);
    }

    private void pushFileToComsumers(TapeFile file) {
        for (FileStreamConsumer consumer : consumers)
            consumer.pushFile(file, currentTimeIndex);
    }
    
    private void reset() {
        interByteGapPulsesLeft = 0;
        bitsReceivedInByte = 0;
        currentBitPulses = new char [scheme.getMaximumBitSize()];
        resetBit();
        currentByte = 0;
        state = State.RECEIVING_BYTE;
        errors = 0;
    }

    public int getNumberOfErrors() {
        return errors;
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        if (pulseType == PulseStreamConsumer.INVALID_PULSE_TOO_SHORT)
            return;

        if (pulsesLeftToSkip > 0) {
            pulsesLeftToSkip--;
            logging.writeFileParsingInformation("Skipping pulse. " + pulsesLeftToSkip + " to go.");
            return;
        }

        this.currentPulse = pulseType;
        this.currentTimeIndex = currentTimeIndex;
        processPulse();
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {
        scheme.setPulseLengthsInNanoseconds(pulseExtractiorParameters);
    }

    private void processPulse() {
        logging.writePulse(currentPulse);
        checkForEndOfFile();

        switch (state) {
            case INTER_BYTE_GAP:
                processInterByteGap();
                break;
            case RECEIVING_BYTE:
                receiveByte();
                break;
        }
    }

    private void checkForEndOfFile() {
        switch (currentPulse) {
            case PulseStreamConsumer.END_OF_STREAM:
            case PulseStreamConsumer.INVALID_PULSE_TOO_LONG:
            case PulseStreamConsumer.SILENCE:
                endOffile();
        }
    }

    private void endOffile() {
        if (data.size() >= MINIMUM_FILE_SIZE) {
            GenericTapeFile file = new GenericTapeFile();
            file.filename = "unnamed." + this.hashCode();
            file.type = "automatic";
            file.data = getDataArray();
            file.setAdditionalInformation(scheme.getDescriptor());
            pushFileToComsumers(file);
        }

        resetData();
        pulsesLeftToSkip = initialPulsesToSkip;
    }

    private void resetData() {
        data.clear();
    }

    private int [] getDataArray() {
        int [] dataArray = new int [data.size()];
        for (int i = 0; i < data.size(); i++)
            dataArray[i] = data.get(i);

        return dataArray;
    }

    private void processInterByteGap() {
        interByteGapPulsesLeft--;
        if (interByteGapPulsesLeft == 0) {
            logging.writePulse(' ');
            state = State.RECEIVING_BYTE;
        }
    }

    private void receiveByte() {
        addPulseToCurrentBit();
        checkIfBitMatchesAndAddToByte();
    }

    private void addPulseToCurrentBit() {
        if (numberOfPulsesReceivedInCurrentBit == scheme.getMaximumBitSize()) {
            logging.writeFileParsingInformation("Maximum bit size exceeded");
            recordError();
            resetBit();
        }

        currentBitPulses[numberOfPulsesReceivedInCurrentBit] = currentPulse;
        numberOfPulsesReceivedInCurrentBit++;
    }

    private void resetBit() {
        numberOfPulsesReceivedInCurrentBit = 0;
    }

    private void recordError() {
        errors++;
    }

    private void checkForEndOfByte() {

    }

    private void checkIfBitMatchesAndAddToByte() {
        checkOneBit();
        checkZeroBit();
    }

    private void checkOneBit() {
        if (numberOfPulsesReceivedInCurrentBit != scheme.getOneBit().length)
            return;

        if (pulsesMatch(scheme.getOneBit(), currentBitPulses))
            addBitToByte(1);
    }

    private void checkZeroBit() {
        if (numberOfPulsesReceivedInCurrentBit != scheme.getZeroBit().length)
            return;

        if (pulsesMatch(scheme.getZeroBit(), currentBitPulses))
            addBitToByte(0);
    }

    private boolean pulsesMatch(char [] first, char [] second) {
        int sizeToCompare = Math.min(first.length, second.length);
        for (int i = 0; i < sizeToCompare; i++) {
            if (first[i] != second[i])
                return false;
        }

        return true;
    }

    private void addBitToByte(int value) {
        if (value == 1) {
            int bitInByteToAddTo;
            if (scheme.mostSignificantBitIsFirst())
                bitInByteToAddTo = 7 - bitsReceivedInByte;
            else
                bitInByteToAddTo = bitsReceivedInByte;

            currentByte |= (1 << bitInByteToAddTo);
        }

        bitsReceivedInByte++;
        if (bitsReceivedInByte == 8)
            finishByte();

        resetBit();
    }

    private void finishByte() {
        logging.writeFileParsingInformation(": " + currentByte);
        recordCurrentByte();
        resetByte();

        if (scheme.getPaddingBetweenBytes() > 0) {
            switchToPadding();
        }
    }

    private void switchToPadding() {
        state = State.INTER_BYTE_GAP;
        interByteGapPulsesLeft = scheme.getPaddingBetweenBytes();
    }

    private void resetByte() {
        currentByte = 0;
        bitsReceivedInByte = 0;
    }

    private void recordCurrentByte() {
        data.add(currentByte);
    }
}
