/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.constant {
    /** Max Level Capping Modes **/
    public class HLSMaxLevelCappingMode {
        /**
         * max capped level should be the one with the dimensions equal or greater than the stage dimensions (so the video will be downscaled)
         */
        public static const DOWNSCALE : String = "downscale";
        
        /**
         *  max capped level should be the one with the dimensions equal or lower than the stage dimensions (so the video will be upscaled)
         */
        public static const UPSCALE : String = "upscale";
    }
}