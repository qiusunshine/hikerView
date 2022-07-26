(function(){
    try {
        function addCss(styles) {
          let css;
          styles = styles.replace(/\n+\s*/g, ' ');
          css = document.createElement('style');
          if (css.styleSheet) css.styleSheet.cssText = styles;
          // Support for IE
          else css.appendChild(document.createTextNode(styles)); // Support for the rest
          css.type = 'text/css';
          document.getElementsByTagName('head')[0].appendChild(css);
        }

        addCss(`
        video::-webkit-media-controls-fullscreen-button {
            background-image: -webkit-image-set(url(data:image/svg+xml;base64,PHN2ZyB0PSIxNjQ2NDY4OTkzNjg5IiBjbGFzcz0iaWNvbiIgdmlld0JveD0iMCAwIDEwMjQgMTAyNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHAtaWQ9IjI4NjMiIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4Ij48cGF0aCBkPSJNNDIuNjY2NjY3IDI1NmEyMTMuMzMzMzMzIDIxMy4zMzMzMzMgMCAwIDEgMjEzLjMzMzMzMy0yMTMuMzMzMzMzaDUxMmEyMTMuMzMzMzMzIDIxMy4zMzMzMzMgMCAwIDEgMjEzLjMzMzMzMyAyMTMuMzMzMzMzdjUxMmEyMTMuMzMzMzMzIDIxMy4zMzMzMzMgMCAwIDEtMjEzLjMzMzMzMyAyMTMuMzMzMzMzSDI1NmEyMTMuMzMzMzMzIDIxMy4zMzMzMzMgMCAwIDEtMjEzLjMzMzMzMy0yMTMuMzMzMzMzVjI1NnogbTIxMy4zMzMzMzMtMTI4YTEyOCAxMjggMCAwIDAtMTI4IDEyOHY1MTJhMTI4IDEyOCAwIDAgMCAxMjggMTI4aDUxMmExMjggMTI4IDAgMCAwIDEyOC0xMjhWMjU2YTEyOCAxMjggMCAwIDAtMTI4LTEyOEgyNTZ6IiBmaWxsPSIjRkZGRkZGIiBwLWlkPSIyODY0Ij48L3BhdGg+PHBhdGggZD0iTTUzMy4zMzMzMzMgMjU2YTQyLjY2NjY2NyA0Mi42NjY2NjcgMCAwIDEgNDIuNjY2NjY3LTQyLjY2NjY2N0g2ODIuNjY2NjY3YTEyOCAxMjggMCAwIDEgMTI4IDEyOHYxMDYuNjY2NjY3YTQyLjY2NjY2NyA0Mi42NjY2NjcgMCAxIDEtODUuMzMzMzM0IDBWMzQxLjMzMzMzM2E0Mi42NjY2NjcgNDIuNjY2NjY3IDAgMCAwLTQyLjY2NjY2Ni00Mi42NjY2NjZoLTEwNi42NjY2NjdhNDIuNjY2NjY3IDQyLjY2NjY2NyAwIDAgMS00Mi42NjY2NjctNDIuNjY2NjY3ek0yNTYgNTMzLjMzMzMzM2E0Mi42NjY2NjcgNDIuNjY2NjY3IDAgMCAxIDQyLjY2NjY2NyA0Mi42NjY2NjdWNjgyLjY2NjY2N2E0Mi42NjY2NjcgNDIuNjY2NjY3IDAgMCAwIDQyLjY2NjY2NiA0Mi42NjY2NjZoMTA2LjY2NjY2N2E0Mi42NjY2NjcgNDIuNjY2NjY3IDAgMSAxIDAgODUuMzMzMzM0SDM0MS4zMzMzMzNhMTI4IDEyOCAwIDAgMS0xMjgtMTI4di0xMDYuNjY2NjY3YTQyLjY2NjY2NyA0Mi42NjY2NjcgMCAwIDEgNDIuNjY2NjY3LTQyLjY2NjY2N3oiIGZpbGw9IiNGRkZGRkYiIHAtaWQ9IjI4NjUiPjwvcGF0aD48L3N2Zz4=) 1x) !important;
        }
        video::-webkit-media-controls-volume-control-container {
            display: none !important;
        }
        `)
    } catch(e){
        console.log(e)
    }

    function log() {
        args = [];
        // Note: arguments is part of the prototype
        for (var i = 0; i < arguments.length; i++) {
          args.push(arguments[i]);
        }
        console.log.apply(console, args);
    }
	function hexify(color) {
	  var values = color
		.replace(/rgba?\(/, '')
		.replace(/\)/, '')
		.replace(/[\s+]/g, '')
		.split(',');
	  var a = parseFloat(values[3] || 1),
		  r = Math.floor(a * parseInt(values[0]) + (1 - a) * 255),
		  g = Math.floor(a * parseInt(values[1]) + (1 - a) * 255),
		  b = Math.floor(a * parseInt(values[2]) + (1 - a) * 255);
	  return "#" +
		("0" + r.toString(16)).slice(-2) +
		("0" + g.toString(16)).slice(-2) +
		("0" + b.toString(16)).slice(-2);
	}
	function getThemeColor() {
	    let ele = document.querySelector("header");
        if(ele!=null){
            let style = document.defaultView.getComputedStyle(ele);
            log("header, backgroundColor = ", style.backgroundColor);
            let color = hexify(style.backgroundColor);
            log("header, color = ", color);
            if(color!=null && color!='#ffffff'){
                fy_bridge_app.setAppBarColor(color,"true");
                return;
            }
        }
        let widthColor = getChildNodeBgColor(document.body.children, "width");
        log("widthColor, "+widthColor);
        if(widthColor!=null && widthColor!="#ffffff"){
            fy_bridge_app.setAppBarColor(widthColor,"true");
            return;
        }
        let headColor = getChildNodeBgColor(document.body.children, "head");
        log("headColor, "+headColor);
        fy_bridge_app.setAppBarColor(headColor,"true");
	}
	function getChildNodeBgColor(nodes, type) {
		if (nodes.length>0) {
			for (var i = 0; i < nodes.length; i++) {
				var color = getBackgroundColor(nodes[i], type);
				if(color != null && color != "" && color!='#ffffff' && color!='#000000'){
					return color;
				}
			}
		}
		var children = [];
		for (var i = 0; i < nodes.length; i++) {
			var child = nodes[i].children;
			if(child.length>0){
				children.push(...child);
			}
		}
		if (children.length>0) {
			return getChildNodeBgColor(children, type);
		}else{
			return "#ffffff";
		}
	}
	function getBackgroundColor(node, type){
		var style = document.defaultView.getComputedStyle(node);
		if(type == "width" && node.offsetWidth*2>document.body.clientWidth){
			return hexify(style.backgroundColor);
		}else if(type == "head" ){
		    if(node.id !=null && typeof node.id =="string" && node.id.indexOf("head") >= 0 && node.tagName == "DIV"){
		        return hexify(style.backgroundColor);
		    }
		    if(node.className !=null && typeof node.className =="string" && node.className.indexOf("head") >= 0 && node.tagName == "DIV"){
                return hexify(style.backgroundColor);
            }
            return "#ffffff";
        }else{
			return "#ffffff";
		}
	}
	//跨域请求
    function request(url, param){
        if(param == null){
            return fy_bridge_app.fetch(url);
        }
        return fy_bridge_app.fetch(url, JSON.stringify(param));
    }
    window.request = request;
    window.setCode = function(){
        try {
            fy_bridge_app.setCode(document.getElementsByTagName('html')[0].outerHTML);
            return '1';
        } catch(e){
            console.log(e);
            fy_bridge_app.setCode("");
            return '0';
        }
    }
    window._getUrls = function() {
        return JSON.parse(fy_bridge_app.getUrls())
    }
    window.fba = fy_bridge_app;
    window.getRequestHeaders = function(){
        return JSON.parse(fy_bridge_app.getRequestHeaders0());
    }
	//异步跨域请求
	function requestAsync(url, param, key, callBack) {
        let rParam = {}, rKey = url;
        if (key == null) {
            callBack = param;
        } else if (callBack == null) {
            rParam = param;
            callBack = key;
        } else {
            rParam = param;
            rKey = key;
        }
        log('1');
        if (typeof callBack === "function") {
            let callbackName = callBack.name;
            if (callbackName === "") {
                if (window.funcSet == null) {
                    window.funcSet = new Map();
                }
                let content = callBack.toString();
                if (window.funcSet.has(content)) {
                    callBack = window.funcSet.get(content);
                } else {
                    for (let i = 0; i < 1000; i++) {
                        if (eval('window.hikerFunc' + i + " == null")) {
                            eval('window.hikerFunc' + i + " = " + content);
                            callBack = 'hikerFunc' + i;
                            window.funcSet.set(content, callBack);
                            break;
                        }
                    }
                }
            }else {
                callBack = callbackName;
            }
        }
        if(rParam == null){
            return fy_bridge_app.fetchAsync(url, rKey, callback);
        }
        fy_bridge_app.fetchAsync(url, JSON.stringify(rParam), rKey, callBack);
    }
    window.requestAsync = requestAsync;

    window.fbaHttpRequest = function(obj){
            if(obj.data && obj.data.getAll != null){
                let param = [];
                for(let k of obj.data.keys()){
                    param.push(k + "=" + obj.data.get(k));
                }
                obj.data = param.join("&");
            } else if(obj.data && Object.prototype.toString.call(obj.data) != '[object String]'){
                obj.data = JSON.stringify(obj.data);
            }
            console.log('GM_xmlhttpRequest, obj:' + JSON.stringify(obj));
            let ticket = new Date().getTime() + "";
            if(obj.onerror){
                window["onerror" + ticket] = obj.onerror;
            }
            if(obj.onload){
                window["onload" + ticket] = obj.onload;
            }
            if(!obj.url.startsWith('http')){
                try{
                    obj.url = new URL(obj.url, location.href).toString();
                    let headers = {
                        Cookie: fy_bridge_app.getCookie(location.href),
                        Referer: location.href
                    };
                    if(obj.headers){
                        for(let k of Object.keys(obj.headers)){
                            headers[k] = obj.headers[k];
                        }
                    }
                    obj.headers = headers;
                } catch(e){
                    console.log('GM_xmlhttpRequest: error: ' + e.toString());
                }
            }
            let key = JSON.stringify({
                url: obj.url,
                ticket: ticket,
                responseType: obj.responseType
            });
            if(obj.onloadstart){
                obj.onloadstart()
            }
            if(obj.onreadystatechange){
                obj.onreadystatechange()
            }
            requestAsync(obj.url, {
                method: obj.method || 'GET',
                timeout: obj.timeout,
                withHeaders: true,
                withStatusCode: true,
                headers: obj.headers || {},
                body: obj.data || null,
                redirect: !(obj.ignoreRedirect || false)
            }, key, function(key, result){
                console.log('GM_xmlhttpRequest, key:' + key + ', data:' + result);
                try {
                    key = JSON.parse(key);
                    let ticket = key.ticket;
                    let responseType = key.responseType;
                    let res = JSON.parse(result);
                    let data = res.body;
                    let statusCode = res.statusCode + "";
                    if(statusCode.startsWith("5") || statusCode.startsWith("4") && window["onerror" + ticket] != null){
                        window["onerror" + ticket](res.error);
                    }
                    if(responseType == "json"){
                        data = JSON.parse(data == null || data == "" ? "{}" : data);
                    }
                    let response = {
                        responseText: res.body,
                        headers: res.headers,
                        response: data,
                        responseJSON: data,
                        responseHeaders: res.headers,
                        status: res.statusCode,
                        finalUrl: res.url
                    }
                    //console.log('GM_xmlhttpRequest, response:' + JSON.stringify(response));
                    if(window["onload" + ticket] != null){
                        window["onload" + ticket](response);
                    }
                }catch(e){
                    console.log('GM_xmlhttpRequest: error: ' + e.toString());
                }
            });
      };
    //JS插件加载
    function mxloadScript(url, callback) {
        var script = document.createElement('script');
        script.type = 'text/javascript';
        script.onload = function() {
            callback();
        }
        script.src = url;
        var heads = document.getElementsByTagName('head');
        if (heads.length > 0) {
            heads[0].appendChild(script);
        } else {
            document.getElementsByTagName('body').item(0).appendChild(script);
        }
    }
    window.addScript = mxloadScript;
    //加载css
    function addStyle(styles) {
        var css = document.createElement('style');
        css.type = 'text/css';
        if (css.styleSheet) css.styleSheet.cssText = styles;
        // Support for IE
        else css.appendChild(document.createTextNode(styles)); // Support for the rest
        document.getElementsByTagName('head')[0].appendChild(css);
    }
    window.addStyle = addStyle;

	//log(getChildNodeBgColor(document.body.children));
	log("fy_bridge_app=>", fy_bridge_app);
	if(window.themeTimer!=null){
        try{
            getThemeColor();
        }catch(e){
        }
	    return
	}else{
        try{
            getThemeColor();
        }catch(e){
        }
        window.themeTimer=1;
	    setTimeout(()=>{
	        try{
                getThemeColor();
            }catch(e){
            }
            setTimeout(()=>{
                try{
                    getThemeColor();
                }catch(e){
                }
                setTimeout(()=>{
                    try{
                        getThemeColor();
                    }catch(e){
                    }
                }, 3000);
            }, 3000);
	    }, 3000);
	}

    /**
     * @name 海阔url点击事件生成工具
     * @Author LoyDgIk
     * @version 4
     */
    ;
    (function(windows) {
        "use strict";
        //var [emptyUrl, lazyRule, rule, x5, input, confirm] = ["hiker://empty",  "@rule=", "x5WebView://", "input://", "confirm://"];
        var head = {
            empty: "hiker://empty",
            lazyRule: "@lazyRule=",
            rule: "@rule=",
            x5: "x5WebView://",
            input: "input://",
            confirm: "confirm://",
            x5Lazy: "x5Rule://",
            select: "select://",
            webLazy: "webRule://",
        }

        function HikerUrl(param1, param2, param3) {
            this.param1 = param1 === "" ? "" : (param1 || head.empty);
            this.param2 = param2 || "";
            this.param3 = param3;
            this.isbase64 = false;
        }

        function $$$(param1, param2, param3) {
            return new HikerUrl(param1, param2, param3);
        }
        //静态方法
        Object.assign($$$, {
            hiker: windows,
            exports: {},
            toString() {
                if (arguments.length === 0) {
                    return "$$$";
                } else {
                    return toStringFun(arguments);
                }
            },
            require(path = "", headers) {
                let req_code = "";
                if (loadedMap.has(path)) {
                    return loadedMap.get(path);
                }
                if (path.startsWith("hiker://page/")) {
                    req_code = JSON.parse(request(path) || '{"rule":""}').rule;
                } else if (path.startsWith("hiker://files/") || path.startsWith("file://")) {
                    req_code = request(path);
                } else if (path.startsWith("http://") || path.startsWith("https://")) {
                    req_code = request(path,headers);
                } else {
                    return {};
                }
                let temexports = $$$.exports;
                new Function(req_code).apply(windows);
                let value = $$$.exports;
                loadedMap.set(path,value);
                $$$.exports = temexports;
                return value;
            },
            type(obj) {
                if (obj == null) {
                    return String(obj);
                }
                return typeof obj === "object" || typeof obj === "function" ?
                    class2type[core_toString.call(obj)] || "object" :
                    typeof obj;
            },
            stringify(Data, Pattern) {
                switch (Object.prototype.toString.call(Data)) {
                    case "[object Undefined]":
                        return "undefined";
                        break;
                    case "[object Null]":
                        return "null";
                        break;
                    case "[object Function]":
                        return Data.toString();
                        break;
                    case "[object Array]":
                        return "[" + Data.map(item => {
                            return $$$.stringify(item);
                        }).toString() + "]";
                        break;
                    case "[object Object]":
                        return "{" + Object.keys(Data).map(item => {
                            return '"' + item + '":' + $$$.stringify(Data[item]);
                        }).join(",") + "}";
                        break;
                    default:
                        return JSON.stringify(Data);
                }
            },
            log(obj) {
                for (let i = 0; i < arguments.length; i++) {
                    fba.log(arguments[i]);
                }
                return obj;
            }

        });
        let dateFormatCache = {};
        let loadedMap=new Map;
        let class2type = {},
            classtype = ["Boolean", "Number", "String,", "Function", "Array", "Date", "RegExp", "Object", "Error", "Symbol", "Window"],
            core_toString = class2type.toString;

        classtype.forEach((name) => {
            class2type["[object " + name + "]"] = name.toLowerCase();
        });

        function base64Func(tg, funcStr) {
            if (tg.isbase64) {
                let q = tg.base64quote;
                return 'eval(base64Decode(' + q + fba.base64Encode(funcStr) + q + '));';
            } else {
                return funcStr;
            }
        }

        function toStringFun(arr) {
            var args = [];
            for (let i = 1, j = 0; i < arr.length; i++, j++) {
                args[j] = $$$.stringify(arr[i]);
            }
            if (typeof arr[0] === "function") {
                return "(" + arr[0] + ")(" + args.toString() + ")";
            } else {
                return "";
            }
        }
        //方法
        $$$.fn = {
            constructor: HikerUrl,
            b64(quote) {
                this.base64quote = quote || "\"";
                this.isbase64 = !this.isbase64;
                return this;
            },
            rule() {
                return this.param1 + head.rule + (this.param2 || "js:" + base64Func(this, toStringFun(arguments)));
            },
            lazyRule() {
                return this.param1 + head.lazyRule + this.param2 + ".js:" + base64Func(this, toStringFun(arguments));
            },
            input() {
                return head.input + JSON.stringify({
                    value: this.param1,
                    hint: this.param2,
                    js: toStringFun(arguments)
                });
            },
            confirm() {
                return head.confirm + this.param1 + ".js:" + base64Func(this, toStringFun(arguments));
            },
            x5Lazy() {
                return head.x5Lazy + this.param1 + "@" + toStringFun(arguments);
            },
            webLazy() {
                return head.webLazy + this.param1 + "@" + toStringFun(arguments);
            },
            select() {
                return head.select + JSON.stringify({
                    title: this.param3,
                    options: Array.isArray(this.param1) ? this.param1 : [],
                    col: this.param2 || 1,
                    js: toStringFun(arguments)
                });
            },
            x5LazyRule(fun) {
                fy_bridge_app.parseLazyRuleAsync(this.param1 + head.lazyRule + this.param2 + ".js:eval(decodeURIComponent(`" + encodeURIComponent(toStringFun(arguments)) + "`))", "console.log(input)");
            }
        }
        HikerUrl.prototype = $$$.prototype = $$$.fn;
        windows.$$$ = Object.seal($$$);
    })(window);
    (function() {
        const data = Symbol("data");
        const build = Symbol("build");
        const forbid = Symbol("forbid");
        let _$_ = $$$;
        let _MY_TYPE_ = typeof MY_TYPE === "undefined" ? "eval" : MY_TYPE;

        function HikerUrl(param) {
            if (!Array.isArray(param)) {
                throw new Error("HikerUrl[U]:非法参数");
            }
            this.param = param;
            this[data] = [];
        }

        function HikerUrlData(input, paramArr, skip) {
            if (!(Array.isArray(input) || input === undefined)) {
                throw new Error("HikerUrlData:非法参数");
            }
            this.input = input || [];
            this.paramArr = paramArr;
            this.skip = skip || 0;
        }

        function then(input, paramArr, skip) {
            return new HikerUrlData(input, paramArr, skip);
        }

        function $U() {
            return new HikerUrl(Array.from(arguments) || []);
        }
        const HIKERSET = ["lazyRule", "rule", "input", "confirm", "select", "x5Lazy", "webLazy"];
        HIKERSET.forEach((key) => {
            HikerUrl.prototype[key] = function(fun) {
                if (this[forbid]) {
                    throw new Error("HikerUrl[U]:rule后不能继续调用");
                } else if (key === "rule") {
                    this[forbid] = true;
                }
                this[data].push([key, fun, [].slice.call(arguments, 1)]);
                return this;
            }
        });
        HikerUrl.prototype[build] = function(param) {
            let funList = this[data];
            if (!(Array.isArray(funList) && funList.length)) {
                throw new Error("HikerUrl[U]:函数调用链不存在");
            }
            let item = funList.shift();
            let $tem = _$_.apply(_$_, this.param);
            if (item[0] == "lazyRule" || item[0] == "rule") {
                $tem.b64("'");
            }
            return $tem[item[0]]((targetFunItem, funList, param) => {
                return $U.runCode(targetFunItem, funList, param);
            }, item, funList, param);
        };
        Object.assign(HikerUrl.prototype, {
            init(inputArr, funList) {
                this.param = inputArr;
                this[data] = funList || [];
                return this;
            }
        });
        Object.assign(HikerUrl, {
            runCode(targetFunItem, funList, param) {
                let [type, fun, paramArr] = targetFunItem;
                _$_.hiker.SUPER = param;
                if (funList.length === 0) {
                    _$_.hiker.then = undefined;
                }
                let paramObject = fun.apply(fun, paramArr);
                if (funList.length && paramObject instanceof HikerUrlData) {
                    funList = paramObject.skip ? funList.slice(paramObject.skip) : funList;
                    let $hikerObject = $U().init(paramObject.input, funList);
                    return $hikerObject[build](paramObject.paramArr);
                } else {
                    return paramObject;
                }
            }
        });

        function buildHikerUrl(data) {
            if (data instanceof HikerUrl) {
                return data[build]();
            } else if (Array.isArray(data)) {
                let layout = [];
                for (let i = 0; i < data.length; i++) {
                    let it = data[i];
                    if (it.url instanceof HikerUrl) {
                        it.url = it.url[build]();
                    }
                    layout.push(it);
                }
                return layout;
            }
        }
        $U.runCode = (a, b, c) => HikerUrl.runCode(a, b, c);
        $U.build = (data) => buildHikerUrl(data);
        _$_.hiker.$U = $U;
        if (_MY_TYPE_ === "eval") {
            _$_.hiker.then = then;
        }
    })()

    if(!window.isPc && fy_bridge_app.isPc() == 'true'){
        window.isPc = true;
        var oMeta = document.createElement('meta');
        oMeta.content = 'target-densitydpi=400';
        oMeta.name = 'viewport';
        document.getElementsByTagName('head')[0].appendChild(oMeta);
    }
})();
