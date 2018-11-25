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

package com.eightbitjim.cassettenibbler.Platforms.Other.MPFI.FileExtraction;

import com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction.GenericTapeFile;

public class MPFTapeFile extends GenericTapeFile {
    private static final int HEADER_LENGTH = 7;

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
                // Header and data as text
                return getASCIIDump().getBytes();

            case EMULATOR:
                // Data only as binary
                return getRawDataStartingAt(HEADER_LENGTH);

            default:
            case BINARY:
                // Header and data as binary
                return getRawData();
        }
    }

    private String getASCIIDump() {
        StringBuilder s = new StringBuilder();
        int byteCount = 0;

        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x", data[i]));
            s.append(" ");

            byteCount++;
            if ((byteCount % 16) == 0)
                s.append("\n");
        }

        if ((byteCount % 16) != 0)
            s.append("\n");

        return s.toString();
    }

    @Override
    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "mpfi.headerAndData";
            case EMULATOR:
                return "mpfi.data";
            case READABLE:
                return "mpfi.headerAndData.txt";
        }
    }
}
