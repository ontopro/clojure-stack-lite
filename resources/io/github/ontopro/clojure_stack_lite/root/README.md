# {{main/ns}}

_This application is generated with [clojure-stack-lite](https://github.com/abogoyavlensky/clojure-stack-lite)._

_TODO: add project description_


## Development

Install Java, Clojure, Babashka, TailwindCSS and other tools manually or via [mise](https://mise.jdx.dev/):

```shell
mise trust && mise install
```

Check all available commands:

```shell
bb tasks
```

Run lint, formatting, tests and checking outdated dependencies:

```shell
bb check
```

Run server with built-in REPL from terminal:

> [!NOTE]
> If you're using PostgreSQL, [Docker](https://docs.docker.com/engine/install/) should be installed

 ```shell
bb clj-repl 
(reset)
````

Once server is started, it will automatically reload on code changes in the backend and TailwindCSS classes.
The server should be available at `http://localhost:8000`.

## Licence

This project has **no licence yet**: the template does not choose one for you. Until you add a
`LICENSE` file nobody else has permission to use, copy or change this code, so choose one before
you publish the repository. [choosealicense.com](https://choosealicense.com) compares the common
ones.

## Update assets

The idea is to vendor all js-files in the project repo eliminating build step for js part.

Once you want to update the version of AlpineJS, HTMX or add a new asset, edit version in bb.edn file at `fetch-assets` and run:

```shell
bb fetch-assets
```

Your assets will be updated in `resources/public` folder.{{readme-deploy-kamal}}
