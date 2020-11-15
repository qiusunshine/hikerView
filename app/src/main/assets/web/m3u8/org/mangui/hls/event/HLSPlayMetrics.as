/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.event {
    /** playback metrics, notified when playback of a given fragment starts **/
    public class HLSPlayMetrics {
        public var level : int;
        public var seqnum : int;
        public var continuity_counter : int;
        public var duration : Number;
        public var audio_only : Boolean;
        public var program_date : Number;
        public var video_width : int;
        public var video_height : int;
        public var auto_level : Boolean;
        public var tag_list : Array;
        public var id3tag_list : Array;

        public function HLSPlayMetrics(level : int, seqnum : int, cc : int, duration : Number, audio_only : Boolean, program_date : Number, video_width : int, video_height : int, auto_level : Boolean, tag_list : Array, id3tag_list : Array) {
            this.level = level;
            this.seqnum = seqnum;
            this.continuity_counter = cc;
            this.duration = duration;
            this.audio_only = audio_only;
            this.program_date = program_date;
            this.video_width = video_width;
            this.video_height = video_height;
            this.auto_level = auto_level;
            this.tag_list = tag_list;
            this.id3tag_list = id3tag_list;
        }
    }
}
