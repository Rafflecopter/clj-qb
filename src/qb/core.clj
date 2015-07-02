(ns qb.core)

(defmulti init!
  "Initialize using a config object.
  Return an instance of a Listener, Sender, or both"
  :type)

(defprotocol Listener
  (listen [instance source]
    "Start a listener.
    Source is implementation-dependent. Could be a path, topic, etc.
    Return a map of {:data chan :stop chan}
    Data channel is channel of items: {:result result-chan :msg msg} to be sent
    See qb.util/wrap-result-chan-xf to help construct this channel
    Upon stop channel closing, listening should cease and channel closed."))

(defprotocol Sender
  (send! [instance destination msg]
    "Send a message to a destination.
    Destination is implementation-dependent. Could be a url, hostname, topic, etc.
    Return a result channel managed by sender."))