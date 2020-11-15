(function(){
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



})();
