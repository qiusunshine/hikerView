(function() {

    window.getImgByUrl = function(url){
        console.log("getImgByUrl, url, ", url);
        var videoElements = document.getElementsByTagName("a");
        for(var i = 0;i < videoElements.length; i++) {
            var videoSrc = videoElements[i].href;
            if(videoSrc==url){
                var img = videoElements[i].getElementsByTagName('img');
                console.log("getImgByUrl, img, ", img);
                if(img!=null && img.length>0){
                    return img[0].src;
                }
            }
        }
    };

    window.getImgUrls = function(){
        var videoElements = document.getElementsByTagName("img");
        if(videoElements == null){
            fy_bridge_app.setImgUrls("");
            return;
        }
        const urls = [];
        let s = "";
        for(var i = 0;i < videoElements.length; i++) {
            if(videoElements[i].width > 70 && videoElements[i].height > 70){
                const src = videoElements[i].src;
                if(src == null || src.length < 5 || s.indexOf("&&" + src) >= 0){
                    continue;
                }
                console.log("image, ", src);
                s = s + "&&" + src;
                urls.push(src);
            }
        }
        fy_bridge_app.setImgUrls(urls.join('&&'));
    };

    window.getImgHref = function(url){
        var videoElements = document.getElementsByTagName("img");
        if(videoElements == null){
            fy_bridge_app.setImgHref("");
            return;
        }
        let s = "";
        for(var i = 0;i < videoElements.length; i++) {
            let src = videoElements[i].src;
            if(src == null || src.length < 5 ){
                continue;
            }
            if(src == url && videoElements[i].parentElement != null){
                var ele = videoElements[i].parentElement;
                while(ele != null){
                    if(ele.href != null){
                        s = ele.href;
                        fy_bridge_app.setImgHref(s);
                        return
                    }
                    ele = ele.parentElement;
                }
            }
        }
        fy_bridge_app.setImgHref(s);
    };

    window.getAText = function(url){
        console.log('getAText', url);
        var videoElements = document.getElementsByTagName("a");
        if(videoElements == null){
            return;
        }
        for(var i = 0;i < videoElements.length; i++) {
            let href = videoElements[i].href;
            if(href == null){
                continue;
            }
            if(href == url){
                console.log('getAText', videoElements[i]);
                fy_bridge_app.copy(videoElements[i].innerText);
                return
            }
        }
    };

    /*改自m浏览器*/
//    if (window.isAdTouchInject == "true") {
//        return;
//    }
//    window.isAdTouchInject = "true";
    window.debug_state = false; // true of false
    window.raw_backcolor; // 选中前的背景颜色
    window.cutSelectObj; // 当前选中对象
    window.cutObjHtml; // 当前对象源码
    window.cutSelectParent; // 当前选中的父对象
    window.cutSelectObj2; // 隐藏的选中项
    window.cutSelectParent2; // 当前选中的父对象
    window.tempEvent = null; //记录event
    window.getAdRuleTag = 0;

    window.listener = function(ev) {
        window.tempEvent = ev;
        if (window.debug_state === true) {
            bug_debug_select(ev);
        }
    };

    function bug_debug_select(ev) {
        if (window.cutSelectObj != null && window.raw_backcolor != null) {
            window.cutSelectObj.style.backgroundColor = window.raw_backcolor;
        }
        var e1 = ev || event; // 选中当前项
        var obj = e1.srcElement ? e1.srcElement : e1.target;
        obj = obj ? obj : ev;
        bug_debug_setObj(obj);
    }

    function bug_debug_setObj(ev) {
        if (window.cutSelectObj != null && window.raw_backcolor != null) {
            window.cutSelectObj.style.backgroundColor = window.raw_backcolor;
        }
        if (ev == null) return;

        window.cutSelectObj = ev;
        window.cutSelectParent = window.cutSelectObj.parentNode;
        bug_debug_reply();

        window.cutObjHtml = window.cutSelectObj.outerHTML;
        window.raw_backcolor = window.cutSelectObj.style.backgroundColor;
        window.cutSelectObj.style.backgroundColor = "#14c47c";

        win_echo();
    }

    // function bug_debug_toParent2() {
    //     window.cutSelectObj2 = window.cutSelectParent2;
    //     window.cutSelectParent2 = window.cutSelectObj2.parentNode;
    //     win_echo();
    // }

    function bug_debug_reply() {
        window.cutSelectObj2 = window.cutSelectObj;
        window.cutSelectParent2 = window.cutSelectParent;
    }

    function win_echo() {
        fy_bridge_app.setAdBlock(window.cutObjHtml, getTagArray(cutSelectObj));
        if(window.getAdRuleTag > 0){
            fy_bridge_app.saveAdBlockRule(getTagArray(cutSelectObj));
            window.getAdRuleTag = window.getAdRuleTag -1;
        }
    }

    //==================转成规则=====================
    function getTagArray(startTag) {
        if (!(startTag instanceof HTMLElement)) {
            return console.error('receive only HTMLElement')
        }
        var parentTagList = [];
        parentTagList = getParentTag(startTag, parentTagList);
        return parentTagList.reverse().join('&&');
    }

    /**
     * 循环取id/class/tag，自底向上，从child一直到body
     * @param startTag
     * @param parentTagList
     * @returns {Array}
     */
    function getParentTag(startTag, parentTagList = []) {
        if (startTag == null || startTag.parentElement == null) {
            return parentTagList;
        }
        if ('BODY' === startTag.nodeName) {
            parentTagList.push('body');
            return parentTagList;
        }
        const parent = startTag.parentElement;
        //先取ID
        if(typeof(startTag)!="object" || startTag.getAttribute == null){
            return getParentTag(parent, parentTagList);
        }
        const id = startTag.getAttribute('id');
        if (id != null) {
            const numId = id.replace(/[^0-9]/ig, "");
            if (numId.length <= 3) {
                //id中数字很多，可能是广告，继续取class
                if(document.getElementById(id) == startTag){
                    parentTagList.push('#' + id);
                    return parentTagList;
                }
            }
        }
        //id没有就再取class
        const cls = startTag.getAttribute('class');
        if (cls != null) {
            const classes = cls.split(' ');
            let cl = ".";
            for (let i = 0; i < classes.length; i++) {
                if(classes[i] == ""){
                    continue;
                }
                const numCls = classes[i].replace(/[^0-9]/ig, "");
                if (numCls.length > 3) {
                    //class中数字很多，可能是广告
                    continue;
                }
                cl = cl + classes[i] + ',' + getInParentIndex(startTag, "class", classes[i]);
                break;
            }
            if (cl != ".") {
                parentTagList.push(cl);
                return getParentTag(parent, parentTagList);
            }
        }
        //class也没有就取tag
        let tag = startTag.localName + ',' + getInParentIndex(startTag, "tag", startTag.localName);
        parentTagList.push(tag);
        return getParentTag(parent, parentTagList);
    }

    function getInParentIndex(startTag, type, name) {
        let count = 0;
        let ele = getPreviousSibling(startTag);
        while(ele!=null){
            if(typeof(ele)!="object" || ele.getAttribute == null){
                ele = getPreviousSibling(ele);
                continue;
            }
            const tag = ele.localName;
            if("script" != tag && "style" != tag){
                if("class" == type){
                    const cls = ele.getAttribute('class');
                    if (cls != null && (cls + " ").indexOf(name + " ") >= 0) {
                        count ++;
                    }
                }else if("tag" == type){
                    if (tag == name) {
                        count ++;
                    }
                }
            }
            ele = getPreviousSibling(ele);
        }
        return count;
    }

    function getPreviousSibling(startTag) {
        return startTag.previousSibling || startTag.previousElementSibling;
    }

    //==================转成规则end=====================

    // 开启或关闭debug
    window.setDebugState = function (state) {
        window.debug_state = state;
        if (state === false) {
            if (window.cutSelectObj != null && window.raw_backcolor != null) {
                window.cutSelectObj.style.backgroundColor = window.raw_backcolor;
            }
        } else {
            //开启debug，主动触发一次
            if (window.tempEvent != null) {
                bug_debug_select(window.tempEvent);
            }
        }
    }
    //获取规则
    window.getAdRule = function () {
        window.getAdRuleTag = 1;
        bug_debug_select(window.tempEvent);
    }
    //选中父节点
    window.touchParent = function() {
        bug_debug_setObj(window.cutSelectParent);
    }
    //选中兄节点
    window.touchLast = function() {
        if(window.cutSelectObj && window.cutSelectObj.previousElementSibling){
            bug_debug_setObj(window.cutSelectObj.previousElementSibling);
        }
    }
    //选中弟节点
    window.touchNext = function() {
        if(window.cutSelectObj && window.cutSelectObj.nextElementSibling){
            bug_debug_setObj(window.cutSelectObj.nextElementSibling);
        }
    }
    //选中子节点
    window.touchChild = function() {
        if(window.cutSelectObj && window.cutSelectObj.firstElementChild){
            bug_debug_setObj(window.cutSelectObj.firstElementChild);
        }
    }
    // window.fy_bridge_app = {};
    // window.fy_bridge_app.setAdBlock = function (html, rule) {
    //     console.log("html, ", html);
    //     console.log("rule, ", rule);
    // }
    // window.debug_state = true;

    window.getTouchElement = function () {
        let result = "";
        try {
            let tag = "";
            let text = "";
            let href = "";
            let img = "";
            let e1 = window.tempEvent; // 选中当前项
            let obj = e1.srcElement ? e1.srcElement : e1.target;
            obj = obj ? obj : e1;
            if(obj.tagName == 'A'){
                href = obj.href;
            } else if(obj.tagName == 'IMG') {
                  img = obj.src;
                  let ele = obj.parentElement;
                  while(ele != null){
                      if(ele.href != null){
                          href = ele.href;
                          break
                      }
                      ele = ele.parentElement;
                  }
            } else if(obj.tagName == 'DIV') {
                  if(obj.innerText && obj.innerText.length > 0){
                        //有文本的不处理
                        return "";
                  }
                  function getBgImg(obj){
                     if(obj.tagName == 'IMG' && obj.src){
                         return obj.src;
                     }
                     let img;
                     try {
                         let bg = document.defaultView.getComputedStyle(obj).backgroundImage;
                         if(bg && bg.includes('url(')){
                             img = bg.match(/url\(["']?([^"']*)["']?\)/)[1];
                             img = new URL(img, document.location.href).href;
                         }
                     }catch(e){}
                     return img;
                  }
                 img = getBgImg(obj) || '';
                 if(img.length <= 0){
                     //优先子元素，然后再子子元素
                     function findImg(children){
                          let next = [];
                          if(children != null && children.length > 0){
                             for(let i = 0; i < children.length; i++){
                                  let g1 = getBgImg(children[i]);
                                  if(g1 && g1.length > 0){
                                      return g1;
                                  }
                                  if(children[i].children){
                                      next = next.concat(children[i].children);
                                  }
                             }
                             if(next && next.length > 0){
                                  return findImg(next);
                             }
                         }
                     }
                     img = findImg(obj.children) || '';
                 }
            } else {
                //其它的不处理
                return obj.tagName.toLowerCase();
            }
            tag = obj.tagName.toLowerCase();
            text = obj.innerText;
            let sp = "@//@";
            result = tag + sp + text + sp + href + sp + img + sp;
        }catch(e){
            return e.toString();
        }
        return result;
    }

    //开始监听
    document.removeEventListener('touchstart', window.listener);
    document.addEventListener('touchstart', window.listener, false);












    //油猴脚本转换
    const VERSION = '20.4.11';
    const DEBUG = false;
    const isTop = window.top === window.self;
    const isNotTop = !isTop;
    const PLUGIN_ATTR = 'userscript';
    const PLUGIN_NAME = 'hiker';

    let Href = location.href;

    function log(...args) {
      if (!DEBUG) return;

      const message =
        `${new Date().toISOString().replace(/.+T|\..+/g, '')} › ` +
        args.map((v) => (typeof v === 'object' ? JSON.stringify(v) : v)).join(' ');

      console.log(
        '%c ' + message,
        'background: #d3f9d8; color: #343a40; padding: 6px; border-radius: 6px;'
      );
    }

    function Is(regex, href = Href) {
      return typeof regex === 'string'
        ? href.includes(regex)
        : regex.test(
            regex.source.includes('=http') ? href : href.replace(/=http[^&]+/, '')
          );
    }

    function IsNot(regex, href) {
      return !Is(regex, href);
    }

    const storePrefix = '海阔视界.';

    const Store = {
      get(key, defaultValue = null) {
        let value = window.localStorage.getItem(storePrefix + key);
        try {
          value = JSON.parse(value);
        } catch (_) {}
        return value !== null ? value : defaultValue;
      },
      set(key, value = null) {
        window.localStorage.setItem(storePrefix + key, JSON.stringify(value));
      },
      remove(key) {
        window.localStorage.removeItem(storePrefix + key);
      },
    };

    async function fetchUrl(url, opts = {}) {
      if(url.startsWith('hiker')){
        return fy_bridge_app.fetch(url, "{}")
      }
      let { name = url, version = VERSION } = opts;

      if (DEBUG) {
        log('fetchUrl:', url);
      }

      let data;

      const matches = url.match(/\/([^\/]+)\/(\d+\.\d+[^\/]+).*(\.\w+)$/);
      if (matches) {
        name = matches[1] + matches[3];
        version = matches[2];
        const cacheData = Store.get(name);

        if (cacheData && cacheData.version === version) {
          data = cacheData.data;
        }
      }

      if (!data) {
        data = await window
          .fetch(url)
          .then((res) => res.text())
          .then((data) => {
            Store.set(name, { data, version });
            return data;
          });
      }

      return data;
    }

    async function addJs(url, opts) {
      const data = await fetchUrl(url, opts);
      eval(data);
    }

    async function addCssUrl(url) {
      const data = await fetchUrl(url);
      addCss(data);
    }

    function addCss(styles) {
      let css;

      if (/^(http|\/)/.test(styles)) {
        return addCssUrl(styles);
      }

      styles = styles.replace(/\n+\s*/g, ' ');
      css = document.createElement('style');

      if (css.styleSheet) css.styleSheet.cssText = styles;
      // Support for IE
      else css.appendChild(document.createTextNode(styles)); // Support for the rest

      css.type = 'text/css';

      document.getElementsByTagName('head')[0].appendChild(css);
    }

    const PurifyStyle = `
    display: none !important;
    visibility: hidden !important;
    width: 0 !important;
    height: 0 !important;
    max-width: 0 !important;
    max-height: 0 !important;
    overflow: hidden !important;
    position: absolute !important;
    left: -99999px !important;
    opacity: 0 !important;
    pointer-events: none !important;`;

    async function home() {
      await addJs('hiker://files/jquery.min.js');
      const $ = jQuery.noConflict(true);
      $(function () {
        $('#home-step-1 > h3').html(
          `第一步：安装一个用户脚本管理器（注意：本软件已内置脚本管理器，不需要额外安装，其它软件才需要）`
        );
      });
    }

    async function go() {
      await addJs('hiker://files/jquery.min.js');

      const $ = jQuery.noConflict(true);

      $(function () {
        log('.install-link:', $('.install-link.is-hiker').length);

        if ($('.install-link.is-hiker').length > 1) return;

        $('#site-name-text a').html(
          `Greasy Fork<span>海阔视界 · 油猴转换 v${VERSION}</span>`
        );

        // fork: [e8a9a09](https://github.com/dankogai/js-base64/blob/master/base64.js)
        function setBase64(global = window) {
          // existing version for noConflict()
          global = global || {};
          var _Base64 = global.Base64;
          var version = '2.5.2';
          // if node.js and NOT React Native, we use Buffer
          var buffer;
          if (typeof module !== 'undefined' && module.exports) {
            try {
              buffer = eval("require('buffer').Buffer");
            } catch (err) {
              buffer = undefined;
            }
          }
          // constants
          var b64chars =
            'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
          var b64tab = (function (bin) {
            var t = {};
            for (var i = 0, l = bin.length; i < l; i++) t[bin.charAt(i)] = i;
            return t;
          })(b64chars);
          var fromCharCode = String.fromCharCode;
          // encoder stuff
          var cb_utob = function (c) {
            if (c.length < 2) {
              var cc = c.charCodeAt(0);
              return cc < 0x80
                ? c
                : cc < 0x800
                ? fromCharCode(0xc0 | (cc >>> 6)) + fromCharCode(0x80 | (cc & 0x3f))
                : fromCharCode(0xe0 | ((cc >>> 12) & 0x0f)) +
                  fromCharCode(0x80 | ((cc >>> 6) & 0x3f)) +
                  fromCharCode(0x80 | (cc & 0x3f));
            } else {
              var cc =
                0x10000 +
                (c.charCodeAt(0) - 0xd800) * 0x400 +
                (c.charCodeAt(1) - 0xdc00);
              return (
                fromCharCode(0xf0 | ((cc >>> 18) & 0x07)) +
                fromCharCode(0x80 | ((cc >>> 12) & 0x3f)) +
                fromCharCode(0x80 | ((cc >>> 6) & 0x3f)) +
                fromCharCode(0x80 | (cc & 0x3f))
              );
            }
          };
          var re_utob = /[\uD800-\uDBFF][\uDC00-\uDFFFF]|[^\x00-\x7F]/g;
          var utob = function (u) {
            return u.replace(re_utob, cb_utob);
          };
          var cb_encode = function (ccc) {
            var padlen = [0, 2, 1][ccc.length % 3],
              ord =
                (ccc.charCodeAt(0) << 16) |
                ((ccc.length > 1 ? ccc.charCodeAt(1) : 0) << 8) |
                (ccc.length > 2 ? ccc.charCodeAt(2) : 0),
              chars = [
                b64chars.charAt(ord >>> 18),
                b64chars.charAt((ord >>> 12) & 63),
                padlen >= 2 ? '=' : b64chars.charAt((ord >>> 6) & 63),
                padlen >= 1 ? '=' : b64chars.charAt(ord & 63),
              ];
            return chars.join('');
          };
          var btoa = global.btoa
            ? function (b) {
                return global.btoa(b);
              }
            : function (b) {
                return b.replace(/[\s\S]{1,3}/g, cb_encode);
              };
          var _encode = function (u) {
            var isUint8Array =
              Object.prototype.toString.call(u) === '[object Uint8Array]';
            return isUint8Array ? u.toString('base64') : btoa(utob(String(u)));
          };
          var encode = function (u, urisafe) {
            return !urisafe
              ? _encode(u)
              : _encode(String(u))
                  .replace(/[+\/]/g, function (m0) {
                    return m0 == '+' ? '-' : '_';
                  })
                  .replace(/=/g, '');
          };
          var encodeURI = function (u) {
            return encode(u, true);
          };
          // decoder stuff
          var re_btou = /[\xC0-\xDF][\x80-\xBF]|[\xE0-\xEF][\x80-\xBF]{2}|[\xF0-\xF7][\x80-\xBF]{3}/g;
          var cb_btou = function (cccc) {
            switch (cccc.length) {
              case 4:
                var cp =
                    ((0x07 & cccc.charCodeAt(0)) << 18) |
                    ((0x3f & cccc.charCodeAt(1)) << 12) |
                    ((0x3f & cccc.charCodeAt(2)) << 6) |
                    (0x3f & cccc.charCodeAt(3)),
                  offset = cp - 0x10000;
                return (
                  fromCharCode((offset >>> 10) + 0xd800) +
                  fromCharCode((offset & 0x3ff) + 0xdc00)
                );
              case 3:
                return fromCharCode(
                  ((0x0f & cccc.charCodeAt(0)) << 12) |
                    ((0x3f & cccc.charCodeAt(1)) << 6) |
                    (0x3f & cccc.charCodeAt(2))
                );
              default:
                return fromCharCode(
                  ((0x1f & cccc.charCodeAt(0)) << 6) | (0x3f & cccc.charCodeAt(1))
                );
            }
          };
          var btou = function (b) {
            return b.replace(re_btou, cb_btou);
          };
          var cb_decode = function (cccc) {
            var len = cccc.length,
              padlen = len % 4,
              n =
                (len > 0 ? b64tab[cccc.charAt(0)] << 18 : 0) |
                (len > 1 ? b64tab[cccc.charAt(1)] << 12 : 0) |
                (len > 2 ? b64tab[cccc.charAt(2)] << 6 : 0) |
                (len > 3 ? b64tab[cccc.charAt(3)] : 0),
              chars = [
                fromCharCode(n >>> 16),
                fromCharCode((n >>> 8) & 0xff),
                fromCharCode(n & 0xff),
              ];
            chars.length -= [0, 0, 2, 1][padlen];
            return chars.join('');
          };
          var _atob = global.atob
            ? function (a) {
                return global.atob(a);
              }
            : function (a) {
                return a.replace(/\S{1,4}/g, cb_decode);
              };
          var atob = function (a) {
            return _atob(String(a).replace(/[^A-Za-z0-9\+\/]/g, ''));
          };
          var _decode = buffer
            ? buffer.from && Uint8Array && buffer.from !== Uint8Array.from
              ? function (a) {
                  return (a.constructor === buffer.constructor
                    ? a
                    : buffer.from(a, 'base64')
                  ).toString();
                }
              : function (a) {
                  return (a.constructor === buffer.constructor
                    ? a
                    : new buffer(a, 'base64')
                  ).toString();
                }
            : function (a) {
                return btou(_atob(a));
              };
          var decode = function (a) {
            return _decode(
              String(a)
                .replace(/[-_]/g, function (m0) {
                  return m0 == '-' ? '+' : '/';
                })
                .replace(/[^A-Za-z0-9\+\/]/g, '')
            );
          };
          var noConflict = function () {
            var Base64 = global.Base64;
            global.Base64 = _Base64;
            return Base64;
          };
          // export Base64
          global.Base64 = {
            VERSION: version,
            atob: atob,
            btoa: btoa,
            fromBase64: decode,
            toBase64: encode,
            utob: utob,
            encode: encode,
            encodeURI: encodeURI,
            btou: btou,
            decode: decode,
            noConflict: noConflict,
            __buffer__: buffer,
          };
        }

        async function install(isOffline, cb) {
          const installUrl = Href.replace(
            /.+scripts\/(\d+-)([^\/]+).*/,
            'https://greasyfork.org/scripts/$1$2/code/$2.user.js'
          );

          log('install:', installUrl);

          const code = await fetchUrl(installUrl);

          const TEMPLATE = `(async function () {
      try {
        // VERSION
        // EXCLUDE
        if (EXCLUDE && EXCLUDE.test(location.href)) return;
        // MATCH
        if (MATCH.test(location.href)) {
          console.log('match: NAME');
          function addCss(styles) {
            let css;

            styles = styles.replace(/\\n+\\s*/g, ' ');
            css = document.createElement('style');

            if (css.styleSheet) css.styleSheet.cssText = styles;
            // Support for IE
            else css.appendChild(document.createTextNode(styles)); // Support for the rest

            css.type = 'text/css';

            document.getElementsByTagName('head')[0].appendChild(css);
          }
          const storePrefix = '海阔视界.';

          const Store = {
            get(key, defaultValue = null) {
              let value = window.localStorage.getItem(storePrefix + key);
              try {
                value = JSON.parse(value);
              } catch (_) {}
              return value !== null ? value : defaultValue;
            },
            set(key, value = null) {
              window.localStorage.setItem(storePrefix + key, JSON.stringify(value));
            },
            remove(key) {
              window.localStorage.removeItem(storePrefix + key);
            },
          };

          const unsafeWindow = window;
          const GM_addStyle = addCss;
          const GM_getValue = Store.get;
          const GM_setValue = Store.set;
          const GM_deleteValue = Store.remove;
          const GM_xmlhttpRequest = window.fbaHttpRequest;
          const GM_setClipboard = function(s){
            let r = $$$().lazyRule((s) => {
                copy(s)
            }, s);
            fy_bridge_app.parseLazyRule(r);
          };
          const GM_download = function(dataUrl,fileName){
             window.open(dataUrl, '_blank')
          }
          const GM_registerMenuCommand = function(name, fn, accessKey){
            return 111
          }
          const GM_unregisterMenuCommand = function(name, fn, accessKey){

          }
          const GM_log = function(msg){
              fy_bridge_app.log(msg);
          }
          const GM_openInTab = function(url, options){
             window.open(url, '_blank')
          }
          // CODE
        }
      } catch (error) {
        console.error(error);
      }
    })();
    `;

          function parseMeta(metaString) {
            log('parseMeta - metaString:', metaString);

            const meta = {};
            metaString
              .trim()
              .split(/[\s\n]*\n[\s\n]*/)
              .forEach((v) => {
                const matches = v.match(/\/\/\s*@(\S+)\s+(.+)/);
                if (!matches) return;
                const key = matches[1];
                const value = matches[2];
                if (!meta[key]) {
                  meta[key] = [];
                }
                meta[key].push(value.trim());
              });

            log('parseMeta - meta:', meta);

            return meta;
          }

          function getUsCode(meta, url) {
            function toRegex(s) {
              return new RegExp(
                s.replace(/\*/g, '(.*?)').replace(/\//g, '\\/'),
                'i'
              );
            }

            let EXCLUDE = '';

            if (meta.exclude) {
              EXCLUDE = toRegex(meta.exclude.join('|'));
            }

            log('EXCLUDE:', EXCLUDE);

            let matchUrls = [];

            if (meta.match) {
              matchUrls = matchUrls.concat(meta.match);
            }
            if (meta.include) {
              matchUrls = matchUrls.concat(meta.include);
            }

            const MATCH = toRegex(matchUrls.join('|'));

            log('MATCH:', MATCH);

            let requireCode = `
            let isRequire = ${!!meta.require},RequireStack = ${(meta.require && meta.require.length) || 0};
            function setInterval0(func, t){
                setTimeout(() => {
                    if(!func()){
                        setInterval0(func, t);
                    }
                }, t);
            }
            `;
            const newline = '\n      ';

            if (meta.require) {
              requireCode += meta.require
                .map((v) => `
                requestAsync("${v}", (key, result) => {
                  fy_bridge_app.putVar("${v}", result);
                });
                setInterval0(() => {
                  if (fy_bridge_app.getVar("${v}") !== "undefined") {
                    eval(fy_bridge_app.getVar("${v}"));
                    RequireStack--;
                    return true;
                  }
                }, 100);
                `)
                .join(newline);
            }

            const code = url.startsWith('http')
              ? `
              requestAsync("${url}", (key, result) => {
                fy_bridge_app.putVar("${url}", result);
              });
              setInterval0(() => {
                if (fy_bridge_app.getVar("${url}") !== "undefined" && RequireStack === 0) {
                  eval(fy_bridge_app.getVar("${url}"));
                  return true;
                }
              }, 100);`
              : `setInterval0(() => {
                if (RequireStack === 0) {
                  eval(fy_bridge_app.base64Decode("${fy_bridge_app.base64Encode(url)}"));
                  return true;
                }
              }, 100);`;

            return TEMPLATE.replace('NAME', meta.name[0])
              .replace(/\/\/ EXCLUDE/, `const EXCLUDE = ${EXCLUDE || '""'};`)
              .replace(/\/\/ VERSION/, `const VERSION = "${VERSION}";`)
              .replace(/\/\/ MATCH/, `const MATCH = ${MATCH};`)
              .replace(
                /\/\/ CODE/,
                `${requireCode}${requireCode ? newline : ''}${code}
    `
              );
          }

          const matches = code.match(
            /\/\/\s*==\s*UserScript\s*==\n*([\s\S\n]+?)\n*\/\/\s*==\s*\/UserScript\s*==\n*([\s\S]+)/
          );

          const metaString = matches[1];
          const codeString = matches[2];
          const meta = parseMeta(metaString);
          const usName = meta.name && meta.name[0];
          log('name:', usName);

          let usCode = getUsCode(meta, isOffline ? codeString : installUrl);
          usCode = `// ==UserScript==
    ${metaString}
    // ==/UserScript==

    // 海阔视界·油猴转换 v${VERSION} - (o˘◡˘o)
    ${usCode}`;
          log(usCode);

          setBase64(window);

          log('Base64:', Base64);

          usCode = Base64.encode(usCode);

          const rule = `海阔视界 · 油猴脚本转换 (o˘◡˘o) ￥js_url￥global_${usName
            .replace(/\s+/g, '')
            .slice(0, 32)}@base64://${usCode}`;

          log(rule.slice(0, 100) + '...' + rule.slice(-100));

          fy_bridge_app.importRule(rule);

          cb();
        }

        $('#install-area').html(
          `<a class="install-link is-hiker is-offline">安装本地版</a>
          <span></span>
          <a class="install-link is-hiker is-online">安装网络版（不推荐）</a>
          <a class="install-help-link" title="如何安装" rel="nofollow" href="/zh-CN/help/installing-user-scripts">?</a><p class="install-hint"><strong>网络版</strong> 会每次加载当前脚本在油猴网站上的最新版代码，加载速度取决于你访问油猴网站的网络速度。</p>`
        );

        $('.install-link')
          .off('click')
          .on('click', function (e) {
            e.preventDefault();
            $('#install-area').addClass('is-installing');
            const $this = $(this);
            $this.addClass('is-active').html('正在安装...');

            install($this.hasClass('is-offline'), function () {
              $this
                .addClass('is-success')
                .removeClass('is-installing')
                .html('安装成功 ✔');
            });
          });
      });
    }

    function ready(fn) {
      if (document.readyState != 'loading') {
        fn();
      } else {
        document.addEventListener('DOMContentLoaded', fn);
      }
    }

    if (isTop && Is(/greasyfork.org\/.*scripts\/\d/)) {
      addCss(`
    .hiker--hide {${PurifyStyle}}

    #site-name > a {
      display: none !important;
    }

    #main-header h1 {
      font-size: 1.5em;
      letter-spacing: 0px;
    }

    #site-name-text a {
      font-size: 20px;
      letter-spacing: 0;
    }

    #site-name-text span {
      font-size: 12px;
      letter-spacing: 0;
      color: #FFC107;
      position: absolute;
      right: 1em;
      top: 1em;
    }

    #script-info header h2 {
      font-size: 1.2em;
      margin-bottom: .5em;
    }

    #install-area {
      font-size: 14px;
    }

    #install-area a {
      border-radius: 2px;
    }

    .install-link {
      margin-right: .5em;
      padding: 5px 10px;
    }

    .install-link.is-offline {
      background-color: #1971c2 !important;
    }

    .install-link.is-online {
      background-color: #AAAAAA !important;
    }

    #install-area.is-installing .install-link {
      pointer-events: none;
    }

    .install-link.is-active {
      background-color: #d9480f !important;
    }

    .install-link.is-success {
      background-color: #2b8a3e !important;
    }

    .install-hint {
      font-size: 13px;
      color: #FF5722;
      padding: 5px;
      border-top: 1px dotted #FF5722;
      border-bottom: 1px dotted #FF5722;
    }
    `);

      try {
        ready(go);
      } catch (error) {
        console.error('油猴脚本转换错误：', error);
      }
    }

    if (isTop && Href.includes("greasyfork.org/zh-CN") && !Href.includes("scripts")) {
        try {
            ready(home);
          } catch (error) {
            console.error('油猴脚本转换错误：', error);
          }
    }

})();