/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.utils {
    public class PTS {
        /* find PTS value nearest a given reference PTS value 
         * 
         * PTS retrieved from demux are within a range of
         * (+/-) 2^32/90 - 1 = (+/-) 47721858
         * when reaching upper limit, PTS will loop to lower limit
         * this cause some issues with fragment duration calculation
         * this method will normalize a given PTS value and output a result 
         * that is closest to provided PTS reference value.
         * i.e it could output values bigger than the (+/-) 2^32/90.
         * this will avoid PTS looping issues.  
         */
        public static function normalize(reference : Number, value : Number) : Number {
            var offset : Number;
            if (reference < value) {
                // - 2^33/90
                offset = -95443717;
            } else {
                // + 2^33/90
                offset = 95443717;
            }
            // 2^32 / 90
            while (!isNaN(reference) && (Math.abs(value - reference) > 47721858)) {
                value += offset;
            }
            return value;
        }
    }
}
