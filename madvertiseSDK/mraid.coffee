# 'private' scope
expandProperties = width: 320, height: 480, useCustomClose: false, isModal: false
states = ["loading", "hidden", "default", "expanded"]
placementType = "inline"
state = "loading"
viewable = false
listeners = {}

this.mraid =      # export public API to global scope

  getVersion: -> "1.0"

  getState: -> state

  isViewable: -> viewable

  close: -> mraid_bridge.close()

  open: (url) -> mraid_bridge.open(url)

  expand: (url...) ->
    mraid_bridge.logMessage("in expand : " + state)
    # comment for testing
    # if state == "default"
    if url?.length == 0
      mraid_bridge.expand()
    else
      mraid_bridge.expand(url[0])

  getPlacementType: -> placementType

  getExpandProperties: -> expandProperties

  setExpandProperties: (properties) ->
    expandProperties.width = properties.width if properties.width
    expandProperties.height = properties.height if properties.height
    expandProperties.useCustomClose = properties.useCustomClose if properties.useCustomClose
    mraid_bridge.setExpandProperties(JSON.stringify(expandProperties))

  useCustomClose: (useCustomCloseParams) ->
    expandProperties.useCustomClose = useCustomCloseParams
    mraid_bridge.setExpandProperties(JSON.stringify(expandProperties))
    
  addEventListener: (event, listener) ->
    if event in ["ready", "stateChange", "viewableChange", "error"]
      mraid_bridge.logMessage("adding event listener for " + event)      	
      (listeners[event] ||= []).push listener

  removeEventListener: (event, listener...) ->
    if listeners[event] && listener.length > 0 # remove one listener[0]
      listeners[event] = (l for l in listeners[event] when l != listener[0])
    else # remove all listeners for this event
      delete listeners[event]


  # internal functions

  fireEvent: (event) ->
    mraid_bridge.logMessage("fireEvent : " + event)
    if listeners[event]
      for listener in listeners[event]
        if event == "ready"
          listener()
        if event == "stateChange"
          listener(state)
        if event == "viewableChange"
          listener(viewable)

  fireErrorEvent: (message, action) ->
    listener(message, action) for listener in listeners["error"]

  setState: (state_id) ->
    # iterating through the array of states is too slow, this switch is way faster.
  	switch state_id
      when 0 then state = "loading"
      when 1 then state = "hidden"
      when 2 then state = "default"
      when 3 then state = "expanded"

    mraid_bridge.logMessage("in setState : " + state)
    mraid.fireEvent("stateChange")

  setViewable: (is_viewable) ->
    viewable = is_viewable
    mraid.fireEvent("viewableChange")

  setPlacementType: (type) ->
    if type == 0
      placementType = "inline"
    else if type == 1
      placementType = "interstitial"