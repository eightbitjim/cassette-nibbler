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

import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseExtractiorParameters;
import com.eightbitjim.cassettenibbler.Platforms.General.PulseExtraction.PulseUtilities;
import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.FileStreamProvider;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;

import java.util.LinkedList;
import java.util.List;

public class BBCFileExtractor implements PulseStreamConsumer, FileStreamProvider {
    private List<FileStreamConsumer> fileStreamConsumers;

    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private long currentTimeIndex;
    private char currentPulse;
    private BBCFileStateMachine stateMachine;

    public BBCFileExtractor(boolean is1200BaudNot300) {
        stateMachine = new BBCFileStateMachine(is1200BaudNot300);
        fileStreamConsumers = new LinkedList<>();
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

    private void pushFileToconsumers(TapeFile file) {
        for (FileStreamConsumer consumer : fileStreamConsumers)
            consumer.pushFile(file, currentTimeIndex);
    }

    private void outputAnyOPartialFilesIfAllowed() {
        if (options.getAttemptToRecoverCorruptedFiles()) {
            // TODO -- need to implement this
            TapeFile file = null;
            pushFileToconsumers(file);
        }
    }

    @Override
    public void pushPulse(char pulseType, long currentTimeIndex) {
        currentPulse = pulseType;
        this.currentTimeIndex = currentTimeIndex;

        if (!PulseUtilities.isPulseAnnotation(pulseType))
            processPulse();
    }

    @Override
    public void notifyChangeOfPulseLengths(PulseExtractiorParameters pulseExtractiorParameters) {

    }

    private void pushEndOfStream() {
        pushFileToconsumers(null);
    }

    private void processPulse() {
        TapeFile file = null;
        switch (currentPulse) {
            case PulseStreamConsumer.END_OF_STREAM:
                outputAnyOPartialFilesIfAllowed();
                pushEndOfStream();
                break;

            default:
                file = stateMachine.addPulse(currentPulse, currentTimeIndex);
                break;
        }

        if (file != null)
            pushFileToconsumers(file);
    }
}
