/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins.traits {
    import org.mangui.hls.HLS;
    import org.mangui.hls.constant.HLSTypes;
    import org.mangui.hls.event.HLSEvent;
    import org.osmf.traits.TimeTrait;
    import org.osmf.events.TimeEvent;

    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    public class HLSTimeTrait extends TimeTrait {
        private var _hls : HLS;

        public function HLSTimeTrait(hls : HLS, duration : Number = 0) {
            CONFIG::LOGGING {
            Log.debug("HLSTimeTrait()");
            }
            super(duration);
            setCurrentTime(0);
            _hls = hls;
            _hls.addEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
            _hls.addEventListener(HLSEvent.PLAYBACK_COMPLETE, _playbackComplete);
        }

        override public function dispose() : void {
            CONFIG::LOGGING {
            Log.debug("HLSTimeTrait:dispose");
            }
            _hls.removeEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
            _hls.removeEventListener(HLSEvent.PLAYBACK_COMPLETE, _playbackComplete);
            super.dispose();
        }

        override protected function signalComplete():void
        {
            // live streams shouldn't end based on TimeTrait
            if (_hls.type !== HLSTypes.LIVE)
            {
                dispatchEvent(new TimeEvent(TimeEvent.COMPLETE));
            }
        }

        /** Update playback position/duration **/
        private function _mediaTimeHandler(event : HLSEvent) : void {
            var newDuration : Number = event.mediatime.duration;
            var newPosition : Number = Math.max(0, event.mediatime.position);
            setDuration(newDuration);
            setCurrentTime(newPosition);
        };

        /** playback complete handler **/
        private function _playbackComplete(event : HLSEvent) : void {
            signalComplete();
        }
    }
}