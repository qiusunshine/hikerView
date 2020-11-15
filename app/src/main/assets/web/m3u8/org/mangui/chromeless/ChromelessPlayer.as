/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.chromeless {
    import flash.display.*;
    import flash.events.*;
    import flash.external.ExternalInterface;
    import flash.geom.Rectangle;
    import flash.media.SoundTransform;
    import flash.media.StageVideo;
    import flash.media.StageVideoAvailability;
    import flash.media.Video;
    import flash.net.URLLoader;
    import flash.net.URLStream;
    import flash.system.Security;
    import flash.utils.getTimer;
    import flash.utils.setTimeout;
    import org.mangui.hls.event.HLSError;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;
    import org.mangui.hls.model.AudioTrack;
    import org.mangui.hls.model.Level;
    import org.mangui.hls.utils.JSURLLoader;
    import org.mangui.hls.utils.JSURLStream;
    import org.mangui.hls.utils.Log;
    import org.mangui.hls.utils.ScaleVideo;

    // import com.sociodox.theminer.*;
    public class ChromelessPlayer extends Sprite {
        /** reference to the framework. **/
        protected var _hls : HLS;
        /** Sheet to place on top of the video. **/
        protected var _sheet : Sprite;
        /** Reference to the stage video element. **/
        protected var _stageVideo : StageVideo = null;
        /** Reference to the video element. **/
        protected var _video : Video = null;
        /** Video size **/
        protected var _videoWidth : int = 0;
        protected var _videoHeight : int = 0;
        /** current media position */
        protected var _mediaPosition : Number;
        protected var _duration : Number;
        /** URL autoload feature */
        protected var _autoLoad : Boolean = false;
        /* JS callback name */
        protected var _callbackName : String;
        /* stats handler */
        private var _statsHandler : StatsHandler;

        /** Initialization. **/
        public function ChromelessPlayer() {
            Security.allowDomain("*");
            Security.allowInsecureDomain("*");
            ExternalInterface.marshallExceptions = true;
            _setupStage();
            _setupSheet();
            _setupExternalGetters();
            _setupExternalCallers();
            _setupExternalCallback();

            setTimeout(_pingJavascript, 50);
        };

        protected function _setupExternalGetters() : void {
            ExternalInterface.addCallback("getCurrentLevel", _getCurrentLevel);
            ExternalInterface.addCallback("getNextLevel", _getNextLevel);
            ExternalInterface.addCallback("getLoadLevel", _getLoadLevel);
            ExternalInterface.addCallback("getLevels", _getLevels);
            ExternalInterface.addCallback("getAutoLevel", _getAutoLevel);
            ExternalInterface.addCallback("getDuration", _getDuration);
            ExternalInterface.addCallback("getPosition", _getPosition);
            ExternalInterface.addCallback("getPlaybackState", _getPlaybackState);
            ExternalInterface.addCallback("getSeekState", _getSeekState);
            ExternalInterface.addCallback("getType", _getType);
            ExternalInterface.addCallback("getmaxBufferLength", _getmaxBufferLength);
            ExternalInterface.addCallback("getminBufferLength", _getminBufferLength);
            ExternalInterface.addCallback("getlowBufferLength", _getlowBufferLength);
            ExternalInterface.addCallback("getmaxBackBufferLength", _getmaxBackBufferLength);
            ExternalInterface.addCallback("getbufferLength", _getbufferLength);
            ExternalInterface.addCallback("getbackBufferLength", _getbackBufferLength);
            ExternalInterface.addCallback("getLogDebug", _getLogDebug);
            ExternalInterface.addCallback("getLogDebug2", _getLogDebug2);
            ExternalInterface.addCallback("getUseHardwareVideoDecoder", _getUseHardwareVideoDecoder);
            ExternalInterface.addCallback("getCapLeveltoStage", _getCapLeveltoStage);
            ExternalInterface.addCallback("getAutoLevelCapping", _getAutoLevelCapping);
            ExternalInterface.addCallback("getflushLiveURLCache", _getflushLiveURLCache);
            ExternalInterface.addCallback("getstartFromLevel", _getstartFromLevel);
            ExternalInterface.addCallback("getseekFromLowestLevel", _getseekFromLevel);
            ExternalInterface.addCallback("getJSURLStream", _getJSURLStream);
            ExternalInterface.addCallback("getPlayerVersion", _getPlayerVersion);
            ExternalInterface.addCallback("getAudioTrackList", _getAudioTrackList);
            ExternalInterface.addCallback("getAudioTrackId", _getAudioTrackId);
            ExternalInterface.addCallback("getStats", _getStats);
        };

        protected function _setupExternalCallers() : void {
            ExternalInterface.addCallback("playerLoad", _load);
            ExternalInterface.addCallback("playerPlay", _play);
            ExternalInterface.addCallback("playerPause", _pause);
            ExternalInterface.addCallback("playerResume", _resume);
            ExternalInterface.addCallback("playerSeek", _seek);
            ExternalInterface.addCallback("playerStop", _stop);
            ExternalInterface.addCallback("playerVolume", _volume);
            ExternalInterface.addCallback("playerSetCurrentLevel", _setCurrentLevel);
            ExternalInterface.addCallback("playerSetNextLevel", _setNextLevel);
            ExternalInterface.addCallback("playerSetLoadLevel", _setLoadLevel);
            ExternalInterface.addCallback("playerSetmaxBufferLength", _setmaxBufferLength);
            ExternalInterface.addCallback("playerSetminBufferLength", _setminBufferLength);
            ExternalInterface.addCallback("playerSetlowBufferLength", _setlowBufferLength);
            ExternalInterface.addCallback("playerSetbackBufferLength", _setbackBufferLength);
            ExternalInterface.addCallback("playerSetflushLiveURLCache", _setflushLiveURLCache);
            ExternalInterface.addCallback("playerSetstartFromLevel", _setstartFromLevel);
            ExternalInterface.addCallback("playerSetseekFromLevel", _setseekFromLevel);
            ExternalInterface.addCallback("playerSetLogDebug", _setLogDebug);
            ExternalInterface.addCallback("playerSetLogDebug2", _setLogDebug2);
            ExternalInterface.addCallback("playerSetUseHardwareVideoDecoder", _setUseHardwareVideoDecoder);
            ExternalInterface.addCallback("playerSetAutoLevelCapping", _setAutoLevelCapping);
            ExternalInterface.addCallback("playerCapLeveltoStage", _setCapLeveltoStage);
            ExternalInterface.addCallback("playerSetAudioTrack", _setAudioTrack);
            ExternalInterface.addCallback("playerSetJSURLStream", _setJSURLStream);
        };

        protected function _setupExternalCallback() : void {
            // Pass in the JavaScript callback name in the `callback` FlashVars parameter.
            _callbackName = LoaderInfo(this.root.loaderInfo).parameters.callback.toString();
        };

        protected function _setupStage() : void {
            stage.scaleMode = StageScaleMode.NO_SCALE;
            stage.align = StageAlign.TOP_LEFT;
            stage.fullScreenSourceRect = new Rectangle(0, 0, stage.stageWidth, stage.stageHeight);
            stage.addEventListener(StageVideoAvailabilityEvent.STAGE_VIDEO_AVAILABILITY, _onStageVideoState);
            stage.addEventListener(Event.RESIZE, _onStageResize);
        }

        protected function _setupSheet() : void {
            // Draw sheet for catching clicks
            _sheet = new Sprite();
            _sheet.graphics.beginFill(0x000000, 0);
            _sheet.graphics.drawRect(0, 0, stage.stageWidth, stage.stageHeight);
            _sheet.addEventListener(MouseEvent.CLICK, _clickHandler);
            _sheet.buttonMode = true;
            addChild(_sheet);
        }

        protected function _trigger(event : String, ...args) : void {
            if (ExternalInterface.available) {
                ExternalInterface.call(_callbackName, event, args);
            }
        };

        /** Notify javascript the framework is ready. **/
        protected function _pingJavascript() : void {
            _trigger("ready", getTimer());
        };

        /** Forward events from the framework. **/
        protected function _completeHandler(event : HLSEvent) : void {
            _trigger("complete");
        };

        protected function _errorHandler(event : HLSEvent) : void {
            var hlsError : HLSError = event.error;
            _trigger("error", hlsError.code, hlsError.url, hlsError.msg);
        };

        protected function _levelLoadedHandler(event : HLSEvent) : void {
            _trigger("levelLoaded", event.loadMetrics);
        };

        protected function _levelEndlistHandler(event : HLSEvent) : void {
            _trigger("levelEndlist", event.level);
        };

        protected function _audioLevelLoadedHandler(event : HLSEvent) : void {
            _trigger("audioLevelLoaded", event.loadMetrics);
        };

        protected function _fragmentLoadedHandler(event : HLSEvent) : void {
            _trigger("fragmentLoaded", event.loadMetrics);
        };

        protected function _fragmentPlayingHandler(event : HLSEvent) : void {
            _trigger("fragmentPlaying", event.playMetrics);
        };

        protected function _manifestLoadedHandler(event : HLSEvent) : void {
            _duration = event.levels[_hls.startLevel].duration;

            if (_autoLoad) {
                _play(-1);
            }

            _trigger("manifest", _duration, event.levels, event.loadMetrics);
        };

        protected function _mediaTimeHandler(event : HLSEvent) : void {
            _duration = event.mediatime.duration;
            _mediaPosition = event.mediatime.position;
            _trigger("position", event.mediatime);

            var videoWidth : int = _video ? _video.videoWidth : _stageVideo.videoWidth;
            var videoHeight : int = _video ? _video.videoHeight : _stageVideo.videoHeight;

            if (videoWidth && videoHeight) {
                var changed : Boolean = _videoWidth != videoWidth || _videoHeight != videoHeight;
                if (changed) {
                    _videoHeight = videoHeight;
                    _videoWidth = videoWidth;
                    _resize();
                    _trigger("videoSize", _videoWidth, _videoHeight);
                }
            }
        };

        protected function _playbackStateHandler(event : HLSEvent) : void {
            _trigger("state", event.state);
        };

        protected function _seekStateHandler(event : HLSEvent) : void {
            _trigger("seekState", event.state);
        };

        protected function _levelSwitchHandler(event : HLSEvent) : void {
            _trigger("switch", event.level);
        };

        protected function _fpsDropHandler(event : HLSEvent) : void {
            _trigger("fpsDrop", event.level);
        };

        protected function _fpsDropLevelCappingHandler(event : HLSEvent) : void {
            _trigger("fpsDropLevelCapping", event.level);
        };

        protected function _fpsDropSmoothLevelSwitchHandler(event : HLSEvent) : void {
            _trigger("fpsDropSmoothLevelSwitch");
        };

        protected function _audioTracksListChange(event : HLSEvent) : void {
            _trigger("audioTracksListChange", _getAudioTrackList());
        }

        protected function _audioTrackChange(event : HLSEvent) : void {
            _trigger("audioTrackChange", event.audioTrack);
        }

        protected function _id3Updated(event : HLSEvent) : void {
            _trigger("id3Updated", event.ID3Data);
        }

        protected function _liveLoadingStalledHandler(event : HLSEvent) : void {
            _trigger("liveLoadingStalled");
        };

        /** Javascript getters. **/
        protected function _getCurrentLevel() : int {
            return _hls.currentLevel;
        };

        protected function _getNextLevel() : int {
            return _hls.nextLevel;
        };

        protected function _getLoadLevel() : int {
            return _hls.loadLevel;
        };

        protected function _getLevels() : Vector.<Level> {
            return _hls.levels;
        };

        protected function _getAutoLevel() : Boolean {
            return _hls.autoLevel;
        };

        protected function _getDuration() : Number {
            return _duration;
        };

        protected function _getPosition() : Number {
            return _hls.position;
        };

        protected function _getPlaybackState() : String {
            return _hls.playbackState;
        };

        protected function _getSeekState() : String {
            return _hls.seekState;
        };

        protected function _getType() : String {
            return _hls.type;
        };

        protected function _getbufferLength() : Number {
            return _hls.stream.bufferLength;
        };

        protected function _getbackBufferLength() : Number {
            return _hls.stream.backBufferLength;
        };

        protected function _getmaxBufferLength() : Number {
            return HLSSettings.maxBufferLength;
        };

        protected function _getminBufferLength() : Number {
            return HLSSettings.minBufferLength;
        };

        protected function _getlowBufferLength() : Number {
            return HLSSettings.lowBufferLength;
        };

        protected function _getmaxBackBufferLength() : Number {
            return HLSSettings.maxBackBufferLength;
        };

        protected function _getflushLiveURLCache() : Boolean {
            return HLSSettings.flushLiveURLCache;
        };

        protected function _getstartFromLevel() : int {
            return HLSSettings.startFromLevel;
        };

        protected function _getseekFromLevel() : int {
            return HLSSettings.seekFromLevel;
        };

        protected function _getLogDebug() : Boolean {
            return HLSSettings.logDebug;
        };

        protected function _getLogDebug2() : Boolean {
            return HLSSettings.logDebug2;
        };

        protected function _getUseHardwareVideoDecoder() : Boolean {
            return HLSSettings.useHardwareVideoDecoder;
        };

        protected function _getCapLeveltoStage() : Boolean {
            return HLSSettings.capLevelToStage;
        };

        protected function _getAutoLevelCapping() : int {
            return _hls.autoLevelCapping;
        };

        protected function _getJSURLStream() : Boolean {
            return (_hls.URLstream is JSURLStream);
        };

        protected function _getPlayerVersion() : Number {
            return 3;
        };

        protected function _getAudioTrackList() : Array {
            var list : Array = [];
            var vec : Vector.<AudioTrack> = _hls.audioTracks;
            for (var i : Object in vec) {
                list.push(vec[i]);
            }
            return list;
        };

        protected function _getAudioTrackId() : int {
            return _hls.audioTrack;
        };

        protected function _getStats() : Object {
            return _statsHandler.stats;
        };

        /** Javascript calls. **/
        protected function _load(url : String) : void {
            _hls.load(url);
        };

        protected function _play(position : Number = -1) : void {
            _hls.stream.play(null, position);
        };

        protected function _pause() : void {
            _hls.stream.pause();
        };

        protected function _resume() : void {
            _hls.stream.resume();
        };

        protected function _seek(position : Number) : void {
            _hls.stream.seek(position);
        };

        protected function _stop() : void {
            _hls.stream.close();
        };

        protected function _volume(percent : Number) : void {
            _hls.stream.soundTransform = new SoundTransform(percent / 100);
        };

        protected function _setCurrentLevel(level : int) : void {
            _hls.currentLevel = level;
        };

        protected function _setNextLevel(level : int) : void {
            _hls.nextLevel = level;
        };

        protected function _setLoadLevel(level : int) : void {
            _hls.loadLevel = level;
        };

        protected function _setmaxBufferLength(newLen : Number) : void {
            HLSSettings.maxBufferLength = newLen;
        };

        protected function _setminBufferLength(newLen : Number) : void {
            HLSSettings.minBufferLength = newLen;
        };

        protected function _setlowBufferLength(newLen : Number) : void {
            HLSSettings.lowBufferLength = newLen;
        };

        protected function _setbackBufferLength(newLen : Number) : void {
            HLSSettings.maxBackBufferLength = newLen;
        };

        protected function _setflushLiveURLCache(flushLiveURLCache : Boolean) : void {
            HLSSettings.flushLiveURLCache = flushLiveURLCache;
        };

        protected function _setstartFromLevel(startFromLevel : int) : void {
            HLSSettings.startFromLevel = startFromLevel;
        };

        protected function _setseekFromLevel(seekFromLevel : int) : void {
            HLSSettings.seekFromLevel = seekFromLevel;
        };

        protected function _setLogDebug(debug : Boolean) : void {
            HLSSettings.logDebug = debug;
        };

        protected function _setLogDebug2(debug2 : Boolean) : void {
            HLSSettings.logDebug2 = debug2;
        };

        protected function _setUseHardwareVideoDecoder(value : Boolean) : void {
            HLSSettings.useHardwareVideoDecoder = value;
        };

        protected function _setCapLeveltoStage(value : Boolean) : void {
            HLSSettings.capLevelToStage = value;
        };

        protected function _setAutoLevelCapping(value : int) : void {
            _hls.autoLevelCapping = value;
        };

        protected function _setJSURLStream(jsURLstream : Boolean) : void {
            if (jsURLstream) {
                _hls.URLstream = JSURLStream as Class;
                _hls.URLloader = JSURLLoader as Class;
                if (_callbackName) {
                    _hls.URLstream.externalCallback = _callbackName;
                    _hls.URLloader.externalCallback = _callbackName;
                }
            } else {
                _hls.URLstream = URLStream as Class;
                _hls.URLloader = URLLoader as Class;
            }
        };

        protected function _setAudioTrack(val : int) : void {
            if (val == _hls.audioTrack) return;
            _hls.audioTrack = val;
            if (!isNaN(_mediaPosition)) {
                _hls.stream.seek(_mediaPosition);
            }
        };

        /** Mouse click handler. **/
        protected function _clickHandler(event : MouseEvent) : void {
            if (stage.displayState == StageDisplayState.FULL_SCREEN_INTERACTIVE || stage.displayState == StageDisplayState.FULL_SCREEN) {
                stage.displayState = StageDisplayState.NORMAL;
            } else {
                stage.displayState = StageDisplayState.FULL_SCREEN;
            }
        };

        /** StageVideo detector. **/
        protected function _onStageVideoState(event : StageVideoAvailabilityEvent) : void {
            var available : Boolean = (event.availability == StageVideoAvailability.AVAILABLE);
            _hls = new HLS();
            _hls.stage = stage;
            // set framerate to 60 fps
            stage.frameRate = 60;
            // set up stats handler
            _statsHandler = new StatsHandler(_hls);
            _hls.addEventListener(HLSEvent.PLAYBACK_COMPLETE, _completeHandler);
            _hls.addEventListener(HLSEvent.ERROR, _errorHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_LOADED, _fragmentLoadedHandler);
            _hls.addEventListener(HLSEvent.AUDIO_LEVEL_LOADED, _audioLevelLoadedHandler);
            _hls.addEventListener(HLSEvent.LEVEL_LOADED, _levelLoadedHandler);
            _hls.addEventListener(HLSEvent.LEVEL_ENDLIST, _levelEndlistHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_PLAYING, _fragmentPlayingHandler);
            _hls.addEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.addEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
            _hls.addEventListener(HLSEvent.PLAYBACK_STATE, _playbackStateHandler);
            _hls.addEventListener(HLSEvent.SEEK_STATE, _seekStateHandler);
            _hls.addEventListener(HLSEvent.LEVEL_SWITCH, _levelSwitchHandler);
            _hls.addEventListener(HLSEvent.AUDIO_TRACKS_LIST_CHANGE, _audioTracksListChange);
            _hls.addEventListener(HLSEvent.AUDIO_TRACK_SWITCH, _audioTrackChange);
            _hls.addEventListener(HLSEvent.ID3_UPDATED, _id3Updated);
            _hls.addEventListener(HLSEvent.FPS_DROP, _fpsDropHandler);
            _hls.addEventListener(HLSEvent.FPS_DROP_LEVEL_CAPPING, _fpsDropLevelCappingHandler);
            _hls.addEventListener(HLSEvent.FPS_DROP_SMOOTH_LEVEL_SWITCH, _fpsDropSmoothLevelSwitchHandler);
            _hls.addEventListener(HLSEvent.LIVE_LOADING_STALLED, _liveLoadingStalledHandler);

            if (available && stage.stageVideos.length > 0) {
                _stageVideo = stage.stageVideos[0];
                _stageVideo.addEventListener(StageVideoEvent.RENDER_STATE, _onStageVideoStateChange);
                _stageVideo.viewPort = new Rectangle(0, 0, stage.stageWidth, stage.stageHeight);
                _stageVideo.attachNetStream(_hls.stream);
            } else {
                _video = new Video(stage.stageWidth, stage.stageHeight);
                _video.addEventListener(VideoEvent.RENDER_STATE, _onVideoStateChange);
                addChild(_video);
                _video.smoothing = true;
                _video.attachNetStream(_hls.stream);
            }
            stage.removeEventListener(StageVideoAvailabilityEvent.STAGE_VIDEO_AVAILABILITY, _onStageVideoState);

            var autoLoadUrl : String = root.loaderInfo.parameters.url as String;
            if (autoLoadUrl != null) {
                _autoLoad = true;
                _load(autoLoadUrl);
            }
        };

        private function _onStageVideoStateChange(event : StageVideoEvent) : void {
            Log.info("Video decoding:" + event.status);
        }

        private function _onVideoStateChange(event : VideoEvent) : void {
            Log.info("Video decoding:" + event.status);
        }

        protected function _onStageResize(event : Event) : void {
            stage.fullScreenSourceRect = new Rectangle(0, 0, stage.stageWidth, stage.stageHeight);
            _sheet.width = stage.stageWidth;
            _sheet.height = stage.stageHeight;
            _resize();
        };

        protected function _resize() : void {
            var rect : Rectangle;
            rect = ScaleVideo.resizeRectangle(_videoWidth, _videoHeight, stage.stageWidth, stage.stageHeight);
            // resize video
            if (_video) {
                _video.width = rect.width;
                _video.height = rect.height;
                _video.x = rect.x;
                _video.y = rect.y;
            } else if (_stageVideo && rect.width > 0) {
                _stageVideo.viewPort = rect;
            }
        }
    }
}
