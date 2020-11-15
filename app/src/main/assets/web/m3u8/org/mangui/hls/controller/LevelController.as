/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.controller {
    import flash.display.Stage;
    import org.mangui.hls.constant.HLSLoaderTypes;
    import org.mangui.hls.constant.HLSMaxLevelCappingMode;
    import org.mangui.hls.event.HLSEvent;
    import org.mangui.hls.event.HLSLoadMetrics;
    import org.mangui.hls.HLS;
    import org.mangui.hls.HLSSettings;
    import org.mangui.hls.model.Level;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
    }
    /** Class that manages auto level selection
     *
     * this is an implementation based on Serial segment fetching method from
     * http://www.cs.tut.fi/~moncef/publications/rate-adaptation-IC-2011.pdf
     */
    public class LevelController {
        /** Reference to the HLS controller. **/
        private var _hls : HLS;
        /** switch up threshold **/
        private var _switchup : Vector.<Number> = null;
        /** switch down threshold **/
        private var _switchdown : Vector.<Number> = null;
        /** bitrate array **/
        private var _bitrate : Vector.<uint> = null;
        /** vector of levels with unique dimension with highest bandwidth **/
        private var _maxUniqueLevels : Vector.<Level> = null;
        /** nb level **/
        private var _nbLevel : int = 0;
        private var _lastSegmentDuration : Number;
        private var _lastFetchDuration : Number;
        private var  lastBandwidth : int;
        private var  _autoLevelCapping : int;
        private var  _startLevel : int = -1;
        private var  _fpsController : FPSController;

        /** Create the loader. **/
        public function LevelController(hls : HLS) : void {
            _hls = hls;
            _fpsController = new FPSController(hls);
            /* low priority listener, so that other listeners with default priority
               could seamlessly set hls.startLevel in their HLSEvent.MANIFEST_PARSED listener */
            _hls.addEventListener(HLSEvent.MANIFEST_PARSED, _manifestParsedHandler, false, int.MIN_VALUE);
            _hls.addEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_LOADED, _fragmentLoadedHandler);
            _hls.addEventListener(HLSEvent.FRAGMENT_LOAD_EMERGENCY_ABORTED, _fragmentLoadedHandler);
        }
        ;

        public function dispose() : void {
            _fpsController.dispose();
            _fpsController = null;
            _hls.removeEventListener(HLSEvent.MANIFEST_PARSED, _manifestParsedHandler);
            _hls.removeEventListener(HLSEvent.MANIFEST_LOADED, _manifestLoadedHandler);
            _hls.removeEventListener(HLSEvent.FRAGMENT_LOADED, _fragmentLoadedHandler);
            _hls.removeEventListener(HLSEvent.FRAGMENT_LOAD_EMERGENCY_ABORTED, _fragmentLoadedHandler);
        }

        private function _fragmentLoadedHandler(event : HLSEvent) : void {
            var metrics : HLSLoadMetrics = event.loadMetrics;
            // only monitor main fragment metrics for level switching
            if(metrics.type == HLSLoaderTypes.FRAGMENT_MAIN) {
                lastBandwidth = metrics.bandwidth;
                _lastSegmentDuration = metrics.duration;
                _lastFetchDuration = metrics.processing_duration;
            }
        }

        private function _manifestParsedHandler(event : HLSEvent) : void {
            if(HLSSettings.autoStartLoad) {
                // upon manifest parsed event, trigger a level switch to load startLevel playlist
                _hls.dispatchEvent(new HLSEvent(HLSEvent.LEVEL_SWITCH, startLevel));
            }
        }

        private function _manifestLoadedHandler(event : HLSEvent) : void {
            var levels : Vector.<Level> = event.levels;
            var maxswitchup : Number = 0;
            var minswitchdwown : Number = Number.MAX_VALUE;
            _nbLevel = levels.length;
            _bitrate = new Vector.<uint>(_nbLevel, true);
            _switchup = new Vector.<Number>(_nbLevel, true);
            _switchdown = new Vector.<Number>(_nbLevel, true);
            _autoLevelCapping = -1;
            _lastSegmentDuration = 0;
            _lastFetchDuration = 0;
            lastBandwidth = 0;

            var i : int;

            for (i = 0; i < _nbLevel; i++) {
                _bitrate[i] = levels[i].bitrate;
            }

            for (i = 0; i < _nbLevel - 1; i++) {
                _switchup[i] = (_bitrate[i + 1] - _bitrate[i]) / _bitrate[i];
                maxswitchup = Math.max(maxswitchup, _switchup[i]);
            }
            for (i = 0; i < _nbLevel - 1; i++) {
                _switchup[i] = Math.min(maxswitchup, 2 * _switchup[i]);

                CONFIG::LOGGING {
                    Log.debug("_switchup[" + i + "]=" + _switchup[i]);
                }
            }

            for (i = 1; i < _nbLevel; i++) {
                _switchdown[i] = (_bitrate[i] - _bitrate[i - 1]) / _bitrate[i];
                minswitchdwown = Math.min(minswitchdwown, _switchdown[i]);
            }
            for (i = 1; i < _nbLevel; i++) {
                _switchdown[i] = Math.max(2 * minswitchdwown, _switchdown[i]);

                CONFIG::LOGGING {
                    Log.debug("_switchdown[" + i + "]=" + _switchdown[i]);
                }
            }

            if (HLSSettings.capLevelToStage) {
                _maxUniqueLevels = _maxLevelsWithUniqueDimensions;
            }
        }
        ;

        public function getAutoStartBestLevel(downloadBandwidth : int, initialDelay : int, lastSegmentDuration : int) : int {
            var bwFactor : Number;
            var max_level : int = _maxLevel;
            // in case initial delay is capped
            if(HLSSettings.autoStartMaxDuration != -1) {
                // if we are above initial delay, stick to level 0
                if(initialDelay >= HLSSettings.autoStartMaxDuration) {
                    return 0;
                } else {
                    // if we still have some time to load another fragment, determine load factor:
                    // if we have 10000ms fragment, and we have 1000s left, we need a bw 10 times bigger to accomodate
                    bwFactor = lastSegmentDuration/(HLSSettings.autoStartMaxDuration-initialDelay);
                }
            } else {
                bwFactor = 1;
            }
            CONFIG::LOGGING {
                Log.debug("getAutoStartBestLevel,initialDelay/max delay/bwFactor=" + initialDelay + "/" + HLSSettings.autoStartMaxDuration + "/" + bwFactor.toFixed(2));
            }
            for (var i : int = max_level; i >= 0; i--) {
                if (_bitrate[i]*bwFactor <= downloadBandwidth) {
                    return i;
                }
            }
            return 0;
        }

        private function get _maxLevelsWithUniqueDimensions() : Vector.<Level> {
            var filter : Function = function(l : Level, i : int, v : Vector.<Level>) : Boolean {
                if (l.width > 0 && l.height > 0) {
                    if (i + 1 < v.length) {
                        var nextLevel : Level = v[i + 1];
                        if (l.width != nextLevel.width && l.height != nextLevel.height) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                return false;
            };

            return _hls.levels.filter(filter);
        }


        /** Return the capping/max level value that could be used by automatic level selection algorithm **/
        public function get autoLevelCapping() : int {
            return _autoLevelCapping;
        }

        /** set the capping/max level value that could be used by automatic level selection algorithm **/
        public function set autoLevelCapping(newLevel : int) : void {
            _autoLevelCapping = newLevel;
        }

        private function get _maxLevel() : int {
            // if set, _autoLevelCapping takes precedence
            if(_autoLevelCapping >= 0) {
                return Math.min(_nbLevel - 1, _autoLevelCapping);
            } else if (HLSSettings.capLevelToStage) {
                var maxLevelsCount : int = _maxUniqueLevels.length;

                if (_hls.stage && maxLevelsCount) {
                    var maxLevel : Level = this._maxUniqueLevels[0],
                    maxLevelIdx : int = maxLevel.index,
                    stage : Stage = _hls.stage,
                    sWidth : Number = stage.stageWidth,
                    sHeight : Number = stage.stageHeight,
                    lWidth : int,
                    lHeight : int,
                    i : int;

                   // retina display support
                   // contentsScaleFactor was added in FP11.5, but this allows us to include the option in all builds
                    try {
                        var contentsScaleFactor : int =  stage['contentsScaleFactor'];
                        sWidth*=contentsScaleFactor;
                        sHeight*=contentsScaleFactor;
                    } catch(e : Error) {
                       // Ignore errors, we're running in FP < 11.5
                    }


                    switch (HLSSettings.maxLevelCappingMode) {
                        case HLSMaxLevelCappingMode.UPSCALE:
                            for (i = maxLevelsCount - 1; i >= 0; i--) {
                                maxLevel = this._maxUniqueLevels[i];
                                maxLevelIdx = maxLevel.index;
                                lWidth = maxLevel.width;
                                lHeight = maxLevel.height;
                                CONFIG::LOGGING {
                                    Log.debug("stage size: " + sWidth + "x" + sHeight + " ,level" + maxLevelIdx + " size: " + lWidth + "x" + lHeight);
                                }
                                if (sWidth >= lWidth || sHeight >= lHeight) {
                                    break;
                                    // from for loop
                                }
                            }
                            break;
                        case HLSMaxLevelCappingMode.DOWNSCALE:
                            for (i = 0; i < maxLevelsCount; i++) {
                                maxLevel = this._maxUniqueLevels[i];
                                maxLevelIdx = maxLevel.index;
                                lWidth = maxLevel.width;
                                lHeight = maxLevel.height;
                                CONFIG::LOGGING {
                                    Log.debug("stage size: " + sWidth + "x" + sHeight + " ,level" + maxLevelIdx + " size: " + lWidth + "x" + lHeight);
                                }
                                if (sWidth <= lWidth || sHeight <= lHeight) {
                                    break;
                                    // from for loop
                                }
                            }
                            break;
                    }
                    CONFIG::LOGGING {
                        Log.debug("max capped level idx: " + maxLevelIdx);
                    }
                }
                return maxLevelIdx;
            } else {
                return _nbLevel - 1;
            }
        }

        /** Update the quality level for the next fragment load. **/
        public function getnextlevel(current_level : int, buffer : Number) : int {
            if (_lastFetchDuration == 0 || _lastSegmentDuration == 0) {
                return 0;
            }

            /* rsft : remaining segment fetch time : available time to fetch next segment
            it depends on the current playback timestamp , the timestamp of the first frame of the next segment
            and TBMT, indicating a desired latency between the time instant to receive the last byte of a
            segment to the playback of the first media frame of a segment
            buffer is start time of next segment
            TBMT is the buffer size we need to ensure (we need at least 2 segments buffered */
            var rsft : Number = 1000 * buffer - 2 * _lastFetchDuration;
            var sftm : Number = Math.min(_lastSegmentDuration, rsft) / _lastFetchDuration;
            var max_level : Number = _maxLevel;
            var switch_to_level : int = current_level;
            // CONFIG::LOGGING {
            // Log.info("rsft:" + rsft);
            // Log.info("sftm:" + sftm);
            // }
            // }
            /* to switch level up :
            rsft should be greater than switch up condition
             */
            if ((current_level < max_level) && (sftm > (1 + _switchup[current_level]))) {
                CONFIG::LOGGING {
                    Log.debug("sftm:> 1+_switchup[_level]=" + (1 + _switchup[current_level]));
                }
                switch_to_level = current_level + 1;
            }

            /* to switch level down :
            rsft should be smaller than switch up condition,
            or the current level is greater than max level
             */ else if ((current_level > max_level && current_level > 0) || (current_level > 0 && (sftm < 1 - _switchdown[current_level]))) {
                CONFIG::LOGGING {
                    Log.debug("sftm < 1-_switchdown[current_level]=" + _switchdown[current_level]);
                }
                var bufferratio : Number = 1000 * buffer / _lastSegmentDuration;
                /* find suitable level matching current bandwidth, starting from current level
                when switching level down, we also need to consider that we might need to load two fragments.
                the condition (bufferratio > 2*_levels[j].bitrate/_lastBandwidth)
                ensures that buffer time is bigger than than the time to download 2 fragments from level j, if we keep same bandwidth.
                 */

                for (var j : int = current_level - 1; j >= 0; j--) {
                    if (_bitrate[j] <= lastBandwidth && (bufferratio > 2 * _bitrate[j] / lastBandwidth)) {
                        switch_to_level = j;
                        break;
                    }
                    if (j == 0) {
                        switch_to_level = 0;
                    }
                }
            }

            // Then we should check if selected level is higher than max_level if so, than take the min of those two
            switch_to_level = Math.min(max_level, switch_to_level);

            CONFIG::LOGGING {
                if (switch_to_level != current_level) {
                    Log.debug("switch to level " + switch_to_level);
                }
            }

            return switch_to_level;
        }

        // get level index of first level appearing in the manifest
        public function get firstLevel() : int {
            var levels : Vector.<Level> = _hls.levels;
            for (var i : int = 0; i < levels.length; i++) {
                if (levels[i].manifest_index == 0) {
                    return i;
                }
            }
            return 0;
        }

        public function isStartLevelSet() : Boolean {
            return (_startLevel >=0);
        }

        /*  set the quality level used when starting a fresh playback */
        public function set startLevel(level : int) : void {
            _startLevel = level;
        };

        public function get startLevel() : int {
            var start_level : int = -1;
            var levels : Vector.<Level> = _hls.levels;
            if (levels) {
                // if set, _startLevel takes precedence
                if(_startLevel >=0) {
                    return Math.min(levels.length-1,_startLevel);
                } else if (HLSSettings.startFromLevel === -2) {
                    // playback will start from the first level appearing in Manifest (not sorted by bitrate)
                    return firstLevel;
                } else if (HLSSettings.startFromLevel === -1 && HLSSettings.startFromBitrate === -1) {
                    /* if startFromLevel is set to -1, it means that effective startup level
                     * will be determined from first segment download bandwidth
                     * let's use lowest bitrate for this download bandwidth assessment
                     * this should speed up playback start time
                     */
                    return 0;
                } else {
                    // set up start level as being the lowest non-audio level.
                    for (var i : int = 0; i < levels.length; i++) {
                        if (!levels[i].audio) {
                            start_level = i;
                            break;
                        }
                    }
                    // in case of audio only playlist, force startLevel to 0
                    if (start_level == -1) {
                        CONFIG::LOGGING {
                            Log.info("playlist is audio-only");
                        }
                        start_level = 0;
                    } else {
                        if (HLSSettings.startFromBitrate > 0) {
                            start_level = findIndexOfClosestLevel(HLSSettings.startFromBitrate);
                        } else if (HLSSettings.startFromLevel > 0) {
                            // adjust start level using a rule by 3
                            start_level += Math.round(HLSSettings.startFromLevel * (levels.length - start_level - 1));
                        }
                    }
                }
                CONFIG::LOGGING {
                    Log.debug("start level :" + start_level);
                }
            }
            return start_level;
        }

        /**
         * @param desiredBitrate
         * @return The index of the level that has a bitrate closest to the desired bitrate.
         */
        private function findIndexOfClosestLevel(desiredBitrate : Number) : int {
            var levelIndex : int = -1;
            var minDistance : Number = Number.MAX_VALUE;
            var levels : Vector.<Level> = _hls.levels;

            for (var index : int = 0; index < levels.length; index++) {
                var level : Level = levels[index];

                var distance : Number = Math.abs(desiredBitrate - level.bitrate);

                if (distance < minDistance) {
                    levelIndex = index;
                    minDistance = distance;
                }
            }
            return levelIndex;
        }

        public function get seekLevel() : int {
            var seek_level : int = -1;
            var levels : Vector.<Level> = _hls.levels;
            if (HLSSettings.seekFromLevel == -1) {
                // keep last level, but don't exceed _maxLevel
                return Math.min(_hls.loadLevel,_maxLevel);
            }

            // set up seek level as being the lowest non-audio level.
            for (var i : int = 0; i < levels.length; i++) {
                if (!levels[i].audio) {
                    seek_level = i;
                    break;
                }
            }
            // in case of audio only playlist, force seek_level to 0
            if (seek_level == -1) {
                seek_level = 0;
            } else {
                if (HLSSettings.seekFromLevel > 0) {
                    // adjust start level using a rule by 3
                    seek_level += Math.round(HLSSettings.seekFromLevel * (levels.length - seek_level - 1));
                }
            }
            CONFIG::LOGGING {
                Log.debug("seek level :" + seek_level);
            }
            return seek_level;
        }
    }
}
