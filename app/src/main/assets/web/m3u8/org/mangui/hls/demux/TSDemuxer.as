/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.demux {
    import flash.events.Event;
    import flash.events.EventDispatcher;
    import flash.events.TimerEvent;
    import flash.net.ObjectEncoding;
    import flash.utils.ByteArray;
    import flash.utils.getTimer;
    import flash.utils.Timer;
    import org.mangui.hls.flv.FLVTag;
    import org.mangui.hls.model.AudioTrack;
    import by.blooddy.crypto.Base64;

    CONFIG::LOGGING {
        import org.mangui.hls.utils.Log;
        import org.mangui.hls.HLSSettings;
        import org.mangui.hls.utils.Hex;
    }
    /** Representation of an MPEG transport stream. **/
    public class TSDemuxer extends EventDispatcher implements Demuxer {
        /** read position **/
        private var _readPosition : uint;
        /** is bytearray full ? **/
        private var _dataComplete : Boolean;
        /** TS Sync byte. **/
        private static const SYNCBYTE : uint = 0x47;
        /** TS Packet size in byte. **/
        private static const PACKETSIZE : uint = 188;
        /** Packet ID of the PAT (is always 0). **/
        private static const PAT_ID : int = 0;
        /** Packet ID of the SDT (is always 17). **/
        private static const SDT_ID : int = 17;
        /** Null Packet PID **/
        private static const NULL_PID : int = 0x1fff;
        /** has PMT been parsed ? **/
        private var _pmtParsed : Boolean;
        /** any unknown PID found ? **/
        private var _unknownPIDFound : Boolean;
        /** PMT PID **/
        private var _pmtId : int;
        /** video PID **/
        private var _avcId : int;
        private var _videoPESfound : Boolean;
        /** audio PID **/
        private var _audioId : int;
        private var _audioIsAAC : Boolean;
        private var _audioPESfound : Boolean;
        /** ID3 PID **/
        private var _id3Id : int;
        /** Vector of audio/video tags **/
        private var _tags : Vector.<FLVTag>;
        /* Vector of buffer */
        private var _dataVector : Vector.<ByteArray>;
        private var _dataOffset : uint;
        /* callback functions for audio selection, and parsing progress/complete */
        private var _callback_audioselect : Function;
        private var _callback_progress : Function;
        private var _callback_complete : Function;
        private var _callback_error : Function;
        private var _callback_videometadata : Function;
        /* current audio PES */
        private var _curAudioPES : ByteArray;
        /* current video PES */
        private var _curVideoPES : ByteArray;
        /* current id3 PES */
        private var _curId3PES : ByteArray;
        /* ADTS frame overflow */
        private var _adtsFrameOverflow : ByteArray;
        /* current NAL unit */
        private var _curNalUnit : ByteArray;
        /* current AVC Tag */
        private var _curVideoTag : FLVTag;
        /* ADIF tag inserted ? */
        private var _adifTagInserted : Boolean = false;
        /* last AVCC byte Array */
        private var _avcc : ByteArray;
        private var _timer : Timer;
        private var _totalBytes : uint;
        private var _audioOnly : Boolean;
        private var _audioFound : Boolean;
        private var _audioSelected : Boolean;

        public static function probe(data : ByteArray) : Boolean {
            var pos : uint = data.position;
            var len : uint = Math.min(data.bytesAvailable, PACKETSIZE * 2);
            for (var i : int = 0; i < len; i++) {
                if (data.readByte() == SYNCBYTE) {
                    // ensure that at least two consecutive TS start offset are found
                    if (data.bytesAvailable > PACKETSIZE) {
                        data.position = pos + i + PACKETSIZE;
                        if (data.readByte() == SYNCBYTE) {
                            data.position = pos + i;
                            return true;
                        } else {
                            data.position = pos + i + 1;
                        }
                    }
                }
            }
            data.position = pos;
            return false;
        }

        /** Transmux the M2TS file into an FLV file. **/
        public function TSDemuxer(callback_audioselect : Function,
                                  callback_progress : Function,
                                  callback_complete : Function,
                                  callback_error : Function,
                                  callback_videometadata : Function,
                                  audioOnly : Boolean) {
            _avcc = null;
            _curAudioPES = null;
            _curVideoPES = null;
            _curId3PES = null;
            _curVideoTag = null;
            _curNalUnit = null;
            _adtsFrameOverflow = null;
            _callback_audioselect = callback_audioselect;
            _callback_progress = callback_progress;
            _callback_complete = callback_complete;
            _callback_error = callback_error;
            _callback_videometadata = callback_videometadata;
            _pmtParsed = false;
            _unknownPIDFound = false;
            _pmtId = _avcId = _audioId = _id3Id = -1;
            _audioIsAAC = false;
            _audioPESfound = _videoPESfound = false;
            _tags = new Vector.<FLVTag>();
            _timer = new Timer(0, 0);
            _audioOnly = audioOnly;
            _audioFound = false;
            _audioSelected = true;
        };

        /** append new TS data */
        public function append(data : ByteArray) : void {
            if (_dataVector == null) {
                _dataVector = new Vector.<ByteArray>();
                _dataComplete = false;
                _readPosition = 0;
                _totalBytes = 0;
                _dataOffset = 0;
                _timer.addEventListener(TimerEvent.TIMER, _parseTimer);
            }
            _dataVector.push(data);
            _totalBytes += data.length;
            _timer.start();
        }

        /** cancel demux operation */
        public function cancel() : void {
            CONFIG::LOGGING {
                Log.debug("TS: cancel demux");
            }
            _dataVector = null;
            _curAudioPES = null;
            _curVideoPES = null;
            _curId3PES = null;
            _curVideoTag = null;
            _curNalUnit = null;
            _adtsFrameOverflow = null;
            _avcc = null;
            _tags = new Vector.<FLVTag>();
            _timer.stop();
            _timer.removeEventListener(TimerEvent.TIMER, _parseTimer);
        }

        public function notifycomplete() : void {
            _dataComplete = true;
        }

        public function get audioExpected() : Boolean {
            return (_pmtParsed == false || _audioId != -1);
        }

        public function get videoExpected() : Boolean {
            return (_pmtParsed == false || _avcId != -1);
        }

        private function getNextTSBuffer(start : int) : ByteArray {
            if(_dataVector && start + 188 <= _totalBytes) {
                // find element matching with start offset
                for(var i : int = 0, offset : int = _dataOffset; i < _dataVector.length; i++) {
                    var buffer : ByteArray = _dataVector[i], bufferLength : int = buffer.length;
                    if(start >= offset && start < offset + bufferLength) {
                        buffer.position = start - offset;
                        if(buffer.bytesAvailable >= PACKETSIZE) {
                            if(_pmtParsed && i) {
                                _dataVector.splice(0,i);
                                _dataOffset = offset;
                            }
                            return buffer;
                        } else {
                            // TS packet overlapping between several buffers
                            var ba : ByteArray = new ByteArray();
                            ba.writeBytes(buffer, buffer.position);
                            while(++i < _dataVector.length && ba.length < PACKETSIZE) {
                                buffer = _dataVector[i];
                                buffer.position = 0;
                                ba.writeBytes(buffer,0,Math.min(PACKETSIZE-ba.position,buffer.length));
                            }
                            if(ba.length == PACKETSIZE) {
                                ba.position = 0;
                                return ba;
                            }
                            // we should never reach this point
                            // if TS overlapping but next buffer not available or next buffer not full enough, return null
                            //Log.error("TS overlapping but next buffer not full enough:" + _readPosition + "/" + _totalBytes + "/" + ba.length);
                            return null;
                        }
                    }
                    offset += bufferLength;
                }
            }
            return null;
        }

		//Read CC track
		private function readCC(pes : PES):void
		{
			var country_code : uint = pes.data.readUnsignedByte();

			if (country_code == 181)
			{
				var provider_code : uint = pes.data.readUnsignedShort();

				if (provider_code == 49)
				{
					var user_structure : uint = pes.data.readUnsignedInt();

					if (user_structure == 0x47413934) // GA94
					{
						var user_data_type : uint = pes.data.readUnsignedByte();

						// CEA-608 wrapped in 708 ( user_data_type == 4 is raw 608, not handled yet )
						if (user_data_type == 3)
						{
							// cc -- the first 8 bits are 1-Boolean-0 and the 5 bits for the number of CCs
							var byte:uint = pes.data.readUnsignedByte();

							// get the total number of cc_datas
							var total:uint = 31 & byte;
							var count:uint = 0;

							// supposedly a flag to process the cc_datas or not
							// isn't working for me, so i don't use it yet
							var process:Boolean = !((64 & byte) == 0);

							var size:uint = total * 3;

							// em_data, do we need? It's not used for anything, but it's there, so i need to pull it out
							var otherByte:uint = pes.data.readUnsignedByte();

							if (pes.data.bytesAvailable >= size)
							{
								// ByteArray for onCaptionInfo event
								var sei : ByteArray = new ByteArray();

								// onCaptionInfo payloads need to know the size of the binary data
								// there's two two bytes we just read, plus the cc_datas, which are 3 bytes each
								sei.writeUnsignedInt(2+3*total);

								// write those two bytes
								sei.writeByte(byte);
								sei.writeByte(otherByte);

								// write the cc_datas
								pes.data.readBytes(sei, 6, 3*total);

								pes.data.position -= total * 3;

								// onCaptionInfo expects Base64 data...
								var sei_data:String = Base64.encode(sei);

								var cc_data:Object = {
									type: "708",
									data: sei_data
								};

								// add a new FLVTag with the onCaptionInfo call
								var tag:FLVTag = new FLVTag(FLVTag.METADATA, pes.pts, pes.pts, false);

								var data : ByteArray = new ByteArray();
								data.objectEncoding = ObjectEncoding.AMF0;
								data.writeObject("onCaptionInfo");
								data.objectEncoding = ObjectEncoding.AMF3;
								data.writeByte(0x11);
								data.writeObject(cc_data);
								tag.push(data, 0, data.length);
								tag.build();
								_tags.push(tag);
							}
							else
							{
								CONFIG::LOGGING {
									Log.warn("not enough bytes to extract caption!");
								}
							}
						}
					}
				}
			}
		}

        /** Parse a limited amount of packets each time to avoid blocking **/
        private function _parseTimer(e : Event) : void {
            var start_time : int = getTimer();
            // if any tags left,
            if (_tags.length) {
                _callback_progress(_tags);
                _tags = new Vector.<FLVTag>();
            }
            /** Byte data to be read **/
            var data : ByteArray = getNextTSBuffer(_readPosition);
            // dont spend more than 10ms demuxing TS packets to avoid loosing frames
            // if frame rate is 60fps, we have 1000/60 = 16.6ms budget total per frame
            while(data  != null && ((getTimer() - start_time) < 10)) {
                _parseTSPacket(data);
                _readPosition+=PACKETSIZE;
                if(data.bytesAvailable < PACKETSIZE) {
                    data = getNextTSBuffer(_readPosition);
                }
            }
            // if we have spare time
            if((getTimer() - start_time) < 10) {
                if (_tags.length) {
                    _callback_progress(_tags);
                    _tags = new Vector.<FLVTag>();
                }
                // if we have spare time
                if((getTimer() - start_time) < 10) {
                    // check if we have finished with reading this TS fragment
                    if (_dataComplete && _readPosition == _totalBytes) {
                        // free ByteArray
                        _dataVector = null;
                        // first check if TS parsing was successful
                        CONFIG::LOGGING {
                            if (_pmtParsed == false) {
                                Log.error("TS: no PMT found, report parsing complete");
                            }
                        }
                        _timer.stop();
                        _flush();
                        _callback_complete();
                    }
                }
            }
        }

        /** flux demux **/
        private function _flush() : void {
            CONFIG::LOGGING {
                Log.debug("TS: flushing demux");
            }
            // check whether last parsed audio PES is complete
            if (_curAudioPES && _curAudioPES.length > 14) {
                var pes : PES = new PES(_curAudioPES);
                // consider that PES with unknown size (length=0 found in header) is complete
                if (pes.len == 0 || (pes.data.length - pes.payload - pes.payload_len) >= 0) {
                    CONFIG::LOGGING {
                        Log.debug2("TS: complete Audio PES found at end of segment, parse it");
                    }
                    // complete PES, parse and push into the queue
                    if (_audioIsAAC) {
                        _parseADTSPES(pes);
                    } else {
                        _parseMPEGPES(pes);
                    }
                    _curAudioPES = null;
                } else {
                    CONFIG::LOGGING {
                        Log.debug("TS: partial audio PES at end of segment");
                    }
                    _curAudioPES.position = _curAudioPES.length;
                }
            }
            // check whether last parsed video PES is complete
            if (_curVideoPES && _curVideoPES.length > 14) {
                pes = new PES(_curVideoPES);
                // consider that PES with unknown size (length=0 found in header) is complete
                if (pes.len == 0 || (pes.data.length - pes.payload - pes.payload_len) >= 0) {
                    CONFIG::LOGGING {
                        Log.debug2("TS: complete AVC PES found at end of segment, parse it");
                    }
                    // complete PES, parse and push into the queue
                    _parseAVCPES(pes);
                    _curVideoPES = null;
                    // push last video tag if any
                    if (_curVideoTag) {
                        if (_curNalUnit && _curNalUnit.length) {
                            _curVideoTag.push(_curNalUnit, 0, _curNalUnit.length);
                        }
                        _curVideoTag.build();
                        _tags.push(_curVideoTag);
                        _curVideoTag = null;
                        _curNalUnit = null;
                    }
                } else {
                    CONFIG::LOGGING {
                        Log.debug("TS: partial AVC PES at end of segment expected/current len:" + pes.payload_len + "/" + ( pes.data.length - pes.payload));
                    }
                    _curVideoPES.position = _curVideoPES.length;
                }
            }
            // check whether last parsed ID3 PES is complete
            if (_curId3PES && _curId3PES.length > 14) {
                var pes3 : PES = new PES(_curId3PES);
                if (pes3.len && (pes3.data.length - pes3.payload - pes3.payload_len) >= 0) {
                    CONFIG::LOGGING {
                        Log.debug2("TS: complete ID3 PES found at end of segment, parse it");
                    }
                    // complete PES, parse and push into the queue
                    _parseID3PES(pes3);
                    _curId3PES = null;
                } else {
                    CONFIG::LOGGING {
                        Log.debug("TS: partial ID3 PES at end of segment");
                    }
                    _curId3PES.position = _curId3PES.length;
                }
            }
            // push remaining tags and notify complete
            if (_tags.length) {
                CONFIG::LOGGING {
                    Log.debug2("TS: flush " + _tags.length + " tags");
                }
                _callback_progress(_tags);
                _tags = new Vector.<FLVTag>();
            }
            if(_avcId !=-1 && _videoPESfound == false) {
                CONFIG::LOGGING {
                    Log.warn("TS: dereference video PID, as no video found in this fragment");
                }
                _avcId = -1;
            }
            if(_audioId !=-1 && _audioPESfound == false) {
                CONFIG::LOGGING {
                    Log.warn("TS: dereference audio PID, as no audio found in this fragment");
                }
                _audioId = -1;
            }
            CONFIG::LOGGING {
                Log.debug("TS: parsing complete");
            }
        }

        /** parse ADTS audio PES packet **/
        private function _parseADTSPES(pes : PES) : void {
            var stamp : int;
            _audioPESfound=true;
            // check if previous ADTS frame was overflowing.
            if (_adtsFrameOverflow && _adtsFrameOverflow.length) {
                // if overflowing, append remaining data from previous frame at the beginning of PES packet
                CONFIG::LOGGING {
                    Log.debug("TS/AAC: append overflowing " + _adtsFrameOverflow.length + " bytes to beginning of new PES packet");
                }
                var ba : ByteArray = new ByteArray();
                ba.writeBytes(_adtsFrameOverflow);
                ba.writeBytes(pes.data, pes.payload);
                pes.data = ba;
                pes.payload = 0;
                _adtsFrameOverflow = null;
            }
            if (isNaN(pes.pts)) {
                CONFIG::LOGGING {
                    Log.warn("TS/AAC: no PTS info in this PES packet,discarding it");
                }
                return;
            }
            // insert ADIF TAG at the beginning
            if (_adifTagInserted == false) {
                var adifTag : FLVTag = new FLVTag(FLVTag.AAC_HEADER, pes.pts, pes.dts, true);
                var adif : ByteArray = AACDemuxer.getADIF(pes.data, pes.payload);
                CONFIG::LOGGING {
                    Log.debug("TS/AAC: insert ADIF TAG");
                }
                adifTag.push(adif, 0, adif.length);
                adifTag.build();
                _tags.push(adifTag);
                _adifTagInserted = true;
            }
            // Store ADTS frames in array.
            var frames : Vector.<AudioFrame> = AACDemuxer.getFrames(pes.data, pes.payload);
            var frame : AudioFrame;
            for (var j : int = 0; j < frames.length; j++) {
                frame = frames[j];
                // Increment the timestamp of subsequent frames.
                stamp = Math.round(pes.pts + j * 1024 * 1000 / frame.rate);
                var curAudioTag : FLVTag = new FLVTag(FLVTag.AAC_RAW, stamp, stamp, false);
                curAudioTag.push(pes.data, frame.start, frame.length);
                curAudioTag.build();
                _tags.push(curAudioTag);
            }
            if (frame) {
                // check if last ADTS frame is overflowing on next PES packet
                var adts_overflow : int = pes.data.length - (frame.start + frame.length);
                if (adts_overflow) {
                    _adtsFrameOverflow = new ByteArray();
                    _adtsFrameOverflow.writeBytes(pes.data, frame.start + frame.length);
                    CONFIG::LOGGING {
                        Log.debug("TS/AAC:ADTS frame overflow:" + adts_overflow);
                    }
                }
            } else {
                // no frame found, add data to overflow buffer
                _adtsFrameOverflow = new ByteArray();
                _adtsFrameOverflow.writeBytes(pes.data, pes.data.position);
                CONFIG::LOGGING {
                    Log.debug("TS/AAC:ADTS frame overflow:" + _adtsFrameOverflow.length);
                }
            }
        };

        /** parse MPEG audio PES packet **/
        private function _parseMPEGPES(pes : PES) : void {
            if (isNaN(pes.pts)) {
                CONFIG::LOGGING {
                    Log.warn("TS/MP3: no PTS info in this MP3 PES packet,discarding it");
                }
                return;
            }
            _audioPESfound=true;
            var tag : FLVTag = new FLVTag(FLVTag.MP3_RAW, pes.pts, pes.dts, false);
            tag.push(pes.data, pes.payload, pes.data.length - pes.payload);
            tag.build();
            _tags.push(tag);
        };

        /** parse AVC PES packet **/
        private function _parseAVCPES(pes : PES) : void {
            var sps : ByteArray;
            var ppsvect : Vector.<ByteArray>;
            var sps_found : Boolean = false;
            var pps_found : Boolean = false;
            var frames : Vector.<VideoFrame> = Nalu.getNALU(pes.data, pes.payload);
            _videoPESfound = true;
            // If there's no NAL unit, push all data in the previous tag, if any exists
            if (!frames.length) {
                if (_curNalUnit) {
                    _curNalUnit.writeBytes(pes.data, pes.payload, pes.data.length - pes.payload);
                }
                CONFIG::LOGGING {
                    if (!_curNalUnit) {
                        Log.warn("TS: no NAL unit found in first (?) video PES packet, discarding data. possible segmentation issue ?");
                    }
                }
                return;
            }
            // If NAL units are not starting right at the beginning of the PES packet, push preceding data into previous NAL unit.
            var overflow : int = frames[0].start - frames[0].header - pes.payload;
            if (overflow && _curNalUnit) {
                _curNalUnit.writeBytes(pes.data, pes.payload, overflow);
            }
            if (isNaN(pes.pts)) {
                CONFIG::LOGGING {
                    Log.warn("TS: no PTS info in this AVC PES packet,discarding it");
                }
                return;
            }

            /* first loop : look for AUD/SPS/PPS NAL unit :
             * AUD (Access Unit Delimiter) are used to detect switch to new video tag
             * SPS/PPS are used to generate AVC HEADER
             */

            for each (var frame : VideoFrame in frames) {
                if (frame.type == 9) {
                    if (_curVideoTag) {
                        /* AUD (Access Unit Delimiter) NAL unit:
                         * we need to push current video tag and start a new one
                         */
                        if (_curNalUnit && _curNalUnit.length) {
                            /* push current data into video tag, if any */
                            _curVideoTag.push(_curNalUnit, 0, _curNalUnit.length);
                        }
                        // only push current tag if AVC HEADER has been pushed already
                        if(_avcc) {
                            _curVideoTag.build();
                            _tags.push(_curVideoTag);
                        }
                        CONFIG::LOGGING {
                            if(!_avcc) {
                                Log.warn("TS: discarding video tag, as AVC HEADER not found yet, fragment not starting with I-Frame ?");
                            }
                        }
                    }
                    _curNalUnit = new ByteArray();
                    _curVideoTag = new FLVTag(FLVTag.AVC_NALU, pes.pts, pes.dts, false);
                    // push NAL unit 9 into TAG
                    _curVideoTag.push(Nalu.AUD, 0, 2);
                } else if (frame.type == 7) {
                    sps_found = true;
                    sps = new ByteArray();
                    pes.data.position = frame.start;
                    pes.data.readBytes(sps, 0, frame.length);
                    // try to retrieve video width and height from SPS
                    var spsInfo : SPSInfo = new SPSInfo(sps);
                    sps.position = 0;
                    if (spsInfo.width && spsInfo.height) {
                        // notify upper layer
                        _callback_videometadata(spsInfo.width, spsInfo.height);
                    }
                } else if (frame.type == 8) {
                    if (!pps_found) {
                        pps_found = true;
                        ppsvect = new Vector.<ByteArray>();
                    }
                    var pps : ByteArray = new ByteArray();
                    pes.data.position = frame.start;
                    pes.data.readBytes(pps, 0, frame.length);
                    ppsvect.push(pps);
                } else if (frame.type == 6) {

					//unescape Emulation Prevention bytes
					Nalu.unescapeStream(pes.data,frame.start,frame.start + frame.length);

					// We already know it's 6, so skip first byte
					pes.data.position = frame.start + 1;

					// we need at least 12 bytes to retrieve Caption length
					if(pes.data.bytesAvailable > 12) {

						// get the SEI payload type
						var payload_type : uint = 0;
						var payload_size : uint = 0;
						while (pes.data.position < frame.start + frame.length) {
							// Parse payload type.
							payload_type= 0;
							do {
								payload_type += pes.data.readUnsignedByte();
							} while (payload_type == 0xFF);
							// Parse payload size.
							payload_size = 0;
							do {
								payload_size += pes.data.readUnsignedByte();
							} while (pes.data.bytesAvailable!=0 && payload_size == 0xFF);
							// Process the payload. We only support EIA-608 payloads currently.
							if (payload_type == 4) {
								readCC(pes);
							} else {
								pes.data.position+=payload_size;
							}
						}
					}
                } else if (frame.type == 0) {
                    // report parsing error
                    if(_callback_error != null) {
                        _callback_error("TS: invalid NALu type found, corrupted fragment ?");
                        return;
                    }
                }
            }
            // if both SPS and PPS have been found, build AVCC and push tag if needed
            if (sps_found && pps_found) {
                var avcc : ByteArray = AVCC.getAVCC(sps, ppsvect);
                // only push AVCC tag if never pushed or avcc different from previous one
                if (_avcc == null || !compareByteArray(_avcc, avcc)) {
                    _avcc = avcc;
                    var avccTag : FLVTag = new FLVTag(FLVTag.AVC_HEADER, pes.pts, pes.dts, true);
                    avccTag.push(avcc, 0, avcc.length);
                    avccTag.build();
                    // Log.debug("TS:AVC:push AVC HEADER");
                    _tags.push(avccTag);
                }
            }

            /*
             * second loop, handle other NAL units and push them in tags accordingly
             */
            for each (frame in frames) {
                if (frame.type <= 6) {
                    if (_curNalUnit && _curNalUnit.length) {
                        if(!_curVideoTag) {
                            // in case AUD unit is missing ...
                            _curVideoTag = new FLVTag(FLVTag.AVC_NALU, pes.pts, pes.dts, false);
                            _curVideoTag.push(Nalu.AUD, 0, 2);
                        }
                        _curVideoTag.push(_curNalUnit, 0, _curNalUnit.length);
                    }
                    _curNalUnit = new ByteArray();
                    _curNalUnit.writeBytes(pes.data, frame.start, frame.length);
                    // Unit type 5 indicates a keyframe.
                    if (frame.type == 5) {
                        _curVideoTag.keyframe = true;
                    } else if (frame.type == 1 || frame.type == 2) {
                        // retrieve slice type by parsing beginning of NAL unit (follow H264 spec, slice_header definition)
                        var ba : ByteArray = pes.data;
                        // +1 to skip NAL unit type
                        ba.position = frame.start + 1;
                        var eg : ExpGolomb = new ExpGolomb(ba);
                        /* add a try/catch,
                         * as NALu might be partial here (in case NALu/slice header is splitted accross several PES packet ... we might end up
                         * with buffer overflow. prevent this and in case of overflow assume it is not a keyframe. should be fixed later on
                         */
                        try {
                            // discard first_mb_in_slice
                            eg.readUE();
                            var type : uint = eg.readUE();
                            if (type == 2 || type == 4 || type == 7 || type == 9) {
                                CONFIG::LOGGING {
                                    Log.debug2("TS: frame_type:" + frame.type + ",keyframe slice_type:" + type);
                                }
                                _curVideoTag.keyframe = true;
                            }
                        } catch(e : Error) {
                            CONFIG::LOGGING {
                                Log.warn("TS: frame_type:" + frame.type + ": slice header splitted accross several PES packets, assuming not a keyframe");
                            }
                            _curVideoTag.keyframe = false;
                        }
                    }
                }
            }
        }

        // return true if same Byte Array
        private function compareByteArray(ba1 : ByteArray, ba2 : ByteArray) : Boolean {
            // compare the lengths
            var size : uint = ba1.length;
            if (ba1.length == ba2.length) {
                ba1.position = 0;
                ba2.position = 0;

                // then the bytes
                while (ba1.position < size) {
                    var v1 : int = ba1.readByte();
                    if (v1 != ba2.readByte()) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        /** parse ID3 PES packet **/
        private function _parseID3PES(pes : PES) : void {
            // note: apple spec does not include having PTS in ID3!!!!
            // so we should really spoof the PTS by knowing the PCR at this point
            if (isNaN(pes.pts)) {
                CONFIG::LOGGING {
                    Log.warn("TS: no PTS info in this ID3 PES packet,discarding it");
                }
                return;
            }

            var pespayload : ByteArray = new ByteArray();
            if (pes.data.length >= pes.payload + pes.payload_len) {
                pes.data.position = pes.payload;
                pespayload.writeBytes(pes.data, pes.payload, pes.payload_len);
                pespayload.position = 0;
            }
            pes.data.position = 0;

            var tag : FLVTag = new FLVTag(FLVTag.METADATA, pes.pts, pes.pts, false);
            var data : ByteArray = new ByteArray();
            data.objectEncoding = ObjectEncoding.AMF0;

            // one or more SCRIPTDATASTRING + SCRIPTDATAVALUE
            data.writeObject("onID3Data");
            // SCRIPTDATASTRING - name of object
            // to pass ByteArray, change to AMF3
            data.objectEncoding = ObjectEncoding.AMF3;
            data.writeByte(0x11);
            // AMF3 escape
            // then write the ByteArray
            data.writeObject(pespayload);
            tag.push(data, 0, data.length);
            tag.build();
            _tags.push(tag);
        }

        /** Parse TS packet. **/
        private function _parseTSPacket(data : ByteArray) : void {
            // Each packet is 188 bytes.
            var todo : uint = PACKETSIZE;
            // Sync byte.
            if (data.readByte() != SYNCBYTE) {
                var pos_start : uint = data.position - 1;
                if (probe(data) == true) {
                    var pos_end : uint = data.position;
                    CONFIG::LOGGING {
                        Log.warn("TS: lost sync between offsets:" + pos_start + "/" + pos_end);
                        if (HLSSettings.logDebug2) {
                            var ba : ByteArray = new ByteArray();
                            data.position = pos_start;
                            data.readBytes(ba, 0, pos_end - pos_start);
                            Log.debug2("TS: lost sync dump:" + Hex.fromArray(ba));
                        }
                    }
                    data.position = pos_end + 1;
                } else {
                    if(_callback_error != null) {
                        _callback_error("TS: Could not parse file: sync byte not found @ offset/len " + data.position + "/" + data.length);
                        return;
                    }
                }
            }
            todo--;
            // Payload unit start indicator.
            var stt : uint = (data.readUnsignedByte() & 64) >> 6;
            data.position--;

            // Packet ID (last 13 bits of UI16).
            var pid : uint = data.readUnsignedShort() & 8191;
            // Check for adaptation field.
            todo -= 2;
            var atf : uint = (data.readByte() & 48) >> 4;
            todo--;
            // Read adaptation field if available.
            if (atf > 1) {
                // Length of adaptation field.
                var len : uint = data.readUnsignedByte();
                todo--;
                // Random access indicator (keyframe).
                // var rai:uint = data.readUnsignedByte() & 64;
                data.position += len;
                todo -= len;
                // Return if there's only adaptation field.
                if (atf == 2 || len == 183) {
                    data.position += todo;
                    return;
                }
            }

            // Parse the PES, split by Packet ID.
            switch (pid) {
                case PAT_ID:
                    todo -= _parsePAT(stt,data);
                    CONFIG::LOGGING {
                        if (_pmtParsed == false) {
                            Log.debug("TS: PAT found.PMT PID:" + _pmtId);
                        }
                    }
                    break;
                case _pmtId:
                    CONFIG::LOGGING {
                        if (_pmtParsed == false || _unknownPIDFound == true) {
                            if(_pmtParsed == false) {
                                Log.debug("TS: PMT found");
                            } else {
                                Log.warn("TS: reparsing PMT, unknown PID found");
                            }
                        }
                    }
                    // always reparse PMT
                    todo -= _parsePMT(stt,data);
                    // if PMT was not parsed before, and some unknown packets have been skipped in between,
                    // rewind to beginning of the stream, it helps recovering bad segmented content
                    // in theory there should be no A/V packets before PAT/PMT)
                    if (_pmtParsed == false && _unknownPIDFound == true) {
                        CONFIG::LOGGING {
                            Log.warn("TS: late PMT found, rewinding at beginning of TS");
                        }
                        _pmtParsed = true;
                        _readPosition = 0;
                        return;
                    }
                    _pmtParsed = true;
                    _unknownPIDFound = false;
                    break;
                case _audioId:
                    if (_pmtParsed == false) {
                        break;
                    }
                    if (stt) {
                        if (_curAudioPES) {
                            if (_audioIsAAC) {
                                _parseADTSPES(new PES(_curAudioPES));
                            } else {
                                _parseMPEGPES(new PES(_curAudioPES));
                            }
                        }
                        _curAudioPES = new ByteArray();
                    }
                    if (_curAudioPES) {
                        _curAudioPES.writeBytes(data, data.position, todo);
                    }
                    CONFIG::LOGGING {
                        if (!_curAudioPES) {
                            Log.warn("TS: Discarding audio packet with id " + pid);
                        }
                    }
                    break;
                case _id3Id:
                    if (_pmtParsed == false) {
                        break;
                    }
                    if (stt) {
                        if (_curId3PES) {
                            _parseID3PES(new PES(_curId3PES));
                        }
                        _curId3PES = new ByteArray();
                    }
                    if (_curId3PES) {
                        // store data.  will normally be in a single TS
                        _curId3PES.writeBytes(data, data.position, todo);
                        var pes : PES = new PES(_curId3PES);
                        if (pes.len && (pes.data.length - pes.payload - pes.payload_len) >= 0) {
                            CONFIG::LOGGING {
                                Log.debug2("TS: complete ID3 PES found, parse it");
                            }
                            // complete PES, parse and push into the queue
                            _parseID3PES(pes);
                            _curId3PES = null;
                        } else {
                            CONFIG::LOGGING {
                                Log.debug("TS: partial ID3 PES");
                            }
                            _curId3PES.position = _curId3PES.length;
                        }
                    }
                    CONFIG::LOGGING {
                        if (!stt && !_curId3PES) {
                            Log.warn("TS: Discarding ID3 packet with id " + pid + " bad TS segmentation ?");
                        }
                    }
                    break;
                case _avcId:
                    if (_pmtParsed == false) {
                        break;
                    }
                    if (stt) {
                        if (_curVideoPES) {
                            _parseAVCPES(new PES(_curVideoPES));
                        }
                        _curVideoPES = new ByteArray();
                    }
                    if (_curVideoPES) {
                        _curVideoPES.writeBytes(data, data.position, todo);
                    }
                    CONFIG::LOGGING {
                        if (!_curVideoPES) {
                            Log.warn("TS: Discarding video packet with id " + pid + " bad TS segmentation ?");
                        }
                    }
                    break;
                case SDT_ID:
                case NULL_PID:
                    break;
                default:
                /* check for unknown PID :
                    video PID not defined and stream is not audio only OR
                    audio PID not defined and audio not found
                    adding this condition is useful to avoid reporting unknown PIDs for streams with multiple audio PIDs for example ...
                */
                    if((_avcId ==-1 && !_audioOnly) ||
                       (_audioId ==-1 && !_audioFound)) {
                        CONFIG::LOGGING {
                            Log.debug("TS: unknown PID:" + pid);
                        }
                        _unknownPIDFound = true;
                    }
                    break;
            }
            // Jump to the next packet.
            data.position += todo;
        };

        /** Parse the Program Association Table. **/
        private function _parsePAT(stt : uint, data : ByteArray) : int {
            var pointerField : uint = 0;
            if (stt) {
                pointerField = data.readUnsignedByte();
                // skip alignment padding
                data.position += pointerField;
            }
            // skip table id
            data.position += 1;
            // get section length
            var sectionLen : uint = data.readUnsignedShort() & 0x3FF;
            if (sectionLen >= 13) {
                // Grab the first PMT ID
                data.position += 7;
                _pmtId = data.readUnsignedShort() & 8191;
            }
            return 13 + pointerField;
        };

        /** Read the Program Map Table. **/
        private function _parsePMT(stt : uint, data : ByteArray) : int {
            var pointerField : uint = 0;

            /** audio Track List */
            var audioList : Vector.<AudioTrack> = new Vector.<AudioTrack>();

            if (stt) {
                pointerField = data.readUnsignedByte();
                // skip alignment padding
                data.position += pointerField;
            }
            // skip table id
            data.position += 1;
            // Check the section length for a single PMT.
            var len : uint = data.readUnsignedShort() & 0x3FF;
            var read : uint = 13;
            data.position += 7;
            // skip program info
            var pil : uint = data.readUnsignedShort() & 0x3FF;
            data.position += pil;
            read += pil;
            // Loop through the streams in the PMT.
            while (read < len) {
                // stream type
                var typ : uint = data.readByte();
                // stream pid
                var sid : uint = data.readUnsignedShort() & 0x1fff;
                if (typ == 0x0F) {
                    // ISO/IEC 13818-7 ADTS AAC (MPEG-2 lower bit-rate audio)
                    audioList.push(new AudioTrack('TS/AAC ' + audioList.length, AudioTrack.FROM_DEMUX, sid, (audioList.length == 0), true));
                } else if (typ == 0x1B) {
                    // ITU-T Rec. H.264 and ISO/IEC 14496-10 (lower bit-rate video)
                    if(_audioOnly == false) {
                        if(_avcId != sid) {
                            _avcId = sid;
                            CONFIG::LOGGING {
                                Log.debug("TS: Selected video PID: " + _avcId);
                            }
                        }
                    } else {
                        CONFIG::LOGGING {
                            Log.warn("TS: discarding video PID found in altaudio Fragment: " + _avcId);
                        }
                    }
                } else if (typ == 0x03 || typ == 0x04) {
                    // ISO/IEC 11172-3 (MPEG-1 audio)
                    // or ISO/IEC 13818-3 (MPEG-2 halved sample rate audio)
                    audioList.push(new AudioTrack('TS/MP3 ' + audioList.length, AudioTrack.FROM_DEMUX, sid, (audioList.length == 0), false));
                } else if (typ == 0x15) {
                    // ID3 pid
                    if(_id3Id != sid) {
                        _id3Id = sid;
                        CONFIG::LOGGING {
                            Log.debug("TS: Selected ID3 PID: " + _id3Id);
                        }
                    }
                }
                // es_info_length
                var sel : uint = data.readUnsignedShort() & 0xFFF;
                data.position += sel;
                // loop to next stream
                read += sel + 5;
            }

            // provide audio track List to audio select callback. this callback will return the selected audio track
            var audioPID : int;
            var audioFound : Boolean = (audioList.length > 0);
            if (audioFound != _audioFound) {
                CONFIG::LOGGING {
                    if (audioFound) {
                        Log.debug("TS: Found " + audioList.length + " audio tracks");
                    }
                }
                _audioFound = audioFound;
                var audioTrack : AudioTrack = _callback_audioselect(audioList);
                if (audioTrack) {
                    audioPID = audioTrack.id;
                    _audioIsAAC = audioTrack.isAAC;
                    _audioSelected = true;
                    CONFIG::LOGGING {
                        Log.debug("TS: selected " + (_audioIsAAC ? "AAC" : "MP3") + " PID: " + audioPID);
                    }
                } else {
                    audioPID = -1;
                    _audioSelected = false;
                    CONFIG::LOGGING {
                        Log.debug("TS: no audio selected");
                    }
                }
                // in case audio PID change, flush any partially parsed audio PES packet
                if (audioPID != _audioId) {
                    _curAudioPES = null;
                    _adtsFrameOverflow = null;
                    _audioId = audioPID;
                }
            }
            return len + pointerField;
        };
    }
}
