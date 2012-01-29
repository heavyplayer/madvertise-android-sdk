
state = "loading"
listeners = {}

mraid =

  # public mraid API

  getVersion: -> "1.0"

  getState: -> state

  addEventListener: (event, listener) ->
    (listeners[event] ||= []).push listener

  removeEventListener: (event, listener...) ->
    if listeners[event] && listener.length > 0 # remove one listener[0]
      listeners[event] = (l for l in listeners[event] when l != listener[0])
    else # remove all listeners for this event
      delete listeners[event]

  # internal functions

  fireEvent: (event) ->
    listener(event) for listener in listeners[event]
