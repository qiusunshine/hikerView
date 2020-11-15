(function(){
    function clearSelectText(){
        var arr = document.getElementsByTagName('a');
        for(var i = 0; i < arr.length; i++){
            var tag = arr[i];
            if(tag != null && tag.tagName != null){
                tag = tag.tagName.toLocaleLowerCase();
                 if(tag != null &&  tag == 'a'){

                     var ele = arr[i];
                     var hed = ele.attributes['copyhref'];
                     if(hed){
                          var aHref = hed.value;
                         ele.removeAttribute('copyhref');
                         ele.setAttribute('href',aHref);
                     }
                 }
             }
        }
    }
    clearSelectText();
})();