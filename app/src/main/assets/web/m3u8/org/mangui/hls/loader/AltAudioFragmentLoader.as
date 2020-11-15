/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.loader {

    import flash.events.*;
    import flash.net.*;
    import flash.utils.ByteArray;
    import flash.utils.getTimer;
    import flash.utils.Timer;
    import org.mangui.hls.constant.HLSLoaderTypes;
    import org.mangui.hls.constant.HLSTypes;
    import org.mangui.hls.demux.Demuxer;
    import org.mangui.hls.demux.DemuxHelper;
    import org.mangui.hls.demux.ID3Tag;
    import org.mangui.hls.event.HLSError;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.event.HLSLoadMetrics;
    import org.mangui.hls.flv.FLVTag;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;
    import org.mangui.hls.model.AudioTrack;
    import org.mangui.hls.model.Fragment;
    import org.mangui.hls.model.FragmentData;
    import org.mangui.hls.model.Level;
    import org.mangui.hls.stream.StreamBuffer;
    import org.mangui.hls.utils.AES;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
        import org.mangui.hls.utils.Hex;
    }
    /** Class that fetches alt audio fragments. **/
    public class AltAudioFragmentLoader {
        /** Reference to the HLS controller. **/
        private var _hls : HLS;
        /** Util for loading the fragment. **/
        private var _fragstreamloader : URLStream;
        /** Util for loading the key. **/
        private var _keystreamloader : URLStream;
        /** key map **/
        private var _keymap : Object;
        /** Did a discontinuity occurs in the stream **/
        private var _hasDiscontinuity : Boolean;
        /** Timer used to monitor/schedule fragment download. **/
        private var _timer : Timer;
        /** requested seek position **/
        private var _seekPosition : Number;
        /** first fragment loaded ? **/
        private var _fragmentFirstLoaded : Boolean;
        /* demux instance */
        private var _demux : Demuxer;
        /* stream buffer instance **/
        private var _streamBuffer : StreamBuffer;
        /* key error/reload */
        private var _keyLoadErrorDate : Number;
        private var _keyRetryTimeout : Number;
        private var _keyRetryCount : int;
        private var _keyLoadStatus : int;
        /* fragment error/reload */
        private var _fragLoadErrorDate : Number;
        private var _fragRetryTimeout : Number;
        private var _fragRetryCount : int;
        private var _fragLoadStatus : int;
        /** reference to audio level */
        private var _level : Level;
        /** reference to previous/current fragment */
        private var _fragPrevious : Fragment;
        private var _fragCurrent : Fragment;
        /* loading state variable */
        private var _loadingState : int;
        /* loading metrics */
        private var _metrics : HLSLoadMetrics;
        private static const LOADING_STOPPED : int = -1;
        private static const LOADING_IDLE : int = 0;
        private static const LOADING_IN_PROGRESS : int = 1;
        private static const LOADING_WAITING_LEVEL_UPDATE : int = 2;
        private static const LOADING_STALLED : int = 3;
        private static const LOADING_FRAGMENT_IO_ERROR : int = 4;
        private static const LOADING_KEY_IO_ERROR : int = 5;
        private static const LOADING_COMPLETED : int = 6;

        /** Create the loader. **/
        public function AltAudioFragmentLoader(hls : HLS, streamBuffer : StreamBuffer) : void {
            _hls = hls;
            _streamBuffer = streamBuffer;
            _timer = new Timer(20, 0);
            _timer.addEventListener(TimerEvent.TIMER, _checkLoading);
            _loadingState = LOADING_STOPPED;
            _keymap = new Object();
        };

        public function dispose() : void {
            stop();
            _timer.removeEventListener(TimerEvent.TIMER, _checkLoading);
            _loadingState = LOADING_STOPPED;
            _keymap = new Object();
        }

        /** update state and level in case of audio level loaded event **/
        private function _audioLevelLoadedHandler(event : HLSEvent) : void {
            if (_loadingState == LOADING_WAITING_LEVEL_UPDATE || _loadingState == LOADING_IDLE) {
                _loadingState = LOADING_IDLE;
                _level = _hls.audioTracks[_hls.audioTrack].level;
                // speed up loading of new fragment
                _timer.start();
            }
        };

        /**  fragment loading Timer **/
        private function _checkLoading(e : Event) : void {
            switch(_loadingState) {
                // nothing to load, stop fragment loader.
                case LOADING_STOPPED:
                    stop();
                    break;
                // nothing to load until level is retrieved
                case LOADING_WAITING_LEVEL_UPDATE:
                // loading already in progress
                case LOADING_IN_PROGRESS:
                    break;
                // no loading in progress, try to load first/next fragment
                case LOADING_IDLE:
                    if (_level) {
                        if (_fragmentFirstLoaded == false) {
                            // just after seek, load first fragment
                            _loadingState = _loadfirstfragment(_seekPosition);
                        } else {
                            if (HLSSettings.maxBufferLength == 0 || _streamBuffer.audioBufferLength < HLSSettings.maxBufferLength) {
                                _loadingState = _loadnextfragment(_fragPrevious);
                            }
                        }
                    } else {
                        // playlist not yet received
                        CONFIG::LOGGING {
                            Log.debug("_checkLoading : playlist not received for audio level:" + _hls.audioTrack);
                        }
                        _loadingState = LOADING_WAITING_LEVEL_UPDATE;
                    }
                    break;
                case LOADING_STALLED:
                    /* next consecutive fragment not found:
                    it could happen on live playlist :
                    - if bandwidth available is lower than lowest quality needed bandwidth
                    - after long pause
                    */
                    CONFIG::LOGGING {
                        Log.warn("loading stalled:stop fragment loading");
                    }
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.LIVE_LOADING_STALLED));
                    stop();
                    break;
                // if key loading failed
                case  LOADING_KEY_IO_ERROR:
                    // compare current date and next retry date.
                    if (getTimer() >= _keyLoadErrorDate) {
                        /* try to reload the key ...
                        calling _loadfragment will also reload key */
                        _loadfragment(_fragCurrent);
                        _loadingState = LOADING_IN_PROGRESS;
                    }
                    break;
                // if fragment loading failed
                case LOADING_FRAGMENT_IO_ERROR:
                    // compare current date and next retry date.
                    if (getTimer() >= _fragLoadErrorDate) {
                        /* try to reload fragment ... */
                        _loadfragment(_fragCurrent);
                        _loadingState = LOADING_IN_PROGRESS;
                    }
                    break;
                case LOADING_COMPLETED:
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.LAST_VOD_FRAGMENT_LOADED));
                    // stop fragment loader, as no other fragments can be loaded
                    stop();
                    break;
                default:
                    CONFIG::LOGGING {
                        Log.error("invalid audio loading state:" + _loadingState);
                    }
                    break;
            }
        }

        public function seek(position : Number) : void {
            // reset IO Error when seeking
            _fragRetryCount = _keyRetryCount = 0;
            _fragRetryTimeout = _keyRetryTimeout = 1000;
            _loadingState = LOADING_IDLE;
            _seekPosition = position;
            _fragmentFirstLoaded = false;
            _fragPrevious = null;
            _level = _hls.audioTracks[_hls.audioTrack].level;
            _hls.addEventListener(HLSEvent.AUDIO_LEVEL_LOADED, _audioLevelLoadedHandler);
            _timer.start();
        }

        /** key load completed. **/
        private function _keyLoadCompleteHandler(event : Event) : void {
            if (_loadingState == LOADING_IDLE)
                return;
            CONFIG::LOGGING {
                Log.debug("key loading completed");
            }
            var hlsError : HLSError;
            // Collect key data
            if ( _keystreamloader.bytesAvailable == 16 ) {
                // load complete, reset retry counter
                _keyRetryCount = 0;
                _keyRetryTimeout = 1000;
                var keyData : ByteArray = new ByteArray();
                _keystreamloader.readBytes(keyData, 0, 0);
                _keymap[_fragCurrent.decrypt_url] = keyData;
                // now load fragment
                try {
                    CONFIG::LOGGING {
                        Log.debug("loading audio fragment:" + _fragCurrent.url);
                    }
                    _fragCurrent.data.bytes = null;
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.FRAGMENT_LOADING, _fragCurrent.url));
                    _fragstreamloader.load(new URLRequest(_fragCurrent.url));
                } catch (error : Error) {
                    hlsError = new HLSError(HLSError.FRAGMENT_LOADING_ERROR, _fragCurrent.url, error.message);
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
                }
            } else {
                hlsError = new HLSError(HLSError.KEY_PARSING_ERROR, _fragCurrent.decrypt_url, "invalid key size: received " + _keystreamloader.bytesAvailable + " / expected 16 bytes");
                _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
            }
        };

        private function _keyLoadHTTPStatusHandler(event : HTTPStatusEvent) : void {
            _keyLoadStatus = event.status;
        }

        private function _keyhandleIOError(message : String) : void {
            CONFIG::LOGGING {
                Log.error("I/O Error while loading key:" + message);
            }
            if (HLSSettings.keyLoadMaxRetry == -1 || _keyRetryCount < HLSSettings.keyLoadMaxRetry) {
                _loadingState = LOADING_KEY_IO_ERROR;
                _keyLoadErrorDate = getTimer() + _keyRetryTimeout;
                CONFIG::LOGGING {
                    Log.warn("retry key load in " + _keyRetryTimeout + " ms, count=" + _keyRetryCount);
                }
                /* exponential increase of retry timeout, capped to keyLoadMaxRetryTimeout */
                _keyRetryCount++;
                _keyRetryTimeout = Math.min(HLSSettings.keyLoadMaxRetryTimeout, 2 * _keyRetryTimeout);
            } else {
                var hlsError : HLSError = new HLSError(HLSError.KEY_LOADING_ERROR, _fragCurrent.decrypt_url, "I/O Error :" + message);
                _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
            }
        }

        private function _fraghandleIOError(message : String) : void {
            /* usually, errors happen in two situations :
            - bad networks  : in that case, the second or third reload of URL should fix the issue
            - live playlist : when we are trying to load an out of bound fragments : for example,
            the playlist on webserver is from SN [51-61]
            the one in memory is from SN [50-60], and we are trying to load SN50.
            we will keep getting 404 error if the HLS server does not follow HLS spec,
            which states that the server should keep SN50 during EXT-X-TARGETDURATION period
            after it is removed from playlist
            in the meantime, ManifestLoader will keep refreshing the playlist in the background ...
            so if the error still happens after EXT-X-TARGETDURATION, it means that there is something wrong
            we need to report it.
             */
            CONFIG::LOGGING {
                Log.error("I/O Error while loading fragment:" + message);
            }
            if (HLSSettings.fragmentLoadMaxRetry == -1 || _fragRetryCount < HLSSettings.fragmentLoadMaxRetry) {
                _loadingState = LOADING_FRAGMENT_IO_ERROR;
                _fragLoadErrorDate = getTimer() + _fragRetryTimeout;
                CONFIG::LOGGING {
                    Log.warn("retry fragment load in " + _fragRetryTimeout + " ms, count=" + _fragRetryCount);
                }
                /* exponential increase of retry timeout, capped to fragmentLoadMaxRetryTimeout */
                _fragRetryCount++;
                _fragRetryTimeout = Math.min(HLSSettings.fragmentLoadMaxRetryTimeout, 2 * _fragRetryTimeout);
            } else {
                if(HLSSettings.fragmentLoadSkipAfterMaxRetry == true) {
                    /* check if loaded fragment is not the last one of a live playlist.
                        if it is the case, don't skip to next, as there is no next fragment :-)
                    */
                    if(_hls.type == HLSTypes.LIVE && _fragCurrent.seqnum == _level.end_seqnum) {
                        _loadingState = LOADING_FRAGMENT_IO_ERROR;
                        _fragLoadErrorDate = getTimer() + _fragRetryTimeout;
                        CONFIG::LOGGING {
                            Log.warn("max load retry reached on last fragment of live playlist, retrying loading this one...");
                        }
                        /* exponential increase of retry timeout, capped to fragmentLoadMaxRetryTimeout */
                        _fragRetryCount++;
                        _fragRetryTimeout = Math.min(HLSSettings.fragmentLoadMaxRetryTimeout, 2 * _fragRetryTimeout);
                    } else {
                        CONFIG::LOGGING {
                            Log.warn("max fragment load retry reached, skip fragment and load next one");
                        }
                        _fragRetryCount = 0;
                        _fragRetryTimeout = 1000;
                        _fragPrevious = _fragCurrent;
                        // set fragment first loaded to be true to ensure that we can skip first fragment as well
                        _fragmentFirstLoaded = true;
                        _loadingState = LOADING_IDLE;
                    }
                } else {
                    var hlsError : HLSError = new HLSError(HLSError.FRAGMENT_LOADING_ERROR, _fragCurrent.url, "I/O Error :" + message);
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
                }
            }
        }

        private function _fragLoadHTTPStatusHandler(event : HTTPStatusEvent) : void {
            _fragLoadStatus = event.status;
        }

        private function _fragLoadProgressHandler(event : ProgressEvent) : void {
            var fragData : FragmentData = _fragCurrent.data;
            if (fragData.bytes == null) {
                fragData.bytes = new ByteArray();
                fragData.bytesLoaded = 0;
                fragData.flushTags();
                _metrics.loading_begin_time = getTimer();

                // decrypt data if needed
                if (_fragCurrent.decrypt_url != null) {
                    _metrics.decryption_begin_time = getTimer();
                    fragData.decryptAES = new AES(_hls.stage, _keymap[_fragCurrent.decrypt_url], _fragCurrent.decrypt_iv, _fragDecryptProgressHandler, _fragDecryptCompleteHandler);
                    CONFIG::LOGGING {
                        Log.debug("init AES context");
                    }
                } else {
                    fragData.decryptAES = null;
                }
            }
            if (event.bytesLoaded > fragData.bytesLoaded
                && _fragstreamloader.bytesAvailable > 0) {  // prevent EOF error race condition
                var data : ByteArray = new ByteArray();
                _fragstreamloader.readBytes(data);
                fragData.bytesLoaded += data.length;
                // CONFIG::LOGGING {
                // Log.debug2("bytesLoaded/bytesTotal:" + event.bytesLoaded + "/" + event.bytesTotal);
                // }
                if (fragData.decryptAES != null) {
                    fragData.decryptAES.append(data);
                } else {
                    _fragDecryptProgressHandler(data);
                }
            }
        }

        /** frag load completed. **/
        private function _fragLoadCompleteHandler(event : Event) : void {
            // load complete, reset retry counter
            _fragRetryCount = 0;
            _fragRetryTimeout = 1000;
            var fragData : FragmentData = _fragCurrent.data;
            if (fragData.bytes == null) {
                CONFIG::LOGGING {
                    Log.warn("fragment size is null, invalid it and load next one");
                }
                _level.updateFragment(_fragCurrent.seqnum, false);
                _loadingState = LOADING_IDLE;
                return;
            }
            CONFIG::LOGGING {
                Log.debug("loading completed");
            }
            _metrics.loading_end_time = getTimer();
            _metrics.size = fragData.bytesLoaded;

            var _loading_duration : uint = _metrics.loading_end_time - _metrics.loading_request_time;
            CONFIG::LOGGING {
                Log.debug("Loading       duration/RTT/length/speed:" + _loading_duration + "/" + (_metrics.loading_begin_time - _metrics.loading_request_time) + "/" + _metrics.size + "/" + Math.round((8000 * _metrics.size / _loading_duration) / 1024) + " kb/s");
            }
            if (fragData.decryptAES) {
                fragData.decryptAES.notifycomplete();
            } else {
                _fragDecryptCompleteHandler();
            }
        }

        private function _fragDecryptProgressHandler(data : ByteArray) : void {
            data.position = 0;
            var fragData : FragmentData = _fragCurrent.data;
            if (_metrics.parsing_begin_time ==0) {
                _metrics.parsing_begin_time = getTimer();
            }
            var bytes : ByteArray = fragData.bytes;
            if (_fragCurrent.byterange_start_offset != -1) {
                bytes.position = bytes.length;
                bytes.writeBytes(data);
                // if we have retrieved all the data, disconnect loader and notify fragment complete
                if (bytes.length >= _fragCurrent.byterange_end_offset) {
                    if (_fragstreamloader.connected) {
                        _fragstreamloader.close();
                        _fragLoadCompleteHandler(null);
                    }
                }
                /* dont do progressive parsing of segment with byte range option */
                return;
            }
            if (_demux == null) {
                /* probe file type */
                bytes.position = bytes.length;
                bytes.writeBytes(data);
                data = bytes;
                _demux = DemuxHelper.probe(data, _level, _fragParsingAudioSelectionHandler, _fragParsingProgressHandler, _fragParsingCompleteHandler, _fragParsingErrorHandler, null, _fragParsingID3TagHandler, true);
            }
            if (_demux) {
                _demux.append(data);
            }
        }

        private function _fragDecryptCompleteHandler() : void {
            if (_loadingState == LOADING_IDLE)
                return;
            var fragData : FragmentData = _fragCurrent.data;

            if (fragData.decryptAES) {
                _metrics.decryption_end_time = getTimer();
                var decrypt_duration : Number = _metrics.decryption_end_time - _metrics.decryption_begin_time;
                CONFIG::LOGGING {
                    Log.debug("Decrypted     duration/length/speed:" + decrypt_duration + "/" + fragData.bytesLoaded + "/" + Math.round((8000 * fragData.bytesLoaded / decrypt_duration) / 1024) + " kb/s");
                }
                fragData.decryptAES = null;
            }

            // deal with byte range here
            if (_fragCurrent.byterange_start_offset != -1) {
                CONFIG::LOGGING {
                    Log.debug("trim byte range, start/end offset:" + _fragCurrent.byterange_start_offset + "/" + _fragCurrent.byterange_end_offset);
                }
                var bytes : ByteArray = new ByteArray();
                fragData.bytes.position = _fragCurrent.byterange_start_offset;
                fragData.bytes.readBytes(bytes, 0, _fragCurrent.byterange_end_offset - _fragCurrent.byterange_start_offset);
                _demux = DemuxHelper.probe(bytes, _level, _fragParsingAudioSelectionHandler, _fragParsingProgressHandler, _fragParsingCompleteHandler, _fragParsingErrorHandler, null, _fragParsingID3TagHandler, true);
                if (_demux) {
                    bytes.position = 0;
                    _demux.append(bytes);
                }
            }

            if (_demux == null) {
                CONFIG::LOGGING {
                    Log.error("unknown audio fragment type");
                    if (HLSSettings.logDebug2) {
                        fragData.bytes.position = 0;
                        var bytes2 : ByteArray = new ByteArray();
                        fragData.bytes.readBytes(bytes2, 0, 512);
                        Log.debug2("frag dump(512 bytes)");
                        Log.debug2(Hex.fromArray(bytes2));
                    }
                }
                // invalid fragment
                _fraghandleIOError("invalid audio fragment received");
                fragData.bytes = null;
                return;
            }
            fragData.bytes = null;
            _demux.notifycomplete();
        }

        /** stop loading fragment **/
        public function stop() : void {
            _stop_load();
            _hls.removeEventListener(HLSEvent.AUDIO_LEVEL_LOADED, _audioLevelLoadedHandler);
            _timer.stop();
            _loadingState = LOADING_STOPPED;
        }

        private function _stop_load() : void {
            if (_fragstreamloader && _fragstreamloader.connected) {
                _fragstreamloader.close();
            }
            if (_keystreamloader && _keystreamloader.connected) {
                _keystreamloader.close();
            }

            if (_demux) {
                _demux.cancel();
            }

            if (_fragCurrent) {
                var fragData : FragmentData = _fragCurrent.data;
                if (fragData.decryptAES) {
                    fragData.decryptAES.cancel();
                    fragData.decryptAES = null;
                }
                fragData.bytes = null;
            }
        }

        /** Catch IO and security errors. **/
        private function _keyLoadErrorHandler(event : ErrorEvent) : void {
            var txt : String;
            var code : int;
            if (event is SecurityErrorEvent) {
                txt = "Cannot load key: crossdomain access denied:" + event.text;
                code = HLSError.KEY_LOADING_CROSSDOMAIN_ERROR;
            } else {
                _keyhandleIOError("HTTP status:" + _keyLoadStatus + ",msg:" + event.text);
            }
        };

        /** Catch IO and security errors. **/
        private function _fragLoadErrorHandler(event : ErrorEvent) : void {
            if (event is SecurityErrorEvent) {
                var txt : String = "Cannot load fragment: crossdomain access denied:" + event.text;
                var hlsError : HLSError = new HLSError(HLSError.FRAGMENT_LOADING_CROSSDOMAIN_ERROR, _fragCurrent.url, txt);
                _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
            } else {
                _fraghandleIOError("HTTP status:" + _fragLoadStatus + ",msg:" + event.text);
            }
        };

        private function _loadfirstfragment(position : Number) : int {
            CONFIG::LOGGING {
                Log.debug("loadfirstaudiofragment(" + position + ")");
            }
            var frag : Fragment = _level.getFragmentBeforePosition(position);
            _hasDiscontinuity = true;
            CONFIG::LOGGING {
                Log.debug("Loading       " + frag.seqnum + " of [" + (_level.start_seqnum) + "," + (_level.end_seqnum) + "]");
            }
            _loadfragment(frag);
            return LOADING_IN_PROGRESS;
        }

        /** Load a fragment **/
        private function _loadnextfragment(frag_previous : Fragment) : int {
            CONFIG::LOGGING {
                Log.debug("loadnextaudiofragment()");
            }
            var new_seqnum : Number;
            var last_seqnum : Number = -1;
            var frag : Fragment;

            last_seqnum = frag_previous.seqnum;
            if (last_seqnum == _level.end_seqnum) {
                // if last segment of level already loaded, return
                if (_hls.type == HLSTypes.VOD) {
                    // if VOD playlist, loading is completed
                    return LOADING_COMPLETED;
                } else {
                    // if live playlist, loading is pending on manifest update
                    return LOADING_WAITING_LEVEL_UPDATE;
                }
            } else {
                // if previous segment is not the last one, increment it to get new seqnum
                new_seqnum = last_seqnum + 1;
                if (new_seqnum < _level.start_seqnum) {
                    // loading stalled ! report to caller
                    return LOADING_STALLED;
                }
            }
            frag = _level.getFragmentfromSeqNum(new_seqnum);
            if (frag == null) {
                CONFIG::LOGGING {
                    Log.warn("error trying to load audio " + new_seqnum + " of [" + (_level.start_seqnum) + "," + (_level.end_seqnum) + "]");
                }
                return LOADING_WAITING_LEVEL_UPDATE;
            } else {
              // check whether there is a discontinuity between last segment and new segment
              _hasDiscontinuity = (frag.continuity != frag_previous.continuity);
                CONFIG::LOGGING {
                    Log.debug("Loading audio " + new_seqnum + " of [" + (_level.start_seqnum) + "," + (_level.end_seqnum) + "]");
                }
                _loadfragment(frag);
                return LOADING_IN_PROGRESS;
            }
        };

        private function _loadfragment(frag : Fragment) : void {
            // postpone URLStream init before loading first fragment
            if (_fragstreamloader == null) {
                var urlStreamClass : Class = _hls.URLstream as Class;
                _fragstreamloader = (new urlStreamClass()) as URLStream;
                _fragstreamloader.addEventListener(IOErrorEvent.IO_ERROR, _fragLoadErrorHandler);
                _fragstreamloader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, _fragLoadErrorHandler);
                _fragstreamloader.addEventListener(ProgressEvent.PROGRESS, _fragLoadProgressHandler);
                _fragstreamloader.addEventListener(HTTPStatusEvent.HTTP_STATUS, _fragLoadHTTPStatusHandler);
                _fragstreamloader.addEventListener(Event.COMPLETE, _fragLoadCompleteHandler);
                _keystreamloader = (new urlStreamClass()) as URLStream;
                _keystreamloader.addEventListener(IOErrorEvent.IO_ERROR, _keyLoadErrorHandler);
                _keystreamloader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, _keyLoadErrorHandler);
                _keystreamloader.addEventListener(HTTPStatusEvent.HTTP_STATUS, _keyLoadHTTPStatusHandler);
                _keystreamloader.addEventListener(Event.COMPLETE, _keyLoadCompleteHandler);
            }
            if (_hasDiscontinuity) {
                _demux = null;
            }
            _metrics = new HLSLoadMetrics(HLSLoaderTypes.FRAGMENT_ALTAUDIO);
            _metrics.level = _level.index;
            _metrics.id = frag.seqnum;
            _metrics.loading_request_time = getTimer();
            _fragCurrent = frag;
            frag.data.auto_level = false;
            if (frag.decrypt_url != null) {
                if (_keymap[frag.decrypt_url] == undefined) {
                    // load key
                    CONFIG::LOGGING {
                        Log.debug("loading key:" + frag.decrypt_url);
                    }
                    _keystreamloader.load(new URLRequest(frag.decrypt_url));
                    return;
                }
            }
            try {
                frag.data.bytes = null;
                CONFIG::LOGGING {
                    Log.debug("loading fragment:" + frag.url);
                }
                _hls.dispatchEvent(new HLSEvent(HLSEvent.FRAGMENT_LOADING, frag.url));
                _fragstreamloader.load(new URLRequest(frag.url));
            } catch (error : Error) {
                var hlsError : HLSError = new HLSError(HLSError.FRAGMENT_LOADING_ERROR, frag.url, error.message);
                _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
            }
        }

        private function _fragParsingErrorHandler(error : String) : void {
            _stop_load();
            _fraghandleIOError(error);
        }

        private function _fragParsingID3TagHandler(id3_tags : Vector.<ID3Tag>) : void {
            _fragCurrent.data.id3_tags = id3_tags;
        }

        /** triggered by demux, it should return the audio track to be parsed */
        private function _fragParsingAudioSelectionHandler(audioTrackList : Vector.<AudioTrack>) : AudioTrack {
            return audioTrackList[0];
        }

        /** triggered when demux has retrieved some tags from fragment **/
        private function _fragParsingProgressHandler(tags : Vector.<FLVTag>) : void {
            CONFIG::LOGGING {
                Log.debug2(tags.length + " tags extracted");
            }
            var fragData : FragmentData = _fragCurrent.data;
            fragData.appendTags(tags);

            if (fragData.metadata_tag_injected == false) {
                fragData.tags.unshift(_fragCurrent.getMetadataTag());
                if (_hasDiscontinuity) {
                  fragData.tags.unshift(new FLVTag(FLVTag.DISCONTINUITY, fragData.dts_min, fragData.dts_min, false));
                }
                fragData.metadata_tag_injected = true;
            }
            // provide tags to StreamBuffer
            _streamBuffer.appendTags(HLSLoaderTypes.FRAGMENT_ALTAUDIO,_fragCurrent.level,_fragCurrent.seqnum ,fragData.tags, fragData.tag_pts_min, fragData.tag_pts_max + fragData.tag_duration, _fragCurrent.continuity, _fragCurrent.start_time + fragData.tag_pts_start_offset / 1000);
            fragData.shiftTags();
        }

        /** triggered when demux has completed fragment parsing **/
        private function _fragParsingCompleteHandler() : void {
            if (_loadingState == LOADING_IDLE)
                return;
            var hlsError : HLSError;
            var fragData : FragmentData = _fragCurrent.data;
            if (!fragData.audio_found && !fragData.video_found) {
                hlsError = new HLSError(HLSError.FRAGMENT_PARSING_ERROR, _fragCurrent.url, "error parsing fragment, no tag found");
                _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
            }
            CONFIG::LOGGING {
                if (fragData.audio_found) {
                    Log.debug("m/M audio PTS:" + fragData.pts_min_audio + "/" + fragData.pts_max_audio);
                }
            }
            // Calculate bandwidth
            _metrics.parsing_end_time = getTimer();
            CONFIG::LOGGING {
                Log.debug("Total Process duration/length/bw:" + _metrics.processing_duration + "/" + _metrics.size + "/" + Math.round(_metrics.bandwidth / 1024) + " kb/s");
            }
            try {
                CONFIG::LOGGING {
                    Log.debug("Loaded        " + _fragCurrent.seqnum + " of [" + (_level.start_seqnum) + "," + (_level.end_seqnum) + "],audio track " + _hls.audioTrack + " m/M PTS:" + fragData.pts_min + "/" + fragData.pts_max);
                }
                _level.updateFragment(_fragCurrent.seqnum, true, fragData.pts_min, fragData.pts_max + fragData.tag_duration);
                // set pts_start here, it might not be updated directly in updateFragment() if this loaded fragment has been removed from a live playlist
                fragData.pts_start = fragData.pts_min;
                _loadingState = LOADING_IDLE;
                if (fragData.tags.length) {
                    if (fragData.metadata_tag_injected == false) {
                        fragData.tags.unshift(_fragCurrent.getMetadataTag());
                        if (_hasDiscontinuity) {
                            fragData.tags.unshift(new FLVTag(FLVTag.DISCONTINUITY, fragData.dts_min, fragData.dts_min, false));
                        }
                        fragData.metadata_tag_injected = true;
                    }
                    _streamBuffer.appendTags(HLSLoaderTypes.FRAGMENT_ALTAUDIO,_fragCurrent.level,_fragCurrent.seqnum , fragData.tags, fragData.tag_pts_min, fragData.tag_pts_max + fragData.tag_duration, _fragCurrent.continuity, _fragCurrent.start_time + fragData.tag_pts_start_offset / 1000);
                    _metrics.duration = fragData.pts_max + fragData.tag_duration - fragData.pts_min;
                    _metrics.id2 = fragData.tags.length;
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.TAGS_LOADED, _metrics));
                    fragData.shiftTags();
                }
                _hls.dispatchEvent(new HLSEvent(HLSEvent.FRAGMENT_LOADED, _metrics));
                _fragmentFirstLoaded = true;
                _fragPrevious = _fragCurrent;
            } catch (error : Error) {
                hlsError = new HLSError(HLSError.OTHER_ERROR, _fragCurrent.url, error.message);
                _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
            }
            // speed up loading of new fragment
            _timer.start();
        }
    }
}
