/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package static.m3u8.org.mangui.hls.utils {
    import by.blooddy.crypto.Base64;
    import flash.events.Event;
    import flash.events.IOErrorEvent;
    import flash.events.ProgressEvent;
    import flash.events.TimerEvent;
    import flash.external.ExternalInterface;
    import flash.net.URLRequest;
    import flash.net.URLStream;
    import flash.utils.ByteArray;
    import flash.utils.getTimer;
    import flash.utils.Timer;

    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    // Fragment Loader
    public dynamic class JSURLStream extends URLStream {
        private var _connected : Boolean;
        private var _resource : ByteArray = new ByteArray();
        /** Timer for decode packets **/
        private var _timer : Timer;
        /** base64 read position **/
        private var _readPosition : uint;
        /** final length **/
        private var _finalLength : uint;
        /** read position **/
        private var _base64Resource : String;
        /* callback names */
        private var _callbackLoaded : String;
        private var _callbackFailure : String;
        /** chunk size to avoid blocking **/
        private static const CHUNK_SIZE : uint = 65536;
        private static var _instanceCount : int = 0;
        /** JS callbacks prefix */
        protected static var _callbackName : String = 'JSLoaderFragment';

        public function JSURLStream() {
            addEventListener(Event.OPEN, onOpen);
            ExternalInterface.marshallExceptions = true;
            super();

            // Connect calls to JS.
            if (ExternalInterface.available) {
                CONFIG::LOGGING {
                    Log.debug("add callback resourceLoaded, id:" + _instanceCount);
                }
                _callbackLoaded = "resourceLoaded" + _instanceCount;
                _callbackFailure = "resourceLoadingError" + _instanceCount;
                // dynamically register callbacks
                this[_callbackLoaded] = function(res:String,len:uint): void { resourceLoaded(res,len)};
                this[_callbackFailure] = function() : void { resourceLoadingError()};
                ExternalInterface.addCallback(_callbackLoaded, this[_callbackLoaded]);
                ExternalInterface.addCallback(_callbackFailure, this[_callbackFailure]);
                _instanceCount++;
            }
        }

        public static function set externalCallback(callbackName: String) : void {
            _callbackName = callbackName;
        }

        protected function _trigger(event : String, ...args) : void {
            if (ExternalInterface.available) {
                ExternalInterface.call(_callbackName, event, args);
            }
        }

        override public function get connected() : Boolean {
            return _connected;
        }

        override public function get bytesAvailable() : uint {
            return _resource.bytesAvailable;
        }

        override public function readByte() : int {
            return _resource.readByte();
        }

        override public function readUnsignedShort() : uint {
            return _resource.readUnsignedShort();
        }

        override public function readBytes(bytes : ByteArray, offset : uint = 0, length : uint = 0) : void {
            _resource.readBytes(bytes, offset, length);
        }

        override public function close() : void {
            if(_timer) {
                _timer.stop();
                _timer.removeEventListener(TimerEvent.TIMER, _decodeData);
            }
            if (ExternalInterface.available) {
                _trigger('abortFragment', ExternalInterface.objectID);
            } else {
                super.close();
            }
        }

        override public function load(request : URLRequest) : void {
            CONFIG::LOGGING {
            Log.debug("JSURLStream.load:" + request.url);
            }
            if (ExternalInterface.available) {
                _trigger('requestFragment', ExternalInterface.objectID, request.url, _callbackLoaded, _callbackFailure);
                this.dispatchEvent(new Event(Event.OPEN));
            } else {
                super.load(request);
            }
        }

        private function onOpen(event : Event) : void {
            _connected = true;
        }

        protected function resourceLoaded(base64Resource : String, len : uint) : void {
            CONFIG::LOGGING {
              Log.debug("resourceLoaded");
            }
            _resource = new ByteArray();
            _readPosition = 0;
            _finalLength = len;
            _timer = new Timer(20, 0);
            _timer.addEventListener(TimerEvent.TIMER, _decodeData);
            _timer.start();
            _base64Resource = base64Resource;
        }

        protected function resourceLoadingError() : void {
            CONFIG::LOGGING {
            Log.debug("resourceLoadingError");
            }
            if(_timer) {
                _timer.stop();
            }
            this.dispatchEvent(new IOErrorEvent(IOErrorEvent.IO_ERROR));
        }

        protected function resourceLoadingSuccess() : void {
            CONFIG::LOGGING {
            Log.debug("resourceLoaded and decoded");
            }
	     _timer.stop();
	     this.dispatchEvent(new Event(Event.COMPLETE));
        }

        /** decrypt a small chunk of packets each time to avoid blocking **/
        private function _decodeData(e : Event) : void {
            var startTime : int = getTimer();
            var decodeCompleted : Boolean = false;
            // dont spend more than 20ms base64 decoding to avoid fps drop
            while ((!decodeCompleted) && ((getTimer() - startTime) < 10)) {
                var startPos : uint = _readPosition,endPos : uint;
                if (_base64Resource.length <= _readPosition + CHUNK_SIZE) {
                    endPos = _base64Resource.length;
                    decodeCompleted = true;
                } else {
                    endPos = _readPosition + CHUNK_SIZE;
                    _readPosition = endPos;
                }
                var tmpString : String = _base64Resource.substring(startPos, endPos);
                var savePosition : uint = _resource.position;
                try {
                    _resource.position = _resource.length;
                    _resource.writeBytes(Base64.decode(tmpString));
                    _resource.position = savePosition;
                } catch (error:Error) {
                    resourceLoadingError();
                }
            }
            this.dispatchEvent(new ProgressEvent(ProgressEvent.PROGRESS, false, false, _resource.length, _finalLength));
            if (decodeCompleted) {
                resourceLoadingSuccess();
            }
        }
    }
}
