/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {
    import flash.utils.ByteArray;
    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
        import org.mangui.hls.HLSSettings;
    }
    public class AVCC {

        /** H264 profiles. **/
        private static const PROFILES : Object = {'66':'H264 Baseline', '77':'H264 Main', '100':'H264 High'};
        /** Get Avcc header from AVC stream
           See ISO 14496-15, 5.2.4.1 for the description of AVCDecoderConfigurationRecord
         **/
        public static function getAVCC(sps : ByteArray, ppsVect : Vector.<ByteArray>) : ByteArray {
            // Write startbyte
            var avcc : ByteArray = new ByteArray();
            avcc.writeByte(0x01);
            // Write profile, compatibility and level.
            avcc.writeBytes(sps, 1, 3);
            // reserved (6 bits), NALU length size - 1 (2 bits)
            avcc.writeByte(0xFC | 3);
            // reserved (3 bits), num of SPS (5 bits)
            avcc.writeByte(0xE0 | 1);
            // 2 bytes for length of SPS
            avcc.writeShort(sps.length);
            // data of SPS
            avcc.writeBytes(sps, 0, sps.length);
            // Number of PPS
            avcc.writeByte(ppsVect.length);
            for each (var pps : ByteArray in ppsVect) {
                // 2 bytes for length of PPS
                avcc.writeShort(pps.length);
                // data of PPS
                avcc.writeBytes(pps, 0, pps.length);
            }
            CONFIG::LOGGING {
                if (HLSSettings.logDebug2) {
                    // Grab profile/level
                    sps.position = 1;
                    var prf : int = sps.readByte();
                    sps.position = 3;
                    var lvl : int = sps.readByte();
                    Log.debug("AVC: " + PROFILES[prf] + ' level ' + lvl);
                }
            }
            avcc.position = 0;
            return avcc;
        }
        ;
    }
}
