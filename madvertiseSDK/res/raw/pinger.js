// this does not work!!
mraid_bridge.ping("blubbi");

// nope, doesn't work!
(function() {
  mraid_bridge.ping("blubbiiiiii");
}).call(this);


// but from here, it's fine
function pingCall() {
	mraid_bridge.ping("blubbi");
};

function pingCallOutside() {
	mraid_bridge.ping("blubbi outside");
};
