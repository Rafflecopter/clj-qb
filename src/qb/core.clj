(ns qb.core
  (:import [java.util Map]))

(defmulti init!
  "Initialize using a config object.
  Return an instance of a Listener, Sender, or both"
  :type)

(defprotocol Listener
  (listen ^Map [instance destination]
    "Start a listener.
    Destination is implementation-dependent. Could be a path, topic, etc.
    Return a map of {:data chan :stop chan}
    Data channel is channel of items: {:result result-chan :msg msg} to be sent
    Upon stop channel closing, listening should cease and channel closed."))

(defprotocol Sender
  (send! [instance destination ^Map msg]
    "Send a message to a destination.
    Destination is implementation-dependent. Could be a url, hostname, topic, etc.
    Return a result channel"))