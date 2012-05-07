(ns pallet.core.session
  "Functions for querying sessions.

   Non-monadic functions."
  (:require
   [pallet.compute :as compute]
   [pallet.node :as node]
   [pallet.utils :as utils]))

(defn safe-id
  "Computes a configuration and filesystem safe identifier corresponding to a
  potentially unsafe ID"
  [#^String unsafe-id]
  (utils/base64-md5 unsafe-id))

;; (defn phase
;;   "Current phase"
;;   [session]
;;   (:phase session))

(defn target-node
  "Target compute service node."
  [session]
  (-> session :server :node))

(defn target-name
  "Name of the target-node."
  [session]
  (node/hostname (target-node session)))

(defn target-id
  "Id of the target-node (unique for provider)."
  [session]
  (node/id (target-node session)))

(defn target-ip
  "IP of the target-node."
  [session]
  (node/primary-ip (target-node session)))





(comment
(defn target-roles
  "Roles of the target server."
  [session]
  [(-> session :server :roles) session])

(defn base-distribution
  "Base distribution of the target-node."
  [session]
  (compute/base-distribution (-> session :server :image)))
(defn os-family*
  "OS-Family of the target-node."
  [session]
  (-> session :server :image :os-family))
)


(defn os-family
  "OS-Family of the target-node."
  [session]
  (node/os-family (target-node session)))

(comment
   (defn os-version*
     "OS-Family of the target-node."
     [session]
     (-> session :server :image :os-version))

   (defn os-version
     "OS-Family of the target-node."
     [session]
     [(os-version* session) session])
)

(defn group-name
  "Group name of the target-node."
  [session]
  (-> session :group :group-name))

(comment
   (defn safe-name
     "Safe name for target machine.
   Some providers don't allow for node names, only node ids, and there is
   no guarantee on the id format."
     [session]
     [(format
       "%s%s"
       (name (group-name session)) (safe-id (name (target-id session))))
      session])
   )


(defn nodes-in-group
  "All nodes in the same tag as the target-node, or with the specified
  group-name."
  ([session group-name]
     (let [[group nodes] (first
                          (filter
                           #(= group-name (:group-name (key %)))
                           (-> session :service-state :group->nodes)))]
       nodes))
  ([session]
     (nodes-in-group session (group-name session))))

(defn groups-with-role
  "All target groups with the specified role."
  [session role]
  (->>
   (keys (-> session :service-state :group->nodes))
   (filter #(when-let [roles (:roles %)] (when (roles role) %)))
   (map :group-name)))

(defn nodes-with-role
  "All target nodes with the specified role."
  [session role]
  (->>
   (-> session :service-state :group->nodes)
   (filter #(when-let [roles (:roles (key %))] (when (roles role) (val %))))
   (apply concat)))

 (comment
   (defn packager
     [session]
     [(node/packager (get-in session [:server :node])) session])
   )

(defn admin-user
  "User that remote commands are run under"
  [session]
  {:pre [session (:user session)]}
  (:user session))

(defn admin-group
  "User that remote commands are run under"
  [session]
  (compute/admin-group
   (node/os-family (target-node session))
   (node/os-version (target-node session))))

(comment
  (defn is-64bit?
    "Predicate for a 64 bit target"
    [session]
    [(node/is-64bit? (-> session :server :node)) session])

  (defn print-errors
    "Display errors from the session results."
    [session]
    (doseq [[target phase-results] (:results session)
            [phase results] phase-results
            result (filter
                    #(or (:error %) (and (:exit %) (not= 0 (:exit %))))
                    results)]
      (println target phase (:err result))))
  )
