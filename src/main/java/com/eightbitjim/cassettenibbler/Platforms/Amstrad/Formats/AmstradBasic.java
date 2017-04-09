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

package com.eightbitjim.cassettenibbler.Platforms.Amstrad.Formats;

import com.eightbitjim.cassettenibbler.TapeExtractionLogging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AmstradBasic {
    InputStream inputStream;
    private String programString;
    TapeExtractionLogging logging = TapeExtractionLogging.getInstance();

    public AmstradBasic(byte [] data) {
        inputStream = new ByteArrayInputStream(data);
        constructProgramString();
    }

    public AmstradBasic(InputStream inputStream) {
        this.inputStream = inputStream;
        constructProgramString();
    }

    private void constructProgramString() {
        StringBuilder builder = new StringBuilder();

        try {
            while (inputStream.available() > 0) {
                Line programLine = new Line(inputStream);
                builder.append(programLine.toString());
                builder.append("\n");
            }
        } catch (IOException e) {
            logging.writeProgramOrEnvironmentError(0, "Problem reading data file");
        }

        programString = builder.toString();
    }

    @Override
    public String toString() {
        return programString;
    }
}
