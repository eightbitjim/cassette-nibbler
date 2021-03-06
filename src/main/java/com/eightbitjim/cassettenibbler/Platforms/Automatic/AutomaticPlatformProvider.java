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

package com.eightbitjim.cassettenibbler.Platforms.Automatic;

import com.eightbitjim.cassettenibbler.PlatformProvider;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.Platforms.C64TurboLoadExtractionPlatform;
import com.eightbitjim.cassettenibbler.Platforms.Automatic.Platforms.AutomaticExtractionPlatform;

public class AutomaticPlatformProvider extends PlatformProvider {
    public AutomaticPlatformProvider() {
        add(new AutomaticExtractionPlatform());
        add(new C64TurboLoadExtractionPlatform());
    }
}
