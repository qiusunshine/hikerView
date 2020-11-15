/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.demux {

    import flash.utils.ByteArray;
    CONFIG::LOGGING {
    import org.mangui.hls.HLSSettings;
    import org.mangui.hls.utils.Log;
    }

    /** Constants and utilities for the H264 video format. **/
    public class Nalu {

        private static var _audNalu : ByteArray;
        // static initializer
        {
            _audNalu = new ByteArray();
            _audNalu.length = 2;
            _audNalu.writeByte(0x09);
            _audNalu.writeByte(0xF0);
        };


        /** Return an array with NAL delimiter indexes. **/
        public static function getNALU(nalu : ByteArray, position : uint) : Vector.<VideoFrame> {
            var len : uint = nalu.length,i : uint = position;
            var unitHeader : int,lastUnitHeader : int = 0;
            var unitStart : int,lastUnitStart : int = 0;
            var unitType : int,lastUnitType : int = 0;
            var audFound : Boolean = false;
            var value : uint,state : uint = 0;
            var units : Vector.<VideoFrame> = new Vector.<VideoFrame>();
            // Loop through data to find NAL startcodes.
            while (i < len) {
                // finding 3 or 4-byte start codes (00 00 01 OR 00 00 00 01)
                value = nalu[i++];
                switch(state)
                {
                    case 0:
                        if(!value) {
                            state = 1;
                            // unitHeader is NAL header offset
                            unitHeader=i-1;
                        }
                        break;
                    case 1:
                        if(value) {
                            state = 0;
                        } else {
                            state = 2;
                        }
                        break;
                    case 2:
                    case 3:
                        if(value) {
                            if(value === 1 && i < len) {
                                unitType = nalu[i] & 0x1f;
                                if(unitType == 9) {
                                    audFound = true;
                                }
                                if(lastUnitStart) {
                                    // use Math.min(4,...) as max header size is 4.
                                    // in case there are any leading zeros
                                    // such as 00 00 00 00 00 00 01
                                    //                  ^^
                                    // we need to ignore them as they are part of previous NAL unit
                                    units.push(new VideoFrame(Math.min(4,lastUnitStart-lastUnitHeader), i-state-1-lastUnitStart, lastUnitStart, lastUnitType));
                                }
                                lastUnitStart = i;
                                lastUnitType = unitType;
                                lastUnitHeader = unitHeader;
                                if(audFound == true && (unitType === 1 || unitType === 5)) {
                                  // OPTI !!! if AUD unit already parsed and if IDR/NDR unit, consider it is last NALu
                                  i = len;
                                }
                            }
                            state = 0;
                        } else {
                            state = 3;
                        }
                        break;
                    default:
                        break;
                }
            }
            //push last unit
            if(lastUnitStart) {
                units.push(new VideoFrame(Math.min(4,lastUnitStart-lastUnitHeader), len-lastUnitStart, lastUnitStart, lastUnitType));
            }
            // Reset position and return results.
            CONFIG::LOGGING {
                if (HLSSettings.logDebug2) {
                    /** H264 NAL unit names. **/
                    const NAMES : Array = ['Unspecified',// 0
                    'NDR',                          // 1
                    'Partition A',                  // 2
                    'Partition B',                  // 3
                    'Partition C',                  // 4
                    'IDR',                          // 5
                    'SEI',                          // 6
                    'SPS',                          // 7
                    'PPS',                          // 8
                    'AUD',                          // 9
                    'End of Sequence',              // 10
                    'End of Stream',                // 11
                    'Filler Data'// 12
                    ];
                    if (units.length) {
                        var txt : String = "AVC: ";
                        for (i = 0; i < units.length; i++) {
                            txt += NAMES[units[i].type] + ","; //+ ":" + units[i].length
                        }
                        Log.debug2(txt.substr(0,txt.length-1) + " slices");
                    } else {
                        Log.debug2('AVC: no NALU slices found');
                    }
                }
            }
            nalu.position = position;
            return units;
        };

        public static function get AUD():ByteArray {
            return _audNalu;
        }
		
		private static function findNextUnescapeIndex(bytes : ByteArray, offset:int, limit:int):int {
			for (var i:int = offset; i < limit - 2; i++) {
				if (bytes[i] == 0x00 && bytes[i + 1] == 0x00 && bytes[i + 2] == 0x03) {
					return i;
				}
			}
			return limit;
		}
		
		private static function arrayCopy(src:ByteArray,  srcPos:int, dest:ByteArray, destPos:int, length:int):void {
			var iterations:int = Math.min(Math.min(length, src.length - srcPos), dest.length - destPos);
			for(var i:int = 0; i < iterations; i++)
				dest[destPos + i] = src[srcPos + i];
		}
		
		public static function unescapeStream(data : ByteArray, position: int,limit: uint):int {
			var scratchEscapeCount:int = 0;
			var scratchEscapePositions:Array = new Array();
					
			while (position < limit) {
				position = findNextUnescapeIndex(data, position, limit);
				if (position < limit) {
					scratchEscapeCount++;
					scratchEscapePositions.push(position);
					position += 3;
				}
			}
			
			var unescapedLength:int = limit - scratchEscapeCount;
			var escapedPosition:int = 0; // The position being read from.
			var unescapedPosition:int = 0; // The position being written to.
			for (var i:int = 0; i < scratchEscapeCount; i++) {
				var nextEscapePosition:int = scratchEscapePositions[i];
				var copyLength:int = nextEscapePosition - escapedPosition;
				arrayCopy(data, escapedPosition, data, unescapedPosition, copyLength);
				unescapedPosition += copyLength;
				data[unescapedPosition++] = 0;
				data[unescapedPosition++] = 0;
				escapedPosition += copyLength + 3;
			}
			
			var remainingLength:int = unescapedLength - unescapedPosition;
			arrayCopy(data, escapedPosition, data, unescapedPosition, remainingLength);
			
			return unescapedLength;
		}
    }
}
