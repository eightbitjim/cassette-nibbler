/*
 * Copyright (c) 2018. James Lean
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

package com.eightbitjim.cassettenibbler.Platforms.Other.MPF1.FileExtraction;

import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;

public class MPFTapeFile extends GenericTapeFile {

    public MPFTapeFile() {
        type = "unknown";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof MPFTapeFile))
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
            case EMULATOR:
            case BINARY:
            default:
                return getRawData();
        }
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "mpf1.bin";
            case EMULATOR:
                return "mpf1.TAP";
            case READABLE:
                return "mpf1.txt";
        }
    }
}
