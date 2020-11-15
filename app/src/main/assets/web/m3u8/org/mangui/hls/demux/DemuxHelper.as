/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {
    import flash.utils.ByteArray;
    import org.mangui.hls.model.Level;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    public class DemuxHelper {
        public static function probe(data : ByteArray,
                                     level : Level,
                                     audioselect : Function,
                                     progress : Function,
                                     complete : Function,
                                     error : Function,
                                     videometadata : Function,
                                     id3tagfound : Function,
                                     audioOnly : Boolean) : Demuxer {
            data.position = 0;
            CONFIG::LOGGING {
                Log.debug("probe fragment type");
            }
            var aac_match : Boolean = AACDemuxer.probe(data);
            var mp3_match : Boolean = MP3Demuxer.probe(data);
            var ts_match : Boolean = TSDemuxer.probe(data);
            CONFIG::LOGGING {
                Log.debug("AAC/MP3/TS match:" + aac_match + "/" + mp3_match + "/" + ts_match);
            }
            /* prioritize level info :
             * if ts_match && codec_avc  => TS demuxer
             * if aac_match && codec_aac => AAC demuxer
             * if mp3_match && codec_mp3 => MP3 demuxer
             * if no codec info in Manifest, use fallback order : AAC/MP3/TS
             */
            if (ts_match && level.codec_h264) {
                CONFIG::LOGGING {
                    Log.debug("TS match + H264 signaled in Manifest, use TS demuxer");
                }
                return new TSDemuxer(audioselect, progress, complete, error, videometadata, audioOnly);
            } else if (aac_match && level.codec_aac) {
                CONFIG::LOGGING {
                    Log.debug("AAC match + AAC signaled in Manifest, use AAC demuxer");
                }
                return new AACDemuxer(audioselect, progress, complete, error, id3tagfound);
            } else if (mp3_match && level.codec_mp3) {
                CONFIG::LOGGING {
                    Log.debug("MP3 match + MP3 signaled in Manifest, use MP3 demuxer");
                }
                return new MP3Demuxer(audioselect, progress, complete, error, id3tagfound);
            } else if (aac_match) {
                return new AACDemuxer(audioselect, progress, complete, error, id3tagfound);
            } else if (mp3_match) {
                return new MP3Demuxer(audioselect, progress, complete, error, id3tagfound);
            } else if (ts_match) {
                return new TSDemuxer(audioselect, progress, complete, error, videometadata, audioOnly);
            } else {
                CONFIG::LOGGING {
                    Log.debug("probe fails");
                }
                return null;
            }
        }
    }
}
