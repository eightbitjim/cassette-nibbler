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

public class zxSpectrumMachineCodeRecogniser extends Recogniser {
    public zxSpectrumMachineCodeRecogniser() {
        name = "zxSpectrumMachineCode";
        itemsToRecognise = new Item [] {
            new HeavyItem(0x1, getLowByte(32766), getHighByte(32766)),
                new HeavyItem(0x1, getLowByte(49150), getHighByte(49150)),
                new HeavyItem(0x1, getLowByte(57342), getHighByte(57342)),
                new HeavyItem(0x1, getLowByte(61438), getHighByte(61438)),
                new HeavyItem(0x1, getLowByte(63486), getHighByte(63486)),
                new HeavyItem(0x1, getLowByte(64510), getHighByte(64510)),
                new HeavyItem(0x1, getLowByte(65022), getHighByte(65022)),
                new HeavyItem(0x1, getLowByte(65278), getHighByte(65278)),
        };

        addAllItemsFrom(new m6502machineCodeRecogniser());
    }

    private int getLowByte(int address) {
        return address % 256;
    }

    private int getHighByte(int address) {
        return address / 256;
    }
}
