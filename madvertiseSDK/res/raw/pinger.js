// this does not work!!
mraid_bridge.ping("blubbi");

// but from here, it's fine
function pingCall() {
	mraid_bridge.ping("blubbi");
};

function pingCallOutside() {
	mraid_bridge.ping("blubbi outside");
};