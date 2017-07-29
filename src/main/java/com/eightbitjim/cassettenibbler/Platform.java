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

package com.eightbitjim.cassettenibbler;

import java.util.Collection;
import java.util.LinkedList;

public abstract class Platform {
    public enum Type { WAVEFORM, FILTERED_WAVEFORM, INTERVAL, PULSE, BYTE, FILE }

    protected Collection <Type> inputTypes;
    protected Collection <Type> outputTypes;

    protected String name;
    protected String description;
    protected String configurationString;

    protected SampleStreamConsumer sampleInput;
    protected SampleStreamConsumer postFilterSampleInput;
    protected IntervalStreamConsumer intervalInput;
    protected PulseStreamConsumer pulseInput;
    protected ByteStreamConsumer byteInput;
    protected IntervalStreamProvider intervalOutput;
    protected PulseStreamProvider pulseOutput;
    protected ByteStreamProvider byteOutput;
    protected FileStreamProvider fileOutput;

    public Platform() {
        name = "UNNAMED";
        description = "";
        configurationString = "";
        initialiseInputTypes();
        initialiseOutputTypes();
    }

    public boolean hasHighProcessingOverhead() {
        return false;
    }

    public void setConfigurationString(String config) {
        configurationString = config;
    }

    private void initialiseInputTypes() {
        inputTypes = new LinkedList<>();
    }

    private void initialiseOutputTypes() {
        outputTypes = new LinkedList<>();
    }

    public boolean hasInputType(Type type) {
        return inputTypes.contains(type);
    }

    public boolean hasOutputType(Type type) {
        return outputTypes.contains(type);
    }

    protected void registerTypes() {
        if (sampleInput != null)
            registerInputTypes(Type.WAVEFORM);

        if (postFilterSampleInput != null)
            registerInputTypes(Type.FILTERED_WAVEFORM);

        if (intervalInput != null)
            registerInputTypes(Type.INTERVAL);

        if (pulseInput != null)
            registerInputTypes(Type.PULSE);

        if (intervalOutput != null)
            registerOutputTypes(Type.INTERVAL);

        if (pulseOutput != null)
            registerOutputTypes(Type.PULSE);

        if (fileOutput != null)
            registerOutputTypes(Type.FILE);

        if (byteOutput != null)
            registerOutputTypes(Type.BYTE);
    }

    protected void registerInputTypes(Type ... types) {
        for (Type type : types)
            inputTypes.add(type);
    }

    protected void registerOutputTypes(Type ... types) {
        for (Type type : types)
            outputTypes.add(type);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public SampleStreamConsumer getWaveformInputPoint() throws PlatformAccessError {
        if (sampleInput != null)
            return sampleInput;
        else
            throw new PlatformAccessError("No waveform input point");
    }

    public SampleStreamConsumer getPostFilterWaveformInputPoint() throws PlatformAccessError {
        if (postFilterSampleInput != null)
            return postFilterSampleInput;
        else
            throw new PlatformAccessError("No post-filter waveform input point");
    }

    public IntervalStreamConsumer getIntervalInputPoint() throws PlatformAccessError {
        if (intervalInput != null)
            return intervalInput;
        else
            throw new PlatformAccessError("No interval input point");
    }

    public PulseStreamConsumer getPulseInputPoint() throws PlatformAccessError {
        if (pulseInput != null)
            return pulseInput;
        else
            throw new PlatformAccessError("No pulse input point");
    }

    public ByteStreamConsumer getByteInputPoint() throws PlatformAccessError {
        if (byteInput != null)
            return byteInput;
        else
            throw new PlatformAccessError("No byte input point");
    }

    public IntervalStreamProvider getIntervalOutputPoint() throws PlatformAccessError {
        if (intervalOutput != null)
            return intervalOutput;
        else
            throw new PlatformAccessError("No interval output point");
    }

    public PulseStreamProvider getRawPulseOutputPoint() throws PlatformAccessError {
        if (pulseOutput != null)
            return pulseOutput;
        else
            throw new PlatformAccessError("No pulse output point");
    }

    public ByteStreamProvider getByteOutputPoint() throws PlatformAccessError {
        if (byteOutput != null)
            return byteOutput;
        else
            throw new PlatformAccessError("No byte output point");
    }

    public FileStreamProvider getFileOutputPoint() throws PlatformAccessError {
        if (fileOutput != null)
            return fileOutput;
        else
            throw new PlatformAccessError("No file output point");
    }
}
