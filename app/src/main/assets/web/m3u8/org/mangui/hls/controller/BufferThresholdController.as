/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.controller {
    import org.mangui.hls.constant.HLSLoaderTypes;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.event.HLSLoadMetrics;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    /** Class that manages buffer threshold values (minBufferLength/lowBufferLength)
     */
    public class BufferThresholdController {
        /** Reference to the HLS controller. **/
        private var _hls : HLS;
        private var _targetduration : Number;
        private var _minBufferLength : Number;

        /** Create the loader. **/
        public function BufferThresholdController(hls : HLS) : void {
            _hls = hls;
            _hls.addEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.addEventListener(HLSEvent.TAGS_LOADED, _fragmentLoadedHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_LOADED, _fragmentLoadedHandler);
        };

        public function dispose() : void {
            _hls.removeEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.removeEventListener(HLSEvent.TAGS_LOADED, _fragmentLoadedHandler);
            _hls.removeEventListener(HLSEvent.FRAGMENT_LOADED, _fragmentLoadedHandler);
        }

        public function get minBufferLength() : Number {
            if (HLSSettings.minBufferLength == -1) {
                return _minBufferLength;
            } else {
                return HLSSettings.minBufferLength;
            }
        }

        public function get lowBufferLength() : Number {
            if (HLSSettings.minBufferLength == -1) {
                // in automode, low buffer threshold should be less than min auto buffer
                return Math.min(minBufferLength / 2, HLSSettings.lowBufferLength);
            } else {
                return HLSSettings.lowBufferLength;
            }
        }

        private function _manifestLoadedHandler(event : HLSEvent) : void {
            _targetduration = event.levels[_hls.startLevel].targetduration;
            _minBufferLength = _targetduration;
        };

        private function _fragmentLoadedHandler(event : HLSEvent) : void {
            var metrics : HLSLoadMetrics = event.loadMetrics;
            // only monitor main fragment metrics for buffer threshold computing
            if(metrics.type == HLSLoaderTypes.FRAGMENT_MAIN) {
                /* set min buf len to be the time to process a complete segment, using current processing rate */
                _minBufferLength = metrics.processing_duration * (_targetduration / metrics.duration);
                // avoid min > max
                if (HLSSettings.maxBufferLength) {
                    _minBufferLength = Math.min(HLSSettings.maxBufferLength, _minBufferLength);
                }

                // avoid _minBufferLength > minBufferLengthCapping
                if (HLSSettings.minBufferLengthCapping > 0) {
                    _minBufferLength = Math.min(HLSSettings.minBufferLengthCapping, _minBufferLength);
                }

                CONFIG::LOGGING {
                    Log.debug2("AutoBufferController:minBufferLength:" + _minBufferLength);
                }
            };
        }
    }
}
