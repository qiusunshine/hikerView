/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.model {
    /** Audio Track identifier **/
    public class AudioTrack {
        public static const FROM_DEMUX : int = 0;
        public static const FROM_PLAYLIST : int = 1;
        public var title : String;
        public var id : int;
        public var source : int;
        public var isDefault : Boolean;
        public var isAAC : Boolean;
        public var level : Level;

        public function AudioTrack(title : String, source : int, id : int, isDefault : Boolean, isAAC : Boolean) {
            this.title = title;
            this.source = source;
            this.id = id;
            this.isDefault = isDefault;
            this.isAAC = isAAC;
        }

        public function toString() : String {
            return "AudioTrack ID: " + id + " Title: " + title + " Source: " + source + " Default: " + isDefault + " AAC: " + isAAC;
        }
    }
}