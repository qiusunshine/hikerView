/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins.traits {
    import org.mangui.hls.HLS;
    import org.mangui.hls.event.HLSEvent;
    import org.osmf.traits.DynamicStreamTrait;
    import org.osmf.utils.OSMFStrings;

    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    public class HLSDynamicStreamTrait extends DynamicStreamTrait {
        private var _hls : HLS;

        public function HLSDynamicStreamTrait(hls : HLS) {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait()");
            }
            _hls = hls;
            _hls.addEventListener(HLSEvent.LEVEL_SWITCH, _levelSwitchHandler);
            super(true, _hls.startLevel, hls.levels.length);
        }

        override public function dispose() : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:dispose");
            }
            _hls.removeEventListener(HLSEvent.LEVEL_SWITCH, _levelSwitchHandler);
            super.dispose();
        }

        override public function getBitrateForIndex(index : int) : Number {
            if (index > numDynamicStreams - 1 || index < 0) {
                throw new RangeError(OSMFStrings.getString(OSMFStrings.STREAMSWITCH_INVALID_INDEX));
            }
            var bitrate : Number = _hls.levels[index].bitrate / 1000;
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:getBitrateForIndex(" + index + ")=" + bitrate);
            }
            return bitrate;
        }

        override public function switchTo(index : int) : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:switchTo(" + index + ")/max:" + maxAllowedIndex);
            }
            if (index < 0 || index > maxAllowedIndex) {
                throw new RangeError(OSMFStrings.getString(OSMFStrings.STREAMSWITCH_INVALID_INDEX));
            }
            autoSwitch = false;
            if (!switching) {
                setSwitching(true, index);
            }
        }

        override protected function autoSwitchChangeStart(value : Boolean) : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:autoSwitchChangeStart:" + value);
            }
            if (value == true && _hls.autoLevel == false) {
                _hls.nextLevel = -1;
            }
        }

        override protected function switchingChangeStart(newSwitching : Boolean, index : int) : void {
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:switchingChangeStart(newSwitching/index):" + newSwitching + "/" + index);
            }
            if (newSwitching) {
                _hls.currentLevel = index;
            }
        }

        /** Update playback position/duration **/
        private function _levelSwitchHandler(event : HLSEvent) : void {
            var newLevel : int = event.level;
            CONFIG::LOGGING {
            Log.debug("HLSDynamicStreamTrait:_qualitySwitchHandler:" + newLevel);
            }
            setCurrentIndex(newLevel);
            setSwitching(false, newLevel);
        };
    }
}