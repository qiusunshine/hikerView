/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.model {
    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    import org.mangui.hls.utils.PTS;

    /** HLS streaming quality level. **/
    public class Level {
        /** audio only Level ? **/
        public var audio : Boolean;
        /** AAC codec signaled ? **/
        public var codec_aac : Boolean;
        /** MP3 codec signaled ? **/
        public var codec_mp3 : Boolean;
        /** H264 codec signaled ? **/
        public var codec_h264 : Boolean;
        /** Level Bitrate. **/
        public var bitrate : uint;
        /** captions . **/
        public var closed_captions : String;
        /** Level Name. **/
        public var name : String;
        /** level index (sorted by bitrate) **/
        public var index : int = 0;
        /** level index (manifest order) **/
        public var manifest_index : int = 0;
        /** video width (from playlist) **/
        public var width : int;
        /** video height (from playlist) **/
        public var height : int;
        /** URL of this bitrate level (for M3U8). (it is a vector so that we can store redundant streams in same level) **/
        public var urls : Vector.<String>;
        // index of used url (non 0 if we switch to a redundant stream)
        private var _redundantStreamId : int = 0;
        /** Level fragments **/
        public var fragments : Vector.<Fragment>;
        /** min sequence number from M3U8. **/
        public var start_seqnum : int;
        /** max sequence number from M3U8. **/
        public var end_seqnum : int;
        /** target fragment duration from M3U8 **/
        public var targetduration : Number;
        /** average fragment duration **/
        public var averageduration : Number;
        /** Total duration **/
        public var duration : Number;
        /**  Audio Identifier **/
        public var audio_stream_id : String;

        /** Create the quality level. **/
        public function Level() : void {
            this.fragments = new Vector.<Fragment>();
        };

        public function get url() : String {
            return urls[_redundantStreamId];
        }

        public function get redundantStreamsNb() : int {
            if(urls && urls.length) {
                return urls.length-1;
            } else {
                return 0;
            }
        }

        public function get redundantStreamId() : int {
            return _redundantStreamId;
        }

        // when switching to a redundant stream, reset fragments. they will be retrieved from new playlist
        public function set redundantStreamId(id : int) : void {
            if(id < urls.length && id != _redundantStreamId) {
                _redundantStreamId = id;
                fragments = new Vector.<Fragment>();
                start_seqnum = end_seqnum = NaN;
            }
        }

        /** Return the Fragment before a given time position. **/
        public function getFragmentBeforePosition(position : Number) : Fragment {
            if (fragments[0].data.valid && position < fragments[0].start_time)
                return fragments[0];

            var len : int = fragments.length;
            for (var i : int = 0; i < len; i++) {
                /* check whether fragment contains current position */
                if (fragments[i].data.valid && fragments[i].start_time <= position && fragments[i].start_time + fragments[i].duration > position) {
                    return fragments[i];
                }
            }
            return fragments[len - 1];
        };

        /** Return the sequence number nearest a given program date **/
        public function getSeqNumNearestProgramDate(program_date : Number) : int {
            if (program_date < fragments[0].program_date)
                return -1;

            var len : int = fragments.length;
            if(len) {
                if (program_date > (fragments[len-1].program_date + 1000*fragments[len-1].duration))
                    return -1;

                for (var i : int = 0; i < len; i++) {
                    var frag : Fragment = fragments[i];
                    /* check whether fragment contains current position */
                    if (frag.data.valid &&
                        Math.abs(frag.program_date - program_date) < Math.abs(frag.program_date + 1000 * frag.duration - program_date)) {
                        return frag.seqnum;
                    }
                }
            }
            return -1;
        };

        /** Return the sequence number nearest a PTS **/
        public function getSeqNumNearestPTS(pts : Number, continuity : int) : Number {
            if (fragments.length == 0)
                return -1;
            var firstIndex : Number = getFirstIndexfromContinuity(continuity);
            if (firstIndex == -1 || isNaN(fragments[firstIndex].data.pts_start_computed))
                return -1;
            var lastIndex : Number = getLastIndexfromContinuity(continuity);

            for (var i : int = firstIndex; i <= lastIndex; i++) {
                var frag : Fragment = fragments[i];
                var start : Number = frag.data.pts_start_computed;
                var duration :Number = frag.duration;
                var end : Number = start + 1000*duration;

                // CONFIG::LOGGING {
                //     Log.debug("getSeqNumNearestPTS: pts/start/end/duration:" + pts + '/' + start + '/' + end + '/' + duration);
                // }
                /* check nearest fragment */
                if ( frag.data.valid &&
                    (duration >= 0) &&
                    // if PTS is closer from start
                    ((Math.abs(start - pts) < Math.abs(end - pts))
                    //  start PTS                     end
                    //    *----|-----------------------*
                    //
                    //  PTS start                 end
                    //   |--*-----------------------*
                    //
                    //
                    // OR if PTS is bigger than start PTS AND more than 10% before frag end
                    //
                    //  start                   PTS  end
                    //    *----------------------|-----*
                    //                             <10%>
                    || ((pts > start) &&
                        (end - pts ) > 100*duration))) {
                    return frag.seqnum;
                }
            }
            // if we are not at the end of the playlist, then return first sn of next cc range
            // this is needed to deal with PTS analysis on streams with discontinuity
            if (lastIndex < end_seqnum) {
                return frag.seqnum+1;
            } else {
                // requested PTS above max PTS of this level
                return Number.POSITIVE_INFINITY;
            }
        };

        public function getLevelstartPTS() : Number {
            if (fragments.length)
                return fragments[0].data.pts_start_computed;
            else
                return NaN;
        }

        /** Return the fragment index from fragment sequence number **/
        public function getFragmentfromSeqNum(seqnum : Number) : Fragment {
            var index : int = getIndexfromSeqNum(seqnum);
            if (index != -1) {
                return fragments[index];
            } else {
                return null;
            }
        }

        /** Return the fragment index from fragment sequence number **/
        private function getIndexfromSeqNum(seqnum : int) : int {
            if (seqnum >= start_seqnum && seqnum <= end_seqnum) {
                return (fragments.length - 1 - (end_seqnum - seqnum));
            } else {
                return -1;
            }
        }

        /** Return the first index matching with given continuity counter **/
        private function getFirstIndexfromContinuity(continuity : int) : int {
            // look for first fragment matching with given continuity index
            var len : int = fragments.length;
            for (var i : int = 0; i < len; i++) {
                if (fragments[i].continuity == continuity)
                    return i;
            }
            return -1;
        }

        /** Return the first seqnum matching with given continuity counter **/
        public function getFirstSeqNumfromContinuity(continuity : int) : Number {
            var index : int = getFirstIndexfromContinuity(continuity);
            if (index == -1) {
                return Number.NEGATIVE_INFINITY;
            }
            return fragments[index].seqnum;
        }

        /** Return the last seqnum matching with given continuity counter **/
        public function getLastSeqNumfromContinuity(continuity : int) : Number {
            var index : int = getLastIndexfromContinuity(continuity);
            if (index == -1) {
                return Number.NEGATIVE_INFINITY;
            }
            return fragments[index].seqnum;
        }

        /** Return the last index matching with given continuity counter **/
        private function getLastIndexfromContinuity(continuity : Number) : int {
            var firstIndex : int = getFirstIndexfromContinuity(continuity);
            if (firstIndex == -1)
                return -1;

            var lastIndex : int = firstIndex;
            // look for first fragment matching with given continuity index
            for (var i : int = firstIndex; i < fragments.length; i++) {
                if (fragments[i].continuity == continuity)
                    lastIndex = i;
                else
                    break;
            }
            return lastIndex;
        }

        /** set Fragments **/
        public function updateFragments(_fragments : Vector.<Fragment>) : void {
            var idx_with_metrics : int = -1;
            var len : int = _fragments.length;
            var continuity_offset : int;
            var frag : Fragment;
            // update PTS from previous fragments
            for (var i : int = 0; i < len; i++) {
                frag = getFragmentfromSeqNum(_fragments[i].seqnum);
                if (frag != null) {
                    continuity_offset = frag.continuity - _fragments[i].continuity;
                    if(!isNaN(frag.data.pts_start)) {
                    _fragments[i].data = frag.data;
                    idx_with_metrics = i;
                    }
                }
            }
            if(continuity_offset) {
                CONFIG::LOGGING {
                    Log.debug("updateFragments: discontinuity sliding from live playlist,take into account discontinuity drift:" + continuity_offset);
                }
                for (i = 0; i < len; i++) {
                     _fragments[i].continuity+= continuity_offset;
                }
            }
            updateFragmentsProgramDate(_fragments);
            fragments = _fragments;

            if (len > 0) {
                start_seqnum = _fragments[0].seqnum;
                end_seqnum = _fragments[len - 1].seqnum;

                if (idx_with_metrics != -1) {
                    frag = fragments[idx_with_metrics];
                    // if at least one fragment contains PTS info, recompute PTS information for all fragments
                    CONFIG::LOGGING {
                        Log.debug("updateFragments: found PTS info from previous playlist,seqnum/PTS:" + frag.seqnum + "/" + frag.data.pts_start);
                    }
                    updateFragment(frag.seqnum, true, frag.data.pts_start, frag.data.pts_start + 1000 * frag.duration);
                } else {
                    CONFIG::LOGGING {
                        Log.debug("updateFragments: unknown PTS info for this level");
                    }
                    duration = _fragments[len - 1].start_time + _fragments[len - 1].duration;
                }
                averageduration = duration / len;
            } else {
                duration = 0;
                averageduration = 0;
            }
        }

        private function updateFragmentsProgramDate(_fragments : Vector.<Fragment>) : void {
            var len : int = _fragments.length;
            var continuity : int;
            var program_date : Number;
            var frag : Fragment;
            for (var i : int = 0; i < len; i++) {
                frag = _fragments[i];
                if (frag.continuity != continuity) {
                    continuity = frag.continuity;
                    program_date = 0;
                }
                if (frag.program_date) {
                    program_date = frag.program_date + 1000 * frag.duration;
                } else if (program_date) {
                    frag.program_date = program_date;
                }
            }
        }

        private function _updatePTS(from_index : int, to_index : int) : void {
            // CONFIG::LOGGING {
            // Log.info("updateFragmentPTS from/to:" + from_index + "/" + to_index);
            // }
            var frag_from : Fragment = fragments[from_index];
            var frag_to : Fragment = fragments[to_index];

            if (frag_from.data.valid && frag_to.data.valid) {
                if (!isNaN(frag_to.data.pts_start)) {
                    // we know PTS[to_index]
                    frag_to.data.pts_start_computed = frag_to.data.pts_start;
                    /* normalize computed PTS value based on known PTS value.
                     * this is to avoid computing wrong fragment duration in case of PTS looping */
                    var from_pts : Number = PTS.normalize(frag_to.data.pts_start, frag_from.data.pts_start_computed);
                    /* update fragment duration.
                    it helps to fix drifts between playlist reported duration and fragment real duration */
                    if (to_index > from_index) {
                        frag_from.duration = (frag_to.data.pts_start - from_pts) / 1000;
                        CONFIG::LOGGING {
                            if (frag_from.duration < 0) {
                                Log.error("negative duration computed for " + frag_from + ", there should be some duration drift between playlist and fragment!");
                            }
                        }
                    } else {
                        frag_to.duration = ( from_pts - frag_to.data.pts_start) / 1000;
                        CONFIG::LOGGING {
                            if (frag_to.duration < 0) {
                                Log.error("negative duration computed for " + frag_to + ", there should be some duration drift between playlist and fragment!");
                            }
                        }
                    }
                } else {
                    // we dont know PTS[to_index]
                    if (to_index > from_index)
                        frag_to.data.pts_start_computed = frag_from.data.pts_start_computed + 1000 * frag_from.duration;
                    else
                        frag_to.data.pts_start_computed = frag_from.data.pts_start_computed - 1000 * frag_to.duration;
                }
            }
        }

        public function updateFragment(seqnum : Number, valid : Boolean, min_pts : Number = 0, max_pts : Number = 0) : void {
            // CONFIG::LOGGING {
            // Log.info("updatePTS : seqnum/min/max:" + seqnum + '/' + min_pts + '/' + max_pts);
            // }
            // get fragment from seqnum
            var fragIdx : int = getIndexfromSeqNum(seqnum);
            if (fragIdx != -1) {
                var frag : Fragment = fragments[fragIdx];
                // update fragment start PTS + duration
                if (valid) {
                    frag.data.pts_start = min_pts;
                    frag.data.pts_start_computed = min_pts;
                    frag.duration = (max_pts - min_pts) / 1000;
                } else {
                    frag.duration = 0;
                }
                frag.data.valid = valid;
                // CONFIG::LOGGING {
                // Log.info("SN["+fragments[fragIdx].seqnum+"]:pts/duration:" + fragments[fragIdx].start_pts_computed + "/" + fragments[fragIdx].duration);
                // }

                // adjust fragment PTS/duration from seqnum-1 to frag 0
                for (var i : int = fragIdx; i > 0 && fragments[i - 1].continuity == frag.continuity; i--) {
                    _updatePTS(i, i - 1);
                    // CONFIG::LOGGING {
                    // Log.info("SN["+fragments[i-1].seqnum+"]:pts/duration:" + fragments[i-1].start_pts_computed + "/" + fragments[i-1].duration);
                    // }
                }

                // adjust fragment PTS/duration from seqnum to last frag
                for (i = fragIdx; i < fragments.length - 1 && fragments[i + 1].continuity == frag.continuity; i++) {
                    _updatePTS(i, i + 1);
                    // CONFIG::LOGGING {
                    // Log.info("SN["+fragments[i+1].seqnum+"]:pts/duration:" + fragments[i+1].start_pts_computed + "/" + fragments[i+1].duration);
                    // }
                }

                // second, adjust fragment offset
                var start_time_offset : Number = fragments[0].start_time;
                var len : int = fragments.length;
                for (i = 0; i < len; i++) {
                    fragments[i].start_time = start_time_offset;
                    start_time_offset += fragments[i].duration;
                    // CONFIG::LOGGING {
                    // Log.info("SN["+fragments[i].seqnum+"]:start_time/continuity/pts/duration:" + fragments[i].start_time + "/" + fragments[i].continuity + "/"+ fragments[i].start_pts_computed + "/" + fragments[i].duration);
                    // }
                }
                duration = start_time_offset;
            } else {
                CONFIG::LOGGING {
                    Log.error("updateFragment:seqnum " + seqnum + " not found!");
                }
            }
        }
    }
}
