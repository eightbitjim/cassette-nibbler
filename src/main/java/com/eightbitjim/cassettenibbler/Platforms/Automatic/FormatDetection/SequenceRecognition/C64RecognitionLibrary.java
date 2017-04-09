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

import java.util.LinkedList;
import java.util.List;

public class C64RecognitionLibrary implements RecognitionLibrary {
    List<Recogniser> recogniserList;

    public C64RecognitionLibrary() {
        recogniserList = new LinkedList<>();
        recogniserList.add(new c64MachineCodeRecogniser());
        recogniserList.add(new RomanTextRecogniser());
    }

    public Recogniser getClosestMatch(byte [] dataBuffer) {
        Recogniser bestRecogniser = null;
        int bestWeight = 0;
        for (Recogniser recogniser : recogniserList) {
            int weight = recogniser.getMatchWeightAgainst(dataBuffer);
            if (weight > bestWeight) {
                bestWeight = weight;
                bestRecogniser = recogniser;
            }
        }

        return bestRecogniser;
    }
}
