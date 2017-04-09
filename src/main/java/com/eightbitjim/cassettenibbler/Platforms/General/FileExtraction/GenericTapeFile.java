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

package com.eightbitjim.cassettenibbler.Platforms.General.FileExtraction;

import com.eightbitjim.cassettenibbler.TapeFile;

import java.util.Arrays;

public class GenericTapeFile extends TapeFile {
    public String filename;
    public String type;

    private boolean errors = false;

    public int [] data;
    public int [] attributeData;

    @Override
    public String toString() {
        StringBuffer out = new StringBuffer();
        out.append(getFilename());

        if (data != null)
            out.append(": Data length:" + data.length);
        else
            out.append(": NO DATA");

        return out.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof GenericTapeFile))
            return false;

        GenericTapeFile other = (GenericTapeFile)o;
        if (filename != null && !filename.equals(other.filename))
            return false;

        if (type != null && !type.equals(other.type))
            return false;

        if (data != null && !Arrays.equals(this.data, other.data))
            return false;

        if (attributeData != null && !Arrays.equals(this.attributeData, other.attributeData))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int code = 0;
        if (data != null)
            code ^= data.hashCode();

        if (attributeData != null)
            code ^= attributeData.hashCode();

        if (filename != null)
            code ^= filename.hashCode();

        if (type != null)
            code ^= type.hashCode();

        return code;
    }

    public int length() {
        if (data == null)
            return 0;

        return data.length;
    }

    public String getFilename() {
        StringBuilder filenameBuilder = new StringBuilder();

        if (filename == null || filename.length() == 0)
            filenameBuilder.append("unnamed");
        else
            filenameBuilder.append(filename.trim());

        filenameBuilder.append(additionalInformation != null && additionalInformation.length() > 0 ? "." + additionalInformation : "");
        filenameBuilder.append(type != null ? "." + getExtension().trim() : "");
        return filenameBuilder.toString();
    }

    public byte [] getDataBytesOfType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            case EMULATOR:
            default:
                return getRawData();
            case READABLE:
                return getAsciiData();
        }
    }

    private byte [] getAsciiData() {
        return getPrintableASCII().getBytes();
    }

    protected String getPrintableASCII() {
     byte [] rawData = getRawData();
     if (rawData == null)
         return "";

     StringBuilder builder = new StringBuilder();
     for (byte b : rawData) {
         char c = (char)Byte.toUnsignedInt(b);
         if (isASCIILetterOrSpace(c))
             builder.append(c);
     }

     return builder.toString();
 }

    private boolean isASCIILetterOrSpace(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == ' ') || (c == '.') || (c == '!');
    }

    protected byte [] getRawData() {
        if (data == null)
            return new byte[0];

        byte [] b = new byte[data.length];
        for (int i = 0; i < data.length; i++)
            b[i] = (byte)data[i];

        return b;
    }

    public static boolean isEndOfStream(TapeFile file) {
        return file == null;
    }

    @Override
    public String getExtension() {
        if (type == null)
            return "general";
        else
            return type;
    }

    public void isInError() {
        errors = true;
    }

    public void notInError() {
        errors = false;
    }

    @Override
    public boolean containsErrors() {
        return errors;
    }
}
