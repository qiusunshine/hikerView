(function(){
	if(window.parsehtml=="parse" && window.parseResult!=null && window.parseResult.list.length>0){
		return;
	}
	window.parsehtml="parse";
	var waitTime = 0;
	console.log("startParse");
	if(typeof rule == "undefined"){
		var rule = "";
	}
	var parseType = window.parseType || "chapter";
	if(parseType == "chapter"){
		rule = window.chapterRule || rule;
	}else if(parseType == "search"){
		rule = window.searchRule || rule;
	}
	startParse(rule);

	function startParse(rule) {
		if (window.hasLoadHtml) {
			return;
		}
		if(rule == null || rule == ""){
			console.log("规则为空");
			return;
		}
		window.parseResult = parseHtmlForData(rule);
//		console.log("window.parseResult, ", JSON.stringify(window.parseResult));
		if (window.parseResult != null && window.parseResult.list != null && window.parseResult.list.length > 0) {
			if(typeof addMoreMsg != "undefined"){
				addMoreMsg();
			}
			loadHtml();
			return;
		}
		if (waitTime > 15000) {
			console.log("15秒还没有获取到数据");
			return;
		}
		setTimeout(function() {
			startParse(rule);
			waitTime = waitTime + 200;
		}, 200);
	}
	
	function parseHtmlForData(rule){
		if(rule == null || rule == ""){
			console.log("规则为空");
			return;
		}
		if(parseType == "chapter"){
			return parseHtmlForChapter(rule);
		}else if(parseType == "search"){
			return parseHtmlForSearch(rule);
		}
	}

	function parseHtmlForSearch(rule){
		console.log("parseHtmlForSearch, rule=", rule);
		let data = {list: []};
		if (rule.indexOf("js:")===0) {
			return;
		}
		let doc = document.body.parentElement;
		let ss = rule.split(";");
		let ss0 = ss[0].split("&&");
		let element = getElement(ss0[0], doc);
		let elements;
		for (let i = 1; i < ss0.length - 1; i++) {
			element = getElement(ss0[i], element);
		}
		elements = selectElements(element, ss0[ss0.length - 1]);
		for (let i = 0; i < elements.length; i++) {
			let elementt = elements[i];
			try {
				let listBean = {};
				//获取名字
				let ss1 = ss[1].split("&&");
				let element2;
				if (ss1.length === 1) {
					element2 = elementt;
				} else {
					element2 = getElement(ss1[0], elementt);
				}
				for (let i = 1; i < ss1.length - 1; i++) {
					element2 = getElement(ss1[i], element2);
				}
				listBean.title = getText(element2, ss1[ss1.length - 1]);
				//获取链接
				let ss2 = ss[2].split("&&");
				let element3;
				if (ss2.length === 1) {
					element3 = elementt;
				} else {
					element3 = getElement(ss2[0], elementt);
				}
				for (let i = 1; i < ss2.length - 1; i++) {
					element3 = getElement(ss2[i], elementt);
				}
				listBean.url = getUrl(element3, ss2[ss2.length - 1], window.location.href);
				//获取更多信息
				if(ss.length > 3 && "*" != ss[3]){
					let ss3 = ss[3].split("&&");
					let element4;
					if (ss3.length === 1) {
						element4 = elementt;
					} else {
						element4 = getElement(ss3[0], elementt);
					}
					for (let i = 1; i < ss3.length - 1; i++) {
						element4 = getElement(ss3[i], elementt);
					}
					listBean.updateTime = getText(element4, ss3[ss3.length - 1]);
				}
				//获取content
				if(ss.length > 4 && "*" != ss[4]){
					let ss3 = ss[4].split("&&");
					let element4;
					if (ss3.length === 1) {
						element4 = elementt;
					} else {
						element4 = getElement(ss3[0], elementt);
					}
					for (let i = 1; i < ss3.length - 1; i++) {
						element4 = getElement(ss3[i], elementt);
					}
					listBean.description = getText(element4, ss3[ss3.length - 1]);
				}
				//获取图片链接
				if(ss.length > 5 && "*" != ss[5]) {
					let ss2 = ss[5].split("&&");
					let element3;
					if (ss2.length === 1) {
						element3 = elementt;
					} else {
						element3 = getElement(ss2[0], elementt);
					}
					for (let i = 1; i < ss2.length - 1; i++) {
						element3 = getElement(ss2[i], elementt);
					}
					listBean.imgUrl = getUrl(element3, ss2[ss2.length - 1], window.location.href);
				}
				data.list.push(listBean);
			} catch (e) {
				console.log(e)
			}
		}
		return data;
	}

	function parseHtmlForChapter(rule){
		console.log("parseHtmlForChapter, rule=", rule);
		let data = {list: []};
		if (rule.indexOf("js:")==0) {
			return;
		}
		try {
			let shouldGetDesc = false;
			let ss = rule.split(";");
			if (ss.length === 6) {
				shouldGetDesc = true;
			}
			//获取图片链接
			if (ss[0].indexOf("*") === 0) {
				data.imgUrl = "*";
			} else {
				let ss0 = ss[0].split("&&");
				let element0 = getElement(ss0[0], document.body.parentElement);
				for (let i = 1; i < ss0.length - 1; i++) {
					element0 = getElement(ss0[i], element0);
				}
				data.imgUrl = getUrl(element0, ss0[ss0.length - 1], window.location.href);
			}
			//获取简介
			let ss1 = ss[1].split("&&");
			let element1 = getElement(ss1[0], document.body.parentElement);
			for (let i = 1; i < ss1.length - 1; i++) {
				element1 = getElement(ss1[i], element1);
			}
			data.description = getText(element1, ss1[ss1.length - 1]);
			//获取描述---哪个源
			let descs = [];
			if (shouldGetDesc) {
				try {
					if (ss[5].indexOf("@")) {
						let descAll = ss[5].split("@");
						let descPre = descAll[0].split("&&");
						let elementDesc = getElement(descPre[0], document.body.parentElement);
						for (let i = 1; i < descPre.length - 1; i++) {
							elementDesc = getElement(descPre[i], elementDesc);
						}
						let elementDescLast = selectElements(elementDesc, descPre[descPre.length - 1]);
						for (let i = 0; i < elementDescLast.length; i++) {
							let descSuf = descAll[1].split("&&");
							let elementDesc2 = getElement(descSuf[0], elementDescLast[i]);
							for (let j = 1; i < descSuf.length - 1; j++) {
								elementDesc2 = getElement(descSuf[j], elementDesc2);
							}
							descs.push(getText(elementDesc2, descSuf[descSuf.length - 1]));
						}
					}
				} catch (e) {
//					console.log(e)
				}
			}
			//循环获取集数
			let descStrs = [];
			let elements = [];
			if (ss[2].indexOf("@")) {
				let kkk = ss[2].split("@");
				let kkk2 = kkk[0].split("&&");
				let element = getElement(kkk2[0], document.body.parentElement);
				for (let i = 1; i < kkk2.length - 1; i++) {
					element = getElement(kkk2[i], element);
				}
				let elements1 = selectElements(element, kkk2[kkk2.length - 1]);
				for (let i = 0; i < elements1.length; i++) {
					let tElements = selectElements(elements1[i], kkk[1]);
					if (i < descs.length) {
						for (let j = 0; j < tElements.length; j++) {
							descStrs.push(descs[i]);
						}
					}
					elements.push(...tElements);
				}
			} else {
				let ss2 = ss[2].split("&&");
				let element = getElement(ss2[0], document.body.parentElement);
				for (let i = 1; i < ss2.length - 1; i++) {
					element = getElement(ss2[i], element);
				}
				elements.push(...selectElements(element, ss2[ss2.length - 1]));
			}
			//获取集数和链接
			let k = 0;
			let placeList = [];
			for (let i = 0; i < elements.length; i++) {
				let elementt = elements[i];
				try {
					let bean = {};
					//获取集数名字
					let ss3 = ss[3].split("&&");
					let element2;
					if (ss3.length === 1) {
						element2 = elementt;
					} else {
						element2 = getElement(ss3[0], elementt);
					}
					for (let i = 1; i < ss3.length - 1; i++) {
						element2 = getElement(ss3[i], element2);
					}
					bean.title = getText(element2, ss3[ss3.length - 1]);
					//获取集数链接
					let ss4 = ss[4].split("&&");
					let element3;
					if (ss4.length === 1) {
						element3 = elementt;
					} else element3 = getElement(ss4[0], elementt);
					for (let i = 1; i < ss4.length - 1; i++) {
						element3 = getElement(ss4[i], element3);
					}
					bean.url = getUrl(element3, ss4[ss4.length - 1], window.location.href);
					bean.type = 1;
					if (k < descStrs.length) {
						if (k === 0 || !descStrs[k] === (descStrs[k - 1])) {
							let chapterBean = {type: 5};
							chapterBean.title = descStrs[k];
							placeList.push(chapterBean);
						}
						bean.desc = descStrs[k];
					}
					placeList.push(bean);
					k++;
				} catch (e) {
				}
			}
			let lastList = [];
			for (let i = 0; i < placeList.length; i++) {
				let d = placeList[i];
				if (d.type === 1) {
					if (lastList.length <= 0 || lastList[lastList.length - 1].title !== d.desc) {
						let l = {title: d.desc, list: []};
						l.list.push(d);
						lastList.push(l);
					} else {
						lastList[lastList.length - 1].list.push(d);
					}
				}
			}
			data.list = lastList;
		} catch (e) {
		}
		data.videoName = document.title.split(" ")[0].split("-")[0];
		return data;
	}

	function getElement(rule, parent) {
		if (rule.indexOf("Text") === 0 || rule.indexOf("Attr") === 0) {
			return parent;
		}
		let normalAttrs = ["href", "src", "class", "title", "alt"];
		for (let i = 0; i < normalAttrs.length; i++) {
			if (normalAttrs[i] === rule) {
				return parent;
			}
		}
		let ors = rule.split("\\|\\|");
		if (ors.length > 1) {
			for (let i = 0; i < ors.length; i++) {
				let e = null;
				try {
					e = getElement(ors[i], parent);
				} catch (e1) {
//					console.log(e1);
				}
				if (e != null) {
					return e;
				}
			}
		}
		let ss01 = rule.split(",");
		if (ss01.length > 1) {
			let index = parseInt(ss01[1]);
			let elements = parent.querySelectorAll(ss01[0]);
			if (index < 0) {
				return elements[elements.length + index];
			} else {
				return elements[index];
			}
		} else return parent.querySelector(ss01);
	}

	function getUrl(element3, lastRule, lastUrl) {
		if (lastRule === "*") {
			return "null";
		}
		let ors = lastRule.split("\\|\\|");
		if (ors.length > 1) {
			for (let i = 0; i < ors.length; i++) {
				let e = null;
				try {
					e = getUrlWithoutOr(element3, ors[i], lastUrl);
				} catch (e1) {
//					console.log(e1);
				}
				if (e != null && e != "") {
					return e;
				}
			}
		}
		return getUrlWithoutOr(element3, lastRule, lastUrl);
	}

	function getBaseUrl() {
		let scheme = window.location.href.split("//")[0] + "//";
		return scheme + window.location.href.split("//")[1].split("/")[0];
	}

	function getUrlWithoutOr(element3, lastRule, lastUrl) {
		let js = "";
		let ss = lastRule.split("\\.js:");
		if (ss.length > 1) {
			lastRule = ss[0];
			js = ss[1];
		}
		let url;
		if (lastRule.indexOf("Text") === 0) {
			url = element3.innerText;
		} else if (lastRule.indexOf("AttrNo") === 0) {
			url = element3.getAttribute(lastRule.replace("AttrNo", ""));
			return (url.indexOf("/") === 0 ? getBaseUrl() : (getBaseUrl() + "/")) + url;
		} else if (lastRule.indexOf("AttrYes") === 0) {
			url = element3.getAttribute(lastRule.replace("AttrYes", ""));
		} else if (lastRule.indexOf("Attr") === 0) {
			url = element3.getAttribute(lastRule.replace("Attr", ""));
		} else {
			url = element3.getAttribute(lastRule);
		}
		if (js == null || js === "") {
			url = url.replace(/^\s+|\s+$/g, '');
		} else {
			try {
				url = eval(js.replace("input", url));
			} catch (e) {
				url = url.replace(/^\s+|\s+$/g, '');
			}
		}
		if (url.indexOf("http") === 0) {
			return url;
		} else if (url.indexOf("//") === 0) {
			return "http:" + url;
		} else if (url.indexOf("magnet") === 0 || url.indexOf("thunder") === 0 || url.indexOf("ftp") === 0 || url.indexOf("ed2k") === 0) {
			return url;
		} else if (url.indexOf("/") === 0) {
			return (url.indexOf("/") === 0 ? getBaseUrl() : (getBaseUrl() + "/")) + url;
		} else if (url.indexOf("./") === 0) {
			let searchUrl = window.location.href;
			let c = searchUrl.split("/");
			if (c.length <= 1) {
				return url;
			}
			let sub = searchUrl.replace(c[c.length - 1], "");
			return sub + url.replace("./", "");
		} else if (url.indexOf("?") === 0) {
			return lastUrl + url;
		} else {
			let urls = url.split("\\$");
			if (urls.length > 1 && urls[1].indexOf("http") === 0) {
				return urls[1];
			}
			if (url.indexOf("url(")) {
				let urls2 = url.split("url\\(");
				if (urls2.length > 1 && urls2[1].indexOf("http") === 0) {
					return urls2[1].split("\\)")[0];
				}
			}
			return (url.indexOf("/") === 0 ? getBaseUrl() : (getBaseUrl() + "/")) + url;
		}
	}

	function getText(element, lastRule) {
		if ("*" === lastRule) {
			return "null";
		}
		let ors = lastRule.split("\\|\\|");
		if (ors.length > 1) {
			for (let i = 0; i < ors.length; i++) {
				let e = null;
				try {
					e = getTextWithoutOr(element, ors[i]);
				} catch (e1) {
//					console.log(e1)
				}
				if (e != null && e != "") {
					return e;
				}
			}
		}
		return getTextWithoutOr(element, lastRule);
	}

	function getTextWithoutOr(element, lastRule) {
		let rules = lastRule.split("!");
		let text = null;
		if (rules.length > 1) {
			if (rules[0] === "Text") {
				text = element.innerText;
			} else if (rules[0].indexOf("Attr")) {
				text = element.getAttribute(rules[0].replace("Attr", ""));
			} else {
				text = element.getAttribute(rules[0]);
			}
			text = text.replace(/^\s+|\s+$/g, '');
			for (let i = 1; i < rules.length; i++) {
				text = text.replace(rules[i], "");
			}
			return text;
		} else {
			if (lastRule === "Text") {
				text = element.innerText;
			} else if (lastRule.indexOf("Attr")) {
				text = element.getAttribute(lastRule.replace("Attr", ""));
			} else {
				text = element.getAttribute(lastRule);
			}
			return text.replace(/^\s+|\s+$/g, '');
		}
	}

	function selectElements(element, rule) {
		let ors = rule.split("\\|\\|");
		let res = [];
		for (let i = 0; i < ors.length; i++) {
			try {
				res.push(...selectElementsWithoutOr(element, ors[i]));
			} catch (e1) {
//				console.log(e1)
			}
		}
		return res;
	}

	function selectElementsWithoutOr(element, rule) {
		let rules = rule.split(",");
		if (rules.length > 1) {
			let indexNumbs = rules[1].split(":", -1);
			let startPos = 0;
			let endPos = 0;
			if (indexNumbs[0] == null || indexNumbs[0] === "") {
				startPos = 0;
			} else {
				try {
					startPos = parseInt(indexNumbs[0]);
				} catch (e) {
				}
			}
			indexNumbs.push("0");
			if (indexNumbs[1] == null || indexNumbs[1] === "") {
				endPos = 0;
			} else {
				try {
					endPos = parseInt(indexNumbs[1]);
				} catch (e) {
				}
			}
			let elements = element.querySelectorAll(rules[0]);
			if (endPos > elements.length) {
				endPos = elements.length;
			}
			if (endPos <= 0) {
				endPos = elements.length + endPos;
			}
			let res = [];
			for (let i = startPos; i < endPos; i++) {
				res.push(elements[i]);
			}
			return res;
		} else {
			return element.querySelectorAll(rule);
		}
	}




	//绑定数据到html
	
	function loadHtml(){
		var parseType = window.parseType || "chapter";
		if(parseType == "chapter"){
			loadHtmlForChapter();
		}else if(parseType == "search"){
			loadHtmlForSearch();
		}
	}

	function loadHtmlForSearch(){
		if(window.hasLoadHtml){
			return;
		}
		window.hasLoadHtml = true;
		let parseResult = window.parseResult;
		//搜索关键词
		let keyWord = parseResult.keyWord || "海阔视界";
		//搜索结果
		let list = parseResult.list || [];
		//转换数据绑定到页面
		let data = '';
		for (let k = 0; k < list.length; k++) {
		  let item = list[k];
		  let title = item.title;
		  let url = item.url;
		  let imgUrl = item.imgUrl || "https://i01piccdn.sogoucdn.com/50a339ceb97dc1a6";
		  if(imgUrl === "*"){
			  imgUrl = "https://i01piccdn.sogoucdn.com/50a339ceb97dc1a6";
		  }
		  let score = item.score || "10";
		  let type = item.type || "";
		  let year = item.year || "";
		  let updateTime = item.updateTime || "";
		  let description = item.description || "";
		  let li = `
		  <div class="stui-pannel stui-pannel-bg clearfix" onclick="window.open('${url}')">
			<div class="stui-pannel-box">
			 <div class="stui-content__thumb">
			  <a class="stui-vodlist__thumb picture v-thumb" title="${title}"><img class="lazyload" src="${imgUrl}" data-original="${imgUrl}"/>
		  <span class="play active hidden-xs"></span>
		  <span class="pic-text text-right"></span>
		  </a>
			 </div>
			 <div class="stui-content__detail">
			  <h1 class="title">${title}<span class="score text-red">${score}</span></h1>
			  <p class="data"><span class="text-muted">类型：</span><a>${type}</a>&nbsp;<span class="split-line"></span></span><a>${year}</a>&nbsp;</p>
			  <p class="data"><span class="text-muted">更新：</span><a>${updateTime}</a>&nbsp;</p>
			  <p class="data" style='display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient: vertical;overflow: hidden;'><span class="text-muted">描述：</span>${description}</p>
			 </div>
			</div>
		  </div>
		  `;
		  data = data + li;
		}
		
		let head = getHead(document.title);
		let body = getBody(data);

		let div = document.createElement('div');
		div.classList.add('HKSJ');
		div.innerHTML = body;

		document.body.classList.add('HKSJ_FADE');
		document.body.appendChild(div);

		setTimeout(function() {
		  document.body.classList.add('HKSJ_FADED');
		  document.head.innerHTML = head;
		}, 2000);
	}

	function loadHtmlForChapter(){
		if(window.hasLoadHtml){
			return;
		}
		window.hasLoadHtml = true;
		let noticeDesc = window.noticeDesc || "小棉袄就是帅";
		let parseResult = window.parseResult;
		//简介等信息
		let videoName = parseResult.videoName || "";
		let score = parseResult.score || "10";
		let imgUrl = parseResult.imgUrl || "https://i01piccdn.sogoucdn.com/50a339ceb97dc1a6";
		imgUrl = imgUrl==="*"?"https://i01piccdn.sogoucdn.com/50a339ceb97dc1a6":imgUrl;
		let type = parseResult.type || "";
		let area = parseResult.area || "";
		let year = parseResult.year || "";
		let updateTime = parseResult.updateTime || "";
		let actors = parseResult.actors || "";
		let director = parseResult.director || "";
		let description = parseResult.description || "";
		//接口和集数
		let list = parseResult.list || [];

		//转换数据绑定到页面
		let data = '';
		for (let k = 0; k < list.length; k++) {
		  let item = list[k];
		  let t = item.title;
		  let title = `
		  <div class="stui-pannel stui-pannel-bg clearfix">
			<div class="stui-pannel-box b playlist mb">
			 <div class="stui-pannel_hd">
		  <div class="stui-pannel__head bottom-line active clearfix">
			 <span class="more text-muted pull-right">${noticeDesc}</span>
			 <h3 class="title">${t}</h3>
			</div>
			 </div>
		  `;
		  let ul = `<div class="stui-pannel_bd col-pd clearfix">
			  <ul class="stui-content__playlist column8 clearfix">`;
		  for (let i = 0; i < item.list.length; i++) {
			let ut = item.list[i].title;
			let uu = item.list[i].url;
			let li = `<li><a href="${uu}">${ut}</a></li>`;
			ul = ul + li;
		  }
		  let div =
			title +
			ul +
			`
		  </ul>
			 </div>
			</div>
		   </div>
		  `;
		  data = data + div;
		}

		let head = getHead(videoName);

		let body = `
		<div class="stui-pannel stui-pannel-bg clearfix">
			<div class="stui-pannel-box">
			 <div class="stui-content__thumb">
			  <a class="stui-vodlist__thumb picture v-thumb" title="${videoName}"><img class="lazyload" src="${imgUrl}" data-original="${imgUrl}"/>
		  <span class="play active hidden-xs"></span>
		  <span class="pic-text text-right"></span>
		  </a>
			 </div>
			 <div class="stui-content__detail">
			  <h1 class="title">${videoName}<span class="score text-red">${score}</span></h1>
			  <p class="data"><span class="text-muted">类型：</span><a>${type}</a>&nbsp;<span class="split-line"></span><span class="text-muted hidden-xs">地区：</span><a>${area}</a>&nbsp;<span class="split-line"></span><span class="text-muted hidden-xs">年份：</span><a>${year}</a>&nbsp;</p>
			  <p class="data"><span class="text-muted">主演：</span><a>${actors}</a>&nbsp;</p>
			  <p class="data"><span class="text-muted">导演：</span><a>${director}</a>&nbsp;</p>
			  <p class="data"><span class="text-muted">更新：</span>${updateTime}</p>

			 </div>
			 <p class="desc" style="margin-top:15px"><span class="left text-muted">简介：</span>
		  <span class="detail-content">${description}</span>
		  </p>
			</div>
		   </div>
		   ${data}
		`;
		body = getBody(body);

		let div = document.createElement('div');
		div.classList.add('HKSJ');
		div.innerHTML = body;

		document.body.classList.add('HKSJ_FADE');
		document.body.appendChild(div);

		setTimeout(function() {
		  document.body.classList.add('HKSJ_FADED');
		  document.head.innerHTML = head;
		}, 2000);
	}
	
	function getHead(title){
		return `
			<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
			<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE10" />
			<meta name="renderer" content="webkit|ie-comp|ie-stand" />
			<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" />
			<title>${title}</title>
		`;
	}
	
	function getBody(main){
		let videoCSS = `
		@charset "utf-8";body,html{width:100%}body{margin:0;font-family:"Microsoft YaHei","微软雅黑","STHeiti","WenQuanYi Micro Hei",SimSun,sans-serif;font-size:14px;line-height:140%}ul,ol,li,p,h1,h2,h3,h4,h5,h6,form,fieldset,table,td,img,tr{margin:0;padding:0;font-weight:400}input,select{font-size:12px;vertical-align:middle;border:none}ul,li{list-style-type:none}img{border:0 none}p{margin:0 0 10px}*{-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box}:after,:before{-webkit-box-sizing:border-box;-moz-box-sizing:border-box;box-sizing:border-box}.container{position:relative;padding-right:15px;padding-left:15px;margin-right:auto;margin-left:auto}.row{position:relative;margin-right:-15px;margin-left:-15px}.container:before,.container:after,.row:before,.row:after,.clearfix:before,.clearfix:after{display:table;content:" ";clear:both}.col-pd,.col-lg-1,.col-lg-10,.col-lg-2,.col-lg-3,.col-lg-4,.col-lg-5,.col-lg-6,.col-lg-7,.col-lg-8,.col-lg-9,.col-md-1,.col-md-10,.col-md-2,.col-md-3,.col-md-4,.col-md-5,.col-md-6,.col-md-7,.col-md-8,.col-md-9,.col-sm-1,.col-sm-10,.col-sm-2,.col-sm-3,.col-sm-4,.col-sm-5,.col-sm-6,.col-sm-7,.col-sm-8,.col-sm-9,.col-xs-1,.col-xs-10,.col-xs-2,.col-xs-3,.col-xs-4,.col-xs-5,.col-xs-6,.col-xs-7,.col-xs-8,.col-xs-9{position:relative;min-height:1px;padding:10px}.col-xs-1,.col-xs-10,.col-xs-2,.col-xs-3,.col-xs-4,.col-xs-5,.col-xs-6,.col-xs-7,.col-xs-8,.col-xs-9,.col-xs-wide-1,.col-xs-wide-10,.col-xs-wide-15,.col-xs-wide-2,.col-xs-wide-25,.col-xs-wide-3,.col-xs-wide-35,.col-xs-wide-4,.col-xs-wide-45,.col-xs-wide-5,.col-xs-wide-55,.col-xs-wide-6,.col-xs-wide-65,.col-xs-wide-7,.col-xs-wide-75,.col-xs-wide-8,.col-xs-wide-85,.col-xs-wide-9,.col-xs-wide-95{float:left}.col-xs-10{width:10%}.col-xs-9{width:11.1111111%}.col-xs-8{width:12.5%}.col-xs-7{width:14.2857143%}.col-xs-6{width:16.6666667%}.col-xs-5{width:20%}.col-xs-4{width:25%}.col-xs-3{width:33.3333333%}.col-xs-2{width:50%}.col-xs-1{width:100%}.col-xs-wide-10{width:10%}.col-xs-wide-9{width:90%}.col-xs-wide-8{width:80%}.col-xs-wide-7{width:70%}.col-xs-wide-6{width:60%}.col-xs-wide-5{width:50%}.col-xs-wide-4{width:40%}.col-xs-wide-3{width:30%}.col-xs-wide-2{width:20%}.col-xs-wide-15{width:15%}.col-xs-wide-95{width:95%}.col-xs-wide-85{width:85%}.col-xs-wide-75{width:75%}.col-xs-wide-65{width:65%}.col-xs-wide-55{width:55%}.col-xs-wide-45{width:45%}.col-xs-wide-35{width:35%}.col-xs-wide-25{width:25%}@media (min-width:768px){.col-sm-1,.col-sm-10,.col-sm-2,.col-sm-3,.col-sm-4,.col-sm-5,.col-sm-6,.col-sm-7,.col-sm-8,.col-sm-9,.col-sm-wide-1,.col-sm-wide-10,.col-sm-wide-15,.col-sm-wide-2,.col-sm-wide-25,.col-sm-wide-3,.col-sm-wide-35,.col-sm-wide-4,.col-sm-wide-45,.col-sm-wide-5,.col-sm-wide-55,.col-sm-wide-6,.col-sm-wide-65,.col-sm-wide-7,.col-sm-wide-75,.col-sm-wide-8,.col-sm-wide-85,.col-sm-wide-9,.col-sm-wide-95{float:left}.col-sm-10{width:10%}.col-sm-9{width:11.1111111%}.col-sm-8{width:12.5%}.col-sm-7{width:14.2857143%}.col-sm-6{width:16.6666667%}.col-sm-5{width:20%}.col-sm-4{width:25%}.col-sm-3{width:33.3333333%}.col-sm-2{width:50%}.col-sm-1{width:100%}.col-sm-wide-10{width:10%}.col-sm-wide-9{width:90%}.col-sm-wide-8{width:80%}.col-sm-wide-7{width:70%}.col-sm-wide-6{width:60%}.col-sm-wide-5{width:50%}.col-sm-wide-4{width:40%}.col-sm-wide-3{width:30%}.col-sm-wide-2{width:20%}.col-sm-wide-15{width:15%}.col-sm-wide-95{width:95%}.col-sm-wide-85{width:85%}.col-sm-wide-75{width:75%}.col-sm-wide-65{width:65%}.col-sm-wide-55{width:55%}.col-sm-wide-45{width:45%}.col-sm-wide-35{width:35%}.col-sm-wide-25{width:25%}}@media (min-width:992px){.col-md-1,.col-md-10,.col-md-2,.col-md-3,.col-md-4,.col-md-5,.col-md-6,.col-md-7,.col-md-8,.col-md-9,.col-md-wide-1,.col-md-wide-10,.col-md-wide-15,.col-md-wide-2,.col-md-wide-25,.col-md-wide-3,.col-md-wide-35,.col-md-wide-4,.col-md-wide-45,.col-md-wide-5,.col-md-wide-55,.col-md-wide-6,.col-md-wide-65,.col-md-wide-7,.col-md-wide-75,.col-md-wide-8,.col-md-wide-85,.col-md-wide-9,.col-md-wide-95{float:left}.col-md-10{width:10%}.col-md-9{width:11.1111111%}.col-md-8{width:12.5%}.col-md-7{width:14.2857143%}.col-md-6{width:16.6666667%}.col-md-5{width:20%}.col-md-4{width:25%}.col-md-3{width:33.3333333%}.col-md-2{width:50%}.col-md-1{width:100%}.col-md-wide-10{width:10%}.col-md-wide-9{width:90%}.col-md-wide-8{width:80%}.col-md-wide-7{width:70%}.col-md-wide-6{width:60%}.col-md-wide-5{width:50%}.col-md-wide-4{width:40%}.col-md-wide-3{width:30%}.col-md-wide-2{width:20%}.col-md-wide-15{width:15%}.col-md-wide-95{width:95%}.col-md-wide-85{width:85%}.col-md-wide-75{width:75%}.col-md-wide-65{width:65%}.col-md-wide-55{width:55%}.col-md-wide-45{width:45%}.col-md-wide-35{width:35%}.col-md-wide-25{width:25%}}@media (min-width:1200px){.col-lg-1,.col-lg-10,.col-lg-2,.col-lg-3,.col-lg-4,.col-lg-5,.col-lg-6,.col-lg-7,.col-lg-8,.col-lg-9,.col-lg-wide-1,.col-lg-wide-10,.col-lg-wide-15,.col-lg-wide-2,.col-lg-wide-25,.col-lg-wide-3,.col-lg-wide-35,.col-lg-wide-4,.col-lg-wide-45,.col-lg-wide-5,.col-lg-wide-55,.col-lg-wide-6,.col-lg-wide-65,.col-lg-wide-7,.col-lg-wide-75,.col-lg-wide-8,.col-lg-wide-85,.col-lg-wide-9,.col-lg-wide-95{float:left}.col-lg-10{width:10%}.col-lg-9{width:11.1111111%}.col-lg-8{width:12.5%}.col-lg-7{width:14.2857143%}.col-lg-6{width:16.6666667%}.col-lg-5{width:20%}.col-lg-4{width:25%}.col-lg-3{width:33.3333333%}.col-lg-2{width:50%}.col-lg-1{width:100%}.col-lg-wide-10{width:10%}.col-lg-wide-9{width:90%}.col-lg-wide-8{width:80%}.col-lg-wide-7{width:70%}.col-lg-wide-6{width:60%}.col-lg-wide-5{width:50%}.col-lg-wide-4{width:40%}.col-lg-wide-3{width:30%}.col-lg-wide-2{width:20%}.col-lg-wide-15{width:15%}.col-lg-wide-95{width:95%}.col-lg-wide-85{width:85%}.col-lg-wide-75{width:75%}.col-lg-wide-65{width:65%}.col-lg-wide-55{width:55%}.col-lg-wide-45{width:45%}.col-lg-wide-35{width:35%}.col-lg-wide-25{width:25%}}@media (max-width:767px){[class*=col-]{padding:5px}}h1{font-size:22px;line-height:28px}h2{font-size:20px;line-height:26px}h3{font-size:18px;line-height:24px}h4{font-size:16px;line-height:22px}h5{font-size:14px;line-height:20px}h6{font-size:12px;line-height:18px}h1,h2,h3,h4,h5,h6{font-weight:400;margin-top:10px;margin-bottom:10px}a,button{text-decoration:none;outline:none;-webkit-tap-highlight-color:rgba(0,0,0,0)}button:hover{cursor:pointer}a:focus,a:hover,a:active{text-decoration:none}.icon{font-size:16px;vertical-align:-1px}.font-16{font-size:16px}.font-14{font-size:14px}.font-12{font-size:12px}.text-center{text-align:center}.text-left{text-align:left}.text-right{text-align:right}.text-overflow{width:100%;overflow:hidden;text-overflow:ellipsis;-o-text-overflow:ellipsis;white-space:nowrap}img{border:0;vertical-align:middle}.img-circle{border-radius:50%}.img-rounded{border-radius:5px}.img-thumbnail{padding:5px;border-radius:5px}.img-responsive{display:block;max-width:100%;height:auto}input,textarea{outline:medium none;outline:none;-webkit-tap-highlight-color:rgba(0,0,0,0)}input.form-control,input.btn{outline:0;-webkit-appearance:none}input[type="checkbox"]{vertical-align:-2px}.form-control{display:block;width:100%;height:30px;padding:2px 10px;font-size:12px;line-height:30px;border-radius:4px;transition:border-color ease-in-out .15s,box-shadow ease-in-out .15s}textarea.form-control{height:auto}.input-list{margin-bottom:20px}.input-list li{padding:10px 20px}.input-list li input.form-control{height:40px}.split-line{display:inline-block;margin-left:12px;margin-right:12px;width:1px;height:14px;vertical-align:-2px}.top-line,.top-line-dot,.bottom-line,.bottom-line-dot{position:relative}.top-line:before,.top-line-dot:before{content:" ";position:absolute;left:0;top:0;right:0;width:100%;height:1px}.bottom-line:after,.bottom-line-dot:before{content:" ";position:absolute;left:0;bottom:0;right:0;width:100%;height:1px}.badge{display:inline-block;margin-right:10px;width:18px;height:18px;text-align:center;line-height:18px;border-radius:2px;font-size:12px}.badge-radius{border-radius:50%}.btn{display:inline-block;padding:6px 35px;font-size:12px;border-radius:4px}.btn .icon{font-size:12px}.btn-lg{padding:12px 30px}.btn-block{display:block;width:100%;text-align:center}.btn.disabled{cursor:not-allowed}.tag{padding-left:10px}.tag li{float:left}.tag-btn,.tag-type{padding-top:10px;padding-left:10px}.tag-btn li,.tag-type li{padding:0 10px 10px 0}.tag-btn li a,.tag-type li a{display:block;padding:0 10px;height:30px;line-height:30px;text-align:center;font-size:12px;border-radius:2px}.tag-btn.active li a,.tag-type.active li a{border-radius:14px}.tag-text li{padding-right:10px;padding-bottom:10px}.nav{height:30px}.nav>li{float:left}.nav-head>li{margin-top:5px;margin-right:30px}.nav-head>li>a{padding-bottom:12px;font-size:16px}.nav-tabs>li{margin-left:30px}.nav-tabs>li>a{display:inline-block;padding:8px 0 10px}.nav-tag>li{margin-left:10px}.nav-tag>li>a{display:inline-block;padding:0 10px;height:25px;line-height:23px;font-size:12px;border-radius:20px}.nav-text>li{line-height:30px}.nav-text>li.active>a{color:#f80}.nav-page{margin-left:10px}.nav-page>li{margin-left:5px}.nav-page>li>a{display:inline-block;font-size:12px;padding:0 6px;height:25px;line-height:23px}.nav-page>li>a>.icon{font-size:12px}.nav-page>li:first-child{margin-left:0}.nav-page>li:first-child>a{border-radius:4px 0 0 4px}.nav-page>li:last-child>a{border-radius:0 4px 4px 0}.pic-tag{position:absolute;z-index:99;padding:2px 5px;font-size:12px;border-radius:2px}.pic-tag-t{top:5px;left:5px}.pic-tag-l{bottom:5px;left:5px}.pic-tag-b{bottom:5px;right:5px}.pic-tag-r{top:5px;right:5px}.pic-tag-h{left:0;top:0;padding:2px 8px;border-radius:0 0 8px 0}.pic-tag-lg{padding:4px 10px}.pic-text{display:block;width:100%;position:absolute;bottom:0;left:0;padding:5px 10px;font-size:12px;overflow:hidden;text-overflow:ellipsis;-o-text-overflow:ellipsis;white-space:nowrap}.pic-text-silde{padding-bottom:20px;font-size:14px}.pic-text-lg{padding:8px 20px;font-size:14px}.pic-title-t{display:block;width:100%;position:absolute;top:0;left:0;padding:5px 10px 10px;font-size:12px;word-break:break-all;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.pic-title-b{display:block;width:100%;position:absolute;bottom:0;left:0;padding:5px 10px;font-size:12px;word-break:break-all;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.stui-pannel{position:relative;margin-bottom:20px}.stui-pannel.stui-pannel-x{margin:-10px -10px 10px}.stui-pannel-bg{border-radius:2px}.stui-pannel-side{padding:0 0 0 20px}.stui-pannel-box{padding:10px}.stui-pannel-box.active{padding:0;margin:-10px}.stui-pannel_hd{padding:10px}.stui-pannel_ft{margin-top:10px;padding:10px 10px 0;text-align:center}.stui-pannel__head{position:relative;height:30px}.stui-pannel__head.active{height:40px}.stui-pannel__head .title{float:left;margin:0;padding-right:10px;line-height:24px}.stui-pannel__head .title img{display:inline-block;width:24px;height:24px;margin-right:10px;vertical-align:-5px}.stui-pannel__head .more{line-height:30px}.stui_nav__taddar .item{display:block;text-align:center}.stui_nav__taddar .item .icon{display:block;text-align:center;font-size:20px;line-height:20px}.stui_nav__taddar .item .icon-img{display:inline-block;width:24px;height:24px}.stui_nav__taddar .item .title{display:block;margin-top:3px;font-size:12px;}.stui_nav__taddar.fixed{position:fixed;bottom:0;width:100%;padding:5px 0 0}.stui-vodlist__thumb{display:block;position:relative;padding-top:150%;background:url(../img/load.gif) no-repeat;background-position:50% 50%;background-size:cover}.stui-vodlist__thumb.square{padding-top:100%;background:url(../img/load_f.gif) no-repeat;background-position:50% 50%;background-size:cover}.stui-vodlist__thumb.active{padding-top:60%;background:url(../img/load_w.gif) no-repeat;background-position:50% 50%;background-size:cover}.stui-vodlist__thumb.picture{background:none;overflow:hidden}.stui-vodlist__thumb.picture img{position:absolute;top:0;left:0;width:100%;height:100%}.stui-vodlist__thumb .play{display:none;position:absolute;top:0;z-index:1;width:100%;height:100%;background:rgba(0,0,0,.1) url(../img/play.png) center no-repeat}.stui-vodlist__thumb .play.active{display:block}.stui-vodlist__thumb:hover .play{display:block}.stui-vodlist__detail.active{padding:0 10px 10px}.stui-vodlist__detail .title{font-size:14px;margin-bottom:0}.stui-vodlist__detail .title.active{font-size:16px;margin-bottom:10px}.stui-vodlist__detail .text{min-height:19px;font-size:12px;margin-bottom:0;margin-top:5px}.stui-vodlist__detail .text-title{font-size:14px;line-height:18px;margin:11px 0 0;height:36px;overflow:hidden;text-overflow:ellipsis;display:-webkit-box;-webkit-box-orient:vertical;-webkit-line-clamp:2}.stui-vodlist__text li a{display:block;padding:10px 15px 10px 0}.stui-vodlist__text.active li a{padding:10px 0 10px}.stui-vodlist__text.active li:first-child a{padding-top:0}.stui-vodlist__text.striped li{padding:10px 0 10px}.stui-vodlist__text.striped li a{display:inline-block;padding:0}.stui-vodlist__text.striped .striped-head{padding:10px;border-radius:5px}.stui-vodlist__text.to-color li{padding:10px;border-radius:5px}.stui-vodlist__text.to-color li a{padding:0}.stui-vodlist__text.downlist{padding-top:0}.stui-vodlist__text.downlist li{padding:10px 0}.stui-vodlist__text.downlist li .text{line-height:30px;overflow:hidden;text-overflow:ellipsis;-o-text-overflow:ellipsis;white-space:nowrap}.stui-vodlist__text.downlist li a{display:inline-block;padding:0}.stui-vodlist__text.downlist li a.btn{padding:4px 12px;margin-left:10px}.stui-vodlist__media li{padding:10px 0 10px}.stui-vodlist__media.active li:first-child{padding-top:0}.stui-vodlist__media.active li:last-child{padding-bottom:0}.stui-vodlist__media li .thumb,.stui-vodlist__media .detail{display:table-cell;vertical-align:top}.stui-vodlist__media li .thumb .w-thumb{width:190px}.stui-vodlist__media li .thumb .v-thumb{width:120px}.stui-vodlist__media li .thumb .f-thumb{width:120px}.stui-vodlist__media li .thumb .m-thumb{width:65px}.stui-vodlist__media li .detail{width:100%;padding:0 20px 0}.stui-vodlist__media li .detail-side{padding:0 10px 0}.stui-vodlist__media li .detail-left{padding:0 20px 0 0}.stui-vodlist__media li .detail .title{margin:10px 0 10px}.stui-vodlist__screen{padding:10px 0 5px}.stui-vodlist__screen:first-child{padding-top:0}.stui-vodlist__screen:last-child{padding-bottom:0}.stui-vodlist__screen li{float:left;width:25%;margin-bottom:5px}.stui-vodlist__rank li{margin-bottom:10px}.stui-vodlist__rank li:last-child{margin-bottom:0}.stui-link__text li{float:left;margin-right:15px;margin-bottom:10px}.stui-link__pic li a{display:block;padding:10px 20px;border-radius:4px}.stui-link__pic li a img{max-width:100%}.stui-content__playlist{position:relative}.stui-content__playlist.overflow{max-height:300px;overflow-y:scroll}.stui-content__playlist li{float:left;margin-bottom:10px;margin-right:10px}.stui-content__playlist li a{display:inline-block;padding:5px 20px;border-radius:5px}.stui-content__playlist.full li{float:none;width:100%;margin-right:0}.stui-content__playlist.column3 li{width:33.3333333%;padding:0 5px;margin-right:0}.stui-content__playlist.column6 li{width:16.6666667%;padding:0 5px;margin-right:0}.stui-content__playlist.column8 li{width:12.5%;padding:0 5px;margin-right:0}.stui-content__playlist.column8 li.more,.stui-content__playlist.column10 li.narrow{width:15%}.stui-content__playlist.column10 li{width:10%;padding:0 5px;margin-right:0}.stui-content__playlist.column10 li.more,.stui-content__playlist.column10 li.narrow{width:20%}.stui-content__playlist.column12 li{width:8.33333333%;padding:0 5px;margin-right:0}.stui-content__playlist.column12 li.more,.stui-content__playlist.column12 li.narrow{width:16.6666667%}.stui-content__playlist.column3,.stui-content__playlist.column6,.stui-content__playlist.column8,.stui-content__playlist.column10,.stui-content__playlist.column12{margin:0 -5px}.stui-content__playlist.column3 li a,.stui-content__playlist.column6 li a,.stui-content__playlist.column8 li a,.stui-content__playlist.column10 li a,.stui-content__playlist.column12 li a{padding:5px}.stui-content__playlist.full li a,.stui-content__playlist.column3 li a,.stui-content__playlist.column6 li a,.stui-content__playlist.column8 li a,.stui-content__playlist.column10 li a,.stui-content__playlist.column12 li a{display:block;text-align:center;overflow:hidden;text-overflow:ellipsis;-o-text-overflow:ellipsis;white-space:nowrap}.stui-screen__list{position:relative;padding:10px 0 5px}.stui-screen__list li{float:left;margin-right:10px;margin-bottom:10px}.stui-screen__list li span{display:inline-block;padding:3px 0 3px}.stui-screen__list li a{display:inline-block;padding:3px 10px;border-radius:2px}.stui-screen__list.letter-list li a{padding:3px 5px}.stui-screen__list.letter-list li.active a{padding:3px 10px}.stui-page{margin-bottom:30px}.stui-page li{display:inline-block;margin-left:10px}.stui-page li .num,.stui-page li a{display:inline-block;padding:5px 15px;border-radius:5px}.stui-page-text{padding:0 20px 20px}.stui-page-text a,.stui-page-text em,.stui-page-text span{font-style:normal;display:inline-block;margin:0;padding:6px 12px;border-radius:2px;cursor:pointer}.stui-page-text .pagego{padding:6px;margin-right:5px;border-radius:5}.stui-page-text .pagebtn{padding:6px 12px;cursor:pointer}.stui-extra{position:fixed;right:50px;bottom:50px}.stui-extra li{position:relative;padding:15px 10px 0}.stui-extra li a,.stui-extra li span{display:block;width:50px;height:50px;line-height:50px;text-align:center;border-radius:4px;font-size:18px;cursor:pointer}.stui-extra li a .icon,.stui-extra li span .icon{font-size:18px}.stui-extra li a img{margin-top:15px;width:20px;height:20px}.stui-extra li .sideslip{display:none;position:absolute;bottom:0;right:100%}.stui-extra li .sideslip:before{content:" ";width:10px;height:10px;-webkit-transform:rotate(-45deg);-ms-transform:rotate(-45deg);transform:rotate(-45deg);border-width:0 1px 1px 0;position:absolute;bottom:20px;right:-6px;border-style:solid}.stui-extra li:hover .sideslip{display:block}.flickity-enabled{position:relative}.flickity-enabled:focus{outline:0}.flickity-viewport{overflow:hidden;position:relative;height:100%}.flickity-slider{position:absolute;width:100%;height:100%}.flickity-viewport.is-pointer-down{cursor:-webkit-grabbing;cursor:grabbing}.flickity-prev-next-button{position:absolute;top:50%;width:30px;height:60px;border:none;background-color:rgba(0,0,0,.6);cursor:pointer;-webkit-transform:translateY(-50%);-ms-transform:translateY(-50%);transform:translateY(-50%)}.flickity-prev-next-button.previous{left:10px;border-radius:0 10px 10px 0}.flickity-prev-next-button.next{right:10px;border-radius:10px 0 0 10px}.flickity-prev-next-button:disabled{opacity:0;cursor:auto}.flickity-prev-next-button svg{position:absolute;left:20%;top:20%;width:60%;height:60%}.flickity-prev-next-button .arrow{fill:#fff}.flickity-prev-next-button.no-svg{color:#fff;font-size:18px}.flickity-page-dots{position:absolute;width:100%;bottom:20px;left:0;padding:0;margin:0;list-style:none;text-align:center;line-height:1}.flickity-page-dots .dot{display:inline-block;width:15px;height:3px;margin:0 5px;background:#f80;opacity:.25;cursor:pointer}.flickity-page-dots .dot.is-selected{opacity:1}.flickity-page{padding-bottom:15px;overflow:hidden}.flickity-page .flickity-page-dots{bottom:5px}.carousel{position:relative;height:100%;overflow:hidden}.carousel .list{padding:0;margin-right:20px}.carousel .wide{width:100%}.carousel_center{padding-bottom:20px}.carousel_center .list a{position:relative}.carousel_center .list a:before{content:"";position:absolute;z-index:1;top:0;left:0;width:100%;height:100%;background-color:rgba(0,0,0,.6)}.carousel_center .list.is-selected a:before{display:none}.popup{position:fixed;right:-100%;top:0;z-index:1001;width:100%;height:100%;overflow-y:scroll;-webkit-overflow-scrolling:touch;-webkit-transition:.5s;-o-transition:.5s;-moz-transition:.5s;-ms-transition:.5s;transition:.5s}.popup-visible{right:0}.popup-head{padding:15px 10px;margin-bottom:10px}.popup-head .title{display:inline-block;margin:0}.popup-head .icon{font-size:20px}.embed-responsive{position:relative;display:block;overflow:hidden;padding:0;height:0}.embed-responsive .embed-responsive-item,.embed-responsive embed,.embed-responsive iframe,.embed-responsive object,.embed-responsive video{position:absolute;top:0;bottom:0;left:0;width:100%;height:100%;border:0}.embed-responsive-16by9{padding-bottom:56.25%}.embed-responsive-4by3{padding-bottom:75%}.fade{opacity:0;-webkit-transition:opacity .15s linear;-o-transition:opacity .15s linear;transition:opacity .15s linear}.fade.in{opacity:1}.tab-content>.tab-pane,.carousel-inner>.item{display:none}.tab-content>.tab-pane.active,.carousel-inner>.item.active{display:block}.dropdown{position:relative}.dropdown-menu{display:none;position:absolute;top:100%;left:-80%;z-index:1000;padding:10px 0;border-radius:2px}.dropdown-menu:before{content:" ";width:10px;height:10px;-webkit-transform:rotate(-45deg);-ms-transform:rotate(-45deg);transform:rotate(-45deg);border-width:1px 1px 0 0;position:absolute;top:-6px;right:20px;border-style:solid}.dropdown-menu>li>a{display:block;padding:6px 20px;text-align:center}.open>.dropdown-menu{display:block}.stui-modal{position:fixed;top:0;right:0;bottom:0;left:0;z-index:1050;display:none;overflow:hidden;-webkit-overflow-scrolling:touch;outline:0}.stui-modal__dialog{position:relative;width:350px;margin:140px auto}.stui-modal__content{position:relative;background-color:#fff;border-radius:6px;outline:0;box-shadow:0 3px 9px rgba(0,0,0,.5)}.modal-open{overflow:hidden}.modal-backdrop{position:fixed;top:0;right:0;bottom:0;left:0;z-index:1040;background-color:#000}.modal-backdrop.fade{opacity:0}.modal-backdrop.fade.in{opacity:.5}.mobile-share{position:fixed;z-index:999;top:0;bottom:0;left:0;width:100%;height:100%;animation:fade-in;animation-duration:.5s;-webkit-animation:fade-in .5s}.share-weixin{background:url(../img/share_weixin.png) rgba(0,0,0,.8) no-repeat;background-position:right top 10px;background-size:80%}.share-other{background:url(../img/share_other.png) rgba(0,0,0,.8) no-repeat;background-position:center bottom 10px;background-size:80%}.relative{position:relative}.top-fixed-up{margin-top:0!important}.top-fixed{-webkit-transition:.5s;-o-transition:.5s;-moz-transition:.5s;-ms-transition:.5s;transition:.5s}.pull-left{float:left!important}.pull-right{float:right!important}.margin-0{margin:0!important}.padding-0{padding:0!important}.margin-t0{margin:0!important}.padding-t0{padding:0!important}.margin-b0{margin:0!important}.padding-b0{padding:0!important}.block{display:block!important}.inline-block{display:inline-block!important}.hide,.visible-lg,.visible-md,.visible-sm,.visible-xs,.visible-mi{display:none!important}.mask{position:fixed;top:0;left:0;bottom:0;z-index:999;width:100%;height:100%;background:rgba(0,0,0,.2);animation:fade-in;animation-duration:.5s;-webkit-animation:fade-in .5s}@keyframes fade-in{0%{opacity:0}40%{opacity:0}100%{opacity:1}}@-webkit-keyframes fade-in{0%{opacity:0}40%{opacity:0}100%{opacity:1}}@media (min-width:1200px){.visible-lg{display:block!important}.hidden-lg{display:none!important}}@media (max-width:1199px) and (min-width:992px){.visible-md{display:block!important}.hidden-md{display:none!important}}@media (max-width:991px) and (min-width:768px){.visible-sm{display:block!important}.hidden-sm{display:none!important}}@media (max-width:991px){.stui-pannel-side{padding:0}.stui-screen__list{height:40px;padding:10px 0 10px;overflow:hidden}.stui-screen__list:last-child{padding:10px 0 0}.stui-screen__list li{margin-right:10px;margin-bottom:0}.stui-screen__list li span{padding:2px 5px 2px 0}.stui-screen__list li a{padding:2px 5px 2px;white-space:nowrap}.stui-screen__list.letter-list li a,.stui-screen__list.letter-list li.active a{padding:2px 5px 2px}.stui-vodlist__detail .title.active{font-size:14px;margin-bottom:0}.stui-vodlist__media li .thumb .v-thumb{width:90px}.stui-vodlist__media li .thumb .w-thumb{width:130px}.stui-vodlist__media li .thumb .f-thumb{width:70px}.stui-content__playlist.column3 li{width:50%}.stui-content__playlist.column6 li{width:25%}.stui-content__playlist.column8 li{width:20%}.stui-content__playlist.column10 li{width:16.6666667%}.stui-modal__dialog{width:320px;margin:50px auto}@media (max-width:767px){.visible-xs{display:block!important}.hidden-xs{display:none!important}body{font-size:12px}h1{font-size:20px;line-height:24px}h2{font-size:18px;line-height:22px}h3{font-size:16px;line-height:20px}h4{font-size:14px;line-height:18px}h5{font-size:12px;line-height:16px}h6{font-size:10px;line-height:14px}.btn{padding:6px 12px}.btn-lg{padding:12px 24px}.nav{height:20px}.nav-head>li{margin-top:2px;margin-right:20px}.nav-head>li>a{padding-bottom:10px;font-size:14px}.nav-tabs>li{margin-left:20px}.nav-tabs>li>a{padding:6px 0 6px}.nav-page>li>a{padding:0 3px;height:20px;line-height:18px}.pic-text-silde{font-size:12px}.top-line:before,.top-line-dot:before{-webkit-transform-origin:0 0;transform-origin:0 0;-webkit-transform:scaleY(.5);transform:scaleY(.5)}.bottom-line:after,.bottom-line-dot:before{-webkit-transform-origin:0 100%;transform-origin:0 100%;-webkit-transform:scaleY(.5);transform:scaleY(.5)}.m-top-line,.m-top-line-dot,.m-bottom-line,.m-bottom-line-dot{position:relative}.m-top-line:before,.m-top-line-dot:before{content:" ";position:absolute;left:0;top:0;right:0;width:100%;height:1px;-webkit-transform-origin:0 0;transform-origin:0 0;-webkit-transform:scaleY(.5);transform:scaleY(.5)}.m-bottom-line:after,.m-bottom-line-dot:before{content:" ";position:absolute;left:0;bottom:0;right:0;width:100%;height:1px;-webkit-transform-origin:0 100%;transform-origin:0 100%;-webkit-transform:scaleY(.5);transform:scaleY(.5)}.stui-pannel{margin-bottom:10px}.stui-pannel.stui-pannel-x{margin:0 0 5px}.stui-pannel-bg{border-radius:0}.stui-pannel-box{padding:10px 5px}.stui-pannel-box.active{margin:5px}.stui-pannel_hd{padding:0 5px 10px}.stui-pannel__head{height:20px}.stui-pannel__head.active{height:30px}.stui-pannel__head .title,.stui-pannel__head .title a{font-size:16px}.stui-pannel__head .title img{width:18px;height:18px;margin-right:5px;vertical-align:-3px}.stui-pannel__head .more{line-height:20px}.stui-vodlist__thumb .tag{padding:0 5px}.stui-vodlist__thumb .silde-title{font-size:12px}.stui-vodlist__detail .title,.stui-vodlist__detail .title.active{margin-bottom:0;font-size:12px}.stui-vodlist__media li .thumb .v-thumb{width:80px}.stui-vodlist__media li .thumb .w-thumb{width:130px}.stui-vodlist__media li .thumb .f-thumb{width:70px}.stui-vodlist__media li .detail{padding-left:10px}.stui-vodlist__media li .detail.active{padding-left:0;padding-right:10px}.stui-vodlist__text li a{padding:10px 0}.stui-vodlist__text.downlist li{padding:5px 0 0}.stui-content__playlist li a{padding:5px 10px;font-size:12px}.stui-content__playlist.column3 li{float:none;width:100%}.stui-content__playlist.column6 li,.stui-content__playlist.column8 li,.stui-content__playlist.column10 li{width:33.3333333%}.stui-page li{float:left;width:20%;margin:0;padding:0 5px 0 5px}.stui-page li a,.stui-page__box li .num{display:block;padding:5px 0;text-align:center}.stui-page li.page-item{width:auto;margin-bottom:5px}.stui-page li.page-item a{padding:5px 15px}.stui-extra{right:15px;bottom:15px}.stui-extra li{padding:8px 0 0}.stui-extra li a,.stui-extra li span{width:35px;height:35px;line-height:35px;font-size:16px}.stui-extra li a .icon,.stui-extra li span .icon{font-size:16px}.stui-extra li a img{margin-top:8px;width:18px;height:18px}.carousel .list{margin-right:10px}.flickity-prev-next-button{display:none}.flickity-prev-next-button.previous{left:5px;border-radius:0 5px 5px 0}.flickity-prev-next-button.next{right:5px;border-radius:5px 0 0 5px}.flickity-page-dots{bottom:15px}.stui-modal__dialog{width:100%;margin:0;padding:50px 20px}}@media (max-width:374px){.visible-mi{display:block!important}.hidden-mi{display:none!important}.stui-vodlist__media li .thumb .v-thumb{width:60px}.stui-vodlist__media li .thumb .w-thumb{width:100px}.stui-vodlist__media li .thumb .f-thumb{width:50px}.stui-vodlist__media li .detail{padding-left:10px}}@charset "utf-8";body{background:#FFF;color:#666}a,h1,h2,h3,h4,h5,h6{color:#333}a:hover{color:#F90}.text-red{color:red}.text-muted{color:#999}.form-control{background-color:#F5F5F5;color:#999;border:1px solid #EEE}.form-control.colorfff{background-color:#FFF}.form-control:focus{border-color:#F90;-webkit-box-shadow:inset 0 1px 1px rgba(255,136,0,.075),0 0 8px rgba(255,136,0,.6)}.btn,.btn:hover,.btn .icon{color:#333}.btn{border:1px solid #EEE}.btn-default{background-color:#f5f5f5;color:#333}.btn-default:hover{background-color:#f0eeee}.btn-primary{background-color:#F90;border:1px solid #F90;color:#FFF}.btn-primary .icon{color:#FFF}.btn-primary:hover{background-color:#F60;border:1px solid #F60;color:#FFF}.btn-primary:hover .icon{color:#FFF}.dropdown-menu{background-color:#FFF;border:1px solid #EEE}.dropdown-menu:before{background-color:#FFF;border-color:#EEE}.dropdown-menu>.active>a,.dropdown-menu>.active>a:focus,.dropdown-menu>.active>a:hover{background-color:#F90;color:#FFF}.split-line{background-color:#EEE}.top-line:before{border-top:1px solid #EEE}.bottom-line:after{border-bottom:1px solid #EEE}.top-line-dot:before{border-top:1px dotted #EEE}.bottom-line-dot:before{border-bottom:1px dotted #EEE}.badge{background-color:#EEE}.badge-first{background-color:#FF4A4A;color:#FFF}.badge-second{background-color:#FF7701;color:#FFF}.badge-third{background-color:#FFB400;color:#FFF}.nav-head>li.active>a,.nav-tabs>li.active>a{border-bottom:2px solid #F90;color:#F90}.nav-tag>li>a,.nav-page>li>a{background-color:#FFF;border:1px solid #EEE;color:#333}.nav-tag>li>a:hover,.nav-tag>li.active a,.nav-page>li>a:hover,.nav-page>li.active>a{background-color:#F90;border:1px solid #F90;color:#FFF}.nav-page>li>a:hover>.icon{color:#fff}.tag-type li a{background-color:#FFF;border:1px solid #EEE;color:#666}.tag-type li a:hover,.tag-type li.active a{background-color:#F90;border:1px solid #F90;color:#FFF}.tag-btn li a{background-color:#F8F8F8;color:#666}.tag-btn li a.active,.tag-text li a.active,.tag-type li a.active{color:#F90}.tag-btn li a:hover,.tag li.active a{background-color:#F90;color:#FFF}.pic-tag{background-color:rgba(0,0,0,.6);color:#FFF}.pic-tag.active,.pic-tag-h{background-color:#F90;color:#FFF}.pic-text,.pic-title-b{background-repeat:no-repeat;background-image:linear-gradient(transparent,rgba(0,0,0,.5));color:#FFF}.pic-text.active{background:rgba(0,0,0,.6);color:#FFF}.pic-title-t{background:linear-gradient(to bottom,rgba(0,0,0,.7) 0%,rgba(0,0,0,0) 100%);color:#FFF}.stui-pannel-bg{background-color:#FFF}.stui_nav__taddar .item .icon,.stui_nav__taddar .item .title{color:#999}.stui_nav__taddar .item.active .title,.stui_nav__taddar .item.active .icon{color:#F90}.stui_nav__taddar.fixed{background-color:#FFF}.stui-vodlist__bg{background-color:#F8F8F8}.stui-vodlist__bg:hover{box-shadow:0 3px 5px rgba(0,0,0,.08)}.stui-vodlist__text.striped .striped-head,.stui-vodlist__text.to-color li:nth-of-type(odd){background-color:#f5f5f5}.stui-link__pic li a{background-color:#FFF;border:1px solid #F5F5F5}.stui-link__pic li a:hover{border:1px solid #F90}.stui-screen__list li a{color:#333}.stui-screen__list li.active a{background-color:#F90;color:#FFF}.stui-content__playlist li a{border:1px solid #EEE}.stui-content__playlist li a:hover,.stui-content__playlist li.active a{border:1px solid #F90;background-color:#F90;color:#FFF}.stui-player__video{background-color:#000}.stui-page li a,.stui-page li .num{background-color:#FFF;border:1px solid #EEE}.stui-page li a:hover,.stui-page li.active a,.stui-page li.active .num,.stui-page li.disabled a{background-color:#F90;color:#FFF;border:1px solid #F90}.stui-page-text a,.stui-page-text em,.stui-page-text span{background-color:#FFF;border:1px solid #EEE}.stui-page-text span.pagenow{background-color:#F90;color:#FFF;border:1px solid #f80}.stui-page-text .pagego{border:1px solid #EEE}.stui-page-text .pagebtn{background-color:#FFF;border:1px solid #EEE}.stui-extra li a,.stui-extra li span{background-color:#FFF;box-shadow:0 1px 4px rgba(0,0,0,.1)}.stui-extra li a.backtop{background-color:rgba(0,0,0,.6);color:#FFF}.stui-extra li .sideslip{background-color:#FFF;box-shadow:0 1px 4px rgba(0,0,0,.1)}.stui-extra li .sideslip:before{background-color:#FFF;border-color:rgba(0,0,0,.1)}.popup{background-color:#F8F8F8}.popup-head{background-color:#FFF}@media (max-width:767px){.form-control{background-color:#F8F8F8;color:#999;border:0}.stui-pannel-bg,.stui-vodlist__bg,.stui-vodlist__bg:hover{box-shadow:none}.m-top-line:before{border-top:1px solid #EEE}.m-bottom-line:after{border-bottom:1px solid #EEE}.m-top-line-dot:before{border-top:1px dotted #EEE}.m-bottom-line-dot:before{border-bottom:1px dotted #EEE}}@charset "utf-8";body{padding-top:80px}.stui-header__top{position:fixed;top:0;z-index:999;width:100%;min-height:60px;background:#292838}.top-fixed-down{margin-top:-80px}.stui-header__side{float:right}.stui-header__logo,.stui-header__menu,.stui-header__search{float:left}.stui-header__logo{margin-top:5px}.stui-header__logo .logo{display:block;width:150px;height:50px;background:url(../img/logo_f.png) no-repeat;background-position:50% 50%;background-size:cover}.stui-header__search{position:relative;width:300px;margin-left:50px;margin-top:12px}.stui-header__search .form-control{height:36px;padding:0 15px;border-radius:18px;background-color:rgba(255,255,255,.2);border:0}.stui-header__search .submit{display:block;position:absolute;top:0;right:0;width:36px;height:34px;line-height:36px;text-align:center;background:none;border:0;cursor:pointer}.stui-header__search .submit .icon{font-size:14px;color:#999}.stui-header__menu{position:relative}.stui-header__menu>li{position:relative;float:left;margin-left:50px;padding:15px 0 10px}.stui-header__menu>li>a{font-size:16px;line-height:30px;color:#fff}.stui-header__menu>li.active>a,.stui-header__menu .dropdown li.active a{background-color:#F90;color:#FFF}.stui-header__menu>li .dropdown{display:none;width:520px;position:absolute;z-index:999;top:100%;left:-200px;padding:20px 10px 10px 20px;border-radius:4px;background-color:#fff;box-shadow:0 2px 8px rgba(0,0,0,.2)}.stui-header__menu>li .dropdown:before{content:" ";width:10px;height:10px;-webkit-transform:rotate(-45deg);-ms-transform:rotate(-45deg);transform:rotate(-45deg);position:absolute;top:-5px;left:230px;background-color:#fff}.stui-header__menu>li:hover .dropdown{display:block}.stui-header__menu .dropdown li{float:left;width:16.6666667%;padding-bottom:10px;padding-right:10px;text-align:center}.stui-header__menu .dropdown li a{display:block;padding:6px;border-radius:4px;background-color:#f5f5f5}.stui-header__user{float:right}.stui-header__user>li{float:left;position:relative;padding:18px 0 10px;margin-left:30px}.stui-header__user>li>a,.stui-header__user>li>a .icon{display:inline-block;font-size:24px;line-height:24px;color:#fff}.stui-header__user>li .dropdown{display:none;position:absolute;z-index:999;width:240px;top:100%;right:-15px;padding:15px;font-size:12px;color:#999;background-color:#fff;box-shadow:0 2px 8px rgba(0,0,0,.2);border-radius:4px;background-color:#fff;box-shadow:0 2px 8px rgba(0,0,0,.2)}.stui-header__user>li .dropdown:before{content:" ";width:10px;height:10px;-webkit-transform:rotate(-45deg);-ms-transform:rotate(-45deg);transform:rotate(-45deg);position:absolute;top:-5px;right:22px;background-color:#fff}.stui-header__user>li:hover .dropdown{display:block}.stui-header__user .dropdown .history li{position:relative;padding:10px 0}.stui-header__user .dropdown .history li:first-child{margin-top:10px}.stui-header__user .dropdown .history li:last-child{padding-bottom:0}.stui-banner{position:relative}.stui-banner__item{position:relative;display:block;width:100%}.stui-banner__pic{display:block;position:relative}.stui-banner__switch{position:absolute;bottom:0;left:0;z-index:99;width:100%;height:120px}.stui-banner__switch ul{margin-top:15px;padding:0 10px;display:-webkit-box;display:-webkit-flex;display:flex}.stui-banner__switch ul li{padding:10px;-webkit-box-flex:1;-webkit-flex:1;flex:1;text-align:center}.stui-banner__switch ul li span{border:3px solid rgba(255,255,255,.8)}.stui-banner__switch ul li.active span{border:3px solid #f60}.carousel-control{position:absolute;z-index:99;top:0;bottom:0;display:block;width:100px;height:100%}.carousel-control .icon{position:absolute;top:50%;margin-top:-25px;display:block;width:50px;height:50px;text-align:center;font-size:30px;line-height:50px;background-color:rgba(0,0,0,.6);color:#fff;border-radius:50%}.carousel-control.left{left:0}.carousel-control.left .icon{left:30px}.carousel-control.right{right:0}.carousel-control.right .icon{right:30px}.stui-pannel-screen{background-color:#292838}.stui-index__screen{position:relative}.stui-index__screen li{position:relative;float:left;width:25%;padding-left:20px;border-left:1px solid #333;text-align:center}.stui-index__screen li:first-child{border-left:0;padding-left:0}.stui-index__screen li a{display:block;float:left;width:33.333333%;line-height:30px;color:#ddd}.stui-content{background-color:#f8f8f8}.stui-content__detail,.stui-content__thumb{display:table-cell;vertical-align:top}.stui-content__thumb .v-thumb{width:190px}.stui-content__thumb .w-thumb{width:300px}.stui-content__detail{width:100%;padding-left:20px}.stui-content__detail .title{margin:10px 0 10px;line-height:30px}.stui-content__detail .title .score{display:inline-block;margin-left:10px;font-family:Georgia,"Times New Roman",Times,serif}.stui-content__detail .data{margin-bottom:10px}.stui-content__detail .data li{float:left;margin-right:20px}.stui-content__detail .desc{padding-left:42px}.stui-content__detail .desc .left{margin-left:-42px}.stui-content__detail .play-btn{padding-left:42px;margin-top:20px}.stui-content__detail .play-btn .share{margin-top:5px}.stui-player__item{padding:0 20px;position:relative}.stui-player__detail{margin-top:20px}.stui-player__detail .more-btn{float:right;padding-top:10px}.stui-player__detail .more-btn li{display:inline-block;margin-left:10px}.stui-player__detail .more-btn .btn{padding:6px 15px}.stui-player__detail .title{margin:0 0 10px}.stui-player__detail .detail-content{padding-top:10px}.stui-player__detail .desc{padding-left:42px}.stui-player__detail .desc .left{margin-left:-42px}.autocomplete-suggestions{padding:0 10px;margin-top:5px;border-radius:4px;background-color:#FFF;box-shadow:0 2px 10px rgba(0,0,0,.05)}.autocomplete-suggestions.active{position:absolute;z-index:9999;top:100%;width:100%}.autocomplete-suggestion{padding:10px 0;cursor:pointer;border-top:1px solid #EEE}.autocomplete-suggestion:first-child{border-top:0}.mac_results{z-index:9999;padding:0 10px;margin-top:5px;border-radius:4px;background-color:#FFF;border:1px solid #EEE;box-shadow:0 2px 10px rgba(0,0,0,.05)}.mac_results li{padding:10px 0;cursor:pointer;border-top:1px solid #EEE}.mac_results li:first-child{border:0}.stui-foot{padding-top:20px;background-color:#f8f8f8}@media (min-width:768px){.container{width:750px}}@media (min-width:990px){.container{width:970px}}@media (min-width:1200px){.container{width:1180px}}@media (min-width:1400px){.container{width:1400px}}@media (max-width:1023px){.stui-header__logo{margin-top:15px}.stui-header__logo .logo{width:127px;height:30px;background:url(../img/logo_min_f.png) no-repeat;background-position:50% 50%;background-size:cover}.stui-header__search{width:200px;margin-left:20px}.stui-header__menu>li{margin-left:20px}.stui-header__user>li{margin-left:20px}.stui-header__menu>li:hover .dropdown,.stui-header__user>li:hover .dropdown{display:none}.stui-banner .flickity-page-dots{bottom:10px}}@media (max-width:767px){body{padding-top:50px}.stui-header__top{min-height:50px}.stui-header__logo{margin-top:10px;margin-left:10px}.stui-header__search{position:absolute;top:-100%;left:10px;right:10px;width:auto;padding-right:30px}.stui-header__search .form-control{height:32px;padding:0 10px;border-radius:16px}.stui-header__search .submit{right:30px;width:32px;height:32px;line-height:30px}.stui-header__search .submit .icon{font-size:12px}.stui-header__search.active{margin:0;top:9px}.stui-header__search .search-close{position:absolute;top:6px;right:0;color:#fff}.stui-header__menu>li{margin-left:20px;padding:15px 0 10px}.stui-header__menu>li>a{line-height:20px}.stui-header__menu>li .dropdown{left:-120px;width:300px}.stui-header__menu>li .dropdown:before{left:150px}.stui-header__menu .dropdown li{width:33.333333%}.stui-header__user{padding-right:10px}.stui-header__user>li{margin-left:20px;padding:10px 0 10px}.stui-header__user>li>a .icon{font-size:20px;line-height:20px}.stui-header__menu>li .dropdown{left:-170px}.stui-header__menu>li .dropdown:before{left:190px}.stui-pannel-m50{margin:0}.stui-index__screen:before,.stui-index__screen:after{display:none}.stui-index__screen li{float:none;border:0;width:100%;padding:15px 0;overflow:hidden}.stui-index__screen li:first-child{padding:0 0 15px}.stui-index__screen li:last-child{padding:15px 0 0}.stui-index__screen li a{float:none;display:inline-block;padding:0;margin-right:20px;width:auto;font-size:14px;line-height:20px;white-space:nowrap}.stui-content__item{padding:20px 15px}.stui-content__thumb .v-thumb{width:100px}.stui-content__thumb .w-thumb{width:160px}.stui-content__detail{padding:0 0 0 10px}.stui-content__detail .title{margin:5px 0 10px;font-size:16px;line-height:18px}.stui-content__detail .data{margin:0 0 10px;font-size:12px}.stui-content__detail .play-btn{margin:0;padding:0}.stui-player__item{padding:0 10px}.stui-player__detail{margin:0}.stui-player__detail .more-btn{margin-bottom:10px}.stui-player__detail .more-btn{float:none;padding:10px;background-color:#222;display:-webkit-box;display:-webkit-flex;display:flex}.stui-player__detail .more-btn li{margin:0;padding:0 5px;-webkit-box-flex:1;-webkit-flex:1;flex:1;text-align:center}.stui-player__detail .more-btn .btn{display:block;padding:6px}.stui-player__detail .data .title{font-size:16px}.stui-player__detail.detail .title{font-size:16px}.stui-mobile__type li{padding:0 20px}.stui-mobile__type li a{display:block;padding:10px 0;text-align:center}.carousel-control{width:50px;height:100%}.carousel-control .icon{margin-top:-15px;width:30px;height:30px;font-size:20px;line-height:30px}.carousel-control.left .icon{left:10px}.carousel-control.right .icon{right:10px}.stui-foot{padding:20px 0}}
		`;

		let body = `
		<div class="container">
		<style type="text/css">
		#vConsole2, #__vconsole {
		  display: none !important;
		  height: 0 !important;
		  visibility: hidden !important;
		  overflow: hidden !important;
		  position: absolute !important:
		  left: -99999px !important;
		}
		.HKSJ_FADE > *:not(.HKSJ) {
		  z-index: 0 !important;
		  overflow: hidden;
		  min-height: 0 !important;
		  max-height: 100vh;
		  animation: FadeOut 2.5s normal forwards ease-in-out;
		}

		.HKSJ_FADED > *:not(.HKSJ) {
		  display: none !important;
		  visibility: hidden !important;
		  z-index: 0 !important;
		}

		.HKSJ_FADE {
		  min-height: 0 !important;
		  max-height: 100vh;
		}

		.HKSJ {
		  position: absolute;
		  background: #fafafa;
		  min-height: 100vh;
		  z-index: 9999999;
		  animation: HKSJ 1.5s normal forwards ease-in-out;
		  padding-bottom: 60px;
		}

		.HKSJ + .HKSJ {
		  display: none !important;
		}

		.HKSJ > .container {
		  margin-top: 0 !important;
		  margin-bottom: 0 !important;
		  padding-top: 0 !important;
		  padding-bottom: 0 !important;
		}

		.HKSJ > .container + .container {
		  display: none !important;
		}

		@keyframes FadeOut {
		  0%   {
			opacity:1;
			max-height: 100vh;
		  }
		  100% {
			opacity:0;
			max-height: 0;
		  }
		}

		@keyframes HKSJ {
		  0%   {
			top: 100vh;
		  }
		  100% {
			top: 0;
		  }
		}
		${videoCSS}
		</style>
		 <div class="row">
		  <div class="col-lg-wide-75 col-xs-1">
		   ${main}
		  </div>
		 </div>
		</div>
		`;
		return body;
	}
})();