{:tags ["cryogen" "dev"],
 :toc false,
 :layout :post,
 :title "My First Cryogen Blog",
 :date "2018-05-23"}

This is my first blog in a long time. 

It will probably not be well-structured, regular-posted, or even coherent at times, but it'll be mine.

____

When I first discovered [cryogen](http://cryogenweb.org/index.html), 
I felt the need to start something creative again. I've tried to start
a few blogs here and there, but never really kept to them, largely due to
the lack of control I felt with most aspects of the site and with the arcane
"deploy" setups I saw. The ties to Clojure introduced me and the principles behind it hooked me.
Particularly, the ability to host it on github.io stood out to me.

### Some Background On Me

- Software Engineer of 5 years in industry
- Started programming at the age of 6
- Like many Clojurists, I use emacs
- Avid [GURPS](https://en.wikipedia.org/wiki/GURPS) GM (maybe a player someday)
- Head full of nonsense

<br/>

#### Deviations From Default Cryogen

I've made only minimal edits to the default available themes at this point,
mostly inverting colors and setting the code theme to [atom-one-dark](https://github.com/jonathanchu/atom-one-dark-theme),
the theme I use in emacs. You can find the source code in [this github repo](https://github.com/wmatson/wrangled-ramblings)

____

While doing my research in getting markdown to work in 
emacs (I'm using [markdown-mode](https://jblevins.org/projects/markdown-mode/) and [pandoc](https://pandoc.org/))
and get started with cyrogen,
I stumbled upon [this blog post](http://blog.bradlucas.com/posts/2017-06-09-blogging-with-cryogen/),
which inspired me to automate some of the expected tedium. His `new-post` emacs function 
used a function or two that weren't in [my setup](https://github.com/wmatson/emacs-config), 
so I decided to create one of my own using my current favorite language, Clojure.

## Cyrogen Post Creation Function (in Clojure)

Many clojure projects that I've started include a `user.clj`. 
This file/namespace is only loaded at dev-time and includes various functions that may be useful during development of a project.
The default Cryogen template includes no such file, as it's intended to be used by anyone by running a terminal command.

I decided to instead use my handy-dandy Clojure REPL for running the blog server while creating posts.
This was as simple as using `ring-jetty-adapter` much as outlined [here](https://github.com/ring-clojure/ring/wiki/Getting-Started).
I did a little more fiddling to get things running as they would from the terminal command and ended up with the following in my user.clj

##### user.clj
```
(ns user
  (:require [ring.adapter.jetty :as jetty]
            [cryogen.server :refer [handler init]]
            [cryogen-core.compiler :refer [read-config]]

(defonce server (atom nil))

(defn start-server
  ([] (start-server 3000))
  ([port]
   (when-not @server
     (init)
     (reset! server (jetty/run-jetty handler {:port port :join? false})))))

(defn stop-server []
  (when @server
    (.stop @server)
    (reset! server nil)))
```

I'm certain there are smarter ways to do this, but it works for me and allows me to easily start and stop the server on any port.

Back to the task at hand.

I decided to respect the `config.edn` provided by cryogen, for the most part.
This should make things a little more usable for the next person to follow, but it also makes the task more interesting.

Reading through the default installation's [source code](https://github.com/wmatson/wrangled-ramblings/blob/7942ef3fbbe767f624647531c0f99dc95624bea9/src/cryogen/server.clj),
I noticed the function `read-config` and ran it through my REPL to find the config I was expecting.

For convenience, I added a couple of libraries to my project.clj:
```
 :dependencies [[org.clojure/clojure "1.8.0"]
                [ring/ring-devel "1.6.3"]
                [compojure "1.6.0"]
                [ring-server "0.5.0"]
                [cryogen-markdown "0.1.7"]
                [cryogen-core "0.1.61"]
                ;; user function dependencies
                [ring/ring-jetty-adapter "1.6.3"]
                [clj-time "0.14.4"]]
```                           
I know these should be in a `:dev` profile, as should the loading of user.clj (rather than putting it in the `src/` folder), but I was lazy at the time.

So, for some forewarning to any Clojurists reading this, 
my personal style is full of newlines, often in places that I don't see commonly in other people's code.
This of course, increases my line count significantly, but I find it makes things more readable and maintainable, personally.
It also gives me the ability to use some fun tricks with [parinfer](https://shaunlebron.github.io/parinfer/) that I may explain in some other post.

Anyway, this is getting long, so here's the code. Maybe I'll do a more thorough breakdown of it another time:
```
(ns user
  (:require [ring.adapter.jetty :as jetty]
            [cryogen.server :refer [handler init]]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [cryogen-core.compiler :refer [read-config]]
            [clj-time.format :as tformat]))

;;Omitting the server stuff for brevity/lack of duplication

(def base-opts {:layout :post
                :tags [""]
                :toc false})

(def base-text
  "###Header
Some Text")

(def base-dir "resources/templates/md/")

(defn- today-str [config]
  (-> config
      :post-date-format
      tformat/formatter
      (tformat/unparse-local-date (time/today))))

(defn new-post-template [config title]
  (let [opts (assoc base-opts
                    :title title
                    :date (today-str config))
        pp-opts (with-out-str (pprint opts))]
    (str pp-opts "\n\n" base-text)))

(defn title->filename [title]
  (-> title
      str/lower-case
      (str/replace #" +" "-")
      (str ".md")))

(defn new-post! [title]
  (let [config (read-config)
        template (new-post-template config title)
        out-location (str base-dir (:post-root config) "/" (title->filename title))]
    (spit (io/file out-location) template)))

;; (new-post! "My First Cryogen Blog")
```


This has plenty of room for improvement and I encourage anyone to contribute. By far, the biggest drawback I have
using this method as opposed to the one posted in [the inspiration for it](http://blog.bradlucas.com/posts/2017-06-09-blogging-with-cryogen/) is that I still have to open a new buffer to begin editing.
