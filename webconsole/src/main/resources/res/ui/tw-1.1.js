// JavaScript Document
// Tab World From BenchSketch.com
// Benchsketch.com/bquery/tab.html for documentation
// You can use it FREE!!! Yay.
// Let me know how it works, or send suggestions, comments, requests to benchsketch@gmail.com
// Thanks
	
(function($){
  $.fn.extend({
			  
   // tabworld function
   tabworld: function(options){
	 
     // Get the user extensions and defaults
      var opts = $.extend({}, $.fn.tabworld.defaults, options);
	 
	 // Content location
	 	if(opts.contloc=="none"){
			contloc=$(this).parent();
		}else{
			contloc=opts.contloc;
		}
	 // Content location
	 	if(opts.tabloc=="none"){
			tabloc=$(this).parent();
		}else{
			tabloc=opts.tabloc;
		}
	 
	 // better define some stuff for safety
	  var newli="",newdiv="";
	 
	 // Start Building Tabs
	  return this.each(function(i){

		
		//start developing basis
		now=$(this);	
		nowid=now.attr("id");
		now.addClass(opts.color);
		
	// tab maker function	
	  // $("#"+nowid+" li").each(function(i){ // lets hide that for now
		$("#"+nowid+" > li").each(function(i){
		
		tabli = $(this);
		// taba = $('#'+nowid+" > li q");
		taba = tabli.children("q");
		$(this).addClass("removeme");
		tabcont = taba.html();
		$(".removeme q").remove();
		licont = tabli.html();
		$(this).remove();
		
		newli += "<li rel='"+nowid+"-"+i+"'><a>"+licont+"</a></li>";
		newdiv += "<div id='"+nowid+"-"+i+"'>"+tabcont+"</div>";

	  });

	// Something weird to gain the location
	 now.remove();
	 $(tabloc).append("<ul id='"+nowid+"-tabworld' class='"+opts.color+"'>"+newli+"</ul>");
	// Fix the ul
	// $("#"+nowid).append(newli);
	// Find the Parent then append the divs
	 // var parent = $("#"+nowid).parent();
	 newdiv = "<div id='"+nowid+"content' class='tabcont'><div class='"+opts.area+"'>"+newdiv+"</div></div>";
	 newdiv = newdiv.replace(/\/>/,">");
	 $(contloc).append(newdiv);	
	
	
	// Find the default
	 ndef = nowid+"-"+opts.dopen;
	 ncon = nowid+"content ."+opts.area+" > div";
	 $('#'+ncon).hide();
	 $('#'+ndef).show();
	 //$('#'+ndef+" > div").show();
	 
	 deftab = $('li[rel='+ndef+"]");
	 deftab.addClass(opts.tabactive);
	 deftab.children("a").addClass(opts.tabactive);
	// Seperate function to start the tabbing
	$("#"+nowid+"-tabworld >li").click(function(){
		here=$(this);
		nbound=here.attr("rel");
		
		// Make the active class / remove it from others
		$("#"+nowid+"-tabworld > li").removeClass(opts.tabactive);
		$("#"+nowid+"-tabworld > li a").removeClass(opts.tabactive);
		here.addClass(opts.tabactive);
		$("."+opts.tabactive+">a").addClass(opts.tabactive);
		
			// The real action! Also detirmine transition
			 if(!opts.speed){
                $('#'+ncon+':visible').hide();
                $('#'+nbound).find("div").show();
                $('#'+nbound).show();
			 }else if(opts.transition=="slide"){
				 $('#'+ncon+':visible').slideUp(opts.speed, function(){	
					$('#'+nbound).find("div").show();
					$('#'+nbound).slideDown(opts.speed);
				 });
			 }else if (opts.transition=="fade"){
				 $('#'+ncon+':visible').fadeOut(opts.speed, function(){	
					$('#'+nbound).find("div").show();
					$('#'+nbound).fadeIn(opts.speed);
				 });
			 }else{
				$('#'+ncon+':visible').hide(opts.speed, function(){	
					$('#'+nbound).find("div").show();
					$('#'+nbound).show(opts.speed);
				 }); 
			 }
	 
	});
	
	  });// end return each (i) 
   }// end tabworld		
  })// end $.fn.extend
  
// Defaults
$.fn.tabworld.defaults = {
 mislpace:'none',
 speed:'fast',
 color:'menu',
 area:'space',
 tabloc:'none',
 contloc:'none',
 dopen:0,
 transition:'fade',
 tabactive:'tabactive'
}; // end defaults
  
})(jQuery);// end function($)