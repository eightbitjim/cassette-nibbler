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

package com.eightbitjim.cassettenibbler.Platforms.Oric.Formats;

import java.io.IOException;
import java.io.InputStream;

public class OricAtmosBasicProgram extends OricOneBasicProgram {

    private InputStream inputStream;
    public OricAtmosBasicProgram(InputStream source) {
        super(source);
    }

    public OricAtmosBasicProgram(byte [] data) {
        super(data);
    }

    @Override
    protected String getBasicStringFor(int b) throws IOException {
        if (b == 0x0e) {
            skipFLoatValue();
            return "";
        } else
            return OricATMOSASCII.printableStringForByteCode(b);
    }
}
