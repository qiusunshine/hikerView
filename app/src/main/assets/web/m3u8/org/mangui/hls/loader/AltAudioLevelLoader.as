/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.loader {
    import flash.events.ErrorEvent;
    import flash.events.IOErrorEvent;
    import flash.events.SecurityErrorEvent;
    import flash.utils.clearTimeout;
    import flash.utils.getTimer;
    import flash.utils.setTimeout;
    import org.mangui.hls.constant.HLSPlayStates;
    import org.mangui.hls.event.HLSError;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.event.HLSLoadMetrics;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;
    import org.mangui.hls.model.AudioTrack;
    import org.mangui.hls.model.Fragment;
    import org.mangui.hls.model.Level;
    import org.mangui.hls.playlist.AltAudioTrack;
    import org.mangui.hls.playlist.Manifest;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    public class AltAudioLevelLoader {
        /** Reference to the hls framework controller. **/
        private var _hls : HLS;
        /** Link to the M3U8 file. **/
        private var _url : String;
        /** Timeout ID for reloading live playlists. **/
        private var _timeoutID : uint;
        /** last reload manifest time **/
        private var _reloadPlaylistTimer : uint;
        /** current audio level **/
        private var _currentTrack : int;
        /** reference to manifest being loaded **/
        private var _manifestLoading : Manifest;
        /** is this loader closed **/
        private var _closed : Boolean = false;
        /* playlist retry timeout */
        private var _retryTimeout : Number;
        private var _retryCount : int;

        /** Setup the loader. **/
        public function AltAudioLevelLoader(hls : HLS) {
            _hls = hls;
            _hls.addEventListener(HLSEvent.PLAYBACK_STATE, _stateHandler);
            _hls.addEventListener(HLSEvent.AUDIO_TRACK_SWITCH, _audioTrackSwitchHandler);
        };

        public function dispose() : void {
            _close();
            _hls.removeEventListener(HLSEvent.PLAYBACK_STATE, _stateHandler);
            _hls.removeEventListener(HLSEvent.AUDIO_TRACK_SWITCH, _audioTrackSwitchHandler);
        }

        /** Loading failed; return errors. **/
        private function _errorHandler(event : ErrorEvent) : void {
            var txt : String;
            var code : int;
            if (event is SecurityErrorEvent) {
                code = HLSError.MANIFEST_LOADING_CROSSDOMAIN_ERROR;
                txt = "Cannot load M3U8: crossdomain access denied:" + event.text;
            } else if (event is IOErrorEvent && (HLSSettings.manifestLoadMaxRetry == -1 || _retryCount < HLSSettings.manifestLoadMaxRetry)) {
                CONFIG::LOGGING {
                    Log.warn("I/O Error while trying to load Playlist, retry in " + _retryTimeout + " ms");
                }
                _timeoutID = setTimeout(_loadAudioLevelPlaylist, _retryTimeout);
                /* exponential increase of retry timeout, capped to manifestLoadMaxRetryTimeout */
                _retryTimeout = Math.min(HLSSettings.manifestLoadMaxRetryTimeout, 2 * _retryTimeout);
                _retryCount++;
                return;
            } else {
                code = HLSError.MANIFEST_LOADING_IO_ERROR;
                txt = "Cannot load M3U8: " + event.text;
            }
            var hlsError : HLSError = new HLSError(code, _url, txt);
            _hls.dispatchEvent(new HLSEvent(HLSEvent.ERROR, hlsError));
        };

        /** parse a playlist **/
        private function _parseAudioPlaylist(string : String, url : String, level : int, metrics : HLSLoadMetrics) : void {
            if (string != null && string.length != 0) {
                CONFIG::LOGGING {
                    Log.debug("audio level " + level + " playlist:\n" + string);
                }
                var frags : Vector.<Fragment> = Manifest.getFragments(string, url, level);
                // set fragment and update sequence number range
                var audioTrack : AudioTrack = _hls.audioTracks[_currentTrack];
                var audioLevel : Level = audioTrack.level;
                if(audioLevel == null) {
                    audioLevel = audioTrack.level = new Level();
                }
                audioLevel.updateFragments(frags);
                audioLevel.targetduration = Manifest.getTargetDuration(string);
                // if stream is live, arm a timer to periodically reload playlist
                if (!Manifest.hasEndlist(string)) {
                    var timeout : int = Math.max(100, _reloadPlaylistTimer + 1000 * audioLevel.averageduration - getTimer());
                    CONFIG::LOGGING {
                        Log.debug("Alt Audio Level Live Playlist parsing finished: reload in " + timeout + " ms");
                    }
                    _timeoutID = setTimeout(_loadAudioLevelPlaylist, timeout);
                }
            }
            metrics.id  = audioLevel.start_seqnum;
            metrics.id2 = audioLevel.end_seqnum;
            _hls.dispatchEvent(new HLSEvent(HLSEvent.AUDIO_LEVEL_LOADED, metrics));
            _manifestLoading = null;
        };

        /** load/reload active M3U8 playlist **/
        private function _loadAudioLevelPlaylist() : void {
            if (_closed) {
                return;
            }
            _reloadPlaylistTimer = getTimer();
            var altAudioTrack : AltAudioTrack = _hls.altAudioTracks[_hls.audioTracks[_currentTrack].id];
            _manifestLoading = new Manifest();
            _manifestLoading.loadPlaylist(_hls,altAudioTrack.url, _parseAudioPlaylist, _errorHandler, _currentTrack, _hls.type, HLSSettings.flushLiveURLCache);
            _hls.dispatchEvent(new HLSEvent(HLSEvent.AUDIO_LEVEL_LOADING, _currentTrack));
        };

        /** When audio track switch occurs, assess the need of loading audio level playlist **/
        private function _audioTrackSwitchHandler(event : HLSEvent) : void {
            _currentTrack = event.audioTrack;
            var audioTrack : AudioTrack = _hls.audioTracks[_currentTrack];
            if (audioTrack.source == AudioTrack.FROM_PLAYLIST) {
                var altAudioTrack : AltAudioTrack = _hls.altAudioTracks[audioTrack.id];
                if (altAudioTrack.url && audioTrack.level == null) {
                    CONFIG::LOGGING {
                        Log.debug("switch to audio track " + _currentTrack + ", load Playlist");
                    }
                    _retryTimeout = 1000;
                    _retryCount = 0;
                    _closed = false;
                    if(_manifestLoading) {
                       _manifestLoading.close();
                       _manifestLoading = null;
                    }
                    clearTimeout(_timeoutID);
                    _timeoutID = setTimeout(_loadAudioLevelPlaylist, 0);
                }
            }
        };

        private function _close() : void {
            CONFIG::LOGGING {
                Log.debug("cancel any audio level load in progress");
            }
            _closed = true;
            clearTimeout(_timeoutID);
            try {
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
        };
    }
}
