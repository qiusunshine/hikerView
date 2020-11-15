/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.model {

    import flash.net.ObjectEncoding;
    import flash.utils.ByteArray;
    import org.mangui.hls.demux.ID3Tag;
    import org.mangui.hls.flv.FLVTag;

    /** Fragment model **/
    public class Fragment {
        /** Duration of this chunk. **/
        public var duration : Number;
        /** Start time of this chunk. **/
        public var start_time : Number;
        /** sequence number of this chunk. **/
        public var seqnum : int;
        /** URL to this chunk. **/
        public var url : String;
        /** level of  this chunk. **/
        public var level : int;
        /** continuity index of this chunk. **/
        public var continuity : int;
        /** program date of this chunk. **/
        public var program_date : Number;
        /** URL of the key used to decrypt content **/
        public var decrypt_url : String;
        /** Initialization Vector to decrypt content **/
        public var decrypt_iv : ByteArray;
        /** byte range start offset **/
        public var byterange_start_offset : int;
        /** byte range offset **/
        public var byterange_end_offset : int;
        /** data **/
        public var data : FragmentData;
        /** custom tags **/
        public var tag_list : Vector.<String>;

        /** Create the fragment. **/
        public function Fragment(url : String, duration : Number, level : int, seqnum : int, start_time : Number, continuity : int, program_date : Number, decrypt_url : String, decrypt_iv : ByteArray, byterange_start_offset : int, byterange_end_offset : int, tag_list : Vector.<String>) {
            this.url = url;
            this.duration = duration;
            this.seqnum = seqnum;
            this.level = level;
            this.start_time = start_time;
            this.continuity = continuity;
            this.program_date = program_date;
            this.decrypt_url = decrypt_url;
            this.decrypt_iv = decrypt_iv;
            this.byterange_start_offset = byterange_start_offset;
            this.byterange_end_offset = byterange_end_offset;
            this.tag_list = tag_list;
            data = new FragmentData();
            // CONFIG::LOGGING {
            // Log.info("Frag["+seqnum+"]:duration/start_time,cc="+duration+","+start_time+","+continuity);
            // }
        };

        public function getMetadataTag() : FLVTag {
            var tag : FLVTag = new FLVTag(FLVTag.METADATA, this.data.dts_min, this.data.dts_min, false);
            var data : ByteArray = new ByteArray();
            data.objectEncoding = ObjectEncoding.AMF0;
            data.writeObject("onHLSFragmentChange");
            data.writeObject(this.level);
            data.writeObject(this.seqnum);
            data.writeObject(this.continuity);
            data.writeObject(this.duration);
            data.writeObject(!this.data.video_found);
            data.writeObject(this.program_date);
            data.writeObject(this.data.video_width);
            data.writeObject(this.data.video_height);
            data.writeObject(this.data.auto_level);
            data.writeObject(this.tag_list.length);
            this.data.id3_tags ? data.writeObject(this.data.id3_tags.length) : data.writeObject(0);
            for each (var custom_tag : String in this.tag_list) {
                data.writeObject(custom_tag);
            }
            for each (var id3_tag : ID3Tag in this.data.id3_tags) {
                data.writeObject(id3_tag.id);
                data.writeObject(id3_tag.flag);
                data.writeObject(id3_tag.base64);
                data.writeObject(id3_tag.data);
            }
            tag.push(data, 0, data.length);
            tag.build();
            return tag;
        }

        public function getSkippedTag() : FLVTag {
            var tag : FLVTag = new FLVTag(FLVTag.METADATA, this.data.pts_start_computed, this.data.pts_start_computed, false);
            var data : ByteArray = new ByteArray();
            data.objectEncoding = ObjectEncoding.AMF0;
            data.writeObject("onHLSFragmentSkipped");
            data.writeObject(this.level);
            data.writeObject(this.seqnum);
            data.writeObject(this.duration);
            tag.push(data, 0, data.length);
            tag.build();
            return tag;
        }

        public function toString() : String {
            return "Fragment (seqnum: " + seqnum + ", start_time:" + start_time + ", duration:" + duration + ")";
        }
    }
}
