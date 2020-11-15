/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.controller {
    // import flash.events.ThrottleEvent;
    // import flash.events.ThrottleType;
    import flash.events.Event;
    import flash.events.TimerEvent;
    import flash.system.Capabilities;
    import flash.utils.getTimer;
    import flash.utils.Timer;
    import org.mangui.hls.constant.HLSPlayStates;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;
    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    /*
     * class that control/monitor FPS
     */
    public class FPSController {
      /** Reference to the HLS controller. **/
      private var _hls : HLS;
      private var _timer : Timer;
      private var _throttling : Boolean;
      private var _playing : Boolean;
      private var _lastTime : int;
      private var _lastDroppedFrames : int;
      // hardcode event name and state to avoid compilation issue with target player < 11.2
      private static const THROTTLE : String = "throttle";
      private static const RESUME : String = "resume";

      public function FPSController(hls : HLS) {
          _hls = hls;
          _throttling = false;
          _playing = false;
          _lastTime = 0;
          /** Check that Flash Player version is sufficient (11.2 or above) to use throttling event **/
          if(_checkVersion() >= 11.2) {
            _hls.addEventListener(HLSEvent.PLAYBACK_STATE, _playbackStateHandler);
            _hls.addEventListener(HLSEvent.STAGE_SET, _stageSetHandler);
          }
      }

      private function _checkVersion() : Number {
          var verArray : Array = Capabilities.version.split(/\s|,/);
          return Number(String(verArray[1] + "." + verArray[2]));
      }

      public function dispose() : void {
        if(_checkVersion() >= 11.2) {
            _hls.removeEventListener(HLSEvent.PLAYBACK_STATE, _playbackStateHandler);
            _hls.removeEventListener(HLSEvent.STAGE_SET, _stageSetHandler);
            if(_timer) {
              _timer.removeEventListener(TimerEvent.TIMER, _checkFPS);
              _timer.stop();
            }
            if(_hls.stage) {
              _hls.stage.removeEventListener(THROTTLE, onThrottle);
            }
          }
      }

      private function _stageSetHandler(event : HLSEvent) : void {
        CONFIG::LOGGING {
          Log.debug("FPSController:stage defined, listen to throttle event");
        }
        _timer = new Timer(HLSSettings.fpsDroppedMonitoringPeriod,0);
        _timer.addEventListener(TimerEvent.TIMER, _checkFPS);
        _timer.start();
        _hls.stage.addEventListener(THROTTLE, onThrottle);
      }

      private function _playbackStateHandler(event : HLSEvent) : void {
        switch(event.state) {
          case HLSPlayStates.PLAYING:
            // start fps monitoring when switching to playing state
            _playing = true;
            _lastTime = 0;
            CONFIG::LOGGING {
              Log.debug("FPSController:playback starting, start monitoring FPS");
            }
            break;
          default:
            _playing = false;
            // stop fps monitoring in all other cases
            CONFIG::LOGGING {
              Log.debug("FPSController:playback stopped, stop monitoring FPS");
            }
            break;
        }
      };

      private function onThrottle(e : Object) : void {
        CONFIG::LOGGING {
             Log.debug("FPSController:onThrottle:" + e.state + ',fps:' + e.targetFrameRate);
        }
        switch(e.state) {
          case RESUME:
            _throttling=false;
            _lastTime = 0;
            break;
          default:
            _throttling=true;
            break;
        }
      }

      private function _checkFPS(e : Event) : void {
        var currentTime : int = getTimer();
        var droppedFrames : int = _hls.stream.info.droppedFrames;
        // monitor only if not throttling AND playing AND we hold a time reference with nb of dropped frames
        if(_throttling == false && _playing == true && _lastTime) {
          var currentPeriod : int = currentTime-_lastTime;
          var currentDropped : int = droppedFrames - _lastDroppedFrames;
          var currentDropFPS : Number = 1000*currentDropped/currentPeriod;
          var currentFPS : Number = _hls.stream.currentFPS;
          CONFIG::LOGGING {
            Log.debug2("FPSController:currentDropped,currentPeriod,currentDropFPS," + currentDropped +',' + currentPeriod +',' + currentDropFPS.toFixed(1));
          }
          if(currentDropFPS > HLSSettings.fpsDroppedMonitoringThreshold*currentFPS) {
            CONFIG::LOGGING {
              Log.warn("FPSController:drop FPS ratio >" + HLSSettings.fpsDroppedMonitoringThreshold + ',drop/displayed:' +currentDropFPS.toFixed(1)+","+currentFPS.toFixed(1) + " in the last " + HLSSettings.fpsDroppedMonitoringPeriod + "ms");
            }
            _hls.dispatchEvent(new HLSEvent(HLSEvent.FPS_DROP, _hls.currentLevel));
            if(HLSSettings.capLevelonFPSDrop) {
              var currentLevel : int = _hls.currentLevel;
              if(currentLevel > 0 && (_hls.autoLevelCapping == -1 || _hls.autoLevelCapping >= _hls.currentLevel)) {
                var capLevel : int = currentLevel-1;
                CONFIG::LOGGING {
                  Log.warn("FPSController:cap level on FPS drop to " + capLevel);
                }
                _hls.autoLevelCapping = capLevel;
                _hls.dispatchEvent(new HLSEvent(HLSEvent.FPS_DROP_LEVEL_CAPPING, capLevel));
                if(HLSSettings.smoothAutoSwitchonFPSDrop) {
                  if(_hls.autoLevel == true) {
                    CONFIG::LOGGING {
                      Log.warn("FPSController:trigger smooth level switch on frame drop");
                    }
                    _hls.nextLevel = -1;
                    _hls.dispatchEvent(new HLSEvent(HLSEvent.FPS_DROP_SMOOTH_LEVEL_SWITCH));
                  }
                }
              }
            }
          }
        }
        _lastTime = currentTime;
        _lastDroppedFrames = droppedFrames;
      }
  }
}
