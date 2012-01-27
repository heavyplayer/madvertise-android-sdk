
state = "loading"
listeners = {}

mraid =

  # public mraid API

  getVersion: -> "1.0"

  getState: -> state

  addEventListener: (event, listener) ->
    (listeners[event] ||= []).push listener


  # internal functions

  fireEvent: (event) ->
    listener(event) for listener in listeners[event]
