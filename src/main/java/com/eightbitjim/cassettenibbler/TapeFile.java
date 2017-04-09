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

package com.eightbitjim.cassettenibbler;

public abstract class TapeFile {
    public enum FormatType {
        BINARY,
        READABLE,
        EMULATOR }

    protected String additionalInformation = "";

    public abstract int length();

    public abstract String getFilename();

    public abstract byte [] getDataBytesOfType(FormatType formatType);

    public String getFileExtensionForType(FormatType formatType) {
        switch (formatType) {
            case BINARY:
            default:
                return "bin";
            case EMULATOR:
                return "emulator";
            case READABLE:
                return "txt";
        }
    }

    public static boolean isEndOfStream(TapeFile file) {
        return file == null;
    }

    public abstract String getExtension();

    public abstract boolean containsErrors();

    public void setAdditionalInformation(String information) {
        if (this.additionalInformation.length() > 0)
            this.additionalInformation += ".";

        this.additionalInformation += information;
    }
}
