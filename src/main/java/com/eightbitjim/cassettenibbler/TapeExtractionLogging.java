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

public class TapeExtractionLogging {
    private static TapeExtractionLogging instance = null;
    private transient TapeExtractionOptions options = TapeExtractionOptions.getInstance();

    protected TapeExtractionLogging() {
        // Exists only to defeat instantiation.
    }

    public static TapeExtractionLogging getInstance() {
        if(instance == null) {
            instance = new TapeExtractionLogging();
        }
        return instance;
    }

    public void writeProgramOrEnvironmentError(long nanoseconds, String message) {
        if (options.getLogVerbosity() != TapeExtractionOptions.LoggingMode.NONE_SHOW_PROGRESS)
            writeLine(nanoseconds, message, true);
    }

    public void writeFileParsingInformation(String message) {
        if (options.getLogVerbosity() == TapeExtractionOptions.LoggingMode.FILE_PARSING_PULSES)
            writeLine(0, message, false);
    }

    public void writeFileParsingInformationWithTimestamp(long nanoseconds, String message) {
        if (options.getLogVerbosity() == TapeExtractionOptions.LoggingMode.FILE_PARSING_PULSES)
            writeLine(nanoseconds, message, true);
    }

    public void writeDataError(long nanoseconds, String message) {
        if (options.getLogVerbosity() != TapeExtractionOptions.LoggingMode.NONE_SHOW_PROGRESS)
            writeLine(nanoseconds, message, true);
    }

    private void writeLine(long nanoseconds, String message, boolean withTimeStamp) {
        if (withTimeStamp) {
            writeString("(" + Long.toString(nanoseconds));
            writeString(") ");
        }

        writeString(message);
        writeString("\n");
    }

    private void writeString(String message) {
        options.logWriter.print(message.toUpperCase());
    }

    public void writePulse(char pulseType) {
        if (options.getLogVerbosity() == TapeExtractionOptions.LoggingMode.FILE_PARSING_PULSES)
            options.logWriter.print(pulseType);
    }
}
