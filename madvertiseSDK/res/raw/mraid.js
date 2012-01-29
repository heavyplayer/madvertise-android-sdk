var listeners, mraid, state;
var __slice = Array.prototype.slice;
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
  setState: function(new_state) {
    state = new_state;
    return fireEvent("stateChange");
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