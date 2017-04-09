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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.FormatDetection.SequenceRecognition;

public class RomanTextRecogniser extends Recogniser {

    public RomanTextRecogniser() {
        name = "textRecogniser";
    }

    public int getMatchWeightAgainst(byte [] dataBuffer) {
        int amountOfText = getNumberOfTextAndSpaceCharactersInARowIn(dataBuffer);
        return amountOfText;
    }

    private int getNumberOfTextAndSpaceCharactersInARowIn(byte [] dataBuffer) {
        int numberOfMatches = 0;
        boolean inValidSequence = false;
        boolean spaceSeen = false;
        boolean letterAfterSpace = false;
        int lengthOfCurrentSequence = 0;

        for (int i = 0; i < dataBuffer.length; i++) {
            char currentCharacter = (char)Byte.toUnsignedInt(dataBuffer[i]);
            if (currentCharacter == ' ' && inValidSequence) {
                spaceSeen = true;
                lengthOfCurrentSequence++;
                continue;
            }

            if (isASCIILetter(currentCharacter)) {
                inValidSequence = true;
                lengthOfCurrentSequence++;
                if (spaceSeen)
                    letterAfterSpace = true;

                continue;
            }

            if (inValidSequence) {
                if (spaceSeen && letterAfterSpace)
                    numberOfMatches += lengthOfCurrentSequence;

                inValidSequence = false;
                letterAfterSpace = false;
                lengthOfCurrentSequence = 0;
                spaceSeen = false;
            }
        }

        return numberOfMatches;
    }

    private boolean isASCIILetter(char value) {
        if ((value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z'))
            return true;
        else
            return false;
    }
}
