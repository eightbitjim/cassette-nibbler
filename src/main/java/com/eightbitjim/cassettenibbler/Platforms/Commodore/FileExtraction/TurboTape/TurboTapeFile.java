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

import com.eightbitjim.cassettenibbler.Platforms.Commodore.Formats.CommodoreBasicProgram;
import com.eightbitjim.cassettenibbler.TapeExtractionLogging;
import com.eightbitjim.cassettenibbler.TapeExtractionOptions;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.LinkedList;
import java.util.List;

public class TurboTapeFile extends TapeFile {

    private List<Integer> data;
    private int currentByte;
    private static final int cascadeStartAddress = 0x0801;

    private transient TapeExtractionLogging logging = TapeExtractionLogging.getInstance();
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();


    public TurboTapeFile() {
        data = new LinkedList<>();
    }

    @Override
    public int hashCode() {
        int hash = 0;

        if (data != null)
            hash ^= data.hashCode();

        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof TurboTapeFile))
            return false;

        TurboTapeFile other = (TurboTapeFile)o;

        return data.equals(other.data);
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public String getFilename() {
        return "unnamed" + getExtension();
    }

    @Override
    public byte[] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return getBasicProgram();
            default:
            case BINARY:
                return getDataBytes();
            case EMULATOR:
                return getPRGForData();
        }
    }

    private byte [] getDataBytes() {
        byte [] dataBytes = new byte[data.size()];
        int count = 0;
        for (Integer i : data) {
            dataBytes[count] = (byte)((int)i);
            count++;
        }

        return dataBytes;
    }

    private byte [] getPRGForData() {
        byte [] dataBytes = new byte[data.size() + 2];
        dataBytes[0] = (byte)(cascadeStartAddress % 256);
        dataBytes[1] = (byte)(cascadeStartAddress / 256);

        int count = 2;
        for (Integer i : data) {
            dataBytes[count] = (byte)((int)i);
            count++;
        }

        return dataBytes;
    }

    private byte [] getBasicProgram() {
        CommodoreBasicProgram program = new CommodoreBasicProgram(getPRGForData());
        return program.toString().getBytes();
    }

    @Override
    public String getExtension() {
        return ".turboTape64";
    }

    @Override
    public boolean containsErrors() {
        return false;
    }

    public void addByteToFile(int currentByte) {
        this.currentByte = currentByte;
        addByteToData();
    }

    private int getBigEndianTwoByteAt(int [] array, int position) {
        return array[position] * 256 + array[position + 1];
    }

    @Override
    public int length() {
        if (data == null)
            return 0;
        else
            return data.size();
    }

    private void addByteToData() {
        data.add(currentByte);
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "commodore.bin";
            case EMULATOR:
                return "commodore.PRG";
            case READABLE:
                return "commodore.txt";
        }
    }
}
