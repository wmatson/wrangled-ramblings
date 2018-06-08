{:layout :post,
 :tags [""],
 :toc false,
 :title "Automated Cryogen Gh Pages Deployments",
 :date "2018-06-08"}


While I was fiddling with getting disqus working, I found that the deployment process to github pages was rather tedious.
So I decided to automate it.

A few googles later, I decided to use [CircleCI](https://circleci.com/) to manage the builds and automatically deploy.
Several blog posts on how to do this using CircleCI 1.0 existed already, but when I went to follow one, I quickly realized
that 1.0 was deprecated and on its way out of support.

Luckily, [someone already did the work](https://blog.frederikring.com/articles/deploying-github-pages-circle-ci/) to
figure out the process. Unfortunately, their method had a couple issues that I'll explain later. Over the course of
experimentation, I found two other resources that proved important: The CircleCI documentation about checkout keys
(found `https://circleci.com/gh/<your-username>/<your-project>/edit#checkout`, at the time of this writing,
 I couldn't find a good link that wasn't project-specific), and [the github documentation](https://github.com/DevProgress/onboarding/wiki/Using-Circle-CI-with-Github-Pages-for-Continuous-Delivery)
on using CircleCI (specifically `Creating a machine user` towards the bottom third of the page).

___

While fiddling with the process, I made sure to set my deploy target to a branch other than `gh-pages` (I used `gh-pages-test` which bares no significance to github)
so as not to disturb the currently-working blog site, I'd recommend you do the same.

___

##Some Caveats
The [blog I followed along](https://blog.frederikring.com/articles/deploying-github-pages-circle-ci/) had a couple issues.

####Node instead of Clojure
[Cryogen](http://cryogenweb.org/) uses Clojure (specifically [Leiningen](https://leiningen.org/) to build the blog site.
As a result, I needed to merge the config.yml in [the blog](https://blog.frederikring.com/articles/deploying-github-pages-circle-ci/)
with [CircleCI's Clojure example](https://circleci.com/docs/2.0/language-clojure/#sample-configuration) 
(I used the one generated when initially setting up a project, which I couldn't quickly find a link to it's the same before `run: lein do ...`).

Additionally, the deploy process ran `npm run build` which won't work with a Clojure project/CircleCI image.
```
...

git checkout $TARGET_BRANCH || git checkout --orphan $TARGET_BRANCH
git rm -rf .
cd ..

npm run build

cp -a build/. out/.

mkdir -p out/.circleci && cp -a .circleci/. out/.circleci/.
cd out

...
```
I removed the `npm run build` line entirely, opting to build before the deploy process.
Instead, I opted to build using `lein run` (This runs the project's main method, Cryogen projects' main builds the blog).
This replaced `lein do test, uberjar` in the configuration.

You can find my final/current working config [here](https://github.com/wmatson/wrangled-ramblings/blob/master/.circleci/config.yml).

####Environment Misconfiguration
First, in their `config.yml` snippet, there was some configuration in the `environment` tag that I had to change for reasons beyond me.
```
    environment:
      - SOURCE_BRANCH: master
      - TARGET_BRANCH: gh-pages
```
should be
```
    environment:
      SOURCE_BRANCH: master
      TARGET_BRANCH: gh-pages
```
notice the now-missing hyphens.

___

Overall, this was an interesting experience, I had enough fun/focus that I lost track of time and went to bed hungry.
Here's my final CircleCI `config.yml` at the time of this writing:
```
version: 2
jobs:
  build:
    branches:
      ignore:
        - gh-pages
    docker:
      # specify the version you desire here
      - image: circleci/clojure:lein-2.7.1

    working_directory: ~/repo

    environment:
      LEIN_ROOT: "true"
      # Customize the JVM maximum heap limit
      JVM_OPTS: -Xmx3200m
      SOURCE_BRANCH: master
      TARGET_BRANCH: gh-pages

    steps:
      - checkout

      # Download and cache dependencies
      - restore_cache:
          keys:
          - v1-dependencies-{{ checksum "project.clj" }}
          # fallback to using the latest cache if no exact match is found
          - v1-dependencies-

      - run: lein deps

      - save_cache:
          paths:
            - ~/.m2
          key: v1-dependencies-{{ checksum "project.clj" }}

      - run: lein run

      - deploy:
          name: Deploy
          command: |
            if [ $CIRCLE_BRANCH == $SOURCE_BRANCH ]; then
              git config --global user.email $GH_EMAIL
              git config --global user.name $GH_NAME

              git clone $CIRCLE_REPOSITORY_URL out

              cd out
              git checkout $TARGET_BRANCH || git checkout --orphan $TARGET_BRANCH
              git rm -rf .
              cd ..

              cp -a resources/public/wrangled-ramblings/. out/.

              mkdir -p out/.circleci && cp -a .circleci/. out/.circleci/.
              cd out

              git add -A
              git commit -m "Automated deployment to GitHub Pages: ${CIRCLE_SHA1}" --allow-empty

              git push origin $TARGET_BRANCH
            fi
```
