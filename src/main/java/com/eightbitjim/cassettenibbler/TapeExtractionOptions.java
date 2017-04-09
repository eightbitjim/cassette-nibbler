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

import java.io.PrintStream;

public class TapeExtractionOptions {
    public enum LoggingMode {
        FILE_PARSING_PULSES,
        VERBOSE,
        MINIMAL,
        NONE_SHOW_PROGRESS}

    protected boolean allowIncorrectFrameChecksums;
    protected boolean allowIncorrectFileChecksums;
    protected boolean attemptToRecoverCorruptedFiles;

    protected LoggingMode loggingMode;
    protected PrintStream logWriter;

    private static TapeExtractionOptions instance = null;

    protected TapeExtractionOptions() {
        loggingMode = LoggingMode.NONE_SHOW_PROGRESS;
        logWriter = System.err;
    }

    public static TapeExtractionOptions getInstance() {
        if(instance == null) {
            instance = new TapeExtractionOptions();
        }
        return instance;
    }

    public TapeExtractionOptions setLogging(PrintStream outputTo, LoggingMode verbosity) {
        loggingMode = verbosity;
        logWriter = outputTo;
        return this;
    }

    public TapeExtractionOptions setLogging(LoggingMode verbosity) {
        loggingMode = verbosity;
        return this;
    }

    public TapeExtractionOptions setLogging(PrintStream outputTo) {
        logWriter = outputTo;
        return this;
    }

    public TapeExtractionOptions setAllowIncorrectFrameChecksums(boolean isEnabled) {
        allowIncorrectFrameChecksums = isEnabled;
        return this;
    }

    public TapeExtractionOptions setAllowIncorrectFileChecksums(boolean isEnabled) {
        allowIncorrectFileChecksums = isEnabled;
        return this;
    }

    public TapeExtractionOptions setAttemptToRecoverCorruptedFiles(boolean isEnabled) {
        attemptToRecoverCorruptedFiles = isEnabled;
        return this;
    }

    public PrintStream getLogWriter() {return logWriter; }

    public LoggingMode getLogVerbosity() {return loggingMode; }

    public boolean getAllowIncorrectFrameChecksums() { return allowIncorrectFrameChecksums; }

    public boolean getAllowIncorrectFileChecksums() { return allowIncorrectFileChecksums; }

    public boolean getAttemptToRecoverCorruptedFiles() { return attemptToRecoverCorruptedFiles; }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Allow incorrect frame checksums : ").append(onOff(allowIncorrectFileChecksums)).append("\n");
        builder.append("Allow incorrect file checksums  : ").append(onOff(allowIncorrectFileChecksums)).append("\n");
        builder.append("Attempt to recover partial files: ").append(onOff(attemptToRecoverCorruptedFiles));
        return builder.toString();
    }

    private String onOff(boolean on) {
        return on ? "ON" : "OFF";
    }
}
