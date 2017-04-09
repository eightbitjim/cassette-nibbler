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

package com.eightbitjim.cassettenibbler.Platforms.Amstrad.FileExtraction;

import java.util.ArrayList;
import java.util.List;

public abstract class FileSubsection {
    protected List<Byte> data;
    protected static final int CHECKSUM_SIZE = 2;
    private static final int SUBSECTION_LENGTH = 256;

    private static final byte TYPE_BYTE_HEADER = 0x2c;
    private static final byte TYPE_BYTE_DATA = 0x16;

    public static FileSubsection getFileBlockOfType(byte typeByte) {
        switch (typeByte) {
            case TYPE_BYTE_HEADER:
                return new HeaderSubsection();
            case TYPE_BYTE_DATA:
            default:
                return new DataSubsection();
        }
    }

    public FileSubsection() {
        data = new ArrayList<>();
    }

    public void addByte(byte value) {
        if (!moreBytesNeeded())
            return;

        data.add(value);
    }

    public boolean moreBytesNeeded() {
        return data.size() < SUBSECTION_LENGTH + CHECKSUM_SIZE;
    }

    public boolean checksumIsValid() {
        return true; // TODO
    }

    public byte [] getData() {
        byte [] arrayToReturn = new byte[data.size()];
        int position = 0;
        for (Byte b : data)
            arrayToReturn[position++] = b;

        return arrayToReturn;
    }

    public List <Byte> getDataAsList() {
        return data;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (Byte b : data)
            hash ^= b.hashCode();

        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof FileSubsection))
            return false;

        FileSubsection other = (FileSubsection) o;
        if ((data != null) && (!data.equals(other.data)))
            return false;

        return true;
    }
}
