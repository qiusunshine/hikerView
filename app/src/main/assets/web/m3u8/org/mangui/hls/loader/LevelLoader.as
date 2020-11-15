/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.loader {
    import flash.events.ErrorEvent;
    import flash.events.Event;
    import flash.events.IOErrorEvent;
    import flash.events.ProgressEvent;
    import flash.events.SecurityErrorEvent;
    import flash.net.URLLoader;
    import flash.net.URLRequest;
    import flash.utils.clearTimeout;
    import flash.utils.getTimer;
    import flash.utils.setTimeout;

    import org.mangui.hls.constant.HLSLoaderTypes;
    import org.mangui.hls.constant.HLSPlayStates;
    import org.mangui.hls.constant.HLSTypes;
    import org.mangui.hls.event.HLSError;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.event.HLSLoadMetrics;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;
    import org.mangui.hls.model.Fragment;
    import org.mangui.hls.model.Level;
    import org.mangui.hls.playlist.AltAudioTrack;
    import org.mangui.hls.playlist.DataUri;
    import org.mangui.hls.playlist.Manifest;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    public class LevelLoader {
        /** Reference to the hls framework controller. **/
        private var _hls : HLS;
        /** levels vector. **/
        private var _levels : Vector.<Level>;
        /** Object that fetches the manifest. **/
        private var _urlloader : URLLoader;
        /** Link to the M3U8 file. **/
        private var _url : String;
        /** are all playlists filled ? **/
        private var _canStart : Boolean;
        /** Timeout ID for reloading live playlists. **/
        private var _timeoutID : uint;
        /** Streaming type (live, ondemand). **/
        private var _type : String;
        /** last reload manifest time **/
        private var _reloadPlaylistTimer : uint;
        /** current load level **/
        private var _loadLevel : int;
        /** reference to manifest being loaded **/
        private var _manifestLoading : Manifest;
        /** is this loader closed **/
        private var _closed : Boolean = false;
        /* playlist retry timeout */
        private var _retryTimeout : Number;
        private var _retryCount : int;
        private var _redundantRetryCount:int;
        /* alt audio tracks */
        private var _altAudioTracks : Vector.<AltAudioTrack>;
        /* manifest load metrics */
        private var _metrics : HLSLoadMetrics;

        /** Setup the loader. **/
        public function LevelLoader(hls : HLS) {
            _hls = hls;
            _hls.addEventListener(HLSEvent.PLAYBACK_STATE, _stateHandler);
            _hls.addEventListener(HLSEvent.LEVEL_SWITCH, _levelSwitchHandler);
            _levels = new Vector.<Level>();
        }

        public function dispose() : void {
            _close();
            if(_urlloader) {
                _urlloader.removeEventListener(Event.COMPLETE, _loadCompleteHandler);
                _urlloader.removeEventListener(ProgressEvent.PROGRESS, _loadProgressHandler);
                _urlloader.removeEventListener(IOErrorEvent.IO_ERROR, _errorHandler);
                _urlloader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, _errorHandler);
                _urlloader = null;
            }
            _hls.removeEventListener(HLSEvent.PLAYBACK_STATE, _stateHandler);
            _hls.removeEventListener(HLSEvent.LEVEL_SWITCH, _levelSwitchHandler);
        }

        /** Loading failed; return errors. **/
        private function _errorHandler(event : ErrorEvent) : void {
            var txt : String = "Cannot load M3U8: " + event.text;
            var code : int = HLSError.MANIFEST_LOADING_IO_ERROR;
            if (event is SecurityErrorEvent) {
                code = HLSError.MANIFEST_LOADING_CROSSDOMAIN_ERROR;
                txt = "Cannot load M3U8: crossdomain access denied:" + event.text;
            } else if (event is IOErrorEvent && (HLSSettings.manifestLoadMaxRetry == -1 || _retryCount < HLSSettings.manifestLoadMaxRetry)) {
                CONFIG::LOGGING {
                    Log.warn("I/O Error while trying to load Playlist, retry in " + _retryTimeout + " ms");
                }
                if(_levels.length) {
                    _timeoutID = setTimeout(_loadActiveLevelPlaylist, _retryTimeout);
                } else {
                    _timeoutID = setTimeout(_loadManifest, _retryTimeout);
                }
                /* exponential increase of retry timeout, capped to manifestLoadMaxRetryTimeout */
                _retryTimeout = Math.min(HLSSettings.manifestLoadMaxRetryTimeout, 2 * _retryTimeout);
                _retryCount++;
                dispatchHLSEvent(HLSEvent.WARNING,code, txt);
                return;
            } else {
                // if we have redundant streams left for that level, switch to it, otherwise retry primary stream
                if(_loadLevel < _levels.length && _levels[_loadLevel].redundantStreamsNb>0 ) {
                    CONFIG::LOGGING {
                        Log.warn("max load retry reached, switch to redundant stream");
                    }
                    // try next redundant stream
                    if (_levels[_loadLevel].redundantStreamId < _levels[_loadLevel].redundantStreamsNb) {
                        _levels[_loadLevel].redundantStreamId++;
                    }
                    // retry primary stream if the last redundant stream has failed and HLSSettings.manifestRedundantLoadmaxRetry allows
                    else if ((HLSSettings.manifestRedundantLoadmaxRetry > 0 && _redundantRetryCount < HLSSettings.manifestRedundantLoadmaxRetry ) || HLSSettings.manifestRedundantLoadmaxRetry==-1 ) {
                            _redundantRetryCount++;
                            _levels[_loadLevel].redundantStreamId = 0;
                    }
                    else {
                        dispatchHLSEvent(HLSEvent.ERROR,code, txt);
                        return;
                    }
                    _timeoutID = setTimeout(_loadActiveLevelPlaylist, 0);
                    _retryTimeout = 1000;
                    _retryCount = 0;
                    dispatchHLSEvent(HLSEvent.WARNING,code, txt);
                    return;
                } else {
                    // if level > 0 and in autolevel, and switch down on level error is activated, trigger LEVEL_LOADING_ABORTED
                    if(_loadLevel && _hls.autoLevel && HLSSettings.switchDownOnLevelError) {
                        _hls.dispatchEvent(new HLSEvent(HLSEvent.LEVEL_LOADING_ABORTED, _loadLevel));
                        return;
                    }
                }
            }
            dispatchHLSEvent(HLSEvent.ERROR,code, txt);
        }

        private function dispatchHLSEvent(event : String,code:int, txt:String):void {
            var hlsError : HLSError = new HLSError(code, _url, txt);
            _hls.dispatchEvent(new HLSEvent(event, hlsError));
        }

        /** Return the current manifest. **/
        public function get levels() : Vector.<Level> {
            return _levels;
        }

        /** Return the stream type. **/
        public function get type() : String {
            return _type;
        }

        public function get altAudioTracks() : Vector.<AltAudioTrack> {
            return _altAudioTracks;
        }

        /** Load the manifest file. **/
        public function load(url : String) : void {
            if(!_urlloader) {
                //_urlloader = new URLLoader();
                var urlLoaderClass : Class = _hls.URLloader as Class;
                _urlloader = (new urlLoaderClass()) as URLLoader;
                _urlloader.addEventListener(Event.COMPLETE, _loadCompleteHandler);
                _urlloader.addEventListener(ProgressEvent.PROGRESS, _loadProgressHandler);
                _urlloader.addEventListener(IOErrorEvent.IO_ERROR, _errorHandler);
                _urlloader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, _errorHandler);
            }
            _close();
            _closed = false;
            _url = url;
            _levels = new Vector.<Level>();
            _canStart = false;
            _reloadPlaylistTimer = getTimer();
            _retryTimeout = 1000;
            _retryCount = 0;
            _redundantRetryCount = 0;
            _altAudioTracks = null;
            _loadManifest();
        }

        /** loading progress handler, use to determine loading latency **/
        private function _loadProgressHandler(event : Event) : void {
            if(_metrics.loading_begin_time == 0) {
                _metrics.loading_begin_time = getTimer();
            }
        }


        /** Manifest loaded; check and parse it **/
        private function _loadCompleteHandler(event : Event) : void {
             _metrics.loading_end_time = getTimer();
            // successful loading, reset retry counter
            _retryTimeout = 1000;
            _retryCount = 0;
            _parseManifest(String(_urlloader.data));
        }

        /** parse a playlist **/
        private function _parseLevelPlaylist(string : String, url : String, level : int, metrics : HLSLoadMetrics) : void {
            var frags : Vector.<Fragment>;
            if (string != null) {
                CONFIG::LOGGING {
                    Log.debug("level " + level + " playlist:\n" + string);
                }
                frags = Manifest.getFragments(string, url, level);
            }

            if(frags && frags.length) {
                // successful loading, reset retry counter
                _retryTimeout = 1000;
                _retryCount = 0;
                // set fragment and update sequence number range
                _levels[level].updateFragments(frags);
                _levels[level].targetduration = Manifest.getTargetDuration(string);
                _hls.dispatchEvent(new HLSEvent(HLSEvent.PLAYLIST_DURATION_UPDATED, _levels[level].duration));
            } else {
                if(HLSSettings.manifestLoadMaxRetry == -1 || _retryCount < HLSSettings.manifestLoadMaxRetry) {
                    CONFIG::LOGGING {
                        Log.warn("empty level Playlist, retry in " + _retryTimeout + " ms");
                    }
                    _timeoutID = setTimeout(_loadActiveLevelPlaylist, _retryTimeout);
                    /* exponential increase of retry timeout, capped to manifestLoadMaxRetryTimeout */
                    _retryTimeout = Math.min(HLSSettings.manifestLoadMaxRetryTimeout, 2 * _retryTimeout);
                    _retryCount++;
                    return;
                } else {
                    var hlsError : HLSError = new HLSError(HLSError.MANIFEST_LOADING_IO_ERROR, _url, "no fragments in playlist");
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
                    return;
                }
            }
            // Check whether the stream is live or not finished yet
            if (Manifest.hasEndlist(string)) {
                setType(HLSTypes.VOD);
                _hls.dispatchEvent(new HLSEvent(HLSEvent.LEVEL_ENDLIST, level));
            } else {
                setType(HLSTypes.LIVE);
                /* in order to determine playlist reload timer,
                    check playback position against playlist duration.
                    if we are less than one frag from the edge of a live playlist, reload playlist
                    twice quicker to discover new fragments quicker and avoid potential buffer starvation.
                */
                var _reloadInterval : Number = _levels[level].averageduration;
                if((_levels[level].duration - _hls.position) < _reloadInterval) {
                    _reloadInterval /= 2;
                }
                // keep at least 1s between requests, in case last one was really slow
                var timeout : int = Math.max(1000,_reloadPlaylistTimer + 1000*_reloadInterval - getTimer());
                CONFIG::LOGGING {
                    Log.debug("Level " + level + " Live Playlist parsing finished: reload in " + timeout + " ms");
                }
                _timeoutID = setTimeout(_loadActiveLevelPlaylist, timeout);
            }
            if (!_canStart) {
                _canStart = ((type == HLSTypes.VOD) && _levels[level].fragments.length > 0) || (_levels[level].fragments.length >= HLSSettings.initialLiveManifestSize);
                CONFIG::LOGGING {
                    if(!_canStart && (type == HLSTypes.LIVE)){
                        Log.warn("Can not start playback of level, reason: not enough segments " +
                                 _levels[level].fragments.length + "<" + HLSSettings.initialLiveManifestSize);
                    }
                }
                if (_canStart) {
                    CONFIG::LOGGING {
                        Log.debug("First level filled with necessary amount of fragments, notify event");
                    }
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.MANIFEST_LOADED, _levels, _metrics));
                }
            }
            metrics.id  = _levels[level].start_seqnum;
            metrics.id2 = _levels[level].end_seqnum;
            _hls.dispatchEvent(new HLSEvent(HLSEvent.LEVEL_LOADED, metrics));
            _manifestLoading = null;
        }

        /** Parse First Level Playlist **/
        private function _parseManifest(string : String) : void {
            var errorTxt : String = null;
            // Check for M3U8 playlist or manifest.
            if (string.indexOf(Manifest.HEADER) == 0) {
                // 1 level playlist, create unique level and parse playlist
                if (string.indexOf(Manifest.FRAGMENT) > 0) {
                    var level : Level = new Level();
                    level.urls = new Vector.<String>();
                    level.urls.push(_url);
                    _levels.push(level);
                    _metrics.parsing_end_time = getTimer();
                    CONFIG::LOGGING {
                        Log.debug("1 Level Playlist, load it");
                    }
                    _parseLevelPlaylist(string, _url, 0,_metrics);
                    _loadLevel = 0;
                    _metrics.type = HLSLoaderTypes.LEVEL_MAIN;
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.MANIFEST_PARSED, _levels));
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.LEVEL_LOADING, 0));
                } else if (string.indexOf(Manifest.LEVEL) > 0) {
                    CONFIG::LOGGING {
                        Log.debug("adaptive playlist:\n" + string);
                    }
                    // adaptative playlist, extract levels from playlist, get them and parse them
                    _levels = Manifest.extractLevels(string, _url);
                    if (_levels.length) {
						/*ckplayer*/
						var m3u8list:Array=[];
						for(var i:int=0;i<_levels.length;i++){
							m3u8list.push(_levels[i].url);
						}
						_hls.dispatchEvent(new HLSEvent(HLSEvent.M3U8_LIST, m3u8list));
                        _metrics.parsing_end_time = getTimer();
                        _loadLevel = -1;
                        _hls.dispatchEvent(new HLSEvent(HLSEvent.MANIFEST_PARSED, _levels));
                        if (string.indexOf(Manifest.ALTERNATE_AUDIO) > 0) {
                            CONFIG::LOGGING {
                                Log.debug("alternate audio level found");
                            }
                            // parse alternate audio tracks
                            _altAudioTracks = Manifest.extractAltAudioTracks(string, _url);
                            CONFIG::LOGGING {
                                if (_altAudioTracks.length > 0) {
                                    Log.debug(_altAudioTracks.length + " alternate audio tracks found");
                                }
                            }
                        }
                    } else {
                        errorTxt = "No level found in Manifest";
                    }
                } else {
                    // manifest start with correct header, but it does not contain any fragment or level info ...
                    errorTxt = "empty Manifest";
                }
            } else {
                errorTxt = "Manifest is not a valid M3U8 file";
            }
            if(errorTxt) {
                _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, new HLSError(HLSError.MANIFEST_PARSING_ERROR, _url, errorTxt)));
            }
        }

        /** load/reload manifest **/
        private function _loadManifest() : void {
            _hls.dispatchEvent(new HLSEvent(HLSEvent.MANIFEST_LOADING, _url));
            _metrics = new HLSLoadMetrics(HLSLoaderTypes.MANIFEST);
            _metrics.loading_request_time = getTimer();
            if (DataUri.isDataUri(_url)) {
                CONFIG::LOGGING {
                    Log.debug("Identified manifest <" + _url + "> as a data URI.");
                }
                _metrics.loading_begin_time = getTimer();
                var data : String = new DataUri(_url).extractData();
                _metrics.loading_end_time = getTimer();
                _parseManifest(data || "");
            } else {
                _urlloader.load(new URLRequest(_url));
            }
        }

        /** load/reload active M3U8 playlist **/
        private function _loadActiveLevelPlaylist() : void {
            if (_closed) {
                return;
            }
            _reloadPlaylistTimer = getTimer();
            // load active M3U8 playlist only
            _manifestLoading = new Manifest();
            _hls.dispatchEvent(new HLSEvent(HLSEvent.LEVEL_LOADING, _loadLevel));
            _manifestLoading.loadPlaylist(_hls,_levels[_loadLevel].url, _parseLevelPlaylist, _errorHandler, _loadLevel, _type, HLSSettings.flushLiveURLCache);
        }

        /** When level switch occurs, assess the need of (re)loading new level playlist **/
        private function _levelSwitchHandler(event : HLSEvent) : void {
            if (_loadLevel != event.level || _levels[_loadLevel].fragments.length == 0) {
                _loadLevel = event.level;
                CONFIG::LOGGING {
                    Log.debug("switch to level " + _loadLevel);
                }
                if (type == HLSTypes.LIVE || _levels[_loadLevel].fragments.length == 0) {
                    _closed = false;
                    CONFIG::LOGGING {
                        Log.debug("(re)load Playlist");
                    }
                    if(_manifestLoading) {
                       _manifestLoading.close();
                       _manifestLoading = null;
                    }
                    clearTimeout(_timeoutID);
                    _timeoutID = setTimeout(_loadActiveLevelPlaylist, 0);
                }
            }
        }

        private function _close() : void {
            CONFIG::LOGGING {
                Log.debug("cancel any manifest load in progress");
            }
            _closed = true;
            clearTimeout(_timeoutID);
            try {
                _urlloader.close();
                if (_manifestLoading) {
                    _manifestLoading.close();
                }
            } catch(e : Error) {
            }
        }

        /** When the framework idles out, stop reloading manifest **/
        private function _stateHandler(event : HLSEvent) : void {
            if (event.state == HLSPlayStates.IDLE) {
                _close();
            }
        }

        private function setType(value: String):void{
            if(value != _type){
                _type = value;

                CONFIG::LOGGING {
                    Log.debug("Stream type did change to " + value);
                }

                _hls.dispatchEvent(new HLSEvent(HLSEvent.STREAM_TYPE_DID_CHANGE, _type));
            }
        }
    }
}
