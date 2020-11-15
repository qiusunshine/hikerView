/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
 
package static.m3u8.org.mangui.osmf.plugins.loader {
    import org.osmf.elements.proxyClasses.LoadFromDocumentLoadTrait;
    import org.osmf.events.LoadEvent;
    import org.osmf.media.MediaResourceBase;
    import org.osmf.traits.LoadState;
    import org.osmf.traits.LoadTrait;
    import org.osmf.traits.LoaderBase;
    import org.osmf.traits.MediaTraitType;
    import org.osmf.utils.OSMFStrings;
    import org.osmf.elements.ProxyElement;

    /**
     * LoadFromDocumentElement is the base class for MediaElements that load documents
     * that contain information about the real MediaElement to expose.  For example, the
     * contents of a SMIL document can be represented as a MediaElement, but until the
     * SMIL document is loaded, it's impossible to know what MediaElement the SMIL
     * document represents.
     * 
     * <p>Because of the dynamic nature of this operation, a LoadFromDocumentElement extends
     * ProxyElement.  When the document has been loaded, this class will set the
     * <code>proxiedElement</code> property to the MediaElement that was generated from
     * the document.</p>
     * 
     * <p>This is an abstract base class, and must be subclassed.</p>
     *  
     * <p>Note: It is simplest to use the MediaPlayer class in conjunction with subclasses of
     * the LoadFromDocumentElement.  If you work directly with a LoadFromDocumentElement, then
     * it's important to listen for events related to traits being added and removed.  If you
     * use the MediaPlayer class with a LoadFromDocumentElement, then the MediaPlayer will
     * automatically listen for these events for you.</p>
     * 
     * @see org.osmf.elements.ProxyElement
     * @see org.osmf.media.MediaElement
     * @see org.osmf.media.MediaPlayer
     *   
     *  @langversion 3.0
     *  @playerversion Flash 10
     *  @playerversion AIR 1.5
     *  @productversion OSMF 1.0
     */
    public class HLSLoadFromDocumentElement extends ProxyElement {
        /**
         * Constructor.
         * 
         * @param resource The resource associated with this element.
         * @param loader The LoaderBase used to load the resource.  Cannot be null.
         * 
         * @throws ArgumentError If loader is null.
         *  
         *  @langversion 3.0
         *  @playerversion Flash 10
         *  @playerversion AIR 1.5
         *  @productversion OSMF 1.0
         */
        public function HLSLoadFromDocumentElement(resource : MediaResourceBase = null, loader : LoaderBase = null) {
            super(null);

            this.loader = loader;
            this.resource = resource;

            if (loader == null) {
                throw new ArgumentError(OSMFStrings.getString(OSMFStrings.NULL_PARAM));
            }
        }

        /**
         * @private
         * 
         * Overriding is necessary because there is a null proxiedElement.
         */
        override public function set resource(value : MediaResourceBase) : void {
            if (_resource != value && value != null) {
                _resource = value;
                loadTrait = new LoadFromDocumentLoadTrait(loader, resource);
                loadTrait.addEventListener(LoadEvent.LOAD_STATE_CHANGE, onLoadStateChange, false, int.MAX_VALUE);

                if (super.getTrait(MediaTraitType.LOAD) != null) {
                    super.removeTrait(MediaTraitType.LOAD);
                }
                super.addTrait(MediaTraitType.LOAD, loadTrait);
            }
        }

        /**
         * @private
         */
        override public function get resource() : MediaResourceBase {
            return _resource;
        }

        private function onLoadStateChange(event : LoadEvent) : void {
            if (event.loadState == LoadState.READY) {
                event.stopImmediatePropagation();

                // Remove the temporary LoadTrait.
                removeTrait(MediaTraitType.LOAD);

                // Tell the soon-to-be proxied element to load itself.
                // Note that we do this before setting it as the proxied
                // element, so as to avoid dispatching a second LOADING
                // event.

                // Set up a listener so that we can prevent the dispatch
                // of a second LOADING event.
                var proxiedLoadTrait : LoadTrait = loadTrait.mediaElement.getTrait(MediaTraitType.LOAD) as LoadTrait;
                proxiedLoadTrait.addEventListener(LoadEvent.LOAD_STATE_CHANGE, onProxiedElementLoadStateChange, false, int.MAX_VALUE);

                // Expose the proxied element.
                proxiedElement = loadTrait.mediaElement;

                // If our proxied element hasn't started loading yet, we should
                // initiate the load.
                if (proxiedLoadTrait.loadState == LoadState.UNINITIALIZED) {
                    proxiedLoadTrait.load();
                }

                function onProxiedElementLoadStateChange(event : LoadEvent) : void {
                    if (event.loadState == LoadState.LOADING) {
                        event.stopImmediatePropagation();
                    } else {
                        proxiedLoadTrait.removeEventListener(LoadEvent.LOAD_STATE_CHANGE, onProxiedElementLoadStateChange);
                    }
                }
            }
        }

        private var _resource : MediaResourceBase;
        private var loadTrait : LoadFromDocumentLoadTrait;
        private var loader : LoaderBase;
    }
}