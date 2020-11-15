(function(){
    function adBlock(rule){
//        console.log('adBlock start');
        if(rule.length<=0){
            return;
        }
        var rules = rule.split('@@');
        for(var i = 0; i<rules.length; i++){
            adBlockPer(rules[i]);
        }
//        console.log('adBlock end');
    }

    function adBlockPer(rule){
//        console.log('adBlockPer, ', rule);
        if(rule.length<=0){
            return;
        }
        var rules = rule.split('&&');
        var ele = getElement(document, rules[0]);
        if(rules.length<2){
            if(ele!=null){
                ele.style.display="none";
            }
        }else{
            if(ele!=null){
                for(var i = 1; i<rules.length; i++){
                    ele = getElement(ele, rules[i]);
                }
                ele.style.display="none";
            }
        }
    }

    function getElement(ele, rule){
//        console.log('getElement, rule=', rule, 'ele, ', ele);
        if(rule.length<=0 || ele == null){
            return;
        }
        var rules = rule.split(',');
        var count = 0;
        if(rules.length>1){
            count = parseInt(rules[1]);
        }
        if(rules[0].indexOf(".")===0){
            return ele.getElementsByClassName(rules[0].replace('.',""))[count];
        }else if(rules[0].indexOf("#")===0){
            return document.getElementById(rules[0].replace('#',""));
        }else{
            return ele.getElementsByTagName(rules[0])[count];
        }
    }
    var rule = "body&&#content_left&&.result,1@@#form&&.s_btn_wr";
    adBlock(rule);
})();
