/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.constant {
    /** Identifiers for the different stream types. **/
    public class HLSLoaderTypes {
        // manifest loader
        public static const MANIFEST : int = 0;
    	// playlist / level loader
        public static const LEVEL_MAIN : int = 1;
    	// playlist / level loader
        public static const LEVEL_ALTAUDIO : int = 2;
        // main fragment loader
        public static const FRAGMENT_MAIN : int = 3;
        // alt audio fragment loader
        public static const FRAGMENT_ALTAUDIO : int = 4;
    }
}