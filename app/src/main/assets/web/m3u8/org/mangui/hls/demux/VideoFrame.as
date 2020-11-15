/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.demux {
    /** Video Frame **/
    public class VideoFrame {
        public var header : int;
        public var start : int;
        public var length : int;
        public var type : int;

        public function VideoFrame(header : int, length : int, start : int, type : int) {
            this.header = header;
            this.start = start;
            this.length = length;
            this.type = type;
        }
    }
}