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

import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;
import com.eightbitjim.cassettenibbler.Platforms.Sinclair.Formats.SpectrumBasicProgram;

public class SpectrumTapeFile extends GenericTapeFile {

    private byte [] headerData;

    public SpectrumTapeFile () {
        type = "unknown";
    }

    public void setHeaderData(int [] data) {
        headerData = new byte[data.length];
        for (int i = 0; i < data.length; i++)
            headerData[i] = (byte)data[i];
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof SpectrumTapeFile))
            return false;

        return super.equals(o);
    }

    @Override
    public String toString() {
        return getFilename();
    }

    @Override
    public String getFilename() {
        StringBuilder filenameBuilder = new StringBuilder();
        if ((filename == null) || (filename.length() == 0))
            filenameBuilder.append("unnamed");
        else
            filenameBuilder.append(filename);

        filenameBuilder.append(".").append(type);

        return filenameBuilder.toString();
    }

    @Override
    public String getExtension() {
        return "";
    }

    @Override
    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case READABLE:
                return getBasicProgram();
            case EMULATOR:
                return getTAPFileData();
            case BINARY:
            default:
                return getRawData();
        }
    }

    private byte [] getBasicProgram() {
        // TODO check for ASCII data files and output those instead of trying to interpret them as data
        SpectrumBasicProgram program = new SpectrumBasicProgram(getRawData());
        return program.toString().getBytes();
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "zxspectrum.bin";
            case EMULATOR:
                return "zxspectrum.TAP";
            case READABLE:
                return "zxspectrum.txt";
        }
    }

    private byte [] getTAPFileData() {
        EmulatorTAPFile tapFile = new EmulatorTAPFile();
        tapFile.addBlock(headerData);
        tapFile.addBlockAndChecksum((byte)0xff, getRawData());
        return tapFile.getData();
    }
}
