/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.stream {
    import flash.utils.flash_proxy;
    import flash.utils.Proxy;

    /** Proxy that allows dispatching internal events fired by Netstream cues to
     *  internal listeners as well as the traditional client object
     */
    public class HLSNetStreamClient extends Proxy {
        private var _delegate : Object;
        private var _callbacks : Object = new Object();

        public function HLSNetStreamClient() {
        }

        public function set delegate(client : Object) : void {
            this._delegate = client;
        }

        public function get delegate() : Object {
            return this._delegate;
        }

        public function registerCallback(name : String, callback : Function) : void {
            _callbacks[name] = callback;
        }

        override flash_proxy function callProperty(methodName : *, ... args) : * {
            var r : * = null;

            if (_callbacks && _callbacks.hasOwnProperty(methodName)) {
                r = _callbacks[methodName](args);
            }

            if (_delegate && _delegate.hasOwnProperty(methodName)) {
                r = _delegate[methodName](args);
            }

            return r;
        }

        override flash_proxy function getProperty(name : *) : * {
            var r : *;
            if (_callbacks && _callbacks.hasOwnProperty(name)) {
                r = _callbacks[name];
            }

            if (_delegate && _delegate.hasOwnProperty(name)) {
                r = _delegate[name];
            }

            return r;
        }

        override flash_proxy function hasProperty(name : *) : Boolean {
            return (_delegate && _delegate.hasOwnProperty(name)) || _callbacks.hasOwnProperty(name);
        }
    }
}
