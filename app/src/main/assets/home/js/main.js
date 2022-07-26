require.config({
	urlArgs: "v=1.48",
	baseUrl: "js/lib"
});

require(['jquery'], function ($) {
	/**
	 * 存储获取数据函数
	 * @function get 存储数据
	 * @function set 获取数据
	 */
	var store = {
		/**
		 * 存储名称为key的val数据
		 * @param {String} key 键值
		 * @param {String} val 数据
		 */
		set: function (key, val) {
			if (!val) {
				return;
			}
			try {
				var json = JSON.stringify(val);
				if (typeof JSON.parse(json) === "object") { // 验证一下是否为JSON字符串防止保存错误
					localStorage.setItem(key, json);
				}
			} catch (e) {
				return false;
			}
		},
		/**
		 * 获取名称为key的数据
		 * @param {String} key 键值
		 */
		get: function (key) {
			return JSON.parse(localStorage.getItem(key));
		},
		has: function (key) {
			if (localStorage.getItem(key)) {
				return true;
			} else {
				return false;
			}
		},
		del: function (key) {
			localStorage.removeItem(key);
		}
	};

	// 存储内容容器
	var Storage = [];

	/**
	 * 加载存储内容
	 */
	var loadStorage = {
		/**
		 * 初始化存储内容
		 */
		initBookMark: function () {
			Storage.bookMark = [{
				name: "精选",
				url: "choice()",
				icon: "icon/discover.png"
			}, {
				
				name: "淘宝",
				url: "https://m.taobao.com",
				icon: "icon/taobao.png"
			}, {
				
				name: "微博",
				url: "https://weibo.com",
				icon: "icon/weibo.png"
			}, {
				name: "哔哩",
				url: "https://m.bilibili.com",
				icon: "icon/bilibilibog.png"
			}, {
				name: "影视",
				url: "https://www.cupfox.com/",
				icon: "icon/yingshi.png"
			}, {
				name: "热榜",
				url: "https://tophub.today",
				icon: "icon/hot.png"
			}, {
				name: "澎湃",
				url: "https://m.thepaper.cn",
				icon: "icon/pengpai.png"
			}, {
				name: "知乎",
				url: "https://www.zhihu.com",
				icon: "icon/zhihu.png"
			},{
				name: "快知",
				url: "http://xiaobaizzz.gitee.io/quick/",
				icon: "icon/one.png"
			},{
				name: "谷歌",
				url: "https://www.google.com/ncr",
				icon: "icon/google.png"
			}];
			store.set("bookMark", Storage.bookMark);
		},
		initSetData: function () {
			Storage.setData = { engines: "baidu", bookcolor: "black", searchHistory: "1" };
			store.set("setData", Storage.setData);
		},
		/**
		 * 加载设置数据 壁纸|LOGO|书签颜色|夜间模式
		 */
		applyItem: function () {
			if (store.has("setData")) {
				Storage.setData = store.get("setData");
			} else {
				this.initSetData();
			}
			if (store.has("bookMark")) {
				Storage.bookMark = store.get("bookMark");
			} else {
				this.initBookMark();
			}
			// 加载LOGO
			if (Storage.setData.logo) {
				$(".logo").html('<img src="' + Storage.setData.logo + '" width="100%" />');
			} else {
				//$(".logo").html('<svg style="max-width:100px;max-height:100px" viewBox="0 0 48 48" version="1.1" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%"><path d="M10.7,21.4c5.1-5.5,10.2-11,15.3-16.5c0.7,3.9,1.4,7.9,2.2,11.8C24.5,20.8,20.8,24.9,17,29  c-2.3,2.5-4.5,5-6.8,7.4c-0.2,0.3-0.6,0.4-1,0.3c-2.8-0.9-5.7-1.9-8.5-2.8c-0.4-0.1-0.7-0.5-0.6-0.9c0.1-0.2,0.2-0.4,0.4-0.5  C4,28.7,7.3,25,10.7,21.4z" fill="#FF3E00"></path><path d="M26.1,4.9c3.2,0.3,6.5,0.5,9.7,0.7c0.6,0.1,1,0.5,1.1,1c3.6,10.5,7.1,21.1,10.6,31.6c0.4,1-0.5,2.1-1.5,2.3  c-3.7,0.7-7.4,1.4-11.2,2.2c-0.5,0.1-1.1,0.1-1.5-0.2c-0.5-0.4-0.5-1.1-0.6-1.7c-1.5-8-3-16-4.5-24.1C27.5,12.8,26.7,8.8,26.1,4.9z" fill="#FFB700"></path><path d="M4.3,12.5c2.2-0.4,4.4-0.7,6.7-1c0.2-0.1,0.5,0.1,0.5,0.4c-0.2,3.2-0.5,6.4-0.7,9.6C7.3,25,4,28.7,0.6,32.4  c-0.2,0.2-0.3,0.3-0.4,0.5c0-0.5,0.1-1,0.2-1.5c1.1-6.1,2.2-12.2,3.3-18.4C3.7,12.8,4,12.5,4.3,12.5z" fill="#00A4F5"></path></svg>');
				$(".logo").html('<img src="icon/logo.png" height="110px" />');
			}
			// 夜间模式 和 壁纸
			var nightMode = {
				on: function () {
					$("body").removeClass('theme-black theme-white').addClass('theme-white');
					$("body").css("background-image", "");
					$("#nightCss").removeAttr('disabled');
				},
				off: function () {
					if (Storage.setData.wallpaper) {
						$("body").css("background-image", "url(" + Storage.setData.wallpaper + ")");
					} else {
						$("body").css("background-image", "");
					}
					$("body").removeClass('theme-black theme-white').addClass('theme-' + Storage.setData.bookcolor);
					$("#nightCss").attr('disabled', true);
				}
			};
			if (Storage.setData.nightMode === "1") {
				nightMode.on();
			} else {
				nightMode.off();
			}
			// 删除掉VIA浏览器夜间模式的暗色支持
			$("head").on("DOMNodeInserted DOMNodeRemoved", function (evt) {
				if (evt.target.id === "via_inject_css_night") {
					if (evt.type === "DOMNodeInserted") {
						$("#via_inject_css_night").html("");
						nightMode.on();
					} else if (evt.type === "DOMNodeRemoved") {
						nightMode.off();
					}
				}
			});
			$("#via_inject_css_night").html("");
		}
	};
	loadStorage.applyItem();

	/**
	 * DOM长按事件
	 */
	$.fn.longPress = function (fn) {
		var timeout = void 0,
			$this = this,
			startPos,
			movePos,
			endPos;
		for (var i = $this.length - 1; i > -1; i--) {
			$this[i].addEventListener("touchstart", function (e) {
				var touch = e.targetTouches[0];
				startPos = { x: touch.pageX, y: touch.pageY };
				timeout = setTimeout(function () {
					if ($this.attr("disabled") === undefined) {
						fn();
					}
				}, 700);
			}, { passive: true });
			$this[i].addEventListener("touchmove", function (e) {
				var touch = e.targetTouches[0];
				movePos = { x: touch.pageX - startPos.x, y: touch.pageY - startPos.y };
				(Math.abs(movePos.x) > 10 || Math.abs(movePos.y) > 10) && clearTimeout(timeout);
			}, { passive: true });
			$this[i].addEventListener("touchend", function () {
				clearTimeout(timeout);
			}, { passive: true });
		}
	};

	/**
	 * 文件上传函数
	 * @function callback 回调函数
	 */
	var uploadFile = function (callback) {
		var input = $('<input type="file">');
		input.bind("change", callback);
		input.click();
	}

	/**
	 * 首页书签模块
	 * @function init 初始化
	 * @function bind 绑定事件
	 * @function del 删除书签
	 * @function add 添加书签
	 */
	var bookMark = {
		init: function () {
			var _ = this;
			_.$bookmark = $('.bookmark');
			var html = '';
			for (var i = 0; i < Storage.bookMark.length; i++) {
				html += '<div class="list" data-url="' + Storage.bookMark[i].url + '"><div class="img" style="background-image:url(' + Storage.bookMark[i].icon + ')"></div><div class="text">' + Storage.bookMark[i].name + "</div></div>";
			}
			_.$bookmark.html(html);
			_.bind();
		},
		bind: function () {
			var _ = this;
			// 绑定书签长按事件
			_.$bookmark.longPress(function () {
				if (_.status !== "editing") {
					_.status = "editing";
//					$('.addbook').remove();
					_.$bookmark.append('<div class="list addbook" style="animation: scale .3s;"><div class="img"><svg viewBox="0 0 1024 1024"><path d="M736.1 480.2H543.8V287.9c0-17.6-14.4-32-32-32s-32 14.4-32 32v192.2H287.6c-17.6 0-32 14.4-32 32s14.4 32 32 32h192.2v192.2c0 17.6 14.4 32 32 32s32-14.4 32-32V544.2H736c17.6 0 32-14.4 32-32 0.1-17.6-14.3-32-31.9-32z" fill="#555"></path></svg></div><div class="text">添加</div></div>');
					$('.addbook').click(function () {
						$('.addbook').remove();
						// 取消书签编辑状态
						$(document).click();
						// 插入html
						$('#app').append('<div class="addbook-shade"><div class="addbook-from"><div class="addbook-title">添加书签</div><div class="addbook-content"><input type="text" class="addbook-input addbook-name" placeholder="名字" /><input type="text" class="addbook-input addbook-url" placeholder="网址" value="http://" /><div id="addbook-upload">点击选择图标</div></div><div class="addbook-btn"><a class="addbook-close">取消</a><a class="addbook-ok">确定</a></div></div></div>');
						//绑定事件
						$("#addbook-upload").click(function () {
							uploadFile(function () {
								var file = this.files[0];
								var reader = new FileReader();
								reader.onload = function () {
									$("#addbook-upload").html('<img src="' + this.result + '"></img><div>' + file.name + "</div>");
								};
								reader.readAsDataURL(file);
							});
						});
						$(".addbook-ok").click(function () {
							var name = $(".addbook-name").val(),
								url = $(".addbook-url").val(),
								icon = $("#addbook-upload img").attr("src");
							if (name.length && url.length) {
								if (!icon) {
									// 绘制文字图标
									var canvas = document.createElement("canvas");
									canvas.height = 100;
									canvas.width = 100;
									var context = canvas.getContext("2d");
									context.fillStyle = "#f5f5f5";
									context.arc(50, 50, 46, Math.PI * 2, 0, true);
									context.fill();
									context.fillStyle = "#222";
									context.font = "40px Arial";
									context.textAlign = "center";
									context.textBaseline = "middle";
									context.fillText(name.substr(0, 1), 50, 52);
									icon = canvas.toDataURL("image/png");
								}
								$(".addbook-close").click();
								bookMark.add(name, url, icon);
							}
						});
						$(".addbook-close").click(function () {
							$(".addbook-shade").css({ "animation": "fadeOut forwards .3s", "pointer-events": "none" });
							$(".addbook-from").css("animation", "down2 forwards .3s");
							setTimeout(function () {
								$(".addbook-shade").remove();
							}, 300);
						});
						$(".addbook-shade").click(function (evt) {
							if (evt.target === evt.currentTarget) {
								$(".addbook-close").click();
							}
						});
					});
					require(['jquery-sortable'], function () {
						_.$bookmark.sortable({
							animation: 150,
							fallbackTolerance: 3,
							touchStartThreshold: 3,
							ghostClass: "ghost",
							filter: ".delbook",
							onEnd: function (evt) {
								var startID = evt.oldIndex,
									endID = evt.newIndex;
								if (startID > endID) {
									Storage.bookMark.splice(endID, 0, Storage.bookMark[startID]);
									Storage.bookMark.splice(startID + 1, 1);
								} else {
									Storage.bookMark.splice(endID + 1, 0, Storage.bookMark[startID]);
									Storage.bookMark.splice(startID, 1);
								}
								store.set("bookMark", Storage.bookMark);
							}
						});
					})
					$(document).click(function () {
						$(document).unbind("click");
						$(".delbook,.addbook").addClass("animation");
						$(".delbook").on('transitionend', function (evt) {
							if (evt.target !== this) {
								return;
							}
							$(".delbook").remove();
							$(".addbook").remove();
							_.$bookmark.sortable("destroy");
							_.status = "";
						});
					});
					var $list = _.$bookmark.find(".list");
					for (var i = $list.length; i > -1; i--) {
						$list.eq(i).find(".img").prepend('<div class="delbook"></div>');
					}
				}
			});
//			_.$bookmark.on('click', function (evt) {
//				if (evt.target !== this || _.status === 'editing' || $('.addbook').hasClass('animation') || Storage.bookMark.length >= 20) {
//					return;
//				}
//				if ($('.addbook').length === 0) {
//					_.$bookmark.append('<div class="list addbook"><div class="img"><svg viewBox="0 0 1024 1024"><path d="M736.1 480.2H543.8V287.9c0-17.6-14.4-32-32-32s-32 14.4-32 32v192.2H287.6c-17.6 0-32 14.4-32 32s14.4 32 32 32h192.2v192.2c0 17.6 14.4 32 32 32s32-14.4 32-32V544.2H736c17.6 0 32-14.4 32-32 0.1-17.6-14.3-32-31.9-32z" fill="#555"></path></svg></div><div class="text">添加</div></div>');
//					$('.addbook').click(function () {
//						$('.addbook').remove();
//						// 取消书签编辑状态
//						$(document).click();
//						// 插入html
//						$('#app').append('<div class="addbook-shade"><div class="addbook-from"><div class="addbook-title">添加书签</div><div class="addbook-content"><input type="text" class="addbook-input addbook-name" placeholder="名字" /><input type="text" class="addbook-input addbook-url" placeholder="网址" value="http://" /><div id="addbook-upload">点击选择图标</div></div><div class="addbook-btn"><a class="addbook-close">取消</a><a class="addbook-ok">确定</a></div></div></div>');
//						//绑定事件
//						$("#addbook-upload").click(function () {
//							uploadFile(function () {
//								var file = this.files[0];
//								var reader = new FileReader();
//								reader.onload = function () {
//									$("#addbook-upload").html('<img src="' + this.result + '"></img><div>' + file.name + "</div>");
//								};
//								reader.readAsDataURL(file);
//							});
//						});
//						$(".addbook-ok").click(function () {
//							var name = $(".addbook-name").val(),
//								url = $(".addbook-url").val(),
//								icon = $("#addbook-upload img").attr("src");
//							if (name.length && url.length) {
//								if (!icon) {
//									// 绘制文字图标
//									var canvas = document.createElement("canvas");
//									canvas.height = 100;
//									canvas.width = 100;
//									var context = canvas.getContext("2d");
//									context.fillStyle = "#f5f5f5";
//									context.arc(50, 50, 46, Math.PI * 2, 0, true);
//									context.fill();
//									context.fillStyle = "#222";
//									context.font = "40px Arial";
//									context.textAlign = "center";
//									context.textBaseline = "middle";
//									context.fillText(name.substr(0, 1), 50, 52);
//									icon = canvas.toDataURL("image/png");
//								}
//								$(".addbook-close").click();
//								bookMark.add(name, url, icon);
//							}
//						});
//						$(".addbook-close").click(function () {
//							$(".addbook-shade").css({ "animation": "fadeOut forwards .3s", "pointer-events": "none" });
//							$(".addbook-from").css("animation", "down2 forwards .3s");
//							setTimeout(function () {
//								$(".addbook-shade").remove();
//							}, 300);
//						});
//						$(".addbook-shade").click(function (evt) {
//							if (evt.target === evt.currentTarget) {
//								$(".addbook-close").click();
//							}
//						});
//					})
//				} else {
//					$(".addbook").addClass("animation");
//					setTimeout(function () {
//						$(".addbook").remove();
//					}, 400);
//				}
//			});
			_.$bookmark.on('click', '.list', function (evt) {
				evt.stopPropagation();
				var dom = $(evt.currentTarget);
				if (_.status !== "editing") {
					var url = dom.data("url");
					if (url) {
						switch (url) {
							case "choice()":
								choice();
								break;
							default:
								location.href = url;
						}
					}
				} else {
					if (evt.target.className === "delbook") {
						_.del(dom.index());
					}
				}
			});
		},
		del: function (index) {
			var _ = this;
			_.$bookmark.css("overflow", "visible");
			var dom = _.$bookmark.find('.list').eq(index);
			dom.css({ transform: "translateY(60px)", opacity: 0, transition: ".3s" });
			dom.on('transitionend', function (evt) {
				if (evt.target !== this) {
					return;
				}
				dom.remove();
				_.$bookmark.css("overflow", "hidden");
			});
			Storage.bookMark.splice(index, 1);
			store.set("bookMark", Storage.bookMark);
		},
		add: function (name, url, icon) {
			var _ = this;
			url = url.match(/:\/\//) ? url : "http://" + url;
			var i = Storage.bookMark.length - 1;
			var dom = $('<div class="list" data-url="' + url + '"><div class="img" style="background-image:url(' + icon + ')"></div><div class="text">' + name + '</div></div>');
			_.$bookmark.append(dom);
			dom.css({ marginTop: "60px", opacity: "0" }).animate({ marginTop: 0, opacity: 1 }, 300);
			Storage.bookMark.push({ name: name, url: url, icon: icon });
			store.set("bookMark", Storage.bookMark);
		}
	};
	// 初始化首页书签模块
	bookMark.init();

	/**
	 * 搜索历史模块
	 * @function init 初始化
	 * @function load 加载HTML
	 * @function bind 绑定事件
	 * @function add 添加历史
	 * @function empty 清空历史
	 */
	var searchHistory = {
		init: function () {
			var _ = this;
			_.$history = $('.history');
			var arr = store.get("history");
			if (arr === null) {
				arr = [];
			}
			Storage.history = arr.slice(0, 10);
			_.load();
			_.bind();
		},
		load: function () {
			var _ = this;
			var html = '';
			var l = Storage.history.length;
			for (var i = 0; i < l; i++) {
				html += '<li>' + Storage.history[i] + '</li>';
			}
			_.$history.find('.content').html(html);
			if (l >= 1) {
				$('.emptyHistory').show();
			} else {
				$('.emptyHistory').hide();
			}
		},
		bind: function () {
			var _ = this;
			// 监听touch事件，防止点击后弹出或收回软键盘
			$('.emptyHistory')[0].addEventListener("touchstart", function (e) {
				e.preventDefault();
			}, false);
			$('.emptyHistory')[0].addEventListener("touchend", function (e) {
				if ($('.emptyHistory').hasClass('animation')) {
					_.empty();
				} else {
					$('.emptyHistory').addClass('animation');
				}
			}, false);
			_.$history.click(function (evt) {
				if (evt.target.nodeName === "LI") {
					$('.search-input').val(evt.target.innerText).trigger("propertychange");
					$('.search-btn').click();
				}
			});
		},
		add: function (text) {
			if (Storage.setData.searchHistory === "1") {
				var _ = this;
				var pos = Storage.history.indexOf(text);
				if (pos !== -1) {
					Storage.history.splice(pos, 1);
				}
				Storage.history.unshift(text);
				_.load();
				store.set("history", Storage.history);
			}
		},
		empty: function () {
			var _ = this;
			Storage.history = [];
			_.load();
			store.set("history", Storage.history);
		}
	}
	// 初始化搜索历史模块
	searchHistory.init();

	/**
	 * 更改地址栏URL参数
	 * @param {string} param 参数
	 * @param {string} value 值
	 * @param {string} url 需要更改的URL,不设置此值会使用当前链接
	 */
	var changeParam = function (param, value, url) {
		url = url || location.href;
		var reg = new RegExp("(^|)" + param + "=([^&]*)(|$)");
		var tmp = param + "=" + value;
		return url.match(reg) ? url.replace(eval(reg), tmp) : url.match("[?]") ? url + "&" + tmp : url + "?" + tmp;
	};

	// 更改URL，去除后面的参数
	history.replaceState(null, document.title, location.origin + location.pathname);

	// 绑定主页虚假输入框点击事件
	$(".ornament-input-group").click(function () {
		$('body').css("pointer-events", "none");
		history.pushState(null, document.title, changeParam("page", "search"));
		$(".s-temp").focus();
		// 隐藏主页
		$(".ornament-input-group,.bookmark").addClass("animation");
		// 显示搜索页
		$(".page-search").show();
		setTimeout(function () {
			$(".page-search").on('transitionend', function (evt) {
				if (evt.target !== this) {
					return;
				}
				$(".page-search").off('transitionend');
				$(".search-input").val('').focus();
				$('body').css("pointer-events", "");
			}).addClass("animation");
			$(".history").show().addClass("animation");
			$(".input-bg").addClass("animation");
			$(".shortcut").addClass("animation");
		}, 1);
	});

	$(".page-search").click(function (evt) {
		if (evt.target === evt.currentTarget) {
			history.go(-1);
		}
	});

	// 返回按键被点击
	window.addEventListener("popstate", function (e) {
		if ($('.page-search').is(":visible")) {
			$('body').css("pointer-events", "none");
			history.replaceState(null, document.title, location.origin + location.pathname);
			//主页
			$(".ornament-input-group,.bookmark").removeClass("animation");
			//搜索页
			$(".history").removeClass("animation");
			$(".input-bg").removeClass("animation");
			$(".shortcut").removeClass("animation");
			$(".page-search").removeClass("animation");
			$(".page-search").on('transitionend', function (evt) {
				if (evt.target !== this) {
					return;
				}
				$(".page-search").off('transitionend');
				$(".page-search").hide();
				//搜索页内容初始化
				$(".suggestion").html("");
				$(".search-btn").html("取消");
				$(".shortcut1").show();
				$(".shortcut2,.shortcut3,.empty-input").hide();
				$(".search-input").val('');
				$('body').css("pointer-events", "");
				$('.emptyHistory').removeClass('animation');
			});
		}
	}, false);

	$(".suggestion").click(function (evt) {
		if (evt.target.nodeName === "SPAN") {
			$('.search-input').focus().val($(evt.target).parent().text()).trigger("propertychange");
			return;
		} else {
			searchText(evt.target.innerText);
		}

	});

	$(".search-input").on("input propertychange", function () {
		var _ = this;
		var wd = $(_).val();
		$(".shortcut1,.shortcut2,.shortcut3").hide();
		if (!wd) {
			$(".history").show();
			$(".empty-input").hide();
			$(".search-btn").html("取消");
			$(".shortcut1").show();
			$(".suggestion").hide().html('');
		} else {
			$(".history").hide();
//			$(".empty-input").show();
			$(".search-btn").html(/[\w\-_]+(\.[\w\-_]+)?([0-9a-z_!~*'().&=+$%-]+:)?([\w\-\.,@?^=%&:/~\+#]*[\w\-\@?^=%&/~\+#])?.*\.(?:(?!(aac|ai|aif|apk|arj|asp|aspx|atom|avi|bak|bat|bin|bmp|cab|cda|cer|cfg|cfm|cgi|class|cpl|cpp|cs|css|csv|cur|dat|db|dbf|deb|dll|dmg|dmp|doc|drv|ejs|eot|eps|exe|flv|fnt|fon|gif|gz|htm|icns|ico|img|ini|iso|jad|jar|java|jpeg|jpg|js|json|jsp|key|lnk|log|mdb|mid|midi|mkv|mov|mpa|mpeg|mpg|msi|odf|odp|ods|odt|ogg|otf|part|pdf|php|pkg|pls|png|pps|ppt|pptx|psd|py|rar|rm|rpm|rss|rtf|sav|sql|svg|svgz|swf|swift|sys|tar|tex|tgz|tif|tmp|toast|ttf|txt|vb|vcd|vob|wav|wbmp|webm|webp|wks|wma|wmv|woff|wpd|wpl|wps|wsf|xhtml|xlr|xls|xml|zip)).)+/.test(wd) ? "进入" : "搜索");
			escape(wd).indexOf("%u") < 0 ? $(".shortcut2").show() : $(".shortcut3").show();
			$.ajax({
				url: "https://suggestion.baidu.com/su",
				type: "GET",
				dataType: "jsonp",
				data: { wd: wd, cb: "sug" },
				timeout: 5000,
				jsonpCallback: "sug",
				success: function (res) {
					if ($(_).val() !== wd) {
						return;
					}
					var data = res.s;
					var isStyle = $(".suggestion").html();
					var html = "";
					for (var i = data.length; i > 0; i--) {
						var style = "";
						if (isStyle === "") {
							style = "animation: fadeInDown both .5s " + (i - 1) * 0.05 + 's"';
						}
						html += '<li style="' + style + '"><div>' + data[i - 1].replace(wd, '<b>' + wd + '</b>') + "</div><span></span></li>";
					}
					$(".suggestion").show().html(html).scrollTop($(".suggestion")[0].scrollHeight);
				}
			});
			$.ajax({
				url: "https://quark.sm.cn/api/qs",
				beforeSend: function(request) {
                    request.setRequestHeader("Referer", "https://quark.sm.cn/");
                },
				type: "GET",
				data: { query: wd },
				timeout: 5000,
				success: function (res) {
					var data = res.data;
					var html = '<li>快搜:</li>';
					for (var i = 0, l = data.length; i < l; i++) {
						html += '<li>' + data[i] + '</li>';
					}
					$('.shortcut3').html(html);
				}
			});
		}
	});

	$(".empty-input").click(function () {
		$(".search-input").focus().val("").trigger("propertychange");
	});

	$(".shortcut1,.shortcut2").click(function (evt) {
		$(".search-input").focus().val($(".search-input").val() + evt.target.innerText).trigger("propertychange");
	});

	$(".shortcut3").click(function (evt) {
		if (evt.target.nodeName === "LI") {
			var text = evt.target.innerText;
			var data = {
				百科: "https://baike.baidu.com/item/",
				视频: "https://www.soku.com/m/y/video?q=",
				豆瓣: "https://m.douban.com/search/?query=",
				新闻: "https://news.baidu.com/news#/search/",
				图片: "https://cn.bing.com/images/search?q=",
				微博: "https://m.weibo.cn/search?containerid=100103type=1&q=",
				音乐: "http://m.music.migu.cn/v3/search?keyword=",
				知乎: "https://www.zhihu.com/search?q=",
				小说: "https://m.qidian.com/search?kw=",
				旅游: "https://h5.m.taobao.com/trip/rx-search/list/index.html?keyword=",
				地图: "https://m.amap.com/search/mapview/keywords=",
				电视剧: "https://m.v.qq.com/search.html?keyWord=",
				股票: "https://emwap.eastmoney.com/info/search/index?t=14&k=",
				汽车: "http://sou.m.autohome.com.cn/zonghe?q="
			}
			if (data[text]) {
				location.href = data[text] + $(".search-input").val();
			}
		}
	});

	$(".search-btn").click(function () {
		var text = $(".search-input").val();
		if ($(".search-btn").text() === "进入") {
			!text.match(/^(ht|f)tp(s?):\/\//) && (text = "http://" + text);
			history.go(-1);
			location.href = text;
		} else {
			if (!text) {
				$(".search-input").blur();
				history.go(-1);
			} else {
				searchText(text);
			}
		}
	});

	$(".search-input").keydown(function (evt) {
		// 使用回车键进行搜索
		evt.keyCode === 13 && $(".search-btn").click();
	});

	// 识别浏览器
	var browserInfo = function () {
		if (window.meta) {
			return 'huicui';
		} else if (window.mbrowser) {
			return 'x';
		} else if (window.hiker) {
			return 'hiker';
		}else if (window.via) {
			return 'via';
		}
	};

	// 搜索函数
	function searchText(text) {
		if (!text) {
			return;
		}
		searchHistory.add(text);
		history.go(-1);
		setTimeout(function () { // 异步执行 兼容QQ浏览器
			if (Storage.setData.engines === "via") {
				window.via.searchText(text);
			} else {
				location.href = {
					baidu: "https://www.baidu.com/s?wd=%s",
					quark: "https://quark.sm.cn/s?q=%s",
					google: "https://www.google.com.hk/search?q=%s",
					bing: "https://cn.bing.com/search?q=%s",
					sm: "https://m.sm.cn/s?q=%s",
					haosou: "https://www.so.com/s?q=%s",
					sogou: "https://www.sogou.com/web?query=%s",
					magi:"https://magi.com/search?q=%s",
					miji:"https://m.mijisou.com/?q=%s",
					doge:"https://www.dogedoge.com/results?q=%s",
					yandex:"https://yandex.com/search/touch/?text=%s",
					diy: Storage.setData.diyEngines
				}[Storage.setData.engines].replace("%s", text);
			}
		}, 1);
	}

	//精选页面
	function choice() {
		// 构建HTML
		var data = { '常用': [{ hl: "百度", shl: "百度一下你就知道", img: "baidu", url: "https://m.baidu.com" }, { hl: "腾讯", shl: "手机腾讯网", img: "qq", url: "https://xw.qq.com" }, { hl: "新浪", shl: "联通世界的超级平台", img: "sina", url: "https://sina.cn" }, { hl: "谷歌", shl: "最大的搜索引擎", img: "google", url: "https://www.google.com.hk" }, { hl: "搜狐", shl: "懂手机更懂你", img: "sina", url: "https://m.sohu.com" }, { hl: "网易", shl: "各有态度", img: "netease", url: "https://3g.163.com" }, { hl: "起点中文网", shl: "精彩小说大全", img: "qidian", url: "https://m.qidian.com" }, { hl: "淘宝", shl: "淘我喜欢", img: "taobao", url: "https://m.taobao.com" }, { hl: "京东", shl: "多好快省品质生活", img: "jd", url: "https://m.jd.com" }, { hl: "百度贴吧", shl: "最大的中文社区", img: "tieba", url: "http://c.tieba.baidu.com" }, { hl: "12306", shl: "你离世界只差一张票", img: "12306", url: "https://www.12306.cn" }, { hl: "飞猪", shl: "阿里旅行再升级", img: "flypig", url: "https://www.fliggy.com" }, { hl: "查快递", shl: "快递查询", img: "express_100", url: "https://m.kuaidi100.com" }, { hl: "优酷", shl: "热门视频全面覆盖", img: "youku", url: "https://www.youku.com" }, { hl: "爱奇艺", shl: "中国领先的视频门户", img: "iqiyi", url: "https://m.iqiyi.com" }, { hl: "斗鱼", shl: "每个人的直播平台", img: "douyu", url: "https://m.douyu.com" }, { hl: "虎牙", shl: "中国领先的互动直播平台", img: "huya", url: "https://m.huya.com" }, { hl: "美团", shl: "吃喝玩乐全都有", img: "meituan", url: "http://i.meituan.com" }, { hl: "小米", shl: "小米官网", img: "xiaomi", url: "https://m.mi.com" }, { hl: "58同城", shl: "让生活更简单", img: "tongcheng", url: "https://m.58.com" }, { hl: "九游", shl: "发现更多好游戏", img: "game_9", url: "http://a.9game.cn" }, { hl: "虎扑", shl: "最篮球的世界", img: "hupu", url: "https://m.hupu.com" }], '科技': [{ hl: "知乎", shl: "知识分享社区", img: "zhihu", url: "https://www.zhihu.com" }, { hl: "36kr", shl: "互联网创业资讯", img: "kr36", url: "https://36kr.com" }, { hl: "少数派", shl: "高质量应用推荐", img: "sspai", url: "https://sspai.com" }, { hl: "爱范儿", shl: "泛科技媒体", img: "ifanr", url: "https://www.ifanr.com" }, { hl: "ZEALER", shl: "电子产品评测网站", img: "zealer", url: "https://m.zealer.com" }, { hl: "瘾科技", shl: "科技新闻和测评", img: "engadget", url: "https://cn.engadget.com" }, { hl: "虎嗅网", shl: "科技媒体", img: "huxiu", url: "https://m.huxiu.com" }, { hl: "品玩", shl: "有品好玩的科技", img: "pingwest", url: "https://www.pingwest.com" }, { hl: "简书", shl: "优质原创的内容社区", img: "jianshu", url: "https://www.jianshu.com" }, { hl: "V2EX", shl: "关于分享和探索的地方", img: "v2ex", url: "https://www.v2ex.com" }], '生活': [{ hl: "豆瓣", shl: "一个神奇的社区", img: "douban", url: "https://m.douban.com/home_guide" }, { hl: "轻芒杂志", shl: "生活兴趣杂志", img: "qingmang", url: "http://zuimeia.com" }, { hl: "ONE", shl: "韩寒监制", img: "one", url: "http://m.wufazhuce.com" }, { hl: "蚂蜂窝", shl: "旅游攻略社区", img: "mafengwo", url: "https://m.mafengwo.cn" }, { hl: "小红书", shl: "可以买到国外的好东西", img: "xiaohongshu", url: "https://www.xiaohongshu.com" }, { hl: "什么值得买", shl: "应该能省点钱吧", img: "smzdm", url: "https://m.smzdm.com" }, { hl: "淘票票", shl: "不看书，就看几场电影吧", img: "taopiaopiao", url: "https://dianying.taobao.com" }, { hl: "下厨房", shl: "是男人就学做几道菜", img: "xiachufang", url: "https://m.xiachufang.com" }, { hl: "ENJOY", shl: "高端美食团购", img: "enjoy", url: "https://enjoy.ricebook.com" }], '工具': [{ hl: "豌豆荚设计", shl: "发现最优美的应用", img: "wandoujia", url: "https://m.wandoujia.com/award" }, { hl: "喜马拉雅听", shl: "音频分享平台", img: "ximalaya", url: "https://m.ximalaya.com" }, { hl: "第二课堂", shl: "守护全国2亿青少年健康成长", img: "2-class", url: "https://m.2-class.com/" }, { hl: "Mozilla", shl: "学习web开发的最佳实践", img: "mozilla", url: "https://developer.mozilla.org/zh-CN" }, { hl: "网易公开课", shl: "人chou就要多学习", img: "netease_edu_study", url: "http://m.open.163.com" }, { hl: "石墨文档", shl: "可多人实时协作的云端文档", img: "sm", url: "https://shimo.im" }] },
			html = '<div class="page-bg"></div><div class="page-choice"><div class="page-content"><ul class="choice-ul">',
			tabHtml = '<li class="current">捷径</li>',
			contentHtml = `<li class="choice-cut swiper-slide">
			<div class="list w2 h2"><a class="flex-1" href="https://quark.sm.cn/s?q=%E4%BB%8A%E5%A4%A9%E5%A4%A9%E6%B0%94"><div class="content weather"><div>访问中</div><div></div><div></div></div></a><a class="flex-right"><div class="content" style="background-image:linear-gradient(148deg, rgb(0, 188, 150) 2%, rgb(129, 239, 201) 98%)"><div class="hl">一言</div><div class="shl" id="hitokoto" style="text-align: center;top: 36px;height: 96px;white-space: pre-line;left: 15px;right: 15px;"><script src="https://v1.hitokoto.cn/?encode=js&select=%23hitokoto" defer></script></div></div></a></div>
			<div class="list h3">
				<div class="flex-left">
					<div class="list cmp-flex"><a href="https://quark.sm.cn/s?q=NBA"><div class="content" style="background-image:linear-gradient(-36deg, rgb(0, 88, 178) 0%, rgb(102, 158, 214) 99%)"><div class="hl">NBA</div><div class="cmp-icon" style="left: 60px; top: 28px; width: 34px; height: 61px; background-image: url(https://image.uc.cn/s/uae/g/3o/broccoli/resource/201912/6abef9b0-1837-11ea-ae2f-d1f91872b195.png);"></div></div></a></div>
					<div class="list cmp-flex"><a href="https://broccoli.uc.cn/apps/pneumonia/routes/index"><div class="content" style="background-image:linear-gradient(136deg, rgb(97, 71, 183) 0%, rgb(132, 113, 196) 100%)"><div style="left:10px" class="hl">新肺炎动态</div><div class="cmp-icon" style="bottom: 0px; width: 47px; height: 45px; background-image: url(https://gw.alicdn.com/L1/723/1579592073/31/78/ef/3178efce546d72e6f0772755ff1020cb.png);"></div></div></a></div>
				</div>
				<a class="flex-1 content" href="https://s.weibo.com/top/summary" style="background-image:linear-gradient(135deg, rgb(34, 34, 80) 1%, rgb(60, 60, 89) 100%)"><div class="hl relative">热搜榜</div><div class="news-list"></div></a>
			</div>
			<div class="list h2">
				<a class="flex-1 content" href="https://quark.sm.cn/s?q=%E7%83%AD%E6%90%9C&tab=zhihu" style="background-image:linear-gradient(135deg, rgb(52, 55, 60) 0%, rgb(77, 78, 86) 100%)">
				<p class="hl relative">知乎热榜</p>
				<div class="audio-list">
				
				<div class="slick-track" style="opacity: 1; transform: translate3d(0px, 0px, 0px);">

				</div>
				
				</div>
				<div class="cmp-icon" style="width: 146px;height: 99px;right: 10px;bottom: 0;background-image: url(https://image.uc.cn/s/uae/g/1y/broccoli/siaNqA6cQ/9JjV6iUso/resources/png/zhihu-icon.1509e7f13366ef5f8c0fab68526ab098.png);"></div>
				</a>
			</div>
			<div class="list"><a class="flex-1 content" href="https://m.qidian.com" style="background-image:linear-gradient(136deg, rgb(144, 148, 155) 0%, rgb(51, 51, 54) 100%)"><p class="hl">起点中文网</p><p class="shl">精彩好书推荐</p><div class="cmp-icon" style="right: 27px; top: 26px; width: 65px; height: 64px; background-image: url(https://image.uc.cn/s/uae/g/3o/broccoli/resource/201910/e6ccf190-fabb-11e9-ba63-ffe4f2687491.png);"></div></a><a class="flex-right content" href="https://quark.sm.cn/api/rest?method=quark_fanyi.dlpage&from=smor&safe=1&schema=v2&format=html&entry=shortcuts" style="linear-gradient(-36deg, rgb(97, 71, 183) 0%, rgb(132, 113, 196) 99%)"><div class="hl">夸克翻译</div><div class="cmp-icon" style="right: 0px; bottom: 0px; width: 47px; height: 45px; background-image: url(https://image.uc.cn/s/uae/g/3o/broccoli/resource/202002/84db9310-52cc-11ea-8024-a1e03ff6fb9b.png);"></div></a></div>
			<div class="list"><a class="flex-left content" style="background-image:linear-gradient(136deg, rgb(255, 81, 81) 0%, rgb(255, 111, 88) 100%)" href="https://quark.sm.cn/api/rest?method=learning_mode.home&format=html&schema=v2"><div class="hl">夸克学习</div><div class="cmp-icon" style="top: 44.5px; width: 42.5px; height: 45.5px; background-image: url(https://image.uc.cn/s/uae/g/3o/broccoli/resource/201912/c69a6570-2265-11ea-ad50-cbf7fc3a7d59.png);"></div></a><a class="flex-1 content" href="https://xw.qq.com" style="background-image:linear-gradient(to right bottom, #becce9, #98b1cf)"><p class="hl" style="left: 76px;top: 30px;">腾讯新闻</p><p class="shl" style="left: 76px;top: 51px;">新闻</p><div class="cmp-icon" style="left: 20px; top: 23px; width: 44px; height: 43.6px; background-image: url(https://image.uc.cn/s/uae/g/3o/broccoli/resource/201910/b56c1ef0-f007-11e9-bbee-8910d21fa281.png);"></div></a></div>
			<div class="list w2"><a class="flex-1 content" href="https://quark.sm.cn/api/rest?format=html&method=lawservice.home&schema=v2" style="background-image:linear-gradient(136deg, rgb(38, 85, 248) 0%, rgb(20, 152, 230) 100%)"><p class="hl">夸克法律检索</p><p class="shl">专业权威法律检索</p><div class="cmp-icon" style="right: 19px; top: 21px; width: 80px; height: 70px; background-image: url(https://image.uc.cn/s/uae/g/3o/broccoli/resource/201912/80869b60-1835-11ea-ae2f-d1f91872b195.png);"></div></a><a class="flex-right content" href="https://quark.sm.cn/s?q=垃圾分类" style="background-image:linear-gradient(to right bottom, #7cecc6, #15b695)"><div class="hl">垃圾分类</div><div class="cmp-icon" style="right: 22px; top: 45px; width: 55px; height: 45px; background-image: url(https://image.uc.cn/s/uae/g/3o/broccoli/resource/201910/d0b3d560-f005-11e9-bbee-8910d21fa281.png);"></div></a></div>
			</li>`;

		$.each(data, function (i, n) {
			tabHtml += "<li>" + i + "</li>";
			contentHtml += '<li class="choice-li swiper-slide">';
			for (var i = 0, l = n.length; i < l; i++) {
				contentHtml += '<a href="' + n[i].url + '"><div><img src="img/choice/' + n[i].img + '.png" /><p>' + n[i].hl + '</p><p>' + n[i].shl + '</p></div></a>';
			}
			contentHtml += '</li>';
		});

		// HTML添加到APP
		$('#app').append(html + tabHtml + '<span class="active-span"></span></ul><div class="choice-swipe"><ul class="swiper-wrapper"><div style="position:absolute;text-align:center;top:50%;width:100%;margin-top:-64px;color:#444">正在加载页面中...</div></ul></div><div class="choice-close"></div></div></div>');

		var dom = $(".choice-ul li");
		var width = dom.width();
		$(".active-span").css("transform", "translate3d(" + (width / 2 - 9) + "px,0,0)");

		setTimeout(function () {
			$(".page-bg").addClass("animation");
			$(".page-choice").addClass("animation");
		}, 1);

		// 动画完成后加载，防止过渡动画卡顿
		$(".page-choice").on("transitionend", function (evt) {
			// 过滤掉子元素
			if (evt.target !== this) {
				return;
			}
			$(".page-choice").off("transitionend");
			$('.swiper-wrapper').html(contentHtml);
			// 绑定事件
			var last_page = 0;

			require(['Swiper'], function (Swiper) {
				var swiper = new Swiper('.choice-swipe', {
					on: {
						slideChange: function () {
							var i = this.activeIndex;
							dom.eq(last_page).removeClass("current");
							$(".active-span").css("transform", "translate3d(" + (width * i + width / 2 - 9) + "px,0,0)");
							dom.eq(i).addClass("current");
							last_page = i;
						}
					}
				});

				// 绑定TAB点击事件
				$(".choice-ul").click(function (evt) {
					if (evt.target.nodeName == "LI") {
						swiper.slideTo($(evt.target).index());
					}
				});
			})

			// 绑定关闭按钮事件
			$(".choice-close").click(function () {
				$(".page-choice").css('pointer-events', 'none').removeClass("animation");
				$(".page-bg").removeClass("animation");
				$(".page-choice").on('transitionend', function (evt) {
					if (evt.target !== this) {
						return;
					}
					$(".page-choice").remove();
					$(".page-bg").remove();
				});
			});

			// 地理位置|天气|气温|空气质量
			requestAsync && requestAsync("https://ai.sm.cn/quark/1/api?format=json&method=weather&callback=weather",
			{method: "GET", headers: {'Content-Type':'application/json;charset=utf8','Referer':'https://ai.sm.cn/'}},
			function(key, r){
			    var res = {};
			    function weather(json){
			        res = json;
			    }
			    eval(r);
			    console.log("地理位置|天气|气温|空气质量11111111", res);
                var data = res.data;
                var color1 = data.color1;
                var color2 = data.color2;
                var location = data.location;
                var temp = data.temp;
                var air = data.air;
                var weather = data.weather;
                var html = '<div>' + temp + '</div><div>' + weather + '</div><div>' + location + ' · ' + air + '</div><div class="cmp-icon" id="lottie-box" style="background-image: url(' + data.lottie + ');"></div>';
                $('.weather').html(html).css("background-image", "linear-gradient(-33deg," + color1 + " 0%," + color2 + " 99%)");
			});
			$.ajax({
				url: "https://ai.sm.cn/quark/1/api?format=json&method=weather&callback=weather",
				type: "get",
				jsonpCallback: "weather",
				headers:{'Content-Type':'application/json;charset=utf8','Referer':'https://ai.sm.cn/'},
				success: function (res) {
					console.log("地理位置|天气|气温|空气质量", res);
					var data = res.data;
					var color1 = data.color1;
					var color2 = data.color2;
					var location = data.location;
					var temp = data.temp;
					var air = data.air;
					var weather = data.weather;
					var html = '<div>' + temp + '</div><div>' + weather + '</div><div>' + location + ' · ' + air + '</div><div class="cmp-icon" id="lottie-box" style="background-image: url(' + data.lottie + ');"></div>';
					$('.weather').html(html).css("background-image", "linear-gradient(-33deg," + color1 + " 0%," + color2 + " 99%)");
				}
			})

			// 微博热搜
			$.ajax({
				url: "https://s.weibo.com/ajax/jsonp/gettopsug?_cb=gettopsug",
				type: "get",
				dataType: "jsonp",
				jsonpCallback: "gettopsug",
				success: function (res) {
					var data = res.data;
					var html = '';
					for (var i = 0; i < 4; i++) {
						html += '<div class="news-item"><div class="news-item-count">' + (i + 1) + '</div><div class="news-item-title">' + data.list[i].word + '</div><div class="news-item-hot">' + data.list[i].num + '</div></div>';
					}
					$('.news-list').html(html);
				}
			});

			//知乎热榜
			$.ajax({
				url: "https://quark.sm.cn/api/rest?method=Newstoplist.zhihu",
				type: "get",
				beforeSend: function(request) {
                    request.setRequestHeader("Referer", "https://quark.sm.cn/");
                },
				success: function (res) {
					var data = res.data;
					var html = '';
					for (var i = 0; i < 8; i++) {
						html += '<div class="audio-item"><div class="audio-item-icon"></div><div class="audio-item-title">' + data[i].title + '</div></div>'
					}
					for (var i = 0; i < 2; i++) {
						html += '<div class="audio-item"><div class="audio-item-icon"></div><div class="audio-item-title">' + data[i].title + '</div></div>'
					}
					var $slick_track = $('.audio-list').find('.slick-track');
					$slick_track.html(html);
					var curIndex = 2;
					setInterval(function () {
						$slick_track.css({
							transform: "translate3d(0px, -" + curIndex * 27 + "px, 0px)",
							transition: "transform 500ms ease 0s"
						});
						curIndex += 2;
						$slick_track.on('transitionend', function (evt) {
							$slick_track.off('transitionend').css({
								transition: ""
							})
							if (curIndex >= 10) {
								curIndex = 2;
								$slick_track.css({
									transform: "translate3d(0px, 0px, 0px)"
								})
							}
						});
					}, 5000);
				}
			});

		})
	}
	
	//定义需要显示的搜索引擎
	var eng = `<li class="set-option" data-value="engines">
					<select class="set-select" style="margin: -15px 0 0 -19px; padding-left: 10px; border-radius:15px 0 0 15px; width: 63px; height: 34.5px; background-color: #EEEEEE; animation: down .3s; border: none; -webkit-appearance: none; -moz-appearance: none;">
						<option value="baidu">&nbsp;百&nbsp;&nbsp;&nbsp;度</option>
						<option value="google">&nbsp;谷&nbsp;&nbsp;&nbsp;歌</option>
						<option value="bing">&nbsp;必&nbsp;&nbsp;&nbsp;应</option>
						<option value="sogou">&nbsp;搜&nbsp;&nbsp;&nbsp;狗</option>
						<option value="haosou">&nbsp;好&nbsp;&nbsp;&nbsp;搜</option>
						<option value="sm">&nbsp;神&nbsp;&nbsp;&nbsp;马</option>
						<option value="quark">&nbsp;夸&nbsp;&nbsp;&nbsp;克</option>
						<option value="magi">&nbsp;麻&nbsp;&nbsp;&nbsp;谷</option>
						<option value="miji">&nbsp;秘&nbsp;&nbsp;&nbsp;迹</option>
						<option value="doge">&nbsp;多&nbsp;&nbsp;&nbsp;吉</option>
						<option value="yandex">&nbsp;裤&nbsp;&nbsp;&nbsp;衩</option>
						<option value="diy">&nbsp;自定义</option>
					</select>
				</li>`;

	//定义设置样式
	var sty = `<div class="set-from">
			<div class="set-header">
				<div class="set-back"></div>
				<p class="set-logo">主页设置</p>
			</div>
			<ul class="set-option-from">
				<li class="set-option" data-value="engines">
					<div class="set-text">
						<p class="set-title">搜索引擎</p>
					</div>
					<select class="set-select">
						<option value="baidu">&nbsp;百&nbsp;&nbsp;&nbsp;度</option>
						<option value="google">&nbsp;谷&nbsp;&nbsp;&nbsp;歌</option>
						<option value="bing">&nbsp;必&nbsp;&nbsp;&nbsp;应</option>
						<option value="sogou">&nbsp;搜&nbsp;&nbsp;&nbsp;狗</option>
						<option value="haosou">&nbsp;好&nbsp;&nbsp;&nbsp;搜</option>
						<option value="sm">&nbsp;神&nbsp;&nbsp;&nbsp;马</option>
						<option value="quark">&nbsp;夸&nbsp;&nbsp;&nbsp;克</option>
						<option value="magi">&nbsp;麻&nbsp;&nbsp;&nbsp;谷</option>
						<option value="miji">&nbsp;秘&nbsp;&nbsp;&nbsp;迹</option>
						<option value="doge">&nbsp;多&nbsp;&nbsp;&nbsp;吉</option>
						<option value="yandex">&nbsp;裤&nbsp;&nbsp;&nbsp;衩</option>
						<option value="diy">&nbsp;自定义</option>
					</select>
				</li>
				<li class="set-option" data-value="wallpaper">
					<div class="set-text">
						<p class="set-title">壁纸</p>
					</div>
				</li>
				<li class="set-option" data-value="logo">
					<p class="set-title">LOGO</p>
				</li>
				<li class="set-option" data-value="bookcolor">
					<div class="set-text">
						<p class="set-title">图标颜色</p>
					</div>
					<select class="set-select">
						<option value="black">深色图标</option>
						<option value="white">浅色图标</option>
					</select>
				</li>
				<li class="set-option" data-value="nightMode">
					<div class="set-text">
						<p class="set-title">夜间模式</p>
					</div>
					<select class="set-select">
						<option value="0">关闭</option>
						<option value="1">开启</option>
					</select>
				</li>
				<li class="set-option" data-value="searchHistory">
					<div class="set-text">
						<p class="set-title">记录搜索历史</p>
					</div>
					<select class="set-select">
						<option value="0">关闭</option>
						<option value="1">开启</option>
					</select>
				</li>
				<li class="set-option" data-value="delLogo">
					<p class="set-title">恢复默认壁纸和LOGO</p>
				</li>
				<li class="set-option" data-value="export">
					<div class="set-text">
						<p class="set-title">导出主页数据</p>
					</div>
				</li>
				<li class="set-option" data-value="import">
					<div class="set-text">
						<p class="set-title">导入主页数据</p>
					</div>
				</li>
				<li class="set-option">
					<div class="set-text">
						<p class="set-title">功能介绍</p>
						<p class="set-description">一、点击Logo进入书签（适配Via、荟萃浏览器、X浏览器、海阔视界）<br>二、长按Logo进入设置（适配所有浏览器）<br>三、长按主页图标管理快捷书签，图标还能拖动排序<br>四、适配市面上常用搜索引擎，若没有您使用的搜索引擎可以选择自定义搜索引擎<br>五、自定义主页壁纸、自定义主页Logo、设置夜间模式、设置深色浅色图标、是否记录搜索历史<br>六、导入导出主页数据，以便方便备份</p>
					</div>
				</li>
				<li class="set-option">
					<div class="set-text">
						<p class="set-title">关于</p>
						<p class="set-description"><a href="http://www.coolapk.com/u/854901">原创：酷安@BigLop</a><br><a href="http://www.coolapk.com/u/696533">修改：酷安@丨晓柏惜梦丨</a><br><a href="https://gitee.com/liumingye/quarkHomePage">仓库：https://gitee.com/liumingye/quarkHomePage</a></p>
					</div>
				</li>
			</ul>
		</div>`;

	//定义设置功能
	var fun = function() {
		$(".set-from").show();

		//if(window.via) { // 只有VIA浏览器才能显示
		//	$('option[value=via]').show();
		//}

		$.each(Storage.setData, function(i, n) {
			var select = $(".set-option[data-value=" + i + "]").find(".set-select");
			if(select) {
				select.val(n);
			}
		});

		$(".set-back").click(function() {
			$(".set-from").css("pointer-events", "none").removeClass("animation");
			$(".set-from").on('transitionend', function(evt) {
				if(evt.target !== this) {
					return;
				}
				$(".set-from").remove();
			});
		});

		$(".set-option").click(function() {
			var value = $(this).data("value");
			if(value === "wallpaper") {
				uploadFile(function() {
					var file = this.files[0];
					var reader = new FileReader();
					reader.onload = function() {
						Storage.setData.wallpaper = this.result;
						store.set("setData", Storage.setData);
						$("body").css("background-image", "url(" + Storage.setData.wallpaper + ")");
					};
					reader.readAsDataURL(file);
				});
			} else if(value === "logo") {
				uploadFile(function() {
					var file = this.files[0];
					var reader = new FileReader();
					reader.onload = function() {
						Storage.setData.logo = this.result;
						store.set("setData", Storage.setData);
						$(".logo").html('<img src="' + Storage.setData.logo + '" />');
					};
					reader.readAsDataURL(file);
				});
			} else if(value === "delLogo") {
				Storage.setData.wallpaper = "";
				Storage.setData.logo = "";
				Storage.setData.bookcolor = "black";
				store.set("setData", Storage.setData);
				location.reload();
			} else if(value === "openurl") {
				open($(this).find('.set-description').text());
			} else if(value === "export") {
				var oInput = $('<input>');
				oInput.val('{"bookMark":' + JSON.stringify(store.get('bookMark')) + '}');
				document.body.appendChild(oInput[0]);
				oInput.select();
				document.execCommand("Copy");
				alert('已复制到剪贴板，请粘贴保存文件。');
				oInput.remove();
			} else if(value === "import") {
				var data = prompt("在这粘贴主页数据");
				try {
					data = JSON.parse(data);
					store.set("bookMark", data.bookMark);
					alert("导入成功!");
					location.reload();
				} catch(e) {
					alert("导入失败!");
				}
			}
		});

		$(".set-select").change(function() {
			var dom = $(this),
				item = dom.parent().data("value"),
				value = dom.val();
			if(item === "engines" && value === "diy") {
				var engines = prompt("输入搜索引擎网址，（用“%s”代替搜索字词）");
				if(engines) {
					Storage.setData.diyEngines = engines;
				} else {
					dom.val(Storage.setData.engines);
					return;
				}
			}
			// 保存设置
			Storage.setData[item] = value;
			store.set("setData", Storage.setData);
			// 应用设置
			loadStorage.applyItem();
		});
		$(".set-from").addClass('animation');
	};

	//logo功能
	$(".logo").click(() => {
		var browser = browserInfo();
		if (browser == 'via') {
			self.location = "folder://";
		} else if (browser == 'x') {
			self.location = "x:bm?sort=default";
		} else if (browser == 'hiker') {
			self.location = "hiker://bookmark";
		} else if (browser == 'huicui') {
			self.location = "meta:bookmark";
		}
	}).longPress(() => {
		$('#app').append(sty);
		fun();
	});
	
	//显示快捷更换搜索引擎
	$(".ornament-input-group").click(() => {
		$('.engine').append(eng);
		fun();
	});
	
	//点击进入设置
	$(".set").click(() => {
		$('#app').append(sty);
		fun();
	});

	// 下滑进入搜索
	require(['touchSwipe'], function () {
		$(".page-home").swipe({
			swipeStatus: function (event, phase, direction, distance) {
				if ($('.delbook').length !== 0) {
					return;
				}
				if (phase === 'move') {
					if (distance <= 10 || direction !== "down") {
						return;
					}
					var height = $(document).height();
					$('.ornament-input-group').css({ 'transform': 'translate3d(0,' + (distance / height) * 50 + 'px,0)', 'transition': 'none' });
					$('.logo').attr("disabled", "disabled").css({ 'opacity': 1 - (distance / height) * 4, 'transition': 'none' });
					$('.bookmark').attr("disabled", "disabled").css({ 'opacity': 1 - (distance / height) * 4, 'transform': 'scale(' + (1 - (distance / height) * .2) + ')', 'transition': 'none' });
				} else if (phase === 'end' || phase === 'cancel') {
					$('.logo').removeAttr("disabled").removeAttr('style');
					$('.bookmark').removeAttr("disabled").removeAttr('style');
					$('.ornament-input-group').removeAttr('style');
					if (distance >= 100 && direction === "down") {
						$('.ornament-input-group').click();
						$('.logo').css('opacity', '0');
						$('.bookmark').css('opacity', '0');
						setTimeout(function () {
							$('.logo').css('opacity', '');
							$('.bookmark').css('opacity', '');
						}, 200);
					}
				}
			}
		});
	})

})