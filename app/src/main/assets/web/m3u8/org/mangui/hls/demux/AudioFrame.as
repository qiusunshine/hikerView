/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {
    /** Audio Frame **/
    public class AudioFrame {
        public var start : int;
        public var length : int;
        public var expected_length : int;
        public var rate : int;

        public function AudioFrame(start : int, length : int, expected_length : int, rate : int) {
            this.start = start;
            this.length = length;
            this.expected_length = expected_length;
            this.rate = rate;
        }
    }
}