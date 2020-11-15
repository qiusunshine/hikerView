/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.event {
    /** Identifiers for the different stream types. **/
    public class HLSMediatime {
        /**  playback position (in seconds), relative to current playlist start.
         * this value could be negative in case of live playlist sliding :
         *  this can happen in case current playback position
         * is in a fragment that has been removed from the playlist
         */
        public var position : Number;
        /** current playlist duration (in seconds) **/
        public var duration : Number;
        /**   live main playlist sliding since previous out of buffer seek()  (in seconds)**/
        public var live_sliding_main : Number;
        /**  live altaudio playlist sliding since previous out of buffer seek()  (in seconds)**/
        public var live_sliding_altaudio : Number;
        /** current buffer duration  (in seconds) **/
        public var buffer : Number;
        /** current buffer duration  (in seconds) **/
        public var backbuffer : Number;
        /** total watched duration  (in seconds) since hls.load(URL) **/
        public var watched : Number;

        public function HLSMediatime(position : Number, duration : Number, buffer : Number, backbuffer : Number, live_sliding_main : Number, live_sliding_altaudio : Number, watched : Number) {
            this.position = position;
            this.duration = duration;
            this.buffer = buffer;
            this.backbuffer = backbuffer;
            this.live_sliding_main = live_sliding_main;
            this.live_sliding_altaudio = live_sliding_altaudio;
            this.watched = watched;
        }
    }
}
