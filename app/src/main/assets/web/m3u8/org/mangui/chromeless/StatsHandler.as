/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.chromeless {
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.event.HLSLoadMetrics;
    import org.mangui.hls.event.HLSPlayMetrics;
    import org.mangui.hls.HLS;
    import org.mangui.hls.stream.HLSNetStream;
    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    /*
     * class that handle per playback session stats
     */
     public class StatsHandler {
        /** Reference to the HLS controller. **/
        private var _hls : HLS;
        private var _stats : Object;
        private var _sumLatency : int;
        private var _sumKbps : int;
        private var _sumAutoLevel : int;
        private var _levelLastAuto : Boolean;

        public function StatsHandler(hls : HLS) {
            _hls = hls;
            _hls.addEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_LOADED, _fragmentLoadedHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_PLAYING,_fragmentPlayingHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_SKIPPED,_fragmentSkippedHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_LOAD_EMERGENCY_ABORTED,_fragmentLoadEmergencyAbortedHandler);
            _hls.addEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
        }

        public function dispose() : void {
            _hls.removeEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.removeEventListener(HLSEvent.FRAGMENT_LOADED, _fragmentLoadedHandler);
            _hls.removeEventListener(HLSEvent.FRAGMENT_PLAYING, _fragmentPlayingHandler);
            _hls.removeEventListener(HLSEvent.FRAGMENT_SKIPPED,_fragmentSkippedHandler);
            _hls.removeEventListener(HLSEvent.FRAGMENT_LOAD_EMERGENCY_ABORTED,_fragmentLoadEmergencyAbortedHandler);
            _hls.removeEventListener(HLSEvent.MEDIA_TIME, _mediaTimeHandler);
        }

        public function get stats() : Object {
            return _stats;
        }

        private function _manifestLoadedHandler(event : HLSEvent) : void {
            _stats = {};
        };

        private function _fragmentLoadedHandler(event : HLSEvent) : void {
            var metrics : HLSLoadMetrics = event.loadMetrics;
            var latency : int = metrics.loading_begin_time-metrics.loading_request_time;
            var bandwidth : int = metrics.bandwidth/1000;
            if(_stats.fragBuffered) {
              _stats.fragLastLatency = latency;
              _stats.fragMinLatency = Math.min(_stats.fragMinLatency,latency);
              _stats.fragMaxLatency = Math.max(_stats.fragMaxLatency,latency);
              _stats.fragLastKbps = bandwidth;
              _stats.fragMinKbps = Math.min(_stats.fragMinKbps,bandwidth);
              _stats.fragMaxKbps = Math.max(_stats.fragMaxKbps,bandwidth);
              _stats.fragBuffered++;
            } else {
                  _stats.fragMinLatency = _stats.fragMaxLatency = latency;
                  _stats.fragMinKbps = _stats.fragMaxKbps = bandwidth;
                  _stats.fragBuffered = 1;
                  _stats.fragBufferedBytes = 0;
                  _sumLatency=0;
                  _sumKbps=0;
            }
            _sumLatency+=latency;
            _sumKbps+=bandwidth;
            _stats.fragBufferedBytes+=metrics.size;
            _stats.fragAvgLatency = Math.round(_sumLatency/_stats.fragBuffered);
            _stats.fragAvgKbps = Math.round(_sumKbps/_stats.fragBuffered);
        }

        private function _fragmentPlayingHandler(event : HLSEvent) : void {
        if(_stats.fragPlaying) {
            _stats.fragPlaying++;
        } else {
            _stats.fragPlaying = 1;
        }
      }

      private function _fragmentSkippedHandler(event : HLSEvent) : void {
        if(_stats.fragSkipped) {
            _stats.fragSkipped++;
        } else {
            _stats.fragSkipped = 1;
        }
      }

      private function _fragmentLoadEmergencyAbortedHandler(event : HLSEvent) : void {
        if(_stats.fragLoadEmergencyAborted) {
            _stats.fragLoadEmergencyAborted++;
        } else {
            _stats.fragLoadEmergencyAborted = 1;
        }
      }

      private function _mediaTimeHandler(event : HLSEvent) : void {
        _stats.droppedFrames = _hls.droppedFrames;
      }
  }
}
