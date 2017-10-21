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

package com.eightbitjim.cassettenibbler.Platforms.Apple.FileExtraction;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import java.util.Collection;
import java.util.LinkedList;

public class AppleFileBlock {
    private LinkedList<Byte> rawData;
    private TapeExtractionLogging logging;

    public AppleFileBlock(String channelName) {
        logging = TapeExtractionLogging.getInstance(channelName);
        rawData = new LinkedList<>();
    }

    public void addByte(byte value) {
        rawData.add(value);
    }

    public Collection<Byte> getRawData() {
        return rawData;
    }

    public byte [] getRawDataAsByteArray() {
        byte [] data = new byte[rawData.size()];
        int position = 0;
        for (Byte value : rawData)
            data[position++] = value;

        return data;
    }

    public boolean checksumIsCorrect() {
        if (rawData.size() == 0)
            return false;

        int checksum = 0xff;
        int count = 0;
        boolean checksumCorrect = false;
        for (Byte value : rawData) {
            int integerValue = Byte.toUnsignedInt(value);

            count++;
            if (count < rawData.size())
                checksum ^= integerValue;
            else {
                checksumCorrect = checksum == integerValue;
                logging.writeFileParsingInformation("Checksum calculated as " + checksum + ", compared to " + integerValue);
            }
        }

        return checksumCorrect;
    }
}
