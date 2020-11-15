/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.hls.utils {
    import org.mangui.hls.HLSSettings;

    import flash.utils.describeType;
    import flash.utils.Dictionary;
    import flash.utils.getDefinitionByName;
    import flash.utils.getQualifiedClassName;

    /**
     * Params2Settings is an helper class that holds every legal external params names
     * which can be used to customize HLSSettings and maps them to the relevant HLSSettings values
     */
    public class Params2Settings {
        /**
         * HLSSettings <-> params maping
         */
        private static var _paramMap : Dictionary = new Dictionary();

        // static initializer
        {
            _initParams();
        }


        /* build map between param name and HLSSettings property
            this is done by enumerating properties : http://stackoverflow.com/questions/13294997/as3-iterating-through-class-variables
        */
        private static function _initParams() : void {
            var description:XML = describeType(HLSSettings);
            var variables:XMLList = description..variable;
            for each(var variable:XML in variables) {
                var name : String = variable.@name;
                var param : String;
                if(name.indexOf("log") == 0) {
                    // loggers params don't need prefix
                    param = name.substr(3);
                } else {
                    param = name;
                }
                // for historical (bad ?) reasons, param names are lowercase
                param = param.toLowerCase();
                _paramMap[param] = name;
            }
        }

        public static function set(key : String, value : Object) : void {
            var param : String = _paramMap[key];
            if (param) {
                // try to assign value with proper object type
                try {
                    var cName : String = getQualifiedClassName(HLSSettings[param]);
                    // AS3 bug: "getDefinitionByName" considers var value, not type, and wrongly (e.g. 3.0 >> "int"; 3.1 >> "Number").
                    var c : Class = cName === "int" ? Number : getDefinitionByName(cName) as Class;
                    // get HLSSetting type
                    HLSSettings[param] = c(value);
                    CONFIG::LOGGING {
                        Log.info("HLSSettings." + param + " = " + HLSSettings[param]);
                    }
                } catch(error : Error) {
                    CONFIG::LOGGING {
                        Log.warn("Can't set HLSSettings." + param);
                    }
                }
            }
        }
    }
}