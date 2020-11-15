/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.flowplayer {

    import flash.display.DisplayObject;
    import flash.media.Video;
    import flash.net.NetConnection;
    import flash.net.NetStream;
    import flash.system.Security;
    import flash.utils.Dictionary;
    import org.flowplayer.controller.StreamProvider;
    import org.flowplayer.controller.TimeProvider;
    import org.flowplayer.controller.VolumeController;
    import org.flowplayer.model.Clip;
    import org.flowplayer.model.ClipError;
    import org.flowplayer.model.ClipEvent;
    import org.flowplayer.model.ClipEventType;
    import org.flowplayer.model.ClipType;
    import org.flowplayer.model.Playlist;
    import org.flowplayer.model.Plugin;
    import org.flowplayer.model.PluginModel;
    import org.flowplayer.view.Flowplayer;
    import org.flowplayer.view.StageVideoWrapper;
    import org.mangui.hls.constant.HLSPlayStates;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.HLS;
    import org.mangui.hls.utils.JSURLLoader;
    import org.mangui.hls.utils.JSURLStream;
    import org.mangui.hls.utils.Params2Settings;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    public class HLSStreamProvider  implements StreamProvider,Plugin {
        private var _volumecontroller : VolumeController;
        private var _playlist : Playlist;
        private var _timeProvider : TimeProvider;
        private var _model : PluginModel;
        private var _player : Flowplayer;
        private var _clip : Clip;
        private var _video : Video;
        /** reference to the framework. **/
        private var _hls : HLS;
        // event values
        private var _duration : Number = 0;
        private var _durationCapped : Number = 0;
        private var _clipStart : Number = 0;
        private var _videoWidth : int = -1;
        private var _videoHeight : int = -1;
        private var _isManifestLoaded : Boolean = false;
        private var _pauseAfterStart : Boolean;
        private var _seekable : Boolean = false;
        private var _streamAttached : Boolean = false;

        public function getDefaultConfig() : Object {
            return null;
        }

        public function onConfig(model : PluginModel) : void {
            CONFIG::LOGGING {
                Log.info("onConfig()");
            }
            _model = model;
        }

        public function onLoad(player : Flowplayer) : void {
            CONFIG::LOGGING {
                Log.info("onLoad()");
            }
            Security.allowDomain("*");
            _player = player;
            _hls = new HLS();
            _hls.stage = player.screen.getDisplayObject().stage;
            _hls.addEventListener(HLSEvent.PLAYBACK_COMPLETE, _completeHandler);
            _hls.addEventListener(HLSEvent.ERROR, _errorHandler);
            _hls.addEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.addEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
            _hls.addEventListener(HLSEvent.PLAYBACK_STATE, _playbackStateHandler);
            _hls.addEventListener(HLSEvent.ID3_UPDATED, _ID3Handler);

            var cfg : Object = _model.config;
            for (var object : String in cfg) {
                var subidx : int = object.indexOf("hls_");
                if (subidx != -1) {
                    if(object.indexOf("jsloader")!=-1) {
                            if(cfg[object] == true) {
                            _hls.URLstream = JSURLStream as Class;
                            _hls.URLloader = JSURLLoader as Class;
                            }
                        } else {
                            Params2Settings.set(object.substr(4), cfg[object]);
                        }
                }
            }

            _model.dispatchOnLoad();
        }

        private function _completeHandler(event : HLSEvent) : void {
            // dispatch a before event because the finish has default behavior that can be prevented by listeners
            _clip.dispatchBeforeEvent(new ClipEvent(ClipEventType.FINISH));
            _clip.startDispatched = false;
        };

        private function _errorHandler(event : HLSEvent) : void {
            _clip.dispatchError(ClipError.STREAM_LOAD_FAILED,event.error.toString());
        };

        private function _ID3Handler(event : HLSEvent) : void {
            _clip.dispatch(ClipEventType.NETSTREAM_EVENT, "onID3", event.ID3Data);
        };

        private function _manifestLoadedHandler(event : HLSEvent) : void {
            _duration = event.levels[_hls.startLevel].duration - _clipStart;
            _isManifestLoaded = true;
            // only update duration if not capped
            if (!_durationCapped) {
                _clip.duration = _duration;
            } else {
                // ensure capped duration is lt real one
                _durationCapped = Math.min(_durationCapped, _duration);
            }
            _clip.stopLiveOnPause = false;
            /*
            var nbLevel = event.levels.length;
            if (nbLevel > 1) {
            var bitrates : Array = new Array();
            for (var i : int = 0; i < nbLevel; i++) {
            var info : Object = new Object();
            var level : Level = event.levels[i];
            info.bitrate = level.bitrate;
            info.url = level.url;
            info.width = level.width;
            info.height = level.height;
            info.isDefault = (i == _hls.startLevel);
            bitrates.push(info);
            }
            _clip.setCustomProperty("bitrates", bitrates);
            }
             */
            _clip.dispatch(ClipEventType.METADATA);
            _seekable = true;
            // real seek position : add clip.start offset. if not defined, use -1 to fix seeking issue on live playlist
            _hls.stream.play(null, (_clip.start == 0) ? -1 : _clipStart);
            _clip.dispatch(ClipEventType.SEEK, 0);
            if (_pauseAfterStart) {
                pause(new ClipEvent(ClipEventType.PAUSE));
            }
        };

        private function _mediaTimeHandler(event : HLSEvent) : void {
            _duration = event.mediatime.duration - _clipStart;
            // only update duration if not capped
            if (!_durationCapped) {
                if(_clip.duration != _duration) {
                    _clip.duration = _duration;
                    _clip.dispatch(ClipEventType.METADATA_CHANGED);
                }
            } else {
                // ensure capped duration is lt real one
                _durationCapped = Math.min(_durationCapped, _duration);
                if (_durationCapped - time <= 0.1) {
                    // reach end of stream, stop playback and simulate complete event
                    _hls.stream.close();
                    _clip.dispatchBeforeEvent(new ClipEvent(ClipEventType.FINISH));
                    _clip.startDispatched = false;
                }
            }
            var videoWidth : int = _video.videoWidth;
            var videoHeight : int = _video.videoHeight;
            if (videoWidth && videoHeight) {
                var changed : Boolean = _videoWidth != videoWidth || _videoHeight != videoHeight;
                if (changed) {
                    CONFIG::LOGGING {
                        Log.info("video size changed to " + videoWidth + "/" + videoHeight);
                    }
                    _videoWidth = videoWidth;
                    _videoHeight = videoHeight;
                    _clip.originalWidth = videoWidth;
                    _clip.originalHeight = videoHeight;
                    if (!_clip.startDispatched) {
                        _clip.dispatch(ClipEventType.START);
                        _clip.startDispatched = true;
                    }
                    _clip.dispatch(ClipEventType.METADATA_CHANGED);
                }
            }
        };

        private function _playbackStateHandler(event : HLSEvent) : void {
            // CONFIG::LOGGING {
            // Log.txt("state:"+ event.state);
            // }
            switch(event.state) {
                case HLSPlayStates.IDLE:
                case HLSPlayStates.PLAYING:
                case HLSPlayStates.PAUSED:
                    _clip.dispatch(ClipEventType.BUFFER_FULL);
                    break;
                case HLSPlayStates.PLAYING_BUFFERING:
                case HLSPlayStates.PAUSED_BUFFERING:
                    _clip.dispatch(ClipEventType.BUFFER_EMPTY);
                    break;
                default:
                    break;
            }
        };

        /**
         * Starts loading the specified clip. Once video data is available the provider
         * must set it to the clip using <code>clip.setContent()</code>. Typically the video
         * object passed to the clip is an instance of <a href="http://livedocs.adobe.com/flash/9.0/ActionScriptLangRefV3/flash/media/Video.html">flash.media.Video</a>.
         *
         * @param event the event that this provider should dispatch once loading has successfully started,
         * once dispatched the player will call <code>getVideo()</code>
         * @param clip the clip to load
         * @param pauseAfterStart if <code>true</code> the playback is paused on first frame and
         * buffering is continued
         * @see Clip#setContent()
         * @see #getVideo()
         */
        public function load(event : ClipEvent, clip : Clip, pauseAfterStart : Boolean = true) : void {
            _clip = clip;
            CONFIG::LOGGING {
                Log.info("load()" + clip.completeUrl);
            }
            _hls.load(clip.completeUrl);
            _pauseAfterStart = pauseAfterStart;
            _durationCapped = clip.duration;
            _clipStart = clip.start;
            clip.type = ClipType.VIDEO;
            clip.dispatch(ClipEventType.BEGIN);
            clip.setNetStream(_hls.stream);
            return;
        }

        /**
         * Gets the <a href="http://livedocs.adobe.com/flash/9.0/ActionScriptLangRefV3/flash/media/Video.html">Video</a> object.
         * A stream will be attached to the returned video object using <code>attachStream()</code>.
         * @param clip the clip for which the Video object is queried for
         * @see #attachStream()
         */
        public function getVideo(clip : Clip) : DisplayObject {
            CONFIG::LOGGING {
                Log.debug("getVideo()");
            }
            if (_video == null) {
                if (clip.useStageVideo) {
                    CONFIG::LOGGING {
                        Log.debug("useStageVideo");
                    }
                    _video = new StageVideoWrapper(clip);
                } else {
                    _video = new Video();
                    _video.smoothing = clip.smoothing;
                }
            }
            return _video;
        }

        /**
         * Attaches a stream to the specified display object.
         * @param video the video object that was originally retrieved using <code>getVideo()</code>.
         * @see #getVideo()
         */
        public function attachStream(video : DisplayObject) : void {
            CONFIG::LOGGING {
                Log.debug("attachStream()");
            }
            if(_streamAttached == false) {
                Video(video).attachNetStream(_hls.stream);
                _streamAttached = true;
            }
            return;
        }

        /**
         * Pauses playback.
         * @param event the event that this provider should dispatch once loading has been successfully paused
         */
        public function pause(event : ClipEvent) : void {
            CONFIG::LOGGING {
                Log.info("pause()");
            }
            _hls.stream.pause();
            if (event) {
                _clip.dispatch(ClipEventType.PAUSE);
            }
            return;
        }

        /**
         * Resumes playback.
         * @param event the event that this provider should dispatch once loading has been successfully resumed
         */
        public function resume(event : ClipEvent) : void {
            CONFIG::LOGGING {
                Log.info("resume()");
            }
            _hls.stream.resume();
            if (event) {
                _clip.dispatch(ClipEventType.RESUME);
            }
            return;
        }

        /**
         * Stops and rewinds to the beginning of current clip.
         * @param event the event that this provider should dispatch once loading has been successfully stopped
         */
        public function stop(event : ClipEvent, closeStream : Boolean = false) : void {
            CONFIG::LOGGING {
                Log.info("stop()");
            }
            _hls.stream.close();
            return;
        }

        /**
         * Seeks to the specified point in the timeline.
         * @param event the event that this provider should dispatch once the seek is in target
         * @param seconds the target point in the timeline
         */
        public function seek(event : ClipEvent, seconds : Number) : void {
            CONFIG::LOGGING {
                Log.info("seek(" + seconds + ")");
            }
            if (Math.abs(time - seconds) > 0.2) {
                // real seek position : add clip.start offset
                _hls.stream.seek(seconds + _clipStart);
            } else {
                CONFIG::LOGGING {
                    Log.warn("seek(" + seconds + ") to current position, discard");
                }
            }
            if (event) {
                _clip.dispatch(ClipEventType.SEEK, seconds);
            }
            return;
        }

        /**
         * File size in bytes.
         */
        public function get fileSize() : Number {
            return 0;
        }

        /**
         * Current playhead time in seconds.
         */
        public function get time() : Number {
            var _time : Number = Math.max(0, _hls.position - _clipStart);
            return _time;
        }

        /**
         * The point in timeline where the buffered data region begins, in seconds.
         */
        public function get bufferStart() : Number {
            var _bufferStart : Number;
            if (!_durationCapped) {
                _bufferStart = Math.min(_hls.position - _hls.stream.backBufferLength - _clipStart, _duration);
            } else {
                _bufferStart = Math.min(_hls.position - _hls.stream.backBufferLength - _clipStart, _durationCapped);
            }
            return Math.max(_bufferStart, 0);
        }

        /**
         * The point in timeline where the buffered data region ends, in seconds.
         */
        public function get bufferEnd() : Number {
            var _bufferEnd : Number;
            if (!_durationCapped) {
                _bufferEnd = Math.min(_hls.stream.bufferLength + _hls.position - _clipStart, _duration);
            } else {
                _bufferEnd = Math.min(_hls.stream.bufferLength + _hls.position - _clipStart, _durationCapped);
            }
            return Math.max(_bufferEnd, 0);
        }

        /**
         * Does this provider support random seeking to unbuffered areas in the timeline?
         */
        public function get allowRandomSeek() : Boolean {
            // CONFIG::LOGGING {
            // Log.info("allowRandomSeek()");
            // }
            return _seekable;
        }

        /**
         * Volume controller used to control the video volume.
         */
        public function set volumeController(controller : VolumeController) : void {
            _volumecontroller = controller;
            _volumecontroller.netStream = _hls.stream;
            return;
        }

        /**
         * Is this provider in the process of stopping the stream?
         * When stopped the provider should not dispatch any events resulting from events that
         * might get triggered by the underlying streaming implementation.
         */
        public function get stopping() : Boolean {
            CONFIG::LOGGING {
                Log.info("stopping()");
            }
            return false;
        }

        /**
         * The playlist instance.
         */
        public function set playlist(playlist : Playlist) : void {
            // CONFIG::LOGGING {
            // Log.debug("set playlist()");
            // }
            _playlist = playlist;
            return;
        }

        public function get playlist() : Playlist {
            CONFIG::LOGGING {
                Log.debug("get playlist()");
            }
            return _playlist;
        }

        /**
         * Adds a callback public function to the NetConnection instance. This public function will fire ClipEvents whenever
         * the callback is invoked in the connection.
         * @param name
         * @param listener
         * @return
         * @see ClipEventType#CONNECTION_EVENT
         */
        public function addConnectionCallback(name : String, listener : Function) : void {
            CONFIG::LOGGING {
                Log.debug("addConnectionCallback()");
            }
            return;
        }

        /**
         * Adds a callback public function to the NetStream object. This public function will fire a ClipEvent of type StreamEvent whenever
         * the callback has been invoked on the stream. The invokations typically come from a server-side app running
         * on RTMP server.
         * @param name
         * @param listener
         * @return
         * @see ClipEventType.NETSTREAM_EVENT
         */
        public function addStreamCallback(name : String, listener : Function) : void {
            CONFIG::LOGGING {
                Log.debug("addStreamCallback()");
            }
            return;
        }

        /**
         * Get the current stream callbacks.
         * @return a dictionary of callbacks, keyed using callback names and values being the callback functions
         */
        public function get streamCallbacks() : Dictionary {
            CONFIG::LOGGING {
                Log.debug("get streamCallbacks()");
            }
            return null;
        }

        /**
         * Gets the underlying NetStream object.
         * @return the netStream currently in use, or null if this provider has not started streaming yet
         */
        public function get netStream() : NetStream {
            CONFIG::LOGGING {
                Log.debug("get netStream()");
            }
            return _hls.stream;
        }

        /**
         * Gets the underlying netConnection object.
         * @return the netConnection currently in use, or null if this provider has not started streaming yet
         */
        public function get netConnection() : NetConnection {
            CONFIG::LOGGING {
                Log.debug("get netConnection()");
            }
            return null;
        }

        /**
         * Sets a time provider to be used by this StreamProvider. Normally the playhead time is queried from
         * the NetStream.time property.
         *
         * @param timeProvider
         */
        public function set timeProvider(timeProvider : TimeProvider) : void {
            CONFIG::LOGGING {
                Log.debug("set timeProvider()");
            }
            _timeProvider = timeProvider;
            return;
        }

        /**
         * Gets the type of StreamProvider either http, rtmp, psuedo.
         */
        public function get type() : String {
            return "httpstreaming";
        }

        /**
         * Switch the stream in realtime with / without dynamic stream switching support
         *
         * @param event ClipEvent the clip event
         * @param clip Clip the clip to switch to
         * @param netStreamPlayOptions Object the NetStreamPlayOptions object to enable dynamic stream switching
         */
        public function switchStream(event : ClipEvent, clip : Clip, netStreamPlayOptions : Object = null) : void {
            CONFIG::LOGGING {
                Log.info("switchStream()");
            }
            return;
        }
    }
}
