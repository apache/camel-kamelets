# Camelet Catalog Website

This section contains the Kamelet catalog website, auto-generated from the source Kamelets.

## Building

To generate the adoc and svg files for the website (you need `go`):

```
# From the /docs directory
go run ./generator/ ../ ./modules/ROOT/
```

To generate the website bundle UI (you need `yarn`):

```
# From the /docs directory
yarn build-ui
```

To preview the website:

```
# From the /docs directory
yarn preview
```
