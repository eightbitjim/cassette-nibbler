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
    private String progressMessage;
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
        progressMessage = title;
        filesExtracted = 0;
        indicatorAlreadyDisplayed = false;
        canMoveCursorUp = !isWindows();
        printTitle();
    }

    private void printTitle() {
        if (progressMessage.length() <= maxWidth) {
            System.err.print(progressMessage);
            for (int i = progressMessage.length(); i < maxWidth; i++)
                System.err.print("-");
        } else {
            System.err.print(progressMessage.substring(progressMessage.length() - maxWidth));
        }

        System.err.println();
    }

    public void setProgressPercent(int percent, String progressMessage) {
        progressPercent = percent;
        this.progressMessage = progressMessage;
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
        builder.append(progressPercent).append("%: ");
        if (progressMessage.length() > 0)
            builder.append(progressMessage).append(" ");

        builder.append("(").append(filesExtracted).append(" file").append(filesExtracted == 1 ? "" : "s");
        builder.append(" extracted)");

        if (progressMessage.length() > 0)
            builder.append(": ").append(progressMessage);

        message = "";
        String outputString = builder.toString();
        outputString = PrintableString.convertToPrintable(outputString);

        if (outputString.length() > maxWidth) {
            outputString = outputString.substring(0, maxWidth);
        }

        System.err.println(outputString);
    }

    private void updateDisplayIfCursorControlAvailable() {
        if (indicatorAlreadyDisplayed) {
            moveUpALine();
            moveUpALine();
            moveUpALine();
            printTitle();
        }

        indicatorAlreadyDisplayed = true;
        int progressWidth = 77;
        int amountFilled = progressPercent * progressWidth / 100;

        StringBuilder progressBar = new StringBuilder();
        progressBar.append('[');

        int i = 0;
        for (; i < amountFilled; i++)
            progressBar.append('#');

        while (progressBar.length() < progressWidth + 1)
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
