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

package com.eightbitjim.cassettenibbler.CommandLine;

import com.eightbitjim.cassettenibbler.FileStreamConsumer;
import com.eightbitjim.cassettenibbler.TapeFile;
import com.eightbitjim.cassettenibbler.Utilities.PrintableString;

public class CommandLineProgressIndicator implements FileStreamConsumer {
    private int progressPercent;
    private String message;
    private int filesExtracted;
    private boolean indicatorAlreadyDisplayed;
    private boolean canMoveCursorUp;
    private static final int maxWidth = 79;
    private static final String ANSI_CSI_UP_LINE = "\u001b[A";
    private static final String ANSI_CSI_ERASE_LINE = "\u001b[2K";

    public CommandLineProgressIndicator(String title) {
        progressPercent = 0;
        message = "";
        filesExtracted = 0;
        indicatorAlreadyDisplayed = false;
        canMoveCursorUp = !isWindows();
        printTitle(title);
    }

    private void printTitle(String title) {
        System.err.print(title);
        if (title.length() < maxWidth) {
            for (int i = title.length(); i < maxWidth; i++)
                System.err.print("-");
            System.err.println();
        }
    }

    public void setProgressPercent(int percent) {
        progressPercent = percent;
        updateDisplay();
    }

    public void setMessage(String message) {
        this.message = message;
        updateDisplay();
    }

    private void updateDisplay() {
        if (canMoveCursorUp)
            updateDisplayIfCursorControlAvailable();
        else
            updateDisplayIfCursorControlNotAvailable();
    }

    private void updateDisplayIfCursorControlNotAvailable() {
        StringBuilder builder = new StringBuilder();
        builder.append(progressPercent).append("%");

        builder.append(" (").append(filesExtracted).append(" file").append(filesExtracted == 1 ? "" : "s");
        builder.append(" extracted)");

        if (message.length() > 0)
            builder.append(": ").append(message);

        message = "";

        String outputString = builder.toString();
        outputString = PrintableString.convertToPrintable(outputString);

        if (outputString.length() > maxWidth) {
            outputString = outputString.substring(0, maxWidth);
        }

        System.err.println(outputString);
    }

    private void updateDisplayIfCursorControlAvailable() {
        if (indicatorAlreadyDisplayed)
            moveUpALine();

        indicatorAlreadyDisplayed = true;
        int progressWidth = 77;
        int amountFilled = progressPercent * progressWidth / 100;

        StringBuilder progressBar = new StringBuilder();
        progressBar.append('[');

        int i = 0;
        for (; i < amountFilled; i++)
            progressBar.append('#');

        for (; i < progressWidth; i++)
            progressBar.append(' ');

        progressBar.append("]");
        System.err.println(progressBar.toString());

        progressBar = new StringBuilder();
        progressBar.append('(');
        progressBar.append(filesExtracted);
        progressBar.append(") ");

        message = PrintableString.convertToPrintable(message);
        if (message.length() + progressBar.length() > maxWidth)
            progressBar.append(message.substring(0, maxWidth - progressBar.length()));
        else
            progressBar.append(message);

        System.err.println(progressBar.toString());
    }

    private void moveUpALine() {
        System.err.print(ANSI_CSI_UP_LINE + ANSI_CSI_ERASE_LINE);
        System.err.print(ANSI_CSI_UP_LINE + ANSI_CSI_ERASE_LINE);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    @Override
    public void pushFile(TapeFile file, long currentTimeIndex) {
        filesExtracted++;
        setMessage(file.getFilename() + " length " + file.length());
    }
}
