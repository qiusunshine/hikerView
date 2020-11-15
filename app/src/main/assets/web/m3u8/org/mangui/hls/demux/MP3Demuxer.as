/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {
    import flash.utils.ByteArray;

    import org.mangui.hls.model.AudioTrack;
    import org.mangui.hls.flv.FLVTag;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    public class MP3Demuxer implements Demuxer {
        /* MPEG1-Layer3 syncword */
        private static const SYNCWORD : uint = 0xFFFB;
        private static const RATES : Array = [44100, 48000, 32000];
        private static const BIT_RATES : Array = [0, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 224000, 256000, 320000, 0];
        private static const SAMPLES_PER_FRAME : uint = 1152;
        /** Byte data to be read **/
        private var _data : ByteArray;
        /* callback functions for audio selection, and parsing progress/complete */
        private var _callback_audioselect : Function;
        private var _callback_progress : Function;
        private var _callback_complete : Function;
        private var _callback_error : Function;
        private var _callback_id3tag : Function;

        /** append new data */
        public function append(data : ByteArray) : void {
            if (_data == null) {
                _data = new ByteArray();
            }
            _data.writeBytes(data);
        }

        /** cancel demux operation */
        public function cancel() : void {
            _data = null;
        }

        public function get audioExpected() : Boolean {
            return true;
        }

        public function get videoExpected() : Boolean {
            return false;
        }

        public function notifycomplete() : void {
            CONFIG::LOGGING {
                Log.debug("MP3: extracting MP3 tags");
            }
            var audioTags : Vector.<FLVTag> = new Vector.<FLVTag>();
            /* parse MP3, convert Elementary Streams to TAG */
            _data.position = 0;
            var id3 : ID3 = new ID3(_data);
            // MP3 should contain ID3 tag filled with a timestamp
            var frames : Vector.<AudioFrame> = getFrames(_data, _data.position);
            var audioTag : FLVTag;
            var stamp : int;
            var i : int = 0;

            while (i < frames.length) {
                stamp = Math.round(id3.timestamp + i * 1024 * 1000 / frames[i].rate);
                audioTag = new FLVTag(FLVTag.MP3_RAW, stamp, stamp, false);
                if (i != frames.length - 1) {
                    audioTag.push(_data, frames[i].start, frames[i].length);
                } else {
                    audioTag.push(_data, frames[i].start, _data.length - frames[i].start);
                }
                audioTag.build();
                audioTags.push(audioTag);
                i++;
            }
            var audiotracks : Vector.<AudioTrack> = new Vector.<AudioTrack>();
            audiotracks.push(new AudioTrack('MP3 ES', AudioTrack.FROM_DEMUX, 0, true,false));
            // report unique audio track. dont check return value as obviously the track will be selected
            _callback_audioselect(audiotracks);
            CONFIG::LOGGING {
                Log.debug("MP3: all tags extracted, callback demux");
            }
            _data = null;
            if(id3.tags.length) {
                _callback_id3tag(id3.tags);
            }
            _callback_progress(audioTags);
            _callback_complete();
        }

        public function MP3Demuxer(callback_audioselect : Function,
                                   callback_progress : Function,
                                   callback_complete : Function,
                                   callback_error : Function,
                                   callback_id3tag : Function) : void {
            _callback_audioselect = callback_audioselect;
            _callback_progress = callback_progress;
            _callback_complete = callback_complete;
            _callback_error = callback_error;
            _callback_id3tag = callback_id3tag;
        };

        public static function probe(data : ByteArray) : Boolean {
            var pos : uint = data.position;
            var id3 : ID3 = new ID3(data);
            // MP3 should contain ID3 tag filled with a timestamp
            if (id3.hasTimestamp) {
                var afterID3 : uint = data.position;
                while (data.bytesAvailable > 1 && (data.position - afterID3) < 100) {
                    // Check for MP3 header
                    var short : uint = data.readUnsignedShort();
                    if (short == SYNCWORD) {
                        CONFIG::LOGGING {
                            Log.debug2("MP3: found header " + short + "@ " + (data.position-2));
                        }
                        data.position = pos;
                        return true;
                    } else {
                        data.position--;
                    }
                }
                data.position = pos;
            }
            return false;
        }

        private static function getFrames(data : ByteArray, position : uint) : Vector.<AudioFrame> {
            var frames : Vector.<AudioFrame> = new Vector.<AudioFrame>();
            var frame_start : uint;
            var frame_length : uint;
            var id3 : ID3 = new ID3(data);
            position += id3.len;
            // Get raw MP3 frames from audio stream.
            data.position = position;
            // we need at least 3 bytes, 2 for sync word, 1 for flags
            while (data.bytesAvailable > 3) {
                frame_start = data.position;
                // frame header described here : http://mpgedit.org/mpgedit/mpeg_format/MP3Format.html
                var short : uint = data.readUnsignedShort();
                if (short == SYNCWORD) {
                    var flag : uint = data.readByte();
                    // (15,12)=(&0xf0 >>4)  Bitrate index
                    var bitrate : uint = BIT_RATES[(flag & 0xf0) >> 4];
                    // (11,10)=(&0xc >> 2) Sampling rate frequency index (values are in Hz)
                    var samplerate : uint = RATES[(flag & 0xc) >> 2];
                    // (9)=(&2 >>1)     Padding bit
                    var padbit : uint = (flag & 2) >> 1;
                    frame_length = (SAMPLES_PER_FRAME / 8) * bitrate / samplerate + padbit;
                    frame_length = Math.round(frame_length);
                    data.position = data.position + (frame_length - 3);
                    frames.push(new AudioFrame(frame_start, frame_length, frame_length, samplerate));
                } else {
                    data.position = data.position - 1;
                }
            }
            data.position = position;
            return frames;
        }
    }
}
