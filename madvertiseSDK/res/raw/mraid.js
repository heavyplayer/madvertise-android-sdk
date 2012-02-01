var expandProperties, listeners, mraid, placementType, state, states, viewable;
var __slice = Array.prototype.slice;
expandProperties = {
  width: 320,
  height: 480,
  useCustomClose: false,
  isModal: false
};
states = ["loading", "hidden", "default", "expanded"];
placementType = "inline";
state = "loading";
viewable = false;
listeners = {};
mraid = {
  getVersion: function() {
    return "1.0";
  },
  getState: function() {
    return state;
  },
  isViewable: function() {
    return viewable;
  },
  close: function() {
    return mraid_bridge.close();
  },
  open: function(url) {
    return mraid_bridge.open(url);
  },
  expand: function() {
    var url;
    url = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
    if (state === "default") {
      if ((url != null ? url.length : void 0) === 0) {
        return mraid_bridge.expand();
      } else {
        return mraid_bridge.expand(url[0]);
      }
    }
  },
  getPlacementType: function() {
    return placementType;
  },
  getExpandProperties: function() {
    return expandProperties;
  },
  setExpandProperties: function(properties) {
    if (properties.width) {
      expandProperties.width = properties.width;
    }
    if (properties.height) {
      expandProperties.height = properties.height;
    }
    if (properties.useCustomClose) {
      expandProperties.useCustomClose = properties.useCustomClose;
    }
    return mraid_bridge.setExpandProperties(JSON.stringify(expandProperties));
  },
  useCustomClose: function(useCustomClose) {
    expandProperties.useCustomClose = useCustomClose;
    return mraid_bridge.setExpandProperties(JSON.stringify(expandProperties));
  },
  addEventListener: function(event, listener) {
    if (event === "ready" || event === "stateChange" || event === "viewableChange" || event === "error") {
      return (listeners[event] || (listeners[event] = [])).push(listener);
    }
  },
  removeEventListener: function() {
    var event, l, listener;
    event = arguments[0], listener = 2 <= arguments.length ? __slice.call(arguments, 1) : [];
    if (listeners[event] && listener.length > 0) {
      return listeners[event] = (function() {
        var _i, _len, _ref, _results;
        _ref = listeners[event];
        _results = [];
        for (_i = 0, _len = _ref.length; _i < _len; _i++) {
          l = _ref[_i];
          if (l !== listener[0]) {
            _results.push(l);
          }
        }
        return _results;
      })();
    } else {
      return delete listeners[event];
    }
  },
  fireEvent: function(event) {
    var listener, _i, _j, _k, _len, _len2, _len3, _ref, _ref2, _ref3, _results, _results2, _results3;
    if (event === "ready") {
      _ref = listeners[event];
      _results = [];
      for (_i = 0, _len = _ref.length; _i < _len; _i++) {
        listener = _ref[_i];
        _results.push(listener());
      }
      return _results;
    } else if (event === "stateChange") {
      _ref2 = listeners[event];
      _results2 = [];
      for (_j = 0, _len2 = _ref2.length; _j < _len2; _j++) {
        listener = _ref2[_j];
        _results2.push(listener(state));
      }
      return _results2;
    } else if (event === "viewableChange") {
      _ref3 = listeners[event];
      _results3 = [];
      for (_k = 0, _len3 = _ref3.length; _k < _len3; _k++) {
        listener = _ref3[_k];
        _results3.push(listener(viewable));
      }
      return _results3;
    }
  },
  fireErrorEvent: function(message, action) {
    var listener, _i, _len, _ref, _results;
    _ref = listeners["error"];
    _results = [];
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      listener = _ref[_i];
      _results.push(listener(message, action));
    }
    return _results;
  },
  setState: function(state_id) {
    state = states[state_id];
    return fireEvent("stateChange");
  },
  setViewable: function(is_viewable) {
    viewable = is_viewable;
    return fireEvent("viewableChange");
  },
  setPlacementType: function(type) {
    if (type === 0) {
      return placementType = "inline";
    } else if (type === 1) {
      return placementType = "interstitial";
    }
  }
};