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

import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.FileStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.LinkedList;
import java.util.List;

public class TurboTapeFileExtractor implements FileStreamProvider, PulseStreamConsumer {
    private List<FileStreamConsumer> fileStreamConsumers = new LinkedList<>();

    private long currentTimeIndex;
    private char currentPulse;
    private TapeFile currentFile;
    private TurboTapeFileStateMachine stateMachine;

    private int mimumAcceptableFileSizeInBytes = 8;

    public TurboTapeFileExtractor(String channelName) {
        stateMachine = new TurboTapeFileStateMachine(channelName);
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

    private void pushFileToConsumer() {
        if (currentFile.length() >= mimumAcceptableFileSizeInBytes) {
            for (FileStreamConsumer consumer : fileStreamConsumers)
                consumer.pushFile(currentFile, currentTimeIndex);
        }
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        this.currentTimeIndex = currentTimeIndex;
        this.currentPulse = pulseType;
        if (!PulseUtilities.isPulseAnnotation(pulseType))
            processPulse();
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {

    }

    private void processPulse() {
        currentFile = stateMachine.pushPulse(currentPulse, currentTimeIndex);
        if (currentFile != null)
            pushFileToConsumer();
    }
}
