/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.demux {
    /** Video Frame **/
    public class ID3Tag {
        public var id : String;
        public var flag : int;
        public var base64 : Boolean;
        public var data : String;

        public function ID3Tag(id : String, flag : int, base64: Boolean, data : String) {
            this.id = id;
            this.flag = flag;
            this.base64 = base64;
            this.data = data;
        }

        public function toString(): String {
            return  "id/flag/base64/data:" + id + '/' + flag + '/' + base64 + '/' + data;
        }
    }
}
