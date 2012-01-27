var listeners, mraid, state;
state = "loading";
listeners = {};
mraid = {
  getVersion: function() {
    return "1.0";
  },
  getState: function() {
    return state;
  },
  addEventListener: function(event, listener) {
    return (listeners[event] || (listeners[event] = [])).push(listener);
  },
  fireEvent: function(event) {
    var listener, _i, _len, _ref, _results;
    _ref = listeners[event];
    _results = [];
    for (_i = 0, _len = _ref.length; _i < _len; _i++) {
      listener = _ref[_i];
      _results.push(listener(event));
    }
    return _results;
  }
};