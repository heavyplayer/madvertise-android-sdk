/*
 * Description: The loader for a richmedia (video + expandable) ad using the MRAID api. This ad has just a base image (w:316px h:728px) and a video (w:316px h:728px)
 * clicking on which causes the ad to expand (w:949px h:728px). The expanded layer has a close button (w:24px h:24px) at the top right corner that is used with customClose
 * Author: Aditya Kalro
 * Company: Yahoo!
 */

document.write("<script src=\"mraid.js\"></script>");

/*
 * Checking for the state of the mraid client library and subscribing to the ready event if necessary 
 * When the client library is ready call the showAd method to render the ad
 */
if (mraid.getState() != 'ready') {
	console.log("MRAID Ad: adding event listener for ready");
	mraid.addEventListener('ready', showAd);
} else {
	showAd();
}

/*
 * The showAd method registers event listeners for the mraid events and renders
 * the base ad (simple image)
 */
function showAd() {
	basePath = "file:///android_asset/MRAID_expandable/src/";
	registerMraidHandlers(mraid, basePath);
	renderBaseAd(mraid, basePath);
	/*
	 * set the expand properties to use the custom close method since the ad
	 * renders it's own close button in the expanded layer
	 */
	mraid.setExpandProperties({
		useCustomClose : true
	});
};

/*
 * Add a listener to the stateChange event to figure out what state the client
 * listener is in and whether to render the rich functionality or not
 */
function registerMraidHandlers(mraid, basePath) {
	mraid.addEventListener("stateChange", function(state) {
		switch (state) {
		// Event trigger when the ad-container goes offscreen
		case "hidden":
			removeOverlayLayer();
			break;
		// Event trigger when the ad-container is onscreen
		case "default":
			renderOverlayLayer(mraid, basePath);
			break;
		}
	});
}

/*
 * Render the base image of the ad (this is what is rendered in the hidden
 * state).
 */
function renderBaseAd(mraid, basePath) {
	var imageURL = basePath + "assets/mraid_column_static.jpg";
	console.log("rendering base ad");
	var baseImage = document.createElement("img");
	baseImage.setAttribute("id", "base_img");
	baseImage.src = imageURL;
	baseImage.setAttribute("style", "border:0px; width:316px; height:728px;");
	document.body.appendChild(baseImage);

}

function resolveVideoPath(path, cb) {
	console.log("in resolveVideoPath");
	cb(path);
}

/*
 * Rendering the rich functionality of the ad (is called when the ad comes
 * onscreen) This renders the richmedia overlay div and adds a video into it.
 * Clicking on the div results in it's expansion (ad_expand method)
 */
function renderOverlayLayer(mraid, basePath) {
	console.log("Rendering overlay layer");
	var overlayContainer = document.createElement("div");
	overlayContainer.setAttribute("id", "overlayContainer_mraid");
	var bkgImagePath = basePath + "assets/mraid_main_bg_949x728.jpg";
	overlayContainer
			.setAttribute(
					"style",
					"width:949px;height:728px;position:absolute;right:-633px;top:0px;z-index:1000;display:none;background:url("
							+ bkgImagePath + ")");
	document.body.appendChild(overlayContainer);
	resolveVideoPath(
			basePath + "assets/mraid_column4.mp4",
			function(videoPath) {
				var closeButtonPath = basePath + "assets/close-button.png";
				overlayContainer.innerHTML = "<video width='316px' height='728px' loop=\"true\" id = \"video_elem\" src='"
						+ videoPath
						+ "'></video>"
						+ "<img style='position:absolute;right:10px;top:10px;' src='"
						+ closeButtonPath + "' width='24px' height='24px'/>";
				var videoPlayer = document.getElementById("video_elem");

				// Make sure video is playing to avoid blink as video
				// player is loading
				videoPlayer.addEventListener("playing", function() {
					console.log("Video has started playing");
					overlayContainer.style.display = "block";
					videoPlayer.style.displya
				}, false);

				videoPlayer.addEventListener("play", function() {
					console.log("Video has started playing");
					overlayContainer.style.display = "block";
				}, false);

				videoPlayer.addEventListener("click", function() {
					ad_expand(mraid, videoPlayer, overlayContainer);
				}, false);

				var closeButton = overlayContainer.getElementsByTagName("img")[0];
				closeButton.addEventListener("click", function() {
					ad_close(mraid, videoPlayer, overlayContainer);
				}, false);

				videoPlayer.play();
			});
}

/*
 * The handler for closing the expanded layer of the ad. The mraid.close method
 * is used here to make sure that the ad publishing app knows about the change
 * in the state of the ad.
 */
function ad_close(mraid, videoPlayer, overlayContainer) {
	overlayContainer.style.webkitTransition = "-webkit-transform 250ms ease-in-out";
	overlayContainer.style.webkitTransform = "translateX(0)";
	overlayContainer.addEventListener("webkitTransitionEnd", function() {
		videoPlayer.style.display = "block";
		overlayContainer.removeEventListener("webkitTransitionEnd",
				arguments.callee, false);
	}, false);
	// Notifies the content of the ad closing
	mraid.close();

}

/*
 * The handler for expanding the ad. The mraid.expand method is used here to
 * make sure that the ad publishing app knows about the change in the state of
 * the ad.
 */

function ad_expand(mraid, videoPlayer, overlayContainer) {
	// Notifies the content of the ad taking over the screen
	mraid.expand();
	videoPlayer.style.display = "none";
	overlayContainer.style.webkitTransition = "-webkit-transform 250ms ease-in-out";
	overlayContainer.style.webkitTransform = "translateX(-633px)";
}

/*
 * Removes the overlay layer when the ad is going offscreen
 */
function removeOverlayLayer() {
	var overlayContainer = document.getElementById("overlayContainer_mraid");
	mraid.close();
}
