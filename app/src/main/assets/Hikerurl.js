/**
 * @name 海阔url点击事件生成工具
 * @Author LoyDgIk
 * @version 2
 */
;(function (window) {
    "use strict";
    var [emptyUrl,lazyRule,rule,x5] = ["hiker://empty","@lazyRule=","@rule=","x5WebView://"];
    function Hikerurl(url,rule) {
        this.url = url==""?"":(url||emptyUrl);
        this.ruleOrdinary = rule||"";
    }
    function HikerurlC(url,rule) {
        return new Hikerurl(url,rule);
    }
    function toStringFun(fun,arg){
        arg = Array.isArray(arg)?arg:Array.prototype.slice.call(arg,1);
        var args = arg.map(item=>{
            if(typeof (item)=="function"||item===undefined||item===null){
                return String(item);
            }else{
                return JSON.stringify(item);
            }
        });
        return "("+fun+")(" + args.join(",") + ")";
    };
    var $ = HikerurlC;
    //静态方法
    $.toString = function(fun){
        return toStringFun(fun,arguments);
    }
    //方法
    $.fn = {
        constructor: HikerurlC,
        __proto__:{
            toStringFun: toStringFun
        },
        rule: function(fun) {
            return this.url + rule + (this.ruleOrdinary||"js:"+toStringFun(fun,this.parameter||arguments));
        },
        lazyRule: function(fun){
            return this.url + lazyRule + this.ruleOrdinary + ".js:"+toStringFun(fun,this.parameter||arguments);
        },
        x5Rule: function(fun){
            return (this.url==""?"":x5)+"javascript:var input="+JSON.stringify(this.url)+";" + toStringFun(fun,this.parameter||arguments);
        },
        param: function(){
            this.parameter = Array.prototype.slice.call(arguments);
            return this;
        }
    }
    Hikerurl.prototype = HikerurlC.prototype = $.fn;
    if(typeof (window.$) === 'undefined') {
        //对外接口
        window.$ = $;
    }
})(this);