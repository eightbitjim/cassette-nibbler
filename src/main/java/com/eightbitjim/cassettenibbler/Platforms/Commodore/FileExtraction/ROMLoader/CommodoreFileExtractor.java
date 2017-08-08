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

import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.FileStreamProvider;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.LinkedList;
import java.util.List;

public class CommodoreFileExtractor implements PulseStreamConsumer, FileStreamProvider {

    private CommodoreBlockRecognisingStateMachine stateMachine;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private List<FileStreamConsumer> consumers;

    private long currentTimeIndex;
    private char currentPulse;

    public CommodoreFileExtractor(String defaultFileExtension, String channelName) {
        consumers = new LinkedList<>();
        stateMachine = new CommodoreBlockRecognisingStateMachine(defaultFileExtension, channelName);
    }

    public void processPulse() {
        stateMachine.processPulse(currentPulse, currentTimeIndex);
        if (currentPulse == PulseStreamConsumer.END_OF_STREAM) {
            stateMachine.getPartialFileOrNull();
        }

        TapeFile file;
        do {
            file = stateMachine.getFile();
            if (file != null)
                pushFileToConsumers(file);
        } while (file != null);

        if (currentPulse == PulseStreamConsumer.END_OF_STREAM)
            pushEndOfStream();
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

    private void pushFileToConsumers(TapeFile file) {
        if (file == null)
            return;

        for (FileStreamConsumer consumer : consumers)
            consumer.pushFile(file, currentTimeIndex);
    }

    private void pushEndOfStream() {
        pushFileToConsumers(null);
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        this.currentPulse = pulseType;
        this.currentTimeIndex = currentTimeIndex;
        if (!PulseUtilities.isPulseAnnotation(pulseType))
            processPulse();
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {

    }

}
