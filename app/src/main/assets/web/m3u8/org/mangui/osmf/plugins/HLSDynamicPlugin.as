/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins {
    import flash.display.Sprite;
    import flash.system.Security;

    import org.osmf.media.PluginInfo;

    public class HLSDynamicPlugin extends Sprite {
        private var _pluginInfo : PluginInfo;

        public function HLSDynamicPlugin() {
            super();
            Security.allowDomain("*");
            _pluginInfo = new HLSPlugin();
        }

        public function get pluginInfo() : PluginInfo {
            return _pluginInfo;
        }
    }
}