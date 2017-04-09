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

public class c64MachineCodeRecogniser extends Recogniser {
    public c64MachineCodeRecogniser() {
        name = "c64machineCode";
        itemsToRecognise = new Item [] {
            new HeavyItem(141, 32, 208),
                new HeavyItem(141, 33, 208),
                new HeavyItem(140, 32, 208),
                new HeavyItem(140, 33, 208),
                new HeavyItem(142, 32, 208),
                new HeavyItem(142, 33, 208)
        };

        addAllItemsFrom(new m6502machineCodeRecogniser());
    }
}
