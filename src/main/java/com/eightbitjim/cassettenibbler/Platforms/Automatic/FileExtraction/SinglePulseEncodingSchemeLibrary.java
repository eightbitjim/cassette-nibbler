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

package com.eightbitjim.cassettenibbler.Platforms.Automatic.FileExtraction;

import com.eightbitjim.cassettenibbler.PulseStreamConsumer;

import java.util.LinkedList;
import java.util.List;

public class SinglePulseEncodingSchemeLibrary {
    List<EncodingScheme> schemes;

    private static final char SHORT = PulseStreamConsumer.SHORT_PULSE;
    private static final char MEDIUM = PulseStreamConsumer.MEDIUM_PULSE;
    private static final char LONG = PulseStreamConsumer.LONG_PULSE;

    public SinglePulseEncodingSchemeLibrary() {
        schemes = new LinkedList<>();
        addEncodingSchemesToList();
    }

    public List <EncodingScheme> getSchemes() {
        return schemes;
    }

    private void addEncodingSchemesToList() {
        int padding = 0;
        schemes.add(new EncodingScheme(new char[]{MEDIUM}, new char[]{LONG}, padding, true));
        schemes.add(new EncodingScheme(new char[]{LONG}, new char[]{MEDIUM}, padding, true));
        schemes.add(new EncodingScheme(new char[]{SHORT}, new char[]{MEDIUM}, padding, true));
        schemes.add(new EncodingScheme(new char[]{MEDIUM}, new char[]{SHORT}, padding, true));
        schemes.add(new EncodingScheme(new char[]{MEDIUM}, new char[]{LONG}, padding, false));
        schemes.add(new EncodingScheme(new char[]{LONG}, new char[]{MEDIUM}, padding, false));
        schemes.add(new EncodingScheme(new char[]{SHORT}, new char[]{MEDIUM}, padding, false));
        schemes.add(new EncodingScheme(new char[]{MEDIUM}, new char[]{SHORT}, padding, false));
    }
}
