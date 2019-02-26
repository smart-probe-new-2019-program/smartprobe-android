function showToast(i){

           // app.makeToast(i);
           alert("hi");
            return false;
        }

  $(document).ready(function(){
            $('.clicked').click(function(){
                $(this).addClass("highlight")


                    $(this).addClass("highlight").delay(500).queue(function(next){
                        $(this).removeClass("highlight");
                        app.makeToast( $(this).attr('id'))
                        next();
                    });
            });



        });
