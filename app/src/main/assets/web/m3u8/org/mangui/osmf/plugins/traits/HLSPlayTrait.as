/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins.traits {
    import org.mangui.hls.HLS;
    import org.mangui.hls.constant.HLSPlayStates;
    import org.mangui.hls.event.HLSEvent;
    import org.osmf.traits.PlayState;
    import org.osmf.traits.PlayTrait;

    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    public class HLSPlayTrait extends PlayTrait {
        private var _hls : HLS;
        private var streamStarted : Boolean = false;

        public function HLSPlayTrait(hls : HLS) {
            CONFIG::LOGGING {
            Log.debug("HLSPlayTrait()");
            }
            super();
            _hls = hls;
            _hls.addEventListener(HLSEvent.PLAYBACK_STATE, _stateChangedHandler);
            _hls.addEventListener(HLSEvent.PLAYBACK_COMPLETE, _playbackComplete);
        }

        override public function dispose() : void {
            CONFIG::LOGGING {
            Log.debug("HLSPlayTrait:dispose");
            }
            _hls.removeEventListener(HLSEvent.PLAYBACK_STATE, _stateChangedHandler);
            _hls.removeEventListener(HLSEvent.PLAYBACK_COMPLETE, _playbackComplete);
            super.dispose();
        }

        override protected function playStateChangeStart(newPlayState:String):void {
            CONFIG::LOGGING {
            Log.info("HLSPlayTrait:playStateChangeStart:" + newPlayState);
            }
            switch (newPlayState) {
                case PlayState.PLAYING:
                    if (!streamStarted) {
                        _hls.stream.play();
                        streamStarted = true;
                    }
                    else {
                        _hls.stream.resume();
                    }
                    break;
                case PlayState.PAUSED:
                    _hls.stream.pause();
                    break;
                case PlayState.STOPPED:
                    streamStarted = false;
                    _hls.stream.close();
                    break;
            }
        }

        /** state changed handler **/
        private function _stateChangedHandler(event:HLSEvent):void {
            switch (event.state) {
                case HLSPlayStates.PLAYING:
                CONFIG::LOGGING {
                    Log.debug("HLSPlayTrait:_stateChangedHandler:setBuffering(true)");
                }
                    if (!streamStarted) {
                        streamStarted = true;
                        play();
                    }
                default:
            }
        }

        /** playback complete handler **/
        private function _playbackComplete(event : HLSEvent) : void {
            stop();
        }
    }
}