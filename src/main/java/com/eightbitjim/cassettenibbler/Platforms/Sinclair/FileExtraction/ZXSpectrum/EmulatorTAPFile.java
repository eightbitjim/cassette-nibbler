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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.FileExtraction.ZXSpectrum;

import java.util.ArrayList;
import java.util.List;

public class EmulatorTAPFile {
    private List<Byte> data;

    public EmulatorTAPFile() {
        data = new ArrayList<>();
    }

    private void addByte(byte b) {
        data.add(b);
    }

    private void addArray(byte [] dataToAdd) {
        for (Byte b : dataToAdd)
            data.add(b);
    }

    private void addTwoByteLittleEndianValue(int value) {
        addByte(getAddressLow(value));
        addByte(getAddressHigh(value));
    }

    private byte [] byteArrayFromDataList() {
        byte [] array = new byte[data.size()];
        int position = 0;
        for (Byte b : data) {
            array[position] = b;
            position++;
        }

        return array;
    }

    private byte getAddressLow(int value) {
        return (byte)(value % 0x100);
    }

    private byte getAddressHigh(int value) {
        return (byte)(value / 0x100);
    }

    public byte [] getData() {
        return byteArrayFromDataList();
    }

    public void addBlock(byte [] data) {
        if (data == null)
            return;

        int size = data.length;
        addTwoByteLittleEndianValue(size);
        addArray(data);
    }

    public void addBlockAndChecksum(byte flagValue, byte [] data) {
        addTwoByteLittleEndianValue(data.length + 2);
        addByte(flagValue);
        addArray(data);
        byte checksum = flagValue;
        for (Byte b : data)
            checksum ^= Byte.toUnsignedInt(b);

        addByte(checksum);
    }
}
