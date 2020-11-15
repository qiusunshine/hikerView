/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 package static.m3u8.org.mangui.osmf.plugins {
    import org.mangui.hls.utils.Params2Settings;
    import org.mangui.osmf.plugins.loader.HLSLoaderBase;
    import org.mangui.osmf.plugins.loader.HLSLoadFromDocumentElement;
    import org.osmf.media.MediaElement;
    import org.osmf.media.MediaFactoryItem;
    import org.osmf.media.MediaFactoryItemType;
    import org.osmf.media.MediaResourceBase;
    import org.osmf.media.PluginInfo;
    
    CONFIG::LOGGING {
    import org.mangui.hls.utils.Log;
    }

    public class HLSPlugin extends PluginInfo {
        public function HLSPlugin(items : Vector.<MediaFactoryItem>=null, elementCreatedNotification : Function = null) {
            items = new Vector.<MediaFactoryItem>();
            items.push(new MediaFactoryItem('org.mangui.osmf.plugins.HLSPlugin', canHandleResource, createMediaElement, MediaFactoryItemType.STANDARD));

            super(items, elementCreatedNotification);
        }

        /**
         * Called from super class when plugin has been initialized with the MediaFactory from which it was loaded.
         * Used for customize HLSSettings with values provided in resource metadata (that was set eg. in flash vars)
         *  
         * @param resource  Provides acces to the resource used to load the plugin and any associated metadata
         * 
         */
        override public function initializePlugin(resource : MediaResourceBase) : void {
            CONFIG::LOGGING {
            Log.debug("OSMF HLSPlugin init");
            }
            metadataParamsToHLSSettings(resource);
        }

        private function canHandleResource(resource : MediaResourceBase) : Boolean {
            return HLSLoaderBase.canHandle(resource);
        }

        private function createMediaElement() : MediaElement {
            return new HLSLoadFromDocumentElement(null, new HLSLoaderBase());
        }

        private function metadataParamsToHLSSettings(resource : MediaResourceBase) : void {
            if (resource == null) {
                return;
            }

            var metadataNamespaceURLs : Vector.<String> = resource.metadataNamespaceURLs;

            // set all legal params values to HLSSetings properties
            for each (var key : String in metadataNamespaceURLs) {
                Params2Settings.set(key, resource.getMetadataValue(key));
            }
        }
    }
}
