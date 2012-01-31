/*
 * Description: The loader for a static (basic clickable) ad using the MRAID api. This ad has just one image w:316px h:728px that clicks 
 * through to a landing page (www.livestand.com)
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
	basePath = "http://localhost:8666/yahoo.ads.mraid_static/";
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
			break;
		// Event trigger when the ad-container is onscreen
		case "default":
			// This is where the impression beacon (if any) should be fired
			break;
		}
	});
}

/*
 * Render the basic ad (an image with wrapped in an anchor element)
 */
function renderBaseAd(mraid, basePath) {
	var landingPage = "http://www.yahoo.com";
	var imageURL = "assets/mraid_column_example.jpg", adImage = "<img width='316px' height='728px' border=0 src='"
			+ imageURL + "'/>";
	var anchor = "<a id=\"base_image_example\" href=\"" + landingPage + "\"> "
			+ adImage + "</a>";
	document.write(anchor);
	var anchorElement = document.getElementById("base_image_example");
	anchorElement.onclick = function() {
		console.log("Clicking on anchorElement = " + anchorElement.href);
		mraid.open(anchorElement.href);
		return false;
	};
}
