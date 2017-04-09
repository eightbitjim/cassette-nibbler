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

package com.eightbitjim.cassettenibbler.Platforms.General.Filters;

import com.eightbitjim.cassettenibbler.SampleStreamConsumer;
import com.eightbitjim.cassettenibbler.Sample;
import com.eightbitjim.cassettenibbler.SampleStreamProvider;

import java.util.LinkedList;
import java.util.List;

public class SampleStreamSplitter implements SampleStreamConsumer, SampleStreamProvider {
    private List <SampleStreamConsumer> consumers = new LinkedList<>();

    @Override
    public void push(Sample sample, double currentTimeIndex) {
        for (SampleStreamConsumer consumer : consumers)
            consumer.push(sample, currentTimeIndex);
    }

    @Override
    public void registerSampleStreamConsumer(SampleStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterSampleStreamConsumer(SampleStreamConsumer consumer) {
        consumers.remove(consumer);
    }
}
