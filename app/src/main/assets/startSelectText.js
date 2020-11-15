(function(){
    function startSelectText(){
        var arr = document.getElementsByTagName('a');
        if(arr != null && arr.length > 1){
             /**安全检查判断是否移除了href属性**/
            if(arr[0].attributes['copyhref']){
                clearSelectText();
                return;
            }
        }

        for(var i = 0; i < arr.length; i++){
            var tag = arr[i];
            if(tag != null && tag.tagName != null){
                tag = tag.tagName.toLocaleLowerCase();
                 if(tag != null &&  tag == 'a'){

                     var ele = arr[i];
                     var hed = ele.href;
                     if(hed){
                         var aHref = hed.value;
                         ele.removeAttribute('href');
                         ele.setAttribute('copyhref',aHref);
                     }
                 }
             }
        }
    }
    startSelectText();
})();