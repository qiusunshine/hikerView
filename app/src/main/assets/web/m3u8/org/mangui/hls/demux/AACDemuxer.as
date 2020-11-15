/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.demux {
    import org.mangui.hls.model.AudioTrack;
    import org.mangui.hls.flv.FLVTag;

    import flash.utils.ByteArray;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    /** Constants and utilities for the AAC audio format, refer to
     *  http://wiki.multimedia.cx/index.php?title=ADTS
     **/
    public class AACDemuxer implements Demuxer {
        /** ADTS Syncword (0xFFF), ID:0 (MPEG4), layer (00) and protection_absent (1:no CRC).**/
        private static const SYNCWORD : uint = 0xFFF1;
        /** ADTS Syncword (0xFFF), ID:1 (MPEG2), layer (00) and protection_absent (1: no CRC).**/
        private static const SYNCWORD_2 : uint = 0xFFF9;
        /** ADTS Syncword (0xFFF), ID:1 (MPEG2), layer (00) and protection_absent (0: CRC).**/
        private static const SYNCWORD_3 : uint = 0xFFF8;
        /** ADTS/ADIF sample rates index. **/
        private static const RATES : Array = [96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350];
        /** ADIF profile index (ADTS doesn't have Null). **/
        private static const PROFILES : Array = ['Null', 'Main', 'LC', 'SSR', 'LTP', 'SBR'];
        /** ADTS not found error **/
        private static const ADTS_NOT_FOUND : String = "AAC:stream did not start with ADTS header";
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
                Log.debug("AAC: extracting AAC tags");
            }
            var audioTags : Vector.<FLVTag> = new Vector.<FLVTag>();
            /* parse AAC, convert Elementary Streams to TAG */
            _data.position = 0;
            var id3 : ID3 = new ID3(_data);
            // AAC should contain ID3 tag filled with a timestamp
            var frames : Vector.<AudioFrame> = AACDemuxer.getFrames(_data, _data.position);
            var adif : ByteArray = getADIF(_data, id3.len);
            if(adif == null && _callback_error != null) {
                _callback_error(ADTS_NOT_FOUND);
                return;
            }
            var adifTag : FLVTag = new FLVTag(FLVTag.AAC_HEADER, id3.timestamp, id3.timestamp, true);
            adifTag.push(adif, 0, adif.length);
            adifTag.build();
            audioTags.push(adifTag);

            var audioTag : FLVTag;
            var stamp : uint;
            var i : int = 0;

            while (i < frames.length) {
                stamp = Math.round(id3.timestamp + i * 1024 * 1000 / frames[i].rate);
                audioTag = new FLVTag(FLVTag.AAC_RAW, stamp, stamp, false);
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
            audiotracks.push(new AudioTrack('AAC ES', AudioTrack.FROM_DEMUX, 0, true, true));
            // report unique audio track. dont check return value as obviously the track will be selected
            _callback_audioselect(audiotracks);
            CONFIG::LOGGING {
                Log.debug("AAC: all tags extracted, callback demux");
            }
            _data = null;
            if(id3.tags.length) {
                _callback_id3tag(id3.tags);
            }
            _callback_progress(audioTags);
            _callback_complete();
        }

        public function AACDemuxer(callback_audioselect : Function,
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
            // AAC should contain ID3 tag filled with a timestamp
            if (id3.hasTimestamp) {
                var afterID3 : uint = data.position;
                while (data.bytesAvailable > 1 && (data.position - afterID3) < 100) {
                    // Check for ADTS header
                    var short : uint = data.readUnsignedShort();
                    if (short == SYNCWORD || short == SYNCWORD_2 || short == SYNCWORD_3) {
                        CONFIG::LOGGING {
                            Log.debug2("AAC: found header " + short + "@ " + (data.position-2));
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

        /** Get ADIF header from ADTS stream. **/
        public static function getADIF(adts : ByteArray, position : uint) : ByteArray {
            adts.position = position;
            var short : uint;
            // we need at least 6 bytes, 2 for sync word, 4 for frame length
            while ((adts.bytesAvailable > 5) && (short != SYNCWORD) && (short != SYNCWORD_2) && (short != SYNCWORD_3)) {
                short = adts.readUnsignedShort();
                adts.position--;
            }
            adts.position++;
            if (short == SYNCWORD || short == SYNCWORD_2 || short == SYNCWORD_3) {
                var profile : uint = (adts.readByte() & 0xF0) >> 6;
                // Correcting zero-index of ADIF and Flash playing only LC/HE.
                if (profile > 3) {
                    profile = 5;
                } else {
                    profile = 2;
                }
                adts.position--;
                var srate : uint = (adts.readByte() & 0x3C) >> 2;
                adts.position--;
                var channels : uint = (adts.readShort() & 0x01C0) >> 6;
                // 5 bits profile + 4 bits samplerate + 4 bits channels.
                var adif : ByteArray = new ByteArray();
                adif.writeByte((profile << 3) + (srate >> 1));
                adif.writeByte((srate << 7) + (channels << 3));
                CONFIG::LOGGING {
                    Log.debug('AAC: ' + PROFILES[profile] + ', ' + RATES[srate] + ' Hz ' + channels + ' channel(s)');
                }
                // Reset position and return adif.
                adts.position -= 4;
                adif.position = 0;
                return adif;
            } else {
                CONFIG::LOGGING {
                    Log.error(ADTS_NOT_FOUND);
                }
                return null;
            }
        };

        /** Get a list with AAC frames from ADTS stream. **/
        public static function getFrames(adts : ByteArray, position : uint) : Vector.<AudioFrame> {
            var frames : Vector.<AudioFrame> = new Vector.<AudioFrame>();
            var frame_start : uint;
            var frame_length : uint;
            var id3 : ID3 = new ID3(adts);
            position += id3.len;
            // Get raw AAC frames from audio stream.
            adts.position = position;
            var samplerate : uint;
            // we need at least 6 bytes, 2 for sync word, 4 for frame length
            while (adts.bytesAvailable > 5) {
                // Check for ADTS header
                var short : uint = adts.readUnsignedShort();
                if (short == SYNCWORD || short == SYNCWORD_2 || short == SYNCWORD_3) {
                    // Store samplerate for offsetting timestamps.
                    if (!samplerate) {
                        samplerate = RATES[(adts.readByte() & 0x3C) >> 2];
                        adts.position--;
                    }
                    // Store raw AAC preceding this header.
                    if (frame_start) {
                        frames.push(new AudioFrame(frame_start, frame_length, frame_length, samplerate));
                    }
                    // protection_absent=1, crc_len = 0,protection_absent=0,crc_len=2
                    var crc_len : int = (1 - (short & 0x1)) << 1;
                    // ADTS header is 7+crc_len bytes.
                    frame_length = ((adts.readUnsignedInt() & 0x0003FFE0) >> 5) - 7 - crc_len;
                    frame_start = adts.position + 1 + crc_len;
                    adts.position += frame_length + 1 + crc_len;
                } else {
                    CONFIG::LOGGING {
                        Log.debug2("no ADTS header found, probing...");
                    }
                    adts.position--;
                }
            }
            if (frame_start) {
                // check if we have a complete frame available at the end, i.e. last found frame is fitting in this PES packet
                var overflow : int = frame_start + frame_length - adts.length;
                if (overflow <= 0 ) {
                    // no overflow, Write raw AAC after last header.
                    frames.push(new AudioFrame(frame_start, frame_length, frame_length, samplerate));
                }
                CONFIG::LOGGING {
                    if (overflow > 0) {
                        Log.debug2("ADTS overflow at the end of PES packet, missing " + overflow + " bytes to complete the ADTS frame");
                    }
                }
            }
            CONFIG::LOGGING {
                if (!frame_start && frames.length == 0) {
                    Log.warn("No ADTS headers found in this stream.");
                }
            }
            adts.position = position;
            return frames;
        };
    }
}
