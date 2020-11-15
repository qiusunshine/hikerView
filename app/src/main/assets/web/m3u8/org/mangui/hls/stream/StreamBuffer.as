/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.stream {
    import flash.events.Event;
    import flash.events.TimerEvent;
    import flash.utils.Dictionary;
    import flash.utils.getTimer;
    import flash.utils.Timer;
    import org.mangui.hls.constant.HLSLoaderTypes;
    import org.mangui.hls.constant.HLSPlayStates;
    import org.mangui.hls.constant.HLSSeekMode;
    import org.mangui.hls.constant.HLSSeekStates;
    import org.mangui.hls.constant.HLSTypes;
    import org.mangui.hls.controller.AudioTrackController;
    import org.mangui.hls.controller.LevelController;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.event.HLSMediatime;
    import org.mangui.hls.flv.FLVTag;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;
    import org.mangui.hls.loader.AltAudioFragmentLoader;
    import org.mangui.hls.loader.FragmentLoader;
    import org.mangui.hls.model.AudioTrack;
    import org.mangui.hls.model.Fragment;
    import org.mangui.hls.model.Level;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    /*
     * intermediate FLV Tag Buffer
     *  input : FLV tags retrieved from different fragment loaders (video/alt-audio...)
     *  output : provide muxed FLV tags to HLSNetStream
     */
    public class StreamBuffer {
        private var _hls : HLS;
        private var _fragmentLoader : FragmentLoader;
        private var _altaudiofragmentLoader : AltAudioFragmentLoader;
        /** Timer used to process FLV tags. **/
        private var _timer : Timer;
        private var _audioTags : Vector.<FLVData>,  _videoTags : Vector.<FLVData>,_metaTags : Vector.<FLVData>, _headerTags : Vector.<FLVData>;
        private var _audioIdx : uint,  _videoIdx : uint,  _metaIdx : uint, _headerIdx : uint;
        private var _fragMainLevel : int, _fragMainSN : int;
        // last main frag injected in NetStream
        private var _fragMainLevelNetStream : int, _fragMainSNNetStream : int;
        private var _fragMainInitialContinuity : int,_fragMainInitialStartPosition : Number,_fragMainInitialPTS : Number;
        private var _fragAltAudioLevel : int, _fragAltAudioSN : int;
        private var _fragAltAudioInitialContinuity : int,_fragAltAudioInitialStartPosition : Number,_fragAltAudioInitialPTS : Number;
        private var _fragMainIdx : uint,  _fragAltAudioIdx : uint;
        private var _filteringStartIdx : uint;
        /** playlist duration **/
        private var _playlistDuration : Number = 0;
        /** requested start position (absolute position) **/
        private var _seekPositionRequested : Number;
        /** real start position , retrieved from first fragment (absolute position) **/
        private var _seekPositionReal : Number;
        /** start position of first injected tag **/
        private var _seekPositionReached : Boolean;
        private static const MIN_NETSTREAM_BUFFER_SIZE : Number = 3.0;
        private static const MAX_NETSTREAM_BUFFER_SIZE : Number = 4.0;
        /** means that last fragment of a VOD playlist has been loaded */
        private var _reachedEnd : Boolean;
        /* are we using alt-audio ? */
        private var _useAltAudio : Boolean;
        /** playlist sliding (non null for live playlist) **/
        private var _liveSlidingMain : Number;
        private var _liveSlidingAltAudio : Number;
        // these 2 variables are used to compute main and altaudio live playlist sliding
        private var _nextExpectedAbsoluteStartPosMain : Number;
        private var _nextExpectedAbsoluteStartPosAltAudio : Number;
        /** is live loading stalled **/
        private var _liveLoadingStalled : Boolean;
        /** is playback completed **/
        private var _playbackCompleted : Boolean;
        /* last media time data */
        private var _lastPos : Number;
        private var _lastBufLen : Number;
        private var _lastDuration : Number;
        private var _lastMediaTimeUpdate : int;
        /* overlapping fragment stuff */
        private var _overlappingTags : Vector.<FLVTag>;
        private var _overlappingStartPosition : Number;
        private var _overlappingMinPTS : Number;


        public function StreamBuffer(hls : HLS, audioTrackController : AudioTrackController, levelController : LevelController) {
            _hls = hls;
            _fragmentLoader = new FragmentLoader(hls, audioTrackController, levelController, this);
            _altaudiofragmentLoader = new AltAudioFragmentLoader(hls, this);
            flushBuffer();
            _timer = new Timer(100, 0);
            _timer.addEventListener(TimerEvent.TIMER, _checkBuffer);
            _hls.addEventListener(HLSEvent.LIVE_LOADING_STALLED, _liveLoadingStalledHandler);
            _hls.addEventListener(HLSEvent.PLAYLIST_DURATION_UPDATED, _playlistDurationUpdated);
            _hls.addEventListener(HLSEvent.LAST_VOD_FRAGMENT_LOADED, _lastVODFragmentLoadedHandler);
            _hls.addEventListener(HLSEvent.AUDIO_TRACK_SWITCH, _audioTrackChange);
            _hls.addEventListener(HLSEvent.PLAYBACK_COMPLETE, _playbackComplete);
        }

        public function dispose() : void {
            flushBuffer();
            _hls.removeEventListener(HLSEvent.LIVE_LOADING_STALLED, _liveLoadingStalledHandler);
            _hls.removeEventListener(HLSEvent.PLAYLIST_DURATION_UPDATED, _playlistDurationUpdated);
            _hls.removeEventListener(HLSEvent.LAST_VOD_FRAGMENT_LOADED, _lastVODFragmentLoadedHandler);
            _hls.removeEventListener(HLSEvent.AUDIO_TRACK_SWITCH, _audioTrackChange);
            _hls.removeEventListener(HLSEvent.PLAYBACK_COMPLETE, _playbackComplete);
            _timer.stop();
            _timer.removeEventListener(TimerEvent.TIMER, _checkBuffer);
            _fragmentLoader.dispose();
            _altaudiofragmentLoader.dispose();
            _fragmentLoader = null;
            _altaudiofragmentLoader = null;
            _hls = null;
            _timer = null;
        }

        public function stop() : void {
            _fragmentLoader.stop();
            _altaudiofragmentLoader.stop();
            flushBuffer();
        }

        /*
         * if requested position is available in StreamBuffer, trim buffer
         * and inject from that point
         * if seek position out of buffer, ask fragment loader to retrieve data
         */
        public function seek(position : Number) : void {
            var loadLevel : Level;
            // cap max position if known playlist duration
            var maxPosition : Number = Number.POSITIVE_INFINITY;
            if(_hls.loadLevel < _hls.levels.length) {
                loadLevel = _hls.levels[_hls.loadLevel];
                // if defined, set max position as being end of playlist - 1 second
                if(!isNaN(loadLevel.duration)) {
                    maxPosition = loadLevel.duration-1;
                }
            }
            if (position == -1 && loadLevel && _hls.type == HLSTypes.LIVE) {
                /*  If start position not specified for a live stream, follow HLS spec :
                    If the EXT-X-ENDLIST tag is not present
                    and the client intends to play the media regularly (i.e. in playlist
                    order at the nominal playback rate), the client SHOULD NOT
                    choose a segment which starts less than three target durations from
                    the end of the Playlist file */
                _seekPositionRequested = Math.max(0, loadLevel.duration - 3 * loadLevel.averageduration);
            } else {
                _seekPositionRequested = Math.min(Math.max(position, 0), maxPosition);
            }
            CONFIG::LOGGING {
                Log.debug("seek : requested position:" + position.toFixed(2) + ",seek position:" + _seekPositionRequested.toFixed(2) + ",min/max buffer position:" + min_pos.toFixed(2) + "/" + max_pos.toFixed(2));
            }
            // check if we can seek in buffer
            if (_seekPositionRequested >= min_pos && _seekPositionRequested <= max_pos) {
                _seekPositionReached = false;
                _audioIdx = _videoIdx = _metaIdx = _headerIdx = 0;
                CONFIG::LOGGING {
                    Log.debug("seek in buffer");
                    // seek position requested is an absolute position, add sliding main to make it absolute
                    _seekPositionRequested+= _liveSlidingMain;
                }
            } else {
                // stop any load in progress ...
                _fragmentLoader.stop();
                _altaudiofragmentLoader.stop();
                // seek position is out of buffer : load from fragment
                _liveLoadingStalled = false;
                _fragmentLoader.seek(_seekPositionRequested);
                // check if we need to use alt audio fragment loader
                if (_hls.audioTracks && _hls.audioTracks.length && _hls.audioTrack >= 0 && _hls.audioTracks[_hls.audioTrack].source == AudioTrack.FROM_PLAYLIST) {
                    CONFIG::LOGGING {
                        Log.info("seek : need to load alt audio track");
                    }
                    _altaudiofragmentLoader.seek(_seekPositionRequested);
                    _useAltAudio = true;
                } else {
                    _useAltAudio = false;
                }
                flushBuffer();
            }
            _playbackCompleted = false;
            _lastMediaTimeUpdate = 0;
            _timer.start();
        }

        public function appendTags(fragmentType : int, fragLevel : int, fragSN : int, tags : Vector.<FLVTag>, min_pts : Number, max_pts : Number, continuity : int, startPosition : Number) : void {
            // compute playlist sliding here :  it is the difference between  expected start position and real start position
            var sliding:Number;
            var nextRelativeStartPos: Number = startPosition + (max_pts - min_pts) / 1000;
            var headerAppended : Boolean = false, metaAppended : Boolean = false;
            // compute sliding in case of live playlist, or in case of VoD playlist that slided in the past (live sliding ended playlist)
            var computeSliding : Boolean = (_hls.type == HLSTypes.LIVE  || _liveSlidingMain || _liveSlidingAltAudio);

            var fragIdx : int;
            if(fragmentType == HLSLoaderTypes.FRAGMENT_MAIN) {
                sliding = _liveSlidingMain;
                // if a new fragment is being appended
                if(fragLevel != _fragMainLevel || fragSN != _fragMainSN) {
                    if(continuity == _fragMainInitialContinuity) {
                        /// don't check overlapping on live playlists or VoD playlists that slided in the past
                        if(!computeSliding) {
                            // this whole part of the code is here to handle playlists with fragments overlapping with each other (when switching level usually)
                            // for example let's imagine we have data already  buffered, and new tags are appended, overlapping with existing buffer :
                            // existing ------------------------------------------->
                            // new tags                                       K--K----K----->
                            //
                            // here we are trying to filter new tags to convert them into the following
                            // existing ------------------------------------------->
                            // filtered new tags                                      K----->
                            // this is more complex than it seems, as we need to deal with keyframes for video and all that kind of stuff
                            // video filtered new tags should start with a keyframe ...
                            // if we already have overlapping tags, adjust startPosition/minPTS to match the one from first tag
                            // we only trigger this logic if detected overlapping is more than 500 ms

                            if(_overlappingTags.length) {
                                startPosition = _overlappingStartPosition;
                                min_pts = _overlappingMinPTS;
                                nextRelativeStartPos = startPosition + (max_pts - min_pts) / 1000;
                            } else {
                                _overlappingStartPosition = startPosition;
                                _overlappingMinPTS = min_pts;
                            }
                            var end : Number = startPosition+(max_pts-min_pts)/1000;
                            // check if any overlapping occurs, let 500ms tolerance
                            if(startPosition+0.5 < max_audio_pos && startPosition+0.5 < max_video_pos) {
                                CONFIG::LOGGING {
                                    Log.warn("fragment overlapping with buffered one, start/end/max_audio_pos/max_video_pos:"+startPosition.toFixed(3)+"/"+ end.toFixed(3) + "/" + max_audio_pos.toFixed(3)+"/"+max_video_pos.toFixed(3));
                                }
                                _overlappingTags = _overlappingTags.concat(tags);
                                // filter out overlapping tags
                                tags = filterOverlappingTags(_overlappingTags, max_pos - startPosition);
                                if(tags.length == 0) {
                                    // not enough data, cannot filter overlapping tags, don't push those tags
                                    // fragloader will call us back later with more data
                                    return;
                                } else {
                                    // reset overlapping tags
                                    _overlappingTags = new Vector.<FLVTag>();
                                    // adjust startPosition and min_pts
                                    startPosition = max_pos;
                                    min_pts = tags[0].pts;
                                    // recompute value
                                    nextRelativeStartPos = startPosition + (max_pts - min_pts) / 1000;
                                }
                            }
                        }
                    } else {
                        // different continuity counter, reset any overlapping tags
                        if(_overlappingTags.length) {
                            // reset overlapping tags
                            _overlappingTags = new Vector.<FLVTag>();
                        }
                    }
                    _fragMainLevel = fragLevel;
                    _fragMainSN = fragSN;
                    _fragMainIdx++;
                    // compute sliding if needed
                    if(computeSliding) {
                        // if not -1 : it is not the first appending for this fragment type : we can compute playlist sliding
                        if(_nextExpectedAbsoluteStartPosMain !=-1) {
                            // if same continuity counter, sliding can be computed using PTS, it will be more accurate
                            if(continuity == _fragMainInitialContinuity) {
                                sliding = _liveSlidingMain = _fragMainInitialStartPosition + (min_pts-_fragMainInitialPTS)/1000 - startPosition;
                                CONFIG::LOGGING {
                                    var debugString : String = "sliding/min_pts/_fragMainInitialPTS/startPosition/_fragMainInitialStartPosition:" + sliding + '/' + min_pts + '/' + _fragMainInitialPTS + '/' + startPosition.toFixed(3) + '/' + _fragMainInitialStartPosition;
                                    // allow negative but small values, that could happen because of frag drift
                                    if(sliding < -1) {
                                        Log.warn('negative sliding:'+debugString);
                                    } else {
                                        Log.debug2('sliding on same cc:'+ debugString);
                                    }
                                }
                            } else {
                                sliding = _liveSlidingMain = _nextExpectedAbsoluteStartPosMain - startPosition;
                                CONFIG::LOGGING {
                                   Log.debug2('sliding on different cc: expectedAbsoluteStart/relativeStart/sliding' + _nextExpectedAbsoluteStartPosMain.toFixed(3) + '/' + startPosition.toFixed(3) + '/' + sliding.toFixed(3));
                                }
                            }
                        } else {
                            _fragMainInitialStartPosition = startPosition;
                            _fragMainInitialPTS = min_pts;
                            _fragMainInitialContinuity = continuity;
                        }
                    }
                    CONFIG::LOGGING {
                        Log.debug('new main frag,start/sliding/cc/idx:' + startPosition.toFixed(3) + '/' + sliding.toFixed(3) + '/' + continuity + '/' + _fragMainIdx);
                    }
                }
                fragIdx = _fragMainIdx;
                /* update next expected absolute start pos at this point : absolute = relative + sliding
                   useful to compute sliding when discontinuity occurs
                */
                _nextExpectedAbsoluteStartPosMain = nextRelativeStartPos + sliding;
            } else {
                sliding = _liveSlidingAltAudio;
                // if a new fragment is being appended
                if(fragLevel != _fragAltAudioLevel || fragSN != _fragAltAudioSN) {
                    _fragAltAudioLevel = fragLevel;
                    _fragAltAudioSN = fragSN;
                    _fragAltAudioIdx++;
                    // compute sliding if needed
                    if(computeSliding) {
                        // if -1 : it is not the first appending for this fragment type : we can compute playlist sliding
                        if(_nextExpectedAbsoluteStartPosAltAudio !=-1) {
                            // if same continuity counter, sliding can be computed using PTS, it will be more accurate
                            if(continuity == _fragAltAudioInitialContinuity) {
                                sliding = _liveSlidingAltAudio = _fragAltAudioInitialStartPosition + (min_pts - _fragAltAudioInitialPTS)/1000 - startPosition;
                            } else {
                                sliding = _liveSlidingAltAudio = _nextExpectedAbsoluteStartPosAltAudio - startPosition;
                            }
                        } else {
                            _fragAltAudioInitialStartPosition = startPosition;
                            _fragAltAudioInitialPTS = min_pts;
                            _fragAltAudioInitialContinuity = continuity;
                        }
                    }
                    CONFIG::LOGGING {
                        Log.debug('new altaudio frag,start/sliding/idx:' + startPosition + '/' + sliding + '/' + _fragAltAudioIdx);
                    }
                }
                fragIdx = _fragAltAudioIdx;
                /* update next expected absolute start pos at this point : absolute = relative + sliding
                   useful to compute sliding when discontinuity occurs
                */
                _nextExpectedAbsoluteStartPosAltAudio = nextRelativeStartPos + sliding;
            }

            for each (var tag : FLVTag in tags) {
//                CONFIG::LOGGING {
//                    Log.debug2('append type/dts/pts:' + tag.typeString + '/' + tag.dts + '/' + tag.pts);
//                }
                var pos : Number = startPosition + (tag.pts - min_pts) / 1000;
                var tagData : FLVData = new FLVData(tag, pos, sliding, continuity, fragmentType, fragIdx, fragLevel, fragSN);
                switch(tag.type) {
                    case FLVTag.DISCONTINUITY:
                    case FLVTag.AAC_HEADER:
                    case FLVTag.AVC_HEADER:
                        _headerTags.push(tagData);
                        headerAppended = true;
                        break;
                    case FLVTag.AAC_RAW:
                    case FLVTag.MP3_RAW:
                        _audioTags.push(tagData);
                        break;
                    case FLVTag.AVC_NALU:
                        _videoTags.push(tagData);
                        break;
                    case FLVTag.METADATA:
                        _metaTags.push(tagData);
                        metaAppended = true;
                        break;
                    default:
                }
            }

            if(headerAppended) {
                _headerTags = _headerTags.sort(compareTags);
            }

            if(metaAppended) {
                _metaTags = _metaTags.sort(compareTags);
            }


            if (_hls.seekState == HLSSeekStates.SEEKING) {
                /* if in seeking mode, force timer start here, this could help reducing the seek time by 100ms */
                _timer.start();
            }
        }

        /** Return current media position **/
        public function get position() : Number {
            switch(_hls.seekState) {
                case HLSSeekStates.SEEKING:
                // _seekPositionRequested is an absolute position, convert it to relative by substracting sliding
                    return  _seekPositionRequested - _liveSlidingMain;
                case HLSSeekStates.SEEKED:
                case HLSSeekStates.IDLE:
                default:
                    /** Relative playback position = Absolute Position (which is Absolute seek position + NetStream playback time) - playlist sliding **/
                    var pos: Number = _seekPositionReal + _hls.stream.time - _liveSlidingMain;
                    if(isNaN(pos)) {
                        pos = 0;
                    }
                    return pos;
            }
        }

        public function get liveSlidingMain() : Number {
            return _liveSlidingMain;
        }

        public function get liveSlidingAltAudio() : Number {
            return _liveSlidingAltAudio;
        }

        public function get reachedEnd() : Boolean {
            return _reachedEnd;
        }

        public function get liveLoadingStalled() : Boolean {
            return _liveLoadingStalled;
        }

        public function flushBuffer() : void {
            _audioTags = new Vector.<FLVData>();
            _videoTags = new Vector.<FLVData>();
            _metaTags = new Vector.<FLVData>();
            _headerTags = new Vector.<FLVData>();
            _overlappingTags = new Vector.<FLVTag>();
            _fragMainLevel = _fragAltAudioLevel = _fragMainLevelNetStream = -1;
            _fragMainSN = _fragAltAudioSN = _fragMainSNNetStream = 0;
            FLVData.refPTSMain = FLVData.refPTSAltAudio = NaN;
            _audioIdx = _videoIdx = _metaIdx = _headerIdx = 0;
            _fragMainIdx = _fragAltAudioIdx = 0;
            _seekPositionReached = false;
            _reachedEnd = false;
            _liveSlidingMain = _liveSlidingAltAudio = 0;
            _nextExpectedAbsoluteStartPosMain = _nextExpectedAbsoluteStartPosAltAudio = -1;
            CONFIG::LOGGING {
                Log.debug("StreamBuffer flushed");
            }
        }

        private function flushAudio() : void {
            // flush audio buffer and AAC HEADER tags (if any)
            _audioTags = new Vector.<FLVData>();
            _audioIdx = 0;
            FLVData.refPTSAltAudio = NaN;
            _nextExpectedAbsoluteStartPosAltAudio = -1;
            _liveSlidingAltAudio = 0;
            var _filteredHeaderTags : Vector.<FLVData> = _headerTags.filter(filterAACHeader);
            _headerIdx -= (_headerTags.length - _filteredHeaderTags.length);
        }

        private function filterAACHeader(item : FLVData, index : int, vector : Vector.<FLVData>) : Boolean {
            return (item.tag.type != FLVTag.AAC_HEADER);
        }

        public function flushLastFragment(fragLevel : int, fragSN : int) : Boolean {
            /* check if we already started injecting that fragment in NetStream
                if that is the case, then we cannot flush seamlessly, return false
            */
            if(fragLevel == _fragMainLevelNetStream && fragSN == _fragMainSNNetStream) {
                CONFIG::LOGGING {
                    Log.debug("StreamBuffer.flushLastFragment, cannot flush, tag already pushed in NetStream");
                }
                return false;
            } else if(fragLevel == _fragMainLevel && fragSN == _fragMainSN) {
                CONFIG::LOGGING {
                    Log.warn("StreamBuffer.flushLastFragment, clip end of StreamBuffer");
                }
                // flush StreamBuffer if needed
                _headerTags = _headerTags.filter(filterlastFragment);
                _videoTags = _videoTags.filter(filterlastFragment);
                _audioTags = _audioTags.filter(filterlastFragment);
                _metaTags = _metaTags.filter(filterlastFragment);
                return true;
            }
            return false;
        }

        private function filterlastFragment(item : FLVData, index : int, vector : Vector.<FLVData>) : Boolean {
            // keep if return true. we want to keep everything but last main fragment appended tags
            return (!(item.loaderType == HLSLoaderTypes.FRAGMENT_MAIN &&  item.fragLevel == _fragMainLevel &&  item.fragSN == _fragMainSN));
        }

        /* compare two tags, smallest continuity
         * then smallest pts. then discontinuity then aac/avc header then metadata, then others
         *
        return a negative number, if x should appear before y in the sorted sequence
        retun 0, if x equals y
        return a positive number, if x should appear after y in the sorted sequence*
         *
         * */
        private function compareTags(x : FLVData, y : FLVData) : Number {
            if (x.continuity != y.continuity) {
                return (x.continuity - y.continuity);
            } else {
                if (x.tag.dts != y.tag.dts) {
                    return (x.tag.dts - y.tag.dts);
                } else {
                    return (gettagrank(x.tag) - gettagrank(y.tag));
                }
            }
        }

        /*
            helper function used to sort tags, lower values have highest priority
        */
        private function gettagrank(tag : FLVTag) : uint {
            switch(tag.type) {
                case FLVTag.DISCONTINUITY:
                    return 0;
                case FLVTag.METADATA:
                    return 1;
                case FLVTag.AVC_HEADER:
                case FLVTag.AAC_HEADER:
                    return 2;
                default:
                    return 3;
            }
        }

        private function get audioExpected() : Boolean {
            return (_fragmentLoader.audioExpected || _useAltAudio);
        }

        private function get videoExpected() : Boolean {
            return _fragmentLoader.videoExpected;
        }

        public function get audioBufferLength() : Number {
            return getbuflen(_audioTags, _audioIdx);
        }

        public function get videoBufferLength() : Number {
            return getbuflen(_videoTags, _videoIdx);
        }

        public function get bufferLength() : Number {
            switch(_hls.seekState) {
                case HLSSeekStates.SEEKING:
                    /* max_pos is a relative max, seekPositionRequested is absolute. we need to add _liveSlidingMain
                       in order to compare apple to apple */
                    return  Math.max(0, max_pos + _liveSlidingMain - _seekPositionRequested);
                case HLSSeekStates.SEEKED:
                    if (audioExpected) {
                        if (videoExpected) {
                            return Math.min(audioBufferLength, videoBufferLength);
                        } else {
                            return audioBufferLength;
                        }
                    } else {
                        return videoBufferLength;
                    }
                case HLSSeekStates.IDLE:
                default:
                    return 0;
            }
        }

        public function get backBufferLength() : Number {
            if (min_pos != Number.POSITIVE_INFINITY) {
                return (position - min_pos);
            } else {
                return 0;
            }
        }

        /** Return the quality level of the next played fragment **/
        public function get nextLevel() : int {
            if(_videoIdx < _videoTags.length) {
                return _videoTags[_videoIdx].fragLevel;
            } else {
                return _hls.currentLevel;
            }
        };

        /*  set quality level for next loaded fragment (-1 for automatic level selection) */
        public function set nextLevel(level : int) : void {
            CONFIG::LOGGING {
                Log.debug("StreamBuffer:set nextLevel:" + level);
            }
            if(_videoIdx < _videoTags.length) {
                /* remove tags not injected into NetStream,
                    as tags are injected on fragment boundary, tags not injected in NetStream corresponds
                    with next fragment tags
                */

                //find idx for next tag not matching with _fragMainLevelNetStream/_fragMainSNNetStream
                for(var i : int = _videoIdx; i < _videoTags.length ; i++) {
                    var tagData : FLVData = _videoTags[i];
                    if(tagData.fragLevel != _fragMainLevelNetStream ||tagData.fragSN != _fragMainSNNetStream) {
                        CONFIG::LOGGING {
                            Log.debug("first video FLV tag from next frag is @ " + i + " of [" + _videoIdx + "," + (_videoTags.length-1) + "]");
                        }
                        var lastLevel : Number =  tagData.fragLevel;
                        var lastSN : Number =  tagData.fragSN;
                        // if we want to seek to the right fragment, we need to set lastFrag with lastSN-1
                        var lastFrag : Fragment = _hls.levels[lastLevel].getFragmentfromSeqNum(lastSN-1);
                        if(lastFrag) {
                            CONFIG::LOGGING {
                                Log.debug("StreamBuffer:lastFrag defined, flush buffer and seekFromLastFrag");
                            }
                            // flush all video tags not injected into NetStream, from next fragment onwards
                            _videoTags.splice(i, _videoTags.length-i);
                            // flush all audio tags from next main fragment onwards
                            _filteringStartIdx = _audioIdx;
                            _audioTags = _audioTags.filter(filternextFragments);
                            // flush all header tags from next main fragment onwards
                            _filteringStartIdx = _headerIdx;
                            _headerTags = _headerTags.filter(filternextFragments);
                            // flush all metadata tags from next main fragment onwards
                            _filteringStartIdx = _metaIdx;
                            _metaTags = _metaTags.filter(filternextFragments);
                            // stop any load in progress ...
                            _fragmentLoader.stop();
                            // seek position is out of buffer : load after fragment
                            _fragmentLoader.seekFromLastFrag(lastFrag);
                            // reset overlapping tags, if any
                            _overlappingTags = new Vector.<FLVTag>();
                        }
                        return;
                    }
                }
                CONFIG::LOGGING {
                    Log.debug("StreamBuffer:can't find tags associated to next frag, don't flush anything");
                }
            }
        };

        private function filternextFragments(item : FLVData, index : int, vector : Vector.<FLVData>) : Boolean {
            // keep if return true
            // we want to keep only if alt audio loader OR
            // index less than start filtering index (i.e. we want to keep backbuffer) OR
            // matching with current fragment appended tags
            return (item.loaderType == HLSLoaderTypes.FRAGMENT_ALTAUDIO ||
                                   index < _filteringStartIdx ||
                                   (item.fragLevel == _fragMainLevelNetStream &&  item.fragSN == _fragMainSNNetStream));
        }

        /**  StreamBuffer Timer, responsible of
         * reporting media time event
         *  injecting tags into NetStream
         *  clipping backbuffer
         */
        private function _checkBuffer(e : Event) : void {
            var pos : Number = position;
            var bufLen : Number = _hls.stream.bufferLength;
            var watched : Number = (_hls.stream as HLSNetStream).watched;
            var currentTime : int = getTimer();
            var dispatchTimeUpdate : Boolean = (currentTime - _lastMediaTimeUpdate > HLSSettings.mediaTimePeriod);
            var duration : Number;
            if(_playbackCompleted) {
                // as playback is completed, align stream duration to last playback position
                duration = position;
            } else {
                duration = _playlistDuration;
            }
            // dispatch media time event only if position/buffer or playlist duration has changed
            if(dispatchTimeUpdate && (pos != _lastPos || bufLen != _lastBufLen || duration != _lastDuration) ) {
                _hls.dispatchEvent(new HLSEvent(HLSEvent.MEDIA_TIME, new HLSMediatime(pos, duration, bufLen, backBufferLength, _liveSlidingMain, _liveSlidingAltAudio,watched)));
                _lastPos = pos;
                _lastDuration = duration;
                _lastBufLen = bufLen;
                _lastMediaTimeUpdate = currentTime;
            }

            var netStreamBuffer : Number = (_hls.stream as HLSNetStream).netStreamBufferLength;
            /* only append tags if seek position has been reached, otherwise wait for more tags to come
             * this is to ensure that accurate seeking will work appropriately
             */
            CONFIG::LOGGING {
                Log.debug2("position/duration/buffer/backBuffer/audio/video/NetStream bufferLength/audioExpected/videoExpected:" + position.toFixed(2) + "/" + duration.toFixed(2) + "/" + bufLen.toFixed(2) + "/" + backBufferLength.toFixed(2) + "/" + audioBufferLength.toFixed(2) + "/" + videoBufferLength.toFixed(2) + "/" + netStreamBuffer.toFixed(2) + "/" + audioExpected + "/" + videoExpected);
            }

            var tagDuration : Number = 0;
            if (_seekPositionReached) {
                if (netStreamBuffer < MIN_NETSTREAM_BUFFER_SIZE && _hls.playbackState != HLSPlayStates.IDLE) {
                    tagDuration = MAX_NETSTREAM_BUFFER_SIZE - netStreamBuffer;
                }
            } else {
                /* seek position not reached yet.
                 * check if buffer max absolute position is greater than requested seek position
                 * if it is the case, then we can start injecting tags in NetStream
                 * max_pos is a relative max, here we need to compare against absolute max position, so
                 * we need to add _liveSlidingMain to convert from relative to absolute
                 */
                if ((max_pos+_liveSlidingMain) >= _seekPositionRequested) {
                    // inject enough tags to reach seek position
                    tagDuration = _seekPositionRequested + MAX_NETSTREAM_BUFFER_SIZE - min_min_pos;
                }
            }
            if (tagDuration > 0) {
                var data : Vector.<FLVData> = shiftMultipleTags(tagDuration);
                if (!_seekPositionReached) {
                    data = seekFilterTags(data, _seekPositionRequested);
                    if(data.length) {
                        _seekPositionReached = true;
                    }
                }

                var tags : Vector.<FLVTag> = new Vector.<FLVTag>();
                for each (var flvdata : FLVData in data) {
                    if(flvdata.loaderType == HLSLoaderTypes.FRAGMENT_MAIN) {
                            _fragMainLevelNetStream = flvdata.fragLevel;
                            _fragMainSNNetStream = flvdata.fragSN;
                    }
                    tags.push(flvdata.tag);
                }
                if (tags.length) {
                    CONFIG::LOGGING {
                        var flvdata0 : FLVData = data[0];
                        Log.debug('appending first level/sn/type/dts/pts/position:' + flvdata0.fragLevel + '/' + flvdata0.fragSN + '/' + flvdata0.tag.typeString + '/' + flvdata0.tag.dts + '/' + flvdata0.tag.pts + '/' + (flvdata0.positionAbsolute - _liveSlidingMain).toFixed(2));
                        flvdata0 = data[data.length-1];
                        Log.debug('appending last  level/sn/type/dts/pts/position:' + flvdata0.fragLevel + '/' + flvdata0.fragSN + '/' + flvdata0.tag.typeString + '/' + flvdata0.tag.dts + '/' + flvdata0.tag.pts + '/' + (flvdata0.positionAbsolute - _liveSlidingMain).toFixed(2));
                    }
                    (_hls.stream as HLSNetStream).appendTags(tags);
                }
            }
            // clip backbuffer if needed
            if (HLSSettings.maxBackBufferLength > 0) {
                _clipBackBuffer(HLSSettings.maxBackBufferLength);
            }
        }

        /* filter incoming tags overlapping with existing buffer */
        private function filterOverlappingTags(tags : Vector.<FLVTag>, overlap : Number) : Vector.<FLVTag> {
            var aacIdx : int,avcIdx : int,disIdx : int,metIdx : int, videoIdx : int,audioIdx : int, lastIdx : int;
            var first_pts : Number = tags[0].pts + 1000*overlap;
            var audio_dts : Number, video_dts : Number;
            var flvData : FLVData = getNextTag(false);
            if(flvData) {
                first_pts = Math.max(first_pts,flvData.tag.pts);
            }
            var filteredTags : Vector.<FLVTag>=  new Vector.<FLVTag>();
            var filteredDuration : Number = 0;
            aacIdx = avcIdx = disIdx = metIdx = videoIdx = audioIdx = lastIdx = -1;
            audio_dts = video_dts = Number.POSITIVE_INFINITY;
            for (var i : int = 0; i < tags.length; i++) {
                var tag : FLVTag = tags[i];
                switch(tag.type) {
                    case FLVTag.DISCONTINUITY:
                        // retrieve last DISCONTINUITY tag before first_pts
                        if (tag.pts <= first_pts)
                            disIdx = i;
                        break;
                    case FLVTag.METADATA:
                        // retrieve last METADATA tag before first_pts
                        if (tag.pts <= first_pts)
                            metIdx = i;
                        break;
                    case FLVTag.AAC_HEADER:
                        // retrieve last AAC_HEADER tag before first_pts
                        if (tag.pts <= first_pts)
                            aacIdx = i;
                        break;
                    case FLVTag.AVC_HEADER:
                        // retrieve last AVC_HEADER tag before first_pts
                        if (tag.pts <= first_pts)
                            avcIdx = i;
                        break;
                    case FLVTag.AVC_NALU:
                        // retrieve first keyframe after first_pts
                        if (videoIdx == -1 && tag.pts > first_pts && tag.keyframe) {
                            lastIdx = videoIdx = i;
                            video_dts = tag.dts;
                            CONFIG::LOGGING {
                                Log.debug("found AVC_NALU_K after overlap @PTS:" + tag.pts);
                            }
                        } else if (videoIdx >=0) {
                            lastIdx = i;
                        }
                         break;
                    case FLVTag.MP3_RAW:
                    case FLVTag.AAC_RAW:
                        // retrieve first frame after first_pts
                        if (audioIdx == -1 && tag.pts >  first_pts) {
                            lastIdx = audioIdx = i;
                            audio_dts = tag.dts;
                        } else if (audioIdx >=0) {
                            lastIdx = i;
                        }
                        break;
                    default:
                        break;
                }
            }

            if(lastIdx >=0) {
                filteredDuration = tags[lastIdx].pts - first_pts;
                CONFIG::LOGGING {
                    Log.debug("filterOverlappingTags : filtered duration:" + Math.round(filteredDuration));
                }
            }

            // only push tags, if we found more than 500ms of tags AND
            // (audio not expected OR audio tags found) AND
            // (video not expected OR video tags found)
            if((!audioExpected || audioIdx>=0) &&
               (!videoExpected || videoIdx>=0) &&
                filteredDuration > 500) {
                // modify PTS for DISCONTINUITY/METADATA/AAC_HEADER/AVC_HEADER tag
                // and push as filtered tag
                if(disIdx >=0) {
                    tags[disIdx].pts = tags[disIdx].dts = Math.min(audio_dts,video_dts);
                    filteredTags.push(tags[disIdx]);
                }

                if(metIdx >=0) {
                    tags[metIdx].pts = tags[metIdx].dts = Math.min(audio_dts,video_dts);
                    filteredTags.push(tags[metIdx]);
                }
                if(aacIdx >=0) {
                    tags[aacIdx].pts = tags[aacIdx].dts = audio_dts;
                    filteredTags.push(tags[aacIdx]);
                }
                if(avcIdx >=0) {
                    tags[avcIdx].pts = tags[avcIdx].dts = video_dts;
                    filteredTags.push(tags[avcIdx]);
                }

                CONFIG::LOGGING {
                    Log.debug("filterOverlappingTags : header appended:" + filteredTags.length);
                }

                for (i= 0; i < tags.length; i++) {
                    tag = tags[i];
                    switch(tag.type) {
                        case FLVTag.AVC_NALU:
                            // push all AVC NALU located after videoIdx
                            if (videoIdx >= 0 && i >=  videoIdx) {
                                filteredTags.push(tag);
                            }
                             break;
                        case FLVTag.MP3_RAW:
                        case FLVTag.AAC_RAW:
                            // push all audio tags located after audioIdx
                            if (audioIdx >=0 && i >=  audioIdx) {
                                filteredTags.push(tag);
                            }
                            break;
                        default:
                            break;
                    }
                }
                CONFIG::LOGGING {
                    Log.debug("filterOverlappingTags : tags appended:" + filteredTags.length);
                }
            } else {
                CONFIG::LOGGING {
                    Log.debug("filterOverlappingTags: only overlapping tags found");
                }
            }
            CONFIG::LOGGING {
                Log.debug("filterOverlappingTags: filtered " + filteredTags.length + " out of " + tags.length);
                // for (i= 0; i < filteredTags.length; i++) {
                //     Log.debug("filteredOverlappingTags: filtered " + filteredTags[i].typeString + '/' + filteredTags[i].dts + '/' + filteredTags[i].pts);
                // }
            }
            return filteredTags;
        }


        /* filter/tweak tags to seek accurately into the stream */
        private function seekFilterTags(tags : Vector.<FLVData>, absoluteStartPosition : Number) : Vector.<FLVData> {
            var aacIdx : int,avcIdx : int,disIdx : int,metIdxMain : int,metIdxAltAudio : int, keyIdx : int,lastIdx : int;
            aacIdx = avcIdx = disIdx = metIdxMain = metIdxAltAudio = keyIdx = lastIdx = -1;
            var filteredTags : Vector.<FLVData>=  new Vector.<FLVData>();
            var idx2Clone : Vector.<int> = new Vector.<int>();

            // loop through all tags and find index position of header tags located before start position
            while(lastIdx ==-1) {
                for (var i : int = 0; i < tags.length; i++) {
                    var data : FLVData = tags[i];
                    if (data.positionAbsolute <= absoluteStartPosition) {
                        lastIdx = i;
                        // current tag is before requested start position
                        // grab AVC/AAC/DISCONTINUITY/METADATA/KEYFRAMES tag located just before
                        switch(data.tag.type) {
                            case FLVTag.DISCONTINUITY:
                                disIdx = i;
                                break;
                            case FLVTag.METADATA:
                                if(data.loaderType == HLSLoaderTypes.FRAGMENT_MAIN) {
                                    metIdxMain = i;
                                } else {
                                    metIdxAltAudio = i;
                                }
                                break;
                            case FLVTag.AAC_HEADER:
                                aacIdx = i;
                                break;
                            case FLVTag.AVC_HEADER:
                                avcIdx = i;
                                break;
                            case FLVTag.AVC_NALU:
                                if (data.tag.keyframe) keyIdx = i;
                            default:
                                break;
                        }
                    } else {
                        break;
                    }
                }
                if(lastIdx == -1) {
                    // all filtered tags are located after seek position ... tweak start position
                    CONFIG::LOGGING {
                        Log.warn("seekFilterTags: startPosition > first tag position:" + absoluteStartPosition.toFixed(3) + '/' + tags[0].positionAbsolute.toFixed(3));
                    }
                    if(absoluteStartPosition != tags[0].positionAbsolute) {
                        absoluteStartPosition = tags[0].positionAbsolute;
                    } else {
                        // nothing found yet, let's return empty an array
                        return filteredTags;
                    }
                }
            }

            if (keyIdx == -1) {
                // audio only stream, no keyframe. we can seek accurately
                keyIdx = lastIdx;
            }

            var firstIdx : int;
            if (HLSSettings.seekMode == HLSSeekMode.ACCURATE_SEEK) {
                // start injecting from last tag before start position
                firstIdx = lastIdx;
            } else {
                // start injecting from keyframe tag
                firstIdx = keyIdx;
            }

            var firstPTS : Number = tags[firstIdx].tag.pts;
            var firstDTS : Number = tags[firstIdx].tag.dts;
            _seekPositionReal = tags[firstIdx].positionAbsolute;

            // inject discontinuity/metadata/AVC header/AAC header if available
            if (disIdx != -1)  idx2Clone.push(disIdx);
            if (metIdxMain != -1)  idx2Clone.push(metIdxMain);
            if (metIdxAltAudio != -1)  idx2Clone.push(metIdxAltAudio);
            if (aacIdx != -1)  idx2Clone.push(aacIdx);
            if (avcIdx != -1)  idx2Clone.push(avcIdx);

            for each (i in idx2Clone) {
                data = tags[i];
                var tagclone : FLVTag = data.tag.clone();
                // use keyframe DTS as reference timestamp
                tagclone.dts = firstDTS;
                if(tagclone.type == FLVTag.AVC_HEADER) {
                    // for video, PTS could be different than PTS, use correct PTS value
                    tagclone.pts = firstPTS;
                } else {
                    // for other tags, force PTS = DTS
                    tagclone.pts = firstDTS;
                }
                var dataclone : FLVData = new FLVData(tagclone, _seekPositionReal, 0, data.continuity, data.loaderType, data.fragIdx, data.fragLevel, data.fragSN);
                filteredTags.push(dataclone);
            }

            // inject tags from nearest keyframe to start position
            for (i = keyIdx; i < lastIdx; i++) {
                data = tags[i];
                // if accurate seek mode, adjust tags with pts and position from start position
                if (HLSSettings.seekMode == HLSSeekMode.ACCURATE_SEEK) {
                    // only push NALU to be able to reconstruct frame at seek position
                    if (data.tag.type == FLVTag.AVC_NALU) {
                        tagclone = data.tag.clone();
                        tagclone.pts = tagclone.dts = firstPTS;
                        dataclone = new FLVData(tagclone, _seekPositionReal, 0, data.continuity, data.loaderType, data.fragIdx, data.fragLevel, data.fragSN);
                        filteredTags.push(dataclone);
                    }
                } else {
                    // keyframe seeking : push straight away
                    filteredTags.push(data);
                }
            }
            // tags located after start position, push straight away
            for (i = lastIdx; i < tags.length; i++) {
                filteredTags.push(tags[i]);
            }
            return filteredTags;
        }

        private function _clipBackBuffer(maxBackBufferLength : Number) : void {
            /*      min_pos      last video
             *                 keyframe before                      current
                             maxBackBufferLength                    position
             *        *--------------K-----------*---------------------*----
             *         <------------><-----------><-------------------->
             *         to be clipped   to be kept        maxBackBufferLength
             */

            // determine clipping position. don't keep negative position (which are out of sliding window and not seekable)
            var clipping_position0 : Number = Math.max(0,position - maxBackBufferLength);
            var clipping_position : Number;

            if(videoExpected) {
                // find last video keyframe before clipping_position : loop through video tags and find last keyframe before clipping position
                for each (var data : FLVData in _videoTags) {
                    if ((data.positionAbsolute - _liveSlidingMain ) <= clipping_position0 && data.tag.keyframe) {
                        clipping_position = data.positionAbsolute - _liveSlidingMain;
                    }
                }

                if(isNaN(clipping_position)) {
                    return;
                }
            } else {
                clipping_position = clipping_position0;
            }
            // don't clip if clipping position is greater than current position ! this could happen on live stream with long pause
            if(clipping_position >= position) {
                return;
            }

            // CONFIG::LOGGING {
            //     if (clipping_position != clipping_position0) {
            //         Log.info("clipping_position/clipping_position0 " + clipping_position + '/' + clipping_position0);
            //     }
            // }

            var clipped_tags : uint = 0;

            // loop through each tag list and clip tags if out of max back buffer boundary
            while (_audioTags.length && (_audioTags[0].positionAbsolute - _liveSlidingMain ) < clipping_position) {
                _audioTags.shift();
                _audioIdx--;
                clipped_tags++;
            }

            while (_videoTags.length && (_videoTags[0].positionAbsolute - _liveSlidingMain ) < clipping_position) {
                _videoTags.shift();
                _videoIdx--;
                clipped_tags++;
            }

            while (_metaTags.length && (_metaTags[0].positionAbsolute - _liveSlidingMain ) < clipping_position) {
                _metaTags.shift();
                _metaIdx--;
                clipped_tags++;
            }

            /* clip header tags : the tricky thing here is that we need to keep the last AAC HEADER / AVC HEADER before clip position
             * if we dont keep these tags, we will have audio/video playback issues when seeking into the buffer
             *
             * so we loop through all header tags and we retrieve the position of last AAC/AVC header tags
             * then if any subsequent header tag is found after clipping position, we create a new Vector in which we first append the previously
             * found AAC/AVC header
             *
             */
            var _aacHeader : FLVData;
            var _avcHeader : FLVData;
            var _disHeader : FLVData;
            var headercounter : uint = 0;
            var _newheaderTags : Vector.<FLVData> = new Vector.<FLVData>();
            for each (data  in _headerTags) {
                if ((data.positionAbsolute - _liveSlidingMain ) < clipping_position) {
                    headercounter++;
                    switch(data.tag.type) {
                        case FLVTag.DISCONTINUITY:
                            _disHeader = data;
                            break;
                        case FLVTag.AAC_HEADER:
                            _aacHeader = data;
                            break;
                        case FLVTag.AVC_HEADER:
                            _avcHeader = data;
                        default:
                            break;
                    }
                } else {
                    /* push header tags located after clipping position into a new array */
                    _newheaderTags.push(data);
                }
            }

            /* now push any DISCONTINUITY/AVC HEADER/AAC HEADER tag located before the clip position */
            if (_disHeader) {
                headercounter--;
                // Log.info("push DISCONTINUITY header tags/position:" + _disHeader.position);
                _disHeader.position = clipping_position;
                _disHeader.sliding = 0;
                _newheaderTags.unshift(_disHeader);
            }
            if (_aacHeader) {
                headercounter--;
                // Log.info("push AAC header tags/position:" + _aacHeader.position);
                _aacHeader.position = clipping_position;
                _aacHeader.sliding = 0;
                _newheaderTags.unshift(_aacHeader);
            }
            if (_avcHeader) {
                headercounter--;
                // Log.info("push AVC header tags/position:" + _avcHeader.position);
                _avcHeader.position = clipping_position;
                _avcHeader.sliding = 0;
                _newheaderTags.unshift(_avcHeader);
            }

            if (headercounter != 0) {
                // Log.info("clipped " + headercounter + " header tags");
                _headerTags = _newheaderTags;
                // we need to adjust headerIdx, as the size of the Vector has been adjusted
                _headerIdx -= headercounter;
                clipped_tags += headercounter;
            }

            CONFIG::LOGGING {
                if (clipped_tags > 0) {
                    Log.debug2("clipped " + clipped_tags + " tags, clipping_position0/clipping_position/position/new backBufferLength :" + clipping_position0.toFixed(3) + '/' + clipping_position.toFixed(3) + '/' + position.toFixed(3) + '/'  + (position-clipping_position).toFixed(3));
                }
            }
        }

        private function _playlistDurationUpdated(event : HLSEvent) : void {
            _playlistDuration = event.duration;
        }

        private function getbuflen(tags : Vector.<FLVData>, startIdx : uint) : Number {
            if (tags.length > startIdx) {
                var startPos : Number = tags[startIdx].positionAbsolute;
                var endPos : Number = tags[tags.length - 1].positionAbsolute;
                return (endPos - startPos);
            } else {
                return 0;
            }
        }

        /*
         * retrieve next tag and update pointer, using the following priority :
         * smallest continuity
         * then smallest dts
         * then header  then video then audio then metadata tags
         */
        private function getNextTag(shift : Boolean) : FLVData {
            var mtag : FLVData ,vtag : FLVData,atag : FLVData, htag : FLVData;

            var continuity : int = int.MAX_VALUE;
            // find smallest continuity counter
            if (_headerTags.length > _headerIdx) {
                htag = _headerTags[_headerIdx];
                continuity = Math.min(continuity, htag.continuity);
            }
            if (_videoTags.length > _videoIdx) {
                vtag = _videoTags[_videoIdx];
                continuity = Math.min(continuity, vtag.continuity);
            }
            if (_audioTags.length > _audioIdx) {
                atag = _audioTags[_audioIdx];
                continuity = Math.min(continuity, atag.continuity);
            }
            if (_metaTags.length > _metaIdx) {
                mtag = _metaTags[_metaIdx];
                continuity = Math.min(continuity, mtag.continuity);
            }
            if (continuity == int.MAX_VALUE)
                return null;

            var dts : Number = Number.MAX_VALUE;
            // for this continuity counter, find smallest DTS

            if (htag && htag.continuity == continuity) dts = Math.min(dts, htag.tag.dts);
            if (vtag && vtag.continuity == continuity) dts = Math.min(dts, vtag.tag.dts);
            if (atag && atag.continuity == continuity) dts = Math.min(dts, atag.tag.dts);
            if (mtag && mtag.continuity == continuity) dts = Math.min(dts, mtag.tag.dts);

            // for this continuity counter, this DTS, prioritize tags with the following order : header/metadata/video/audio
            if (htag && htag.continuity == continuity && htag.tag.dts == dts) {
                if(shift) _headerIdx++;
                return htag;
            }
            if (mtag && mtag.continuity == continuity && mtag.tag.dts == dts) {
                if(shift) _metaIdx++;
                return mtag;
            }
            if (vtag && vtag.continuity == continuity && vtag.tag.dts == dts) {
                if(shift) _videoIdx++;
                return vtag;
            } else {
                if(shift) _audioIdx++;
                return atag;
            }
        }

        // shift tags until different frag
        private function shiftMultipleTags(maxDuration : Number) : Vector.<FLVData> {
            var tags : Vector.<FLVData>=  new Vector.<FLVData>(),
                mainIdx : uint = 0,
                altIdx : uint = 0,
                firstPos : Number = undefined,
                tagPos : Number,
                tag : FLVData,
                isMain : Boolean,
                tagIdx : uint;

            // get tag, without shifting it
             while ((tag = getNextTag(false)) != null) {
                isMain = (tag.loaderType == HLSLoaderTypes.FRAGMENT_MAIN);
                tagIdx = tag.fragIdx;
                tagPos = tag.positionAbsolute;
                if(isNaN(firstPos)) {
                    firstPos = tagPos;
                }
                if(isMain && !mainIdx) {
                    // first tag coming from main frag loader, store idx
                    mainIdx = tagIdx;
                } else if(!isMain && !altIdx) {
                    // first tag coming from alt audio frag loader, store idx
                    altIdx = tagIdx;
                } else {
                    // not first tag, ensure idx is within same frag, otherwise exit
                    if ( (tagPos - firstPos) > maxDuration && ((isMain && tagIdx != mainIdx) || (!isMain && tagIdx != altIdx))) {
                        // CONFIG::LOGGING {
                        //     Log.info('discard level/sn/type/dts/pts:' + tag.fragLevel + '/' + tag.fragSN + '/' + tag.tag.typeString + '/' + tag.tag.dts + '/' + tag.tag.pts);
                        // }
                        break;
                    }
                }
                // tag matches, shift it and push it in new array
                tags.push(getNextTag(true));
            }
            return tags;
        }

        private function get min_pos() : Number {
            if (audioExpected) {
                if (videoExpected) {
                    return Math.max(min_audio_pos, min_video_pos);
                } else {
                    return min_audio_pos;
                }
            } else {
                return min_video_pos;
            }
        }

        private function get min_min_pos() : Number {
            if (audioExpected) {
                if (videoExpected) {
                    return Math.min(min_audio_pos, min_video_pos);
                } else {
                    return min_audio_pos;
                }
            } else {
                return min_video_pos;
            }
        }

        private function get min_audio_pos() : Number {
            var min_pos_ : Number = Number.POSITIVE_INFINITY;
            if (_audioTags.length) min_pos_ = Math.min(min_pos_, _audioTags[0].positionAbsolute - _liveSlidingMain );
            return min_pos_;
        }

        private function get min_video_pos() : Number {
            var min_pos_ : Number = Number.POSITIVE_INFINITY;
            if (_videoTags.length) min_pos_ = Math.min(min_pos_, _videoTags[0].positionAbsolute - _liveSlidingMain );
            return min_pos_;
        }

        private function get max_pos() : Number {
            if (audioExpected) {
                if (videoExpected) {
                    return Math.min(max_audio_pos, max_video_pos);
                } else {
                    return max_audio_pos;
                }
            } else {
                return max_video_pos;
            }
        }

        private function get max_audio_pos() : Number {
            var max_pos_ : Number = Number.NEGATIVE_INFINITY;
            if (_audioTags.length) max_pos_ = Math.max(max_pos_, _audioTags[_audioTags.length - 1].positionAbsolute - _liveSlidingMain );
            return max_pos_;
        }

        private function get max_video_pos() : Number {
            var max_pos_ : Number = Number.NEGATIVE_INFINITY;
            if (_videoTags.length) max_pos_ = Math.max(max_pos_, _videoTags[_videoTags.length - 1].positionAbsolute - _liveSlidingMain );
            return max_pos_;
        }

        private function _lastVODFragmentLoadedHandler(event : HLSEvent) : void {
            CONFIG::LOGGING {
                Log.debug("last fragment loaded");
            }
            _reachedEnd = true;
        }

        private function _audioTrackChange(event : HLSEvent) : void {
            CONFIG::LOGGING {
                Log.debug("StreamBuffer : audio track changed, flushing audio buffer:" + event.audioTrack);
            }
            flushAudio();
        }

        /** monitor fragment loader stall events, arm a boolean  **/
        private function _liveLoadingStalledHandler(event : HLSEvent) : void {
            _liveLoadingStalled = true;
        }

        /** playback complete handler **/
        private function _playbackComplete(event : HLSEvent) : void {
            // reset lastMediaTimeUpdate to quickly trigger a MEDIA_TIME event
            _lastMediaTimeUpdate = 0;
            _playbackCompleted = true;
        }
    }
}

import org.mangui.hls.constant.HLSLoaderTypes;
import org.mangui.hls.flv.FLVTag;


class FLVData {
    public var tag : FLVTag;
    public var position : Number;
    public var sliding : Number;
    public var continuity : int;
    public var loaderType : int;
    public var fragIdx : int;
    public var fragLevel : int;
    public var fragSN : int;
    public static var refPTSMain : Number;
    public static var refPTSAltAudio : Number;

    public function FLVData(tag : FLVTag, position : Number, sliding : Number, continuity : int, loaderType : int, fragIdx : int, fragLevel : int, fragSN : int) {
        this.tag = tag;
        // relative position
        this.position = position;
        this.sliding = sliding;
        this.continuity = continuity;
        this.loaderType = loaderType;
        this.fragIdx = fragIdx;
        this.fragLevel = fragLevel;
        this.fragSN = fragSN;
        switch(loaderType) {
            case HLSLoaderTypes.FRAGMENT_MAIN:
                if(isNaN(refPTSMain)) {
                    refPTSMain = tag.pts - 1000*position - sliding;
                }
                break;
            case HLSLoaderTypes.FRAGMENT_ALTAUDIO:
            default:
                if(isNaN(refPTSAltAudio)) {
                    refPTSAltAudio = tag.pts - 1000*position - sliding;
                }
                break;
        }
    }

    // return absolute tag position, compared to beginning of playback and main playlist
    public function get positionAbsolute() : Number {
        var pos : Number = this.position + this.sliding;
        // if this tag is altaudio, and we know ref PTS for main, we need to offset absolute pos by PTS difference
        if(loaderType == HLSLoaderTypes.FRAGMENT_ALTAUDIO && !isNaN(refPTSMain)) {
            pos += (refPTSAltAudio - refPTSMain)/1000;
        }
        return pos;
    }
}
