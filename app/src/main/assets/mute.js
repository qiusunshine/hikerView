(function(){
    function muteMe(elem) {
        try{
            elem.pause();
        }catch(e){
        }
        try{
            elem.Pause();
        }catch(e){
        }
    }

    // Try to mute all video and audio elements on the page
    function mutePage() {
        var videos = document.querySelectorAll("video"),
            audios = document.querySelectorAll("audio"),
            v2 = document.querySelectorAll("embed"),
            v3 = document.querySelectorAll("#player");
        try{
            [].forEach.call(videos, function(video) { muteMe(video); });
        }catch(e){
        }
        try{
            [].forEach.call(audios, function(audio) { muteMe(audio); });
        }catch(e){
        }
        try{
            [].forEach.call(v2, function(v) { muteMe(v); });
        }catch(e){
        }
        try{
            [].forEach.call(v3, function(v) { muteMe(v); });
        }catch(e){
        }
        try{
            var pauseIcon = document.querySelector("#app .play-board .cover .icon");
            if(pauseIcon != null){
                pauseIcon.click();
            }
        }catch(e){}
    }
    mutePage();
    console.log("mutePage");
})();
