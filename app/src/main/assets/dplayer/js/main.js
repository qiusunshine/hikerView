function loadData(){
	$.get("listWebsites", function(result){
		var data = JSON.parse(result);
		parseHtml(data);
	});
}

function parseHtml(data){
	parseHtmlByData(data.bookmarks,"我的书签-快速访问");
	parseHtmlByData(data.collections,"我的收藏-近期浏览");
	parseHtmlByData(data.lives,"我的直链-在线直播");
	parseHtmlByData(data.websites,"我的搜索源-网站聚合");
}

function parseHtmlByData(lives, title){
	if(lives.length <= 0){
		return;
	}
	var html = "<div class=\"indexType\">\n" +
				"<div class=\"typeBar\">\n" +
					"<li class=\"shenghuo\"></li>\n" +
					"<li><a>" + title + "</a></li>\n" +
				"</div>\n" +
				"<div class=\"cnt\">\n";
	for(let i = 0; i < lives.length; i++){
		let live = lives[i];
		let h1 = "<div class=\"mainBoxNav\">\n" +
						"<li><a>[" + live.name + "]</a></li>\n" +
						"<li class=\"more\"><a href=\"#\">&bull;&bull;&bull;</a></li>\n" +
					"</div>\n" +
					"<div class=\"eachBox links_simple\">\n";
		for(let j = 0; j < live.lives.length; j++){
			let live1 = live.lives[j];
			let h2 = "<li class=\"linkFlag colorFlag\">\n" + 
							"<a target=\"_blank\" title=\"" + live1.name + "\" href=\"" + live1.url + "\">" + live1.name + "</a>\n" + 
					 "</li>\n";
			h1 = h1 + h2;
		}
		h1 = h1 + "</div>\n";
		html = html + h1;
	}
	html = html + "</div>\n</div>\n";
	$("#mainBox").append(html);
}
loadData();