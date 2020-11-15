/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.event {
    /** Fragment Loading metrics **/
    public class HLSLoadMetrics {
        /* Loader Type : refer to HLSLoaderTypes for enumeration */
        public var type : int;
        /* level of loaded content */
        public var level : int;
        /* id of loaded content : should be SN for fragment, startSN for playlist */
        public var id : int;
        /* id2 of loaded content : endSN for playlist, nb tags for tags loaded */
        public var id2 : int;
        /** fragment/playlist size  **/
        public var size : int;
        /** fragment/playlist duration  **/
        public var duration : Number;
        /** loading request/start/end time **/
        public var loading_request_time : int;
        public var loading_begin_time : int;
        public var loading_end_time : int;
        /** decryption begin/end time (for fragment only) **/
        public var decryption_begin_time : int;
        public var decryption_end_time : int;
        /** parsing begin/end time (for fragment only) */
        public var parsing_begin_time : int;
        public var parsing_end_time : int;

        public function HLSLoadMetrics(type : int) {
            this.type = type;
        }

        public function get bandwidth() : int {
            return size * 8000 / (parsing_end_time - loading_request_time);
        }

        public function get processing_duration() : int {
            return parsing_end_time-loading_request_time;
        }
    }
}
