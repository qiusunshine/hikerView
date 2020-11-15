/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins.traits {
    import org.mangui.hls.HLS;
    import org.mangui.hls.event.HLSEvent;
    import org.osmf.media.MediaResourceBase;
    import org.osmf.net.NetStreamLoadTrait;
    import org.osmf.traits.LoaderBase;

    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    public class HLSNetStreamLoadTrait extends NetStreamLoadTrait {
        private var _hls : HLS;
        private var _timeLoaded : Number;
        private var _timeTotal : Number;

        public function HLSNetStreamLoadTrait(hls : HLS, duration : Number, loader : LoaderBase, resource : MediaResourceBase) {
            CONFIG::LOGGING {
            Log.debug("HLSNetStreamLoadTrait()");
            }
            super(loader, resource);
            _hls = hls;
            _timeLoaded = 0;
            _timeTotal = duration;
            super.netStream = _hls.stream;
            _hls.addEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
        }

        override public function dispose() : void {
            CONFIG::LOGGING {
            Log.debug("HLSNetStreamLoadTrait:dispose");
            }
            _hls.removeEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
            super.dispose();
        }

        override public function get bytesLoaded() : Number {
            return _timeLoaded;
        }

        override public function get bytesTotal() : Number {
            return _timeTotal;
        }

        public function get hls() : HLS {
            return _hls;
        }

        /**  **/
        private function _mediaTimeHandler(event : HLSEvent) : void {
            var timeTotal : Number = Math.round(10 * event.mediatime.duration) / 10;
            var timeLoaded : Number = Math.max(0,Math.round(10 * (event.mediatime.position + event.mediatime.buffer)) / 10);

            if (_timeTotal != timeTotal) {
                if (timeTotal < _timeLoaded || timeTotal < 0) {
                    timeTotal = NaN;
                }
                _timeTotal = timeTotal;
                setBytesTotal(timeTotal);
            }

            if (_timeLoaded != timeLoaded && timeLoaded <= timeTotal) {
                _timeLoaded = timeLoaded;
                setBytesLoaded(timeLoaded);
            }
        };
    }
}