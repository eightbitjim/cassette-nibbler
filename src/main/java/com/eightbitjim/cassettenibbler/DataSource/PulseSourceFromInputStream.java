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

package com.eightbitjim.cassettenibbler.DataSource;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;
import com.eightbitjim.cassettenibbler.PulseStreamProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class PulseSourceFromInputStream implements PulseStreamProvider {
    private List<PulseStreamConsumer> consumers = new LinkedList<>();
    private InputStream inputStream;

    public static final int END_OF_STREAM = -1;

    public static final int SUCCESS = 0;

    private int timeCount = 0;

    public PulseSourceFromInputStream(InputStream stream) {
        inputStream = stream;
    }

    @Override
    public void registerPulseStreamConsumer(PulseStreamConsumer consumer) {
        if (!consumers.contains(consumer))
            consumers.add(consumer);
    }

    @Override
    public void deregisterPulseStreamConsumer(PulseStreamConsumer consumer) {
        consumers.remove(consumer);
    }

    public int getNextPulseAndPushToConsumers() throws IOException {
        if (inputStream.available() < 1) {
            pushPulseToConsumers(PulseStreamConsumer.END_OF_STREAM);
            return END_OF_STREAM;
        }

        int nextPulse = inputStream.read();
        if (nextPulse < 0) {
            pushPulseToConsumers(PulseStreamConsumer.END_OF_STREAM);
            return END_OF_STREAM;
        } else {
            pushPulseToConsumers((char)nextPulse);
            return SUCCESS;
        }
    }

    private void pushPulseToConsumers(char pulse) {
        for (PulseStreamConsumer consumer : consumers)
            consumer.pushPulse(pulse, timeCount++);
    }
}
