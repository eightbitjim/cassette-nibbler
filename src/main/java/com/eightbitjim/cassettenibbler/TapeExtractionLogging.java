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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.TreeMap;

public class TapeExtractionLogging {
    private TapeExtractionOptions options = TapeExtractionOptions.getInstance();
    private static TreeMap<String, TapeExtractionLogging> instances = new TreeMap<>();
    private LoggingChannel channel;
    private static final String LOG_FILENAME_SUFFIX = ".txt";

    private TapeExtractionLogging(String channelName) {
        channel = new LoggingChannel();
        channel.name = channelName;

        String filename = options.getLogBaseFilename();
        if ((filename == null) || (options.getLogVerbosity() == TapeExtractionOptions.LoggingMode.NONE))
            channel.stream = System.err;
        else
            try {
                channel.stream = new PrintStream(new FileOutputStream(filename + channelName + LOG_FILENAME_SUFFIX));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open log file for output");
                channel.stream = System.err;
            }
    }

    public static TapeExtractionLogging getInstance(String channelName) {
        TapeExtractionLogging instance = instances.get(channelName);
        if(instance == null) {
            instance = new TapeExtractionLogging(channelName);
            instances.put(channelName, instance);
        }

        return instance;
    }

    public void writeProgramOrEnvironmentError(long nanoseconds, String message) {
        if (options.getLogVerbosity() != TapeExtractionOptions.LoggingMode.NONE)
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
        if (options.getLogVerbosity() != TapeExtractionOptions.LoggingMode.NONE)
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
        channel.stream.print(message.toUpperCase());
    }

    public void writePulse(char pulseType) {
        if (options.getLogVerbosity() == TapeExtractionOptions.LoggingMode.FILE_PARSING_PULSES)
            channel.stream.print(pulseType);
    }
}

class LoggingChannel {
    String name;
    PrintStream stream;
}
