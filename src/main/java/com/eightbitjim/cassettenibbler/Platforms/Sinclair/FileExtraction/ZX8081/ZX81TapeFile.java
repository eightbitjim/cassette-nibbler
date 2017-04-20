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

package com.eightbitjim.cassettenibbler.Platforms.Sinclair.FileExtraction.ZX8081;

import com.eightbitjim.cassettenibbler.Platforms.Sinclair.Formats.ZX81PFileExtract;
import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.Arrays;

public class ZX81TapeFile extends TapeFile {
    private static final int FILE_START_ADDRESS = 16393;
    private static final int ERROR = -1;

    private static final int VERSION_NUMBER_ADDRESS = 16393;
    private static final int STACK_START_ADDRESS = 16410;
    private static final int STACK_END_ADDRESS = 16412;
    private static final int SCANLINES_ADDRESS = 16424;
    private static final int PROGRAM_END_ADDRESS = 16404;
    private static final int PROGRAM_START_ABSOLUTE_ADDRESS = 16509;
    private static final int EXPECTED_VERSION_NUMBER = 0;
    private static final int EXPECTED_SCANLINES_PAL = 55;
    private static final int EXPECTED_SCANLINES_NTSC = 31;

    private int [] data;
    private int [] filenameData;
    private String filename;
    private boolean atLeastOneError;

    @Override
    public int hashCode() {
        int hash = 0;
        if (data != null)
            hash ^= Arrays.hashCode(data);

        if (filenameData != null)
            hash ^= Arrays.hashCode(filenameData);

        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof ZX81TapeFile))
            return false;

        ZX81TapeFile other = (ZX81TapeFile) o;
        if (!Arrays.equals(data, other.data))
            return false;

        if (!Arrays.equals(filenameData, other.filenameData))
            return false;

        return true;
    }

    @Override
    public int length() {
        if (data == null)
            return 0;
        else
            return data.length;
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public String getFilename() {
        StringBuffer filenameBuilder = new StringBuffer();
        if (filename == null)
            filenameBuilder.append("UNNAMED");
        else
            filenameBuilder.append(filename);

        return filenameBuilder.toString();
    }

    @Override
    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return getBasicProgram();
            case BINARY:
            default:
                return getRawData();
            case EMULATOR:
                return getRawData(); // p file is the same as raw data
        }
    }

    private byte [] getRawData() {
        if (data == null)
            return null;

        byte [] dataBytes = new byte[data.length];
        for (int i = 0; i < data.length; i++)
            dataBytes[i] = (byte)data[i];

        return dataBytes;
    }

    private byte [] getBasicProgram() {
        ZX81PFileExtract program = new ZX81PFileExtract(getRawData());
        return program.toString().getBytes();
    }

    @Override
    public String getExtension() {
        return "";
    }

    @Override
    public boolean containsErrors() {
        return atLeastOneError;
    }

    public void hasAnError() {
        atLeastOneError = true;
    }

    public void setData(int [] data) {
        this.data = data;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public boolean basicValesAreCorrect() {
        // Check:
        // 1. Version number is 0
        // 2. Stack start @16410 >= stack end @16412
        // 3. @16424Number of scanlines above picture is 55 or 31
        // 4. Program end address @16404 > start address 16509
        if (getByteValueAtAddress(VERSION_NUMBER_ADDRESS) != EXPECTED_VERSION_NUMBER)
            return false;

        if (getTwoByteValueAtAddress(STACK_END_ADDRESS) < getTwoByteValueAtAddress(STACK_START_ADDRESS))
            return false;

        if (getByteValueAtAddress(SCANLINES_ADDRESS) != EXPECTED_SCANLINES_NTSC &&
                getByteValueAtAddress(SCANLINES_ADDRESS) != EXPECTED_SCANLINES_PAL)
            return false;

        if (getTwoByteValueAtAddress(PROGRAM_END_ADDRESS) < PROGRAM_START_ABSOLUTE_ADDRESS)
            return false;

        return true;
    }

    private int getByteValueAtAddress(int memoryAddress) {
        memoryAddress -= FILE_START_ADDRESS;
        if (memoryAddress < 0)
            return ERROR;

        if (data.length <= memoryAddress)
            return ERROR;

        return data[memoryAddress];
    }

    private int getTwoByteValueAtAddress(int memoryAddress) {
        int value1 = getByteValueAtAddress(memoryAddress);
        int value2 = getByteValueAtAddress(memoryAddress + 1);
        if (value1 == ERROR || value2 == ERROR)
            return ERROR;
        else
            return value1 + value2 * 256;
    }


    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "zx81.bin";
            case EMULATOR:
                return "zx81.P";
            case READABLE:
                return "zx81.txt";
        }
    }
}
