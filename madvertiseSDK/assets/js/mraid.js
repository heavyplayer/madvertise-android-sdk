/*
 * Copyright 2011 madvertise Mobile Advertising GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//TODO:Tons of stuff!

mraid.getVersion = function() { 
	return '1.0';
}

var mExpandProperties;
mraid.getExpandProperties = function() {
	return mExpandProperties;
}

function setExpandProperties(properties) {
	mExpandProperties = properties;
}

var mPlacementType;
mraid.getPlacementType = function() {
	return mPlacementType;
}

function setPlacementType(type) {
	mPlacementType = type;
}

var mState;
mraid.getState = function() {
	return mState;
}

function setState(state) {
	mState = state;
}

var mIsViewable;
mraid.isViewable = function() {
	return mIsViewable;
}

function setViewable(isViewable) {
	mIsViewable = isViewable;
}