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

    int [] data;
    int [] filenameData;
    String filename;

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
        return false;
    }

    public void setData(int [] data) {
        this.data = data;
    }

    public void setFilename(String filename) {
        this.filename = filename;
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
