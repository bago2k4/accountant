# Accountant

Accountant is a ClojureScript library to make navigation in single-page
applications simple. It expects you to use [Secretary 2](https://github.com/gf3/secretary/tree/v2.0.0) to define your routes.

By default, clicking a link in a ClojureScript application that isn't a simple
URL fragment will trigger a full page reload. This defeats the purpose of using
such elegant frameworks as Om, Reagent, et al.

With Accountant, links that correspond to defined Secretary routes will trigger
dispatches to those routes and update the browser's path, but won't reload the
page.

Accountant also lets you navigate the app to a new URL directly, rather than through
`<a>` tags.

Be aware that Accountant relies on the browser's HTML5 history, so older
browsers will be left behind.

## Installation

Just add the following to your `project.clj`:

```clojure
:dependencies [venantius/accountant "0.2-SNAPSHOT"]
```

## Usage

When you configure secretary 2 you create a url dispatcher like this:

```clojure
(ns your.ns
  (:require [secretary.core :as secretary :refer-macros [defroute]]))

(defroute secretary-defroute-home "/" {:as params}
  ...)

(defroute secretary-defroute-user "/user" {:as params}
  ...)

(def secretary-dispatcher
  (secretary/uri-dispatcher [secretary-defroute-home
                             secretary-defroute-user]))
```

this is used by accountant to let secretary handle the new window location.

Also, to let accountant check for the handled routes, you need a list of all the paths:

```clojure
(def routes-stack [
  "/"       ; route used in secretary-defroute-home
  "/user"   ; route used in secretary-defroute-user
])
```

At this point all you have to do to get Accountant working is the following:

```clojure
(ns your-app-ns
  (:require [accountant.core :as accountant]))

(accountant/configure-navigation! secretary-dispatcher routes-stack)
```

...and you're good to go!

You can also use Accountant to set the current path in the browser, e.g.

```clojure
(accountant/navigate! "/foo/bar/baz")
```

If you want to dispatch the current path, just add the following:

```clojure
(dispatch-current! secretary-dispatcher)
```

## License

Copyright © 2015 W. David Jarvis

Distributed under the Eclipse Public License, the same as Clojure.
