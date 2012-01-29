
expandProperties = width: 320, height: 480, useCustomClose: false, isModal: false
states = ["loading", "hidden", "default", "expanded"]
state = "loading"
listeners = {}

mraid =

  # public mraid API

  getVersion: -> "1.0"

  getState: -> state

  getExpandProperties: -> expandProperties

  setExpandProperties: (properties) ->
    expandProperties.width = properties.width if properties.width
    expandProperties.height = properties.height if properties.height
    expandProperties.useCustomClose = properties.useCustomClose if properties.useCustomClose
    mraid_bridge.setExpandProperties(JSON.stringify(expandProperties))

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

  setState: (state_id) ->
    state = states[state_id]
    fireEvent("stateChange")


