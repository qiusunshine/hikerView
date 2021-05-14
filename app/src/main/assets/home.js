var version="V1.9.9";
var localUrl;
var newPlanJsUrl;
var htmlUrl;
var newPlanUrls=getVar("newPlanUrls");
if(newPlanUrls != ""){
    newPlanUrls=JSON.parse(newPlanUrls);
    localUrl=newPlanUrls.localUrl;
    htmlUrl=newPlanUrls.htmlUrl;
    newPlanJsUrl=newPlanUrls.newPlanJsUrl;
}else{
    throw "没有传入newPlanUrls";
}
var updateLog=[
    {
        "date": "2021/2/25",
        "records": [
            "优化：更改设置不在黑屏"
        ]
    },{
        "date": "2021/2/23",
        "records": [
            "‘‘新功能’’：输入框外部调用",
            "用法：预处理写入我的主页预处理代码并添加putVar2('exteriorUrl',JSON.stringify({path:'储存地址',input:[{id:'储存的key'}],fill:true/*是否自动填充*/}))"
        ]
    },{
        "date": "2021/2/23",
        "records": [
            "‘‘新功能’’：更新日志",
            "‘‘新功能’’：云剪贴板分享配置",
            "修复：修复一些奇怪的bug",
            "修复：云剪贴板将规则编码后上传防止部分错误",
            "其他：常用首页加载数改为区间，请重新设置",
            "其他：删除更新规则功能，改为跟随海阔更新"
        ]
    },{
        "date": "2021/2/22",
        "records": [
            "‘‘新功能’’：配置管理",
            "修复：导入失败"
        ]
    },{
        "date": "2021/2/21",
        "records": [
            "修复：修复导入后输入框消失",
            "其他：首页规则配置文件地址修改",
            "其他：增加部分说明"
        ]
    },{
        "date": "2021/2/20",
        "records": [
            "‘‘新功能’’：列表折叠",
            "‘‘新功能’’：网络一言"
        ]
    }
]
/**
 * 本地url工具
 * @function init 初始化
 * @function getLocal 获取本地JSON
 * @function getLocalUrl 获取本地路径
 */
var LocalUrlTool=function(localUrl){
    this.localUrl=localUrl;
    this.init();
}
LocalUrlTool.prototype={
    init:function(){
        try{
            this.getLocal();
        }catch(e){
            writeFile(this.localUrl,'{"apply":"默认"}');
        }
    },
    getLocal:function(){
        return JSON.parse(fetch(localUrl,{}));
    },
    getLocalUrl:function(){
        return this.localUrl;
    },
    saveData:function(data){
        writeFile(this.localUrl,JSON.stringify(data, null, 4));
    }
}
var localUrlTool=new LocalUrlTool(localUrl);//实例化localUrlTool
/**
 * localStorage
 * @function setItem 保存数据
 * @function getItem 读取数据
 * @function removeItem 删除数据
 */
var LocalStorage=function(){
    this.localData=localUrlTool.getLocal();
    this.apply=this.localData.apply;
    if(this.localData[this.apply]==undefined){
        this.init();
    }
    this.localConfig=this.localData[this.apply];
}
LocalStorage.prototype={
    init:function(){
        this.localData[this.apply]={};
        localUrlTool.saveData(this.localData);
    },
    setItem:function(key,value){
        this.localConfig[key]=value;
        this.localData[this.apply]=this.localConfig
        localUrlTool.saveData(this.localData);
    },
    getItem:function(key){
        if(!this.localConfig.hasOwnProperty(key)){
            return undefined;
        }
        return this.localConfig[key];
    },
    removeItem:function(key){
        delete this.localConfig[key];
    }
}
var localStorages=new LocalStorage();
var dataStorage={
    set:function(key,val){
        if(!val){
            return;
        }
        try{
            var json=JSON.stringify(val);
            if(typeof JSON.parse(json) === "object") { // 验证一下是否为JSON字符串防止保存错误
                localStorages.setItem(key,json);
            }
        }catch(e){
            return false;
        }
    },
    get:function(key){
            if(this.has(key)) {
                return JSON.parse(localStorages.getItem(key));
            }else{
                return undefined;
            }
	   	},
    has:function(key){
            if(localStorages.getItem(key)){
                return true;
            }else{
                return false;
            }
    },
    del:function(key){
            localStorages.removeItem(key);
    }
}
var textFun={
    getBigText:function(text){
        return "<big>" + text + "</big>";
    },
    getStrongText:function(text){
        return '<strong>' + text + '</strong>';
    },
    getSmallText:function(text){
        return "<small>" + text + "</small>";
    },
    getColorText:function(text,color){
        return '<font color="' + color + '">' + text + '</font>';
    },
    setTitle:function(text){
        return this.getStrongText(this.getBigText(text));
    },
    getImg:function(imgurl){
        return '<img src="'+imgurl+'"/>';
    },
    getHText:function(text,h){
        return '<'+h+'>'+text+'</'+h+'>'
    },
    getPText:function(text){
        return "<p>"+text+"</p>"
    }
}
var configClass=function(){
    this.localData=localUrlTool.getLocal();
    this.apply=this.localData.apply;
}
configClass.prototype={
    set:function(key,val){
        var config=val;
        if(typeof config !== "object"){
            config=JSON.parse(config);
        }
        this.localData[key]={
            bookmarkList:JSON.stringify(config.bookmarkList),
            setData:JSON.stringify(config.setData)
        };
        localUrlTool.saveData(this.localData);
    },
    setApply:function(key){
        this.localData.apply=key;
        localUrlTool.saveData(this.localData);
    },
    setOldData:function(key,val){
        this.localData[key]=val;
        localUrlTool.saveData(this.localData);
    },
    del:function(key){
        delete this.localData[key];
        localUrlTool.saveData(this.localData);
    },
    rename:function(key,newKey){
        this.localData[newKey]=this.localData[key];
        this.del(key);
        localUrlTool.saveData(this.localData);
    }
}
//书签列表
var bookmarksClass=function(bookList){
    this.defaultList={"data":[{"name":"头像","url":"内置元素，勿删！","icon":"","desc":"","type":"@js:avatarLoad();","status":false,"accessRights":false},{"name":"logo","url":"内置元素，勿删！","icon":"","desc":"","type":"@js:logoLoad();","status":true,"accessRights":false},{"name":"搜索框","url":"内置元素，勿删！","icon":"","desc":"","type":"@js:searchLoad();","status":true,"accessRights":false},{"name":"豆瓣","url":"hiker://home@豆瓣新版R||https://m.douban.com/movie/","icon":"hiker://images/豆瓣","desc":"","type":"icon_2","notes":"","status":true},{"name":"海阔视界论坛","url":"https://haikuoshijie.cn","icon":"hiker://images/bbs","desc":"","type":"icon_2","notes":"","status":true},{"name":"My Settings","url":"http://","icon":"","desc":"","type":"text_1|折叠(4)","notes":"","status":true},{"name":"书签","url":"hiker://bookmark","icon":"hiker://images/书签","desc":"","type":"icon_small_4","notes":"","status":true},{"name":"历史","url":"hiker://history","icon":"hiker://images/历史","desc":"","type":"icon_small_4","notes":"","status":true},{"name":"云备份","url":"hiker://webdav","icon":"hiker://images/云备份","desc":"","type":"icon_small_4","notes":"","status":true},{"name":"设置","url":"hiker://empty#noHistory##noRecordHistory#@rule=js:eval(fetch('"+newPlanJsUrl+"',{}));settingLoad()","icon":"hiker://images/设置","desc":"","type":"icon_small_4","notes":"设置","status":true},{"name":"","url":"hiker://collection","icon":"@js:colPicUrl","desc":"@js:'我的收藏（'+collection.length+'）'","type":"movie_2|收藏","notes":"收藏预览","status":true},{"name":"我的频道@js:\"‘‘\"+input+\"（\"+home.length+\"）’’\"","url":"hiker://home","icon":"","desc":"","type":"text_2|规则","notes":"规则","status":true},{"name":"常用首页","url":"","icon":"","desc":"","type":"text_1|折叠(1)","notes":"","status":true},{"name":"常用首页列表","url":"内置元素，勿删！","icon":"","desc":"","type":"@js:homeList()","notes":"","status":true,"accessRights":false}]};
    this.bookList={data:bookList};
    this.init(bookList);
}
bookmarksClass.prototype={
    init:function(obj){
        if(obj==undefined||obj=={}){
            this.reset();
            this.bookList=this.defaultList;
            /*var settingTs={
                "name":"进入设置(该提示只弹出一次)",
                "url":this.getSettingsIcon().url,
                "icon":"",
                "desc":"进入->添加“设置图标”到首页",
                "type":"text_center_1",
                "status":true
            };
            this.bookList.data.unshift(settingTs);*/
            writeFile(htmlUrl,newPlanHtml);
        }
    },
    reset:function(){
        dataStorage.set("bookmarkList",this.defaultList.data);
    },
    getJson:function(){
        return this.bookList.data;
    },
    has:function(books){
        return JSON.stringify(this.getJson()).includes(JSON.stringify(books));
    },
    get:function(index){
        return this.getJson()[index];
    },
    add:function(books){
        var data = this.getJson();
        if(this.has(this.getAddIcon().url)){
            data.splice(data.length-1,0,books);
        }else{
            data.push(books);
        }
        dataStorage.set("bookmarkList",data);
    },
    del:function(index){
        var data = this.getJson();
        data.splice(index,1);
        dataStorage.set("bookmarkList",data);
    },
    exchange:function(firstIndex,secondIndex){
        var data = this.getJson();
        var tem = data[firstIndex];
        data[firstIndex] = data[secondIndex];
        data[secondIndex] = tem;
        dataStorage.set("bookmarkList",data);
    },
    move:function(firstIndex,secondIndex){
        var data = this.getJson();
        var tem=data[firstIndex];
        this.del(firstIndex);
        data.splice(secondIndex,0,tem);
        dataStorage.set("bookmarkList",data);
    },
    modify:function(index,books){
        var data = this.getJson();
        data.splice(index,1,books);
        dataStorage.set("bookmarkList",data);
    },
    getAddIcon:function(){
        return {
            "name":"",
            "url":setClickUrl(3,"addBookmark"),
            "icon":"hiker://images/添加",
            "desc":"",
            "type":"icon_small_4",
            "notes":"添加书签",
            "status":true
        };
    },
    getSettingsIcon:function(){
        return {
            "name":"设置",
            "url":"hiker://empty#noHistory##noRecordHistory#"+base64Decode("QHJ1bGU9")+"js:eval(fetch('"+newPlanJsUrl+"',{}));settingLoad()",
            "icon":"hiker://images/设置",
            "desc":"",
            "type":"icon_small_4",
            "notes":"设置",
            "status":true
        };
    }
}
//设置
var settingsClass=function(localConfig){
    this.initialConfig={/*logoOpen:[true,0],*/logoType:["pic_1_full",0],localYiYan:[true,0],internetYiYanClass:["",1],/*searchOpen:[true,0],avatarOpen:[true,0],*/myYiYan:["长风破浪会有时，直挂云帆济沧海。||人生得意须尽欢，莫使金樽空对月。||书山有路勤为径，学海无涯苦作舟。||唯有长江水，无语东流。||莫愁前路无知己，天下谁人不识君。",1],logoJumpLink:["@js:topPicUrl",1],homeListClass:["",1],topPic:["hiker://images/logo",1],homeListType:["text_3",-2],searchText:["",1],avatarPic:["",1],homeIntervalCount:["1-9",1],logoDescOpen:[true,-2],topPic_topMargin:["auto",-2],topPic_bottomMargin:["auto",-2]};
    this.localConfig=localConfig;
    this.init(localConfig);
}
settingsClass.prototype={
    init:function(obj){
        if(obj==undefined){
            this.reset();
            this.localConfig=this.initialConfig;
        }
        for(var key in this.initialConfig){
            if(this.localConfig[key]==undefined){
                this.localConfig[key]=this.initialConfig[key];
                dataStorage.set("setData",this.localConfig);
            }
        }
    },
    reset:function(){
        dataStorage.set("setData",this.initialConfig);
    },
    getJson:function(){
        return this.localConfig;
    },
    set:function(key,val){
        this.localConfig[key][0]=val;
        dataStorage.set("setData",this.localConfig);
    },
    get:function(key){
        var tem=this.localConfig[key];
        if(tem==undefined){
            return tem;
        }else{
            return tem[0];
        }
    },
    has:function(key){
        return this.localConfig.hasOwnProperty(key);
    },
    apply:function(bookArr){
        var data=[];
        var yiYan=this.get("myYiYan").split("||");
        yiYan=yiYan[Math.floor(Math.random() * yiYan.length)];
        var _this=this;
        var time_Second=new Date().getTime();
        var collection;
        var colPicUrl;
        var home;
        var topPicUrl=executeJs(_this.get("topPic"));
        function getCollection(){
            var tem=fetch("hiker://collection");
            if(tem == null || tem == ""){
                tem = "[]";
            }
            return JSON.parse(tem);
        }
        function getHome(){
            var tem =fetch("hiker://home");
            if(tem == null || tem == ""){
                tem = "[]";
            }
            return JSON.parse(tem);
        }
        function logoLoad(){
            //if(_this.get("logoOpen")){
                var topPicTopMargin=_this.get("topPic_topMargin");
                var topPicBottomMargin=_this.get("topPic_bottomMargin");
                if(_this.get("logoType")=="pic_1_full"&&topPicUrl=="hiker://images/logo"&&topPicTopMargin=="auto"&&topPicBottomMargin=="auto"){
                    topPicTopMargin=15;
                    topPicBottomMargin=4;
                }
                topPicTopMargin=topPicTopMargin=="auto"?0:parseInt(topPicTopMargin);
                topPicBottomMargin=topPicBottomMargin=="auto"?0:parseInt(topPicBottomMargin);
                for (let i = 0; i < topPicTopMargin; i++) {
                    data.push({
                        col_type: "big_blank_block"
                    });
                }
                if(_this.get("localYiYan")||_this.get("logoType")=="pic_1_full"){
                    data.push({
                        title:yiYan,
                        url:executeJs(_this.get("logoJumpLink")),
                        pic_url:topPicUrl,
                        col_type:_this.get("logoType")
                    });
                }else{
                    var yiyan=JSON.parse(fetch('https://v1.hitokoto.cn/?c='+_this.get("internetYiYanClass"),{}));
                    var temPic={
                        title:yiyan.hitokoto,
                        url:executeJs(_this.get("logoJumpLink")),
                        desc:'出处：\t《' + yiyan.from + '》\n作者：\t' + (yiyan.from_who==null?"未知":yiyan.from_who),
                        pic_url:topPicUrl,
                        col_type:_this.get("logoType")
                    };
                    if(!_this.get("logoDescOpen")) temPic["desc"]=undefined;
                    data.push(temPic)
                }
                for (let i = 0; i < topPicBottomMargin; i++) {
                    data.push({
                        col_type: "big_blank_block"
                    });
                }
            //}
        }
        function avatarLoad(){
            //if(_this.get("avatarOpen")){
                data.push({
                    title:yiYan,
                    url:"toast://"+yiYan,
                    pic_url:_this.get("avatarPic"),
                    col_type:"avatar"
                });
            //}
        }
        function searchLoad(){
            //if(_this.get("searchOpen")){
                data.push({
                    title: executeJs(_this.get("searchText")),
                    url: 'hiker://search',
                    col_type: "icon_1_search",
                    pic_url: "hiker://images/search"
                });
            //}
        }
        function homeList(){
            home=home||getHome();
            var temHomeList;
            if(_this.get("homeListClass")==""){
                temHomeList=home
            }else{
                temHomeList=home.filter((data)=>data.group==_this.get("homeListClass"));
            }
            var homeIntervalCount=_this.get("homeIntervalCount").split("-");
            var maxHomeCount=parseInt(homeIntervalCount[1]);
            var minHomeCount=parseInt(homeIntervalCount[0])-1;
            var type=_this.get("homeListType");
            for(let i=minHomeCount;i>-1&&i<temHomeList.length && i<maxHomeCount;i++){
                let k = temHomeList[i];
                data.push({
                    title:k.title ,
                    url: "hiker://home@"+k.title,
                    col_type: type
                });
            }
        }
        function specialfeatures(tem){
            if(tem.type.includes("收藏")){
                collection=collection||getCollection();
                colPicUrl="hiker://images/card_bg";
                for(let i=collection.length-1;i>=0;i--){
                    if(collection[i].picUrl!=null){
                        colPicUrl=collection[i].picUrl;
                        break;
                    }
                }
            }else{
                home=home||getHome();
            }
            var colType=tem.type.split("|")[0];
            data.push({
                title:executeJs(tem.name),
                desc:executeJs(tem.desc),
                url:tem.url,
                col_type:colType,
                pic_url:executeJs(tem.icon)
            });

        }
        function executeJs(text){
             if(text.includes("@js:")){
                 try{
                     var input = text.split("@js:")[0];
                     var code = text.split("@js:")[1];
                     return eval(code);
                 }catch(e){
                     data.unshift({
                         title: e,
                         col_type: "rich_text"
                     });
                 }
             }else{
                 return text;
             }
        }
        function collapseList(tem){
            var colType=tem.type.split("|")[0];
            var index=tem.type.match(/折叠\((.*?)\)/)[1];
            var booksIndex=bookArr.indexOf(tem);
            data.push({
                title:executeJs(tem.name),
                url:setClickUrl(0,index+"#"+booksIndex,function(text){
                    var bookMark = new bookmarksClass(dataStorage.get("bookmarkList"));
                    var index = parseInt(text.split("#")[0]);
                    var booksIndex = parseInt(text.split("#")[1]);
                    var json = bookMark.getJson();
                    if(index>=0){
                        booksIndex+=1
                        for(var i=booksIndex;i<json.length&&i<booksIndex+index;i++){
                            if(json[i].type.includes("折叠")) break;
                            json[i].status=!json[i].status;
                            bookMark.modify(i,json[i]);
                        }
                    }else{
                        var indexs=index+1
                        booksIndex-=1
                        for(var i=booksIndex;i>=0&&i>=booksIndex+indexs;i--){
                            if(json[i].type.includes("折叠")) break;
                            json[i].status=!json[i].status;
                            bookMark.modify(i,json[i]);
                        }
                    }
                    refreshPage(false);
                    return "toast://已切换";
                }),
                desc:executeJs(tem.desc),
                pic_url:executeJs(tem.icon),
                col_type:colType
                })
        }
        for(var item of bookArr){
            if(!item.status){continue};
            if(item.type.includes("@js:")){
                executeJs(item.type);
            }else if(item.type.includes("折叠")){
                collapseList(item);
            }else if(item.type.includes("收藏")||item.type.includes("规则")){
                specialfeatures(item);
            }else{
                data.push({
                    title:executeJs(item.name),
                    url:item.url,
                    desc:executeJs(item.desc),
                    pic_url:executeJs(item.icon),
                    col_type:item.type
                })
            }
        }
        return data;
    }
}
//加载首页
function homeLoad(){
    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
    var settings=new settingsClass(dataStorage.get("setData"));
    var data=settings.apply(bookMark.getJson());
    setResult(data);
}
//点击事件
function setClickUrl(pattern,parameter,fun){
    if(pattern==0){
        return "hiker://empty#noLoading#@lazyRule=.js:eval(fetch('"+newPlanJsUrl+"',{}));var settings=new settingsClass(dataStorage.get('setData'));("+fun+")('"+parameter+"')";
    }else if(pattern==1){
        return "hiker://empty#noHistory##noRecordHistory#@rule=js:eval(fetch('"+newPlanJsUrl+"',{}));var settings=new settingsClass(dataStorage.get('setData'));("+fun+")('"+parameter+"')";
    }else if(pattern==2){
        return "hiker://empty#noHistory##noRecordHistory#@lazyRule=.js:putVar('cKey','"+parameter+"');'"+htmlUrl+"'";
    }else if(pattern==3){
        return "hiker://empty#noHistory##noRecordHistory#@lazyRule=.js:putVar('cKey','"+parameter+"');'x5://"+htmlUrl+"'";
    }

}
//字典
var dictionary = {
    "logoOpen":"logo开关",
    "searchOpen":"搜索开关",
    "avatarOpen":"头像开关",
    "logoType":"logo样式$TS：pic_1_full会闪屏慎用",
    "myYiYan":"我的一言$TS：可以添加多个，用\"||\"隔开",
    "topPic":"logo链接",
    "avatarPic":"头像链接",
    "aboutAuthor":"关于作者",
    "localConfigTiaocheng":"本地配置调试",
    "_import":"导入配置",
    "_export":"导出配置",
    "listManagement":"书签管理",
    "addSettingsIcon":"添加“设置图标”到首页",
    "addNewbooksIcon":"添加“快捷添加书签图标”到首页",
    "importHtml":"导入HTML",
    "recoverBookmark":"恢复默认书签",
    "recoverConfig":"恢复默认设置",
    "searchText":"搜索框文本",
    "updateLog":"更新日志",
    "localYiYan":"logo使用本地一言$TS：logo样式为pic_1_card时生效",
    "homeIntervalCount":"常用首页加载区间$TS：闭区间",
    "homeListClass":"常用列表所属分类",
    "true":"开启",
    "false":"关闭",
    "internetYiYanClass":"网络一言类型$TS：a动画,b漫画,c游戏,d文学,e原创,f来自网络,g其他,h影视,i诗词,j网易云,k哲学,l抖机灵,其他作为动画类型",
    "configManagement":"配置管理",
    "oldTurnNew":"本地配置文件转移",
    "logoJumpLink":"logo跳转链接"
};
function translate(text){
    /*if(dictionary.has(text)){
        return dictionary.get(text);
    }*/
    return dictionary[text]||"";
}
//设置界面
function settingLoad(){
    function click(key){
        if(/*key=="logoOpen"||key=="searchOpen"||key=="avatarOpen"||*/key=="localYiYan"){
            return setClickUrl(0,key,function(key){
                settings.set(key,!settings.get(key));
                refreshPage(false);
                return "toast://修改成功";
            });
        }else if(key=="logoType"){
            return setClickUrl(0,key,function(key){
                if(settings.get(key)=="pic_1_full"){
                    settings.set(key,"pic_1_card");
                }else{
                    settings.set(key,"pic_1_full");
                }
                refreshPage(false);
                return "toast://修改成功";
            });
        }else if(key=="myYiYan"||key=="topPic"||key=="avatarPic"||key=="localConfigTiaocheng"||key=="searchText"||key=="homeIntervalCount"||key=="homeListClass"||key=="internetYiYanClass"||key=="logoJumpLink"){
            return setClickUrl(3,key);
        }else if(key=="addSettingsIcon"){
            return setClickUrl(0,key,function(key){
                var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
                var addL=bookMark.getSettingsIcon();
                if(!bookMark.has(addL.url)){
                    bookMark.add(addL);
                    return "toast://添加成功";
                }else{
                    return "toast://已经添加过了";
                }
            });
        }else if(key=="listManagement"){
            return setClickUrl(1,key,function(key){
                listManagement();
            });
        }else if(key=="_import"){
            return setClickUrl(1,key,function(key){
                putVar2("cKey",key)
                importLoad();
            });
        }else if(key=="_export"){
            return setClickUrl(1,key,function(key){
                exportLoad();
            });
        }else if(key=="addNewbooksIcon"){
            return setClickUrl(0,key,function(key){
                var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
                var addL=bookMark.getAddIcon();
                if(!bookMark.has(addL.url)){
                    bookMark.add(addL);
                    return "toast://添加成功";
                }else{
                    return "toast://已经添加过了";
                }
            });
        }else if(key=="aboutAuthor"){
            return "http://haikuoshijie.cn/user/84";
        }else if(key=="importHtml"){
            //return "rule://"+base64Encode("海阔视界，本地文件￥file_url￥hiker://files/newPlan.html@https://gitee.com/LoyDgIk/LoyDgIk_Rule/raw/master/newPlan/newPlan.html");
            return setClickUrl(0,key,function(key){
                writeFile(htmlUrl,newPlanHtml);
                if(fetch(htmlUrl,{})!=newPlanHtml){
                    return "toast://导入失败";
                }else{
                    return "toast://导入成功";
                }
            });
        }else if(key=="recoverConfig"){
            return setClickUrl(0,key,function(key){
                settings.reset();
                return "toast://重置成功";
            });
        }else if(key=="recoverBookmark"){
            return setClickUrl(0,key,function(key){
                var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
                bookMark.reset();
                return "toast://重置成功";
            });
        }else if(key=="updateLog"){
            /*
            return setClickUrl(0,key,function(key){
                var cloudJs = fetch("https://gitee.com/LoyDgIk/LoyDgIk_Rule/raw/master/newPlan/NewPlanJS.js",{});
                var cloudHtml = fetch("https://gitee.com/LoyDgIk/LoyDgIk_Rule/raw/master/newPlan/newPlan.html",{});
                var localJs = fetch(newPlanJsUrl,{});
                var localHtml = fetch(htmlUrl,{});
                if(cloudJs==""||cloudHtml==""){
                    return "toast://作者可能已删库跑了,云端代码为空";
                }
                if(cloudJs!=localJs||cloudHtml!=localHtml){
                    if(cloudJs!=localJs){
                        writeFile(newPlanJsUrl,cloudJs);
                    }
                    if(cloudHtml!=localHtml){
                        writeFile(htmlUrl,cloudHtml);
                    }
                    refreshPage(false);
                    return "toast://更新成功";
                }else{
                    return "toast://已是最新版本";
                }
            });*/
            return setClickUrl(1,key,function(key){
                updateLogLoad();
            });
        }else if(key=="configManagement"){
            return setClickUrl(1,key,function(key){
                configManagement();
            });
        }else if(key=="oldTurnNew"){
            return setClickUrl(0,key,function(key){
                var configList= new configClass;
                var oldConfig=JSON.parse(fetch("hiker://files/NewPlanFile.json",{}));
                configList.setOldData(configList.apply,oldConfig);
                return "toast://转移成功";

            });
        }
    }
    var nonConfig={
        listManagement:["列表管理",0],
        configManagement:["配置管理",0],
        _import:["",-1],
        _export:["",-1],
        importHtml:["如果修改配置，添加书签出现网页加载失败，请尝试导入该配置",-1],
        recoverConfig:["",-1],
        recoverBookmark:["",-1],
        localConfigTiaocheng:["",-1],
        addSettingsIcon:["",-1],
        addNewbooksIcon:["",-1],
        oldTurnNew:["BetaV1.9.5之前的配置文件转为新版本的,注意会覆盖当前配置",-1],
        updateLog:["",-1],
        aboutAuthor:["作者：@LoyDgIk\n版本："+version,-1],
    };
    var data=[];
    var settings=new settingsClass(dataStorage.get("setData"));
    var settingList=Object.assign(JSON.parse(JSON.stringify(nonConfig)),settings.getJson());
    var settingKeyList=Object.assign(nonConfig,settings.initialConfig);
    function setSettingList(mode, title, type){
        if(title !=""){
             data.push({
                 title: textFun.setTitle(title),
                 col_type: "rich_text"
             });
        }
        for(var key in settingKeyList) {
            if(settingList[key][1]==mode){
                if(settings.has(key)){
                    var textAndDesc= translate(key).split("$");
                    var desc="";
                    if(textAndDesc[1]!=undefined){
                        desc="\n"+textAndDesc[1];
                    }
                    data.push({
                        title: textAndDesc[0],
                        desc: "当前数据："+settings.get(key)+desc,
                        url: click(key),
                        col_type: type
                    });
                }else{
                    data.push({
                        title: translate(key),
                        desc: settingList[key][0],
                        url: click(key),
                        col_type: type
                    });
                }
            }
        }
    }
    setSettingList(0, '常用设置', 'text_1');
    setSettingList(1, '接口设置', 'text_1');
    setSettingList(-1, '其他项目', 'text_1');
    setResult(data);
    putVar2("listPattern","modify");
}
//书签管理界面
function listManagement(){
    var data=[];
    var listPattern=getVar("listPattern","modify");
    var listDisplay=getVar("listDisplay","longitudinal");
    data.push({
        title: "修改",
        url: setClickUrl(0,null,function(){
            putVar2("listPattern","modify");
            refreshPage(false);
            return "toast://  修改";
        }),
        img: "hiker://images/修改",
        col_type: "icon_small_4"
    });
    data.push({
        title: "删除",
        url: setClickUrl(0,null,function(){
            putVar2("listPattern","delete");
            putVar2("aFlag","");
            refreshPage(false);
            return "toast://  删除";
        }),
        img: "hiker://images/删除",
        col_type: "icon_small_4"
    });
    data.push({
        title: "移动",
        url: setClickUrl(0,null,function(){
            putVar2("listPattern","transposition");
            putVar2("aFlag","");
            refreshPage(false);
            return "toast://  移动";
        }),
        img: "hiker://images/移动",
        col_type: "icon_small_4"
    });
    data.push({
        title: "开关",
        url: setClickUrl(0,null,function(){
            putVar2("listPattern","switch");
            refreshPage(false);
            return "toast://  开关";
        }),
        img: "hiker://images/开关",
        col_type: "icon_small_4"
    });
    data.push({
        title: listDisplay=="longitudinal"?"列表显示 ☑":"列表显示 ☐",
        url: setClickUrl(0,null,function(){
            putVar2("listDisplay","longitudinal");
            refreshPage(false);
            return "toast://列表显示";
        }),
        col_type: "text_2"
    });
    data.push({
        title: listDisplay=="preview"?"首页预览 ☑":"首页预览 ☐",
        url: setClickUrl(0,null,function(){
            putVar2("listDisplay","preview");
            refreshPage(false);
            return "toast://首页预览";
        }),
        col_type: "text_2"
    });
    data.push({
        title: textFun.setTitle("当前模式："+textFun.getColorText(listPattern,"#ff7800")),
        col_type: "rich_text"
    });
    data.push({
        col_type: "line_blank"
    });
    function setChooseText(text,index){
        if(text.length>40){
            text=text.slice(0,37)+"...";
        }
        /*
        if(text.includes("@js")){
            text=bookMarksList[key].notes||"";
        }*/
        if(getVar("aFlag")==index&&(listPattern=="delete"||listPattern=="transposition")){
            return "‘‘\""+text+"\"√’’";
        }else{
            return text;
        }
    }
    function click(key){
        if(listPattern=="modify"&&bookMarksList[key].accessRights==undefined){
            return setClickUrl(3,"modify#"+key);
        }else if(listPattern=="delete"&&bookMarksList[key].accessRights==undefined){
            return setClickUrl(0,key,function(index){
                var flag = getVar("aFlag");
                if(flag!=""&&flag==index){
                    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
                    bookMark.del(index);
                    putVar2("aFlag","");
                    refreshPage(false);
                    return "toast://删除成功";
                }else{
                    putVar2("aFlag",index);
                    refreshPage(false);
                    return "toast://请确认删除";
                }
            })
        }else if(listPattern=="transposition"){
            return setClickUrl(0,key,function(index){
                var flag = getVar("aFlag");
                if(flag!=""){
                    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
                    bookMark.move(flag,index);
                    putVar2("aFlag","");
                    refreshPage(false);
                    return "toast://已移动";
                }else{
                    putVar2("aFlag",index);
                    refreshPage(false);
                    return "toast://请确认移动位置";
                }
            })
        }else if(listPattern=="switch"/*&&bookMarksList[key].accessRights==undefined*/){
            return setClickUrl(0,key,function(index){
                var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
                var tem=bookMark.get(index);
                tem.status=!tem.status;
                bookMark.modify(index,tem);
                refreshPage(false);
                return "toast://修改成功";
            })
        }else{
            return "toast://操作失败或没有权限";
        }
    }
    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
    if(getVar("listDisplay","longitudinal")=="longitudinal"){
        data.push({
            title: "添加",
            url: setClickUrl(3,"addBookmark"),
            col_type: 'text_center_1'
        });
        data.push({
            col_type: "line"
        });
        data.push({
            col_type: "line"
        });
        var bookMarksList=bookMark.getJson();
        for(var key in bookMarksList){
            data.push({
                title: setChooseText(bookMarksList[key].notes||bookMarksList[key].name||"",key),
                url: click(key),
                desc: "状态："+translate(bookMarksList[key].status.toString())+"\n链接："+bookMarksList[key].url,
                col_type: "text_1"
            })
        }
    }else{
        var settings=new settingsClass(dataStorage.get("setData"));
        var d=settings.apply(bookMark.getJson());
        d.forEach((item,key)=>{item.url="toast://当前处于预览模式";data.push(item)});
    }
    putVar2("listDisplay","longitudinal");
    setResult(data);
}
//配置管理
function configManagement(){
    var data=[];
    var configList=localUrlTool.getLocal();
    var apply=configList.apply;
    data.push({
        desc:'0',
        col_type: 'x5_webview_single'
    });
    data.push({
            title: "新建配置",
            url: "x5WebView://javascript:function writeFile(filePath,content){fy_bridge_app.writeFile(filePath,content)}function fetch(url,headers){return fy_bridge_app.fetch(url,headers)}function getVar(key){return fy_bridge_app.getVar(key)}eval(fy_bridge_app.fetch('"+newPlanJsUrl+"',{}));newConfig();",
            col_type: 'text_center_1'
    });
    function fun(key){
        var type="text_2";
        if(apply!=key){
            type="text_3"
            data.push({
                title: "删除",
                url:setClickUrl(0,key,function(key){
                    var configList= new configClass;
                    configList.del(key);
                    refreshPage(false);
                    return "toast://已删除";
                }),
                col_type: type
            });
        }
        data.push({
            title: "重命名",
            url:"x5WebView://javascript:function writeFile(filePath,content){fy_bridge_app.writeFile(filePath,content)}function fetch(url,headers){return fy_bridge_app.fetch(url,headers)}function getVar(key){return fy_bridge_app.getVar(key)}eval(fy_bridge_app.fetch('"+newPlanJsUrl+"',{}));configRename('"+key+"');",
            col_type: type
        });
        data.push({
            title: "应用",
            url:setClickUrl(0,key,function(key){
                var configList= new configClass;
                configList.setApply(key);
                refreshPage(false);
                return "toast://已应用";
            }),
            col_type: type
        });
        putVar2("key","");
    }
    for(key in configList){
        if(key=="apply") continue;
        data.push({
            title:key==apply?"☑\t\t"+key:"☐\t\t"+key,
            url:setClickUrl(0,key,function(key){
                putVar2("key",key);
                refreshPage(false);
                return "toast://选择你需要的操作"
            }),
            col_type: "text_1",
        });
        if(getVar("key")==key){
            fun(key)
        }
    }
    setResult(data);
}
//配置重命名
function configRename(keys){
    var configName=prompt("请为该配置命名");
    if(configName==null){
        return;
    }else if(configName==""){
        alert("请输入名称");
        return;
    }
    var configList= new configClass;
    for(key in configList.localData){
        if(key==configName){
            alert("该名称已存在请重新输入");
            return;
        }
    }
    configList.rename(keys,configName);
    if(configList.apply==keys){
        configList.setApply(configName);
    }
    fy_bridge_app.refreshPage(false);
    alert("重命名成功");
}
//新建配置
function newConfig(){
    //alert("测试")
    var configName=prompt("请为该配置命名");
    if(configName==null){
        return;
    }else if(configName==""){
        alert("请输入名称");
        return;
    }
    var configList= new configClass;
    for(key in configList.localData){
        if(key==configName){
            alert("该名称已存在请重新输入");
            return;
        }
    }
    configList.setApply(configName);
    configList.set(configName,{});
    fy_bridge_app.refreshPage(false);
    alert("新建成功");
}
//帮助文档
/*function helpLoad(){
}*/
//更新日志
function updateLogLoad(){
    var data =[];
    for(var i=0;i<updateLog.length;i++){
        var log=updateLog[i];
        data.push({
            title:textFun.getStrongText(log.date),
            col_type:"rich_text"
        });
        data.push({
            col_type:"line"
        });
        data.push({
            col_type:"line"
        });
        for(var text of log.records){
             data.push({
                title:text,
                url:"toast://"+text,
                col_type:"text_1"
             });
        }
    }
    setResult(data);
}
//导入界面
function importLoad(){
    var data=[];
    if(getVar("inputStatus","true")=="true"){
        data.push({
            url:htmlUrl,
            desc:'100%&&float',
            col_type: 'x5_webview_single'
        });
    }else{
        refreshX5WebView("");
        data.push({
           title:"导入预览",
           url:"toast://导入预览",
           col_type:"text_center_1"
        });
        data.push({
            title: "取消",
            url: setClickUrl(0,null,function(){
                refreshPage(false);
                return "toast://已取消";
            }),
            col_type: "text_3"
        });
        data.push({
            title: "保存配置",
            url: setClickUrl(0,null,function(){
                var configList= new configClass;
                configList.set(getVar("configName"),getVar("inputRule"));
                refreshPage(false);
                return "toast://已保存";
            }),
            col_type: "text_3"
        });
        data.push({
            title: "保存并应用",
            url: setClickUrl(0,null,function(){
                var configList= new configClass;
                configList.set(getVar("configName"),getVar("inputRule"));
                configList.setApply(getVar("configName"));
                refreshPage(false);
                return "toast://应用成功，主页刷新生效";
            }),
            col_type: "text_3"
       });
       var inputRule=JSON.parse(getVar("inputRule"));
       var bookMark=new bookmarksClass(inputRule.bookmarkList);
       var settings=new settingsClass(inputRule.setData);
       var d=settings.apply(bookMark.getJson());
       d.forEach((item,key)=>{item.url="toast://当前处于预览模式";data.push(item)});
       putVar2("inputStatus","true");
    }
    setResult(data);
}
//导出界面
function exportLoad(){
    var data=[];
    data.push({
        desc:'0',
        col_type: 'x5_webview_single'
    });
    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
    var settings=new settingsClass(dataStorage.get("setData"));
    var allData = JSON.stringify({
        bookmarkList:bookMark.getJson(),
        setData:settings.getJson()
    });
    data.push({
        title: textFun.setTitle("单项导出"),
        col_type: "rich_text"
    });
    data.push({
        title: "进入列表",
        url:setClickUrl(1,null,function(){
            single();
        }),
        col_type: "text_1"
    });
    data.push({
        title: textFun.setTitle("全部数据"),
        col_type: "rich_text"
    });
    data.push({
        title: "一键复制",
        url:"x5WebView://javascript:function writeFile(filePath,content){fy_bridge_app.writeFile(filePath,content)}function fetch(url,headers){return fy_bridge_app.fetch(url,headers)}function getVar(key){return fy_bridge_app.getVar(key)}eval(fy_bridge_app.fetch('"+newPlanJsUrl+"',{}));copy();",
        col_type: "text_2"
    });
    data.push({
        title: "云剪贴板",
        url:setClickUrl(0,null,function(){
            var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
            //var settings=new settingsClass(dataStorage.get("setData"));
            var json={
                bookmarkList:bookMark.getJson(),
                setData:settings.getJson()
            }
            var jsonBody=JSON.stringify(json);
            var responseText=fetch("http://netcut.cn/api/note/create/",{
                method:"POST",
                body:"note_name="+"hkzc"+(new Date().getTime())
                    +"&note_content="+base64Encode(jsonBody)
            });
            //setError(responseText)
            try{
                responseText=JSON.parse(responseText);
                if(responseText.error!=""){
                    setError(responseText.error);
                    return "toast://提交出错";
                }
            }catch(e){
                setError(e);
                return "toast://提交出错";
            }
            refreshX5WebView("javascript:function writeFile(filePath,content){fy_bridge_app.writeFile(filePath,content)}function fetch(url,headers){return fy_bridge_app.fetch(url,headers)}function getVar(key){return fy_bridge_app.getVar(key)}eval(fy_bridge_app.fetch('"+newPlanJsUrl+"',{}));copy(-1,'"+responseText.data.note_id+"');");
            return "toast://提交成功";
        }),
        col_type: "text_2"
    });
    data.push({
        title: allData,
        col_type: "rich_text"
    });
    setResult(data);
}
//单项导出
function single(){
    var data=[];
    data.push({
        desc:'0',
	      	 col_type: 'x5_webview_single'
    });
    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
    var bookMarksList=bookMark.getJson();
    for(var i in bookMarksList){
        var item=bookMarksList[i]
        if(item.accessRights!=undefined){
            continue;
        }
        data.push({
            title:item.notes||item.name||"",
            url:"x5WebView://javascript:function writeFile(filePath,content){fy_bridge_app.writeFile(filePath,content)}function fetch(url,headers){return fy_bridge_app.fetch(url,headers)}function getVar(key){return fy_bridge_app.getVar(key)}eval(fy_bridge_app.fetch('"+newPlanJsUrl+"',{}));copy("+i+");",
            col_type: "text_1"
        });
    }
    setResult(data);
}
//一键复制导出配置
function copy(mg,v){
    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
    var settings=new settingsClass(dataStorage.get("setData"));
    document.write('<input type="text" id="wechat" value="">');
    var text = document.getElementById('wechat');
    if(mg==undefined){
        var data =JSON.stringify({
            bookmarkList:bookMark.getJson(),
            setData:settings.getJson()
        });
        text.value=data;
    }else if(mg==-1){
        text.value="http://netcut.cn/p/"+v+" 我的主页(newPlan)配置";
    }else{
        var item=bookMark.getJson()[mg]
        text.value="单项导入:$"+item.name+"$"+JSON.stringify(item);
    }
    text.select();
    document.execCommand('Copy');
    alert('已复制到剪贴板');
    document.body.innerHTML="";
}

var newPlanHtml='<html lang="zh">\
    <head>\
        <meta charset="UTF-8">\
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>\
        <meta name="viewport" content="width=device-width, initial-scale=1.0,user-scalable=no">\
        <title>\
            LoyDgIk<>\
        </title>\
        <style type="text/css">\
            *{\
                overflow:hidden;\
                margin:0;padding:0;outline:0;user-select:none;-webkit-tap-highlight-color:transparent;\
                font-family: "Noto Sans CJK SC", "Source Han Sans CN", sans-serif !important;\
            }\
            body {\
                font-size: 14px;\
            }\
                body,html {\
                height: 100%;\
            }\
            .addbook-sites{\
                overflow: auto;\
                height: 100%;\
                padding: 20px 20px 50px;\
            }\
            .addbook-input{\
                width: 100%;\
                border: 0;\
                color: var(--dark);\
                background: #f9f9f9;\
                border-radius: 10px;\
                padding: 16px 10px;\
                margin-bottom: 10px;\
            }\
            .addbook-ok{\
                text-align:center;\
                margin: 0 auto;\
                background-color: #193ee8;\
                color:#fff;\
                width: 130px;\
                height: 40px;\
                line-height: 40px;\
                border-radius:999px;\
                margin-top: 20px;\
                font-weight: bold;\
            }\
        </style>\
    </head>\
    <body>\
        <div id="container"></div>\
        <script type="text/javascript">\
            function writeFile(filePath,content){\
                fy_bridge_app.writeFile(filePath,content);\
            }\
            function fetch(url,headers){\
                return fy_bridge_app.fetch(url,headers);\
            }\
            function getVar(key){\
                return fy_bridge_app.getVar(key);\
            }\
            var newPlanJsUrlH=JSON.parse(getVar("newPlanUrls")).newPlanJsUrl;\
            eval(fetch(newPlanJsUrlH,{}));\
            htmlLoad();\
        </script>\
    </body>\
</html>'
//HTML加载
function htmlLoad(){
    function getIdText(id){
        return document.getElementById(id).value;
    }
    function getDE(id){
        return document.getElementById(id);
    }
    var bookMark=new bookmarksClass(dataStorage.get("bookmarkList"));
    var settings=new settingsClass(dataStorage.get("setData"));
    var cKey = getVar("cKey");
    var container=getDE("container");
    if(cKey=="addBookmark"){
        container.innerHTML = '<div class="addbook-sites">\
            <input type="text" class="addbook-input" id="addbook-name" placeholder="输入书签名(title)" />\
            <input type="text" class="addbook-input" id="addbook-url" placeholder="输入网址(url)" value="http://" />\
            <input type="text" class="addbook-input" id="addbook-picurl" placeholder="输入图片地址(pic_url)" />\
            <input type="text" class="addbook-input" id="addbook-desc" placeholder="输入详细(desc)" />\
            <input type="text" class="addbook-input" id="addbook-type" placeholder="输入类型(col_type)" value="icon_small_4"/>\
            <input type="text" class="addbook-input" id="addbook-notes" placeholder="备注名(书签管理 优先显示名称)" />\
            <div class="addbook-ok"id="addbook-ok">确认添加</div>\
            </div>';
        getDE("addbook-ok").onclick=function(){
            var name=getIdText("addbook-name");
            var notes=getIdText("addbook-notes");
            if(name==""&&notes==""){
                showToast("书签名和备注至少填一项");
                return;
            }
            bookMark.add({
                name:name,
                url:getIdText("addbook-url"),
                icon:getIdText("addbook-picurl"),
                desc:getIdText("addbook-desc"),
                type:getIdText("addbook-type"),
                notes:notes,
                status:true
            });
            showToast("添加成功",500);
            setTimeout(()=>{fy_bridge_app.back(true)},650);
        }
    }else if(cKey=="DZexteriorQuote"){
        try{
            container.innerHTML = '<div class="addbook-sites" id="addbook-sites"></div>';
            var custom=getVar("exteriorUrl");
            if(custom==""){
                throw "exteriorUrl未传入";
            }
            custom=JSON.parse(custom);
            var json=fetch(custom.path);
            var sites=getDE("addbook-sites");
            for(var i=0;i<custom.input.length;i++){
                var tem=document.createElement("input");
                tem.setAttribute("class","addbook-input");
                var attribute=custom.input[i];
                for(key in attribute){
                    tem.setAttribute(key,attribute[key]);
                }
                sites.appendChild(tem);
            }
            var ok=document.createElement("div");
            ok.setAttribute("class","addbook-ok");
            ok.setAttribute("id","addbook-ok");
            ok.innerHTML="确定";
            sites.appendChild(ok);
            if(json!=""&&custom.fill){
                json=JSON.parse(json);
                for(var i=0;i<custom.input.length;i++){
                    getDE(custom.input[i].id).value=json[custom.input[i].id]||"";
                }
            }
            getDE("addbook-ok").onclick=function(){
                var idData={};
                for(var i=0;i<custom.input.length;i++){
                    if(custom.input[i].force&&getIdText(custom.input[i].id)==""){
                        showToast(custom.input[i].id+"未输入");
                        return;
                    }
                    idData[custom.input[i].id]=getIdText(custom.input[i].id)||"";
                }
                fy_bridge_app.writeFile(custom.path,JSON.stringify(idData,null,4));
                showToast("操作成功");
            }
        }catch(e){
            alert(e);
        }
    }else if(cKey.includes("modify")){
        var index = cKey.split("#")[1];
        var qriginal = bookMark.get(index);
        container.innerHTML = '<div class="addbook-sites">\
            <input type="text" class="addbook-input" id="addbook-name" placeholder="输入书签名(title)" />\
            <input type="text" class="addbook-input" id="addbook-url" placeholder="输入网址(url)" />\
            <input type="text" class="addbook-input" id="addbook-picurl" placeholder="输入图片地址(pic_url)" />\
            <input type="text" class="addbook-input" id="addbook-desc" placeholder="输入详细(desc)" />\
            <input type="text" class="addbook-input" id="addbook-type" placeholder="输入类型(col_type)" />\
            <input type="text" class="addbook-input" id="addbook-notes" placeholder="备注名(书签管理 优先显示名称)" />\
            <div class="addbook-ok"id="addbook-ok">确认修改</div>\
            </div>';
        getDE("addbook-name").value=qriginal.name;
        getDE("addbook-url").value=qriginal.url;
        getDE("addbook-picurl").value=qriginal.icon;
        getDE("addbook-desc").value=qriginal.desc;
        getDE("addbook-type").value=qriginal.type;
        getDE("addbook-notes").value=qriginal.notes||"";
        getDE("addbook-ok").onclick=function(){
            var name=getIdText("addbook-name");
            var notes=getIdText("addbook-notes");
            if(name==""&&notes==""){
                showToast("书签名和备注至少填一项");
                return;
            }
            bookMark.modify(index,{
                name:name,
                url:getIdText("addbook-url"),
                icon:getIdText("addbook-picurl"),
                desc:getIdText("addbook-desc"),
                type:getIdText("addbook-type"),
                notes:notes,
                status:qriginal.status
            })
            showToast("修改成功");
        }
    }else if(cKey=="localConfigTiaocheng"){
        container.innerHTML = '<div class="addbook-sites">\
            <input type="text" class="addbook-input" id="addbook-key" placeholder="输入代码" />\
            <div class="addbook-ok"id="addbook-ok">执行</div>\
            </div>';
        getDE("addbook-ok").onclick=function(){
            try{
                eval(getIdText("addbook-key"));
                showToast("执行成功");
            }catch(e){
                showToast("错误代码："+e,3000);
            }
        }
    }else if(cKey=="_import"){
        container.innerHTML = '<div class="addbook-sites">\
            <input type="text" class="addbook-input" id="addbook-key" placeholder="输入Json。。。" />\
            <div class="addbook-ok"id="addbook-ok">导入配置</div>\
            </div>';
        getDE("addbook-ok").onclick=function(){
            try{
                var inputs=getIdText("addbook-key");
                if(inputs==""){
                    showToast("输入后再点吧");
                    return;
                }
                if(inputs.search(/单项导入:\$.*?\$/)==0){
                    var data=JSON.parse(inputs.replace(/单项导入:\$.*?\$/,""));
                    bookMark.add(data);
                    showToast("导入成功");
                    setTimeout(()=>{fy_bridge_app.refreshPage(false)},2300);
                }else if(inputs.search(/批量导入:\$.*?\$/)==0){
                    var temArr;
                    eval("temArr="+inputs.replace(/批量导入:\$.*?\$/,""));
                    var type=inputs.match(/批量导入:\$(.*?)\$/)[1];
                    temArr.forEach(item=>{
                        var ep=item.split("@@");
                        bookMark.add({
                            name:ep[0],
                            url:ep[1],
                            icon:ep[2]||"",
                            desc:"",
                            type:type,
                            notes:ep[0],
                            status:true
                        });
                    });
                    showToast("导入成功",800);
                    setTimeout(()=>{fy_bridge_app.refreshPage(false)},1000);
                }else{
                    /*
                    var data=JSON.parse(inputs);
                    dataStorage.set("bookmarkList",data.bookmarkList);
                    dataStorage.set("setData",data.setData);
                    */
                    if(inputs.includes("http://netcut.cn/p/")){
                        eval(request('hiker://files/aes.js'))
                        var id=(inputs.split(/\s/)[0]).replace("http://netcut.cn/p/","");
                        var data=fy_bridge_app.fetch("http://netcut.cn/api/note/data/?note_id="+id,{});
                        inputs=CryptoJS.enc.Base64.parse(JSON.parse(data).data.note_content).toString(CryptoJS.enc.Utf8);
                    }
                    //alert(inputs)
                    try{
                        JSON.parse(inputs);
                    }catch(e){
                        showToast("导入失败\t错误代码:"+e,3000);
                        return;
                    }
                    var configList=localUrlTool.getLocal();
                    var configName=prompt("请为该配置命名");
                    if(configName==null||configName==""){
                        showToast("请输入名称");
                        return;
                    }
                    for(key in configList){
                        if(key==configName){
                            showToast("该名称已存在请重新输入");
                            return;
                        }
                    }
                    fy_bridge_app.putVar("configName",configName);
                    fy_bridge_app.putVar("inputRule",inputs);
                    fy_bridge_app.putVar("inputStatus","false");
                    fy_bridge_app.refreshPage(false);
                }
            }catch(e){
                showToast("导入失败\t错误代码:"+e,3000);
            }
        }
    }else{
        container.innerHTML = '<div class="addbook-sites">\
            <input type="text" class="addbook-input" id="addbook-key" placeholder=""/>\
            <div class="addbook-ok"id="addbook-ok" >保存</div>\
            </div>';
        getDE("addbook-key").value=settings.get(cKey);
        getDE("addbook-ok").onclick=function(){
            settings.set(cKey,getIdText("addbook-key"));
            showToast("保存成功");
        }
    }
    function showToast(msg, duration) {
        duration = isNaN(duration) ? 2000 : duration;
        var m = document.createElement('div');
        m.innerHTML = msg;
        m.style.cssText = "width:60%; min-width:180px; background:#000; opacity:0.6; height:auto;min-height: 30px; color:#fff; line-height:30px; text-align:center; border-radius:4px; position:fixed; top:20%; left:20%; z-index:999999;";
        document.body.appendChild(m);
        setTimeout(function () {
            var d = 0.5;
            m.style.webkitTransition = '-webkit-transform ' + d + 's ease-in, opacity ' + d + 's ease-in';
            m.style.opacity = '0';
            setTimeout(function () { document.body.removeChild(m) }, d * 1000);
        }, duration);
    }
}